package com.intellij.plugins.haxe.runner.debugger.browser;

import com.intellij.plugins.haxe.runner.debugger.dap.client.DapClient;
import com.intellij.plugins.haxe.runner.debugger.dap.client.DapEndpoint;
import com.intellij.plugins.haxe.runner.debugger.dap.ide.AdapterTargetsSmartStepHandler;
import com.intellij.plugins.haxe.runner.debugger.dap.ide.DapBackend;
import com.intellij.plugins.haxe.runner.debugger.dap.ide.DapDebugProcess;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.Request;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.requests.ConfiguredLaunchRequest;
import com.intellij.plugins.haxe.HaxeDebuggerBundle;
import com.intellij.plugins.haxe.runner.debugger.browser.BrowserRunConfiguration.BrowserFamily;
import com.intellij.xdebugger.stepping.XSmartStepIntoHandler;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The node TEST debug backend: the RUNNER spawns the debuggee itself —
 * {@code node --inspect-brk=<port> <tests.js>} — so the test protocol stays
 * on the real process stdout and drives the SM console exactly like a plain
 * run; this backend spawns the pinned vscode-js-debug adapter (the chromium
 * lane's, shared store and pin) and ATTACHES it to the inspector port. The
 * parent/child session dance and the wire quirks are
 * {@link BrowserDebugBackend}'s (fire-and-forget launch, startDebugging
 * hand-over, deferred launch response); the mux presents the process plus
 * any worker_threads as one session. {@code continueOnAttach} releases the
 * {@code --inspect-brk} hold once configuration is done — breakpoints are
 * installed before the first test line runs.
 */
public class NodeTestDebugBackend implements DapBackend {

  private final Path nodeExecutable; // runs the adapter AND the debuggee
  private final int inspectorPort;
  private final String workDirectory;

  private volatile Process adapterProcess;
  private volatile BufferedReader adapterStdout;
  private volatile Map<String, Object> launchConfig;
  private volatile DapClient parentClient;
  private volatile JsDebugSessionMux sessionMux;

  public NodeTestDebugBackend(Path nodeExecutable, int inspectorPort, String workDirectory) {
    this.nodeExecutable = nodeExecutable;
    this.inspectorPort = inspectorPort;
    this.workDirectory = workDirectory;
  }

  public int getInspectorPort() {
    return inspectorPort;
  }

  @Override
  public DapEndpoint connect() throws IOException {
    NodeLocator.requireModern(nodeExecutable);
    AdapterStore store = new AdapterStore(BrowserDebugBackend.adapterStoreRoot());
    // the same bundle display name the browser lane reports, so one missing
    // artifact reads identically from both entry points
    String adapterName = BrowserRunConfiguration.adapterDisplayName(BrowserFamily.CHROMIUM);
    Path dapServerJs = BrowserDebugBackend.installedAdapterEntry(store, AdapterPin.JS_DEBUG, adapterName);
    BrowserAdapterLauncher.LaunchedAdapter launched = BrowserAdapterLauncher.launchJsDebug(nodeExecutable, dapServerJs);
    adapterProcess = launched.process();
    adapterStdout = launched.stdout();

    DapClient parent = BrowserDebugBackend.connectWithRetry(launched.port());
    parentClient = parent;
    Map<String, Object> childConfig;
    try {
      childConfig = BrowserDebugBackend.runParentHandshake(parent, "node", attachConfig());
    } catch (IOException e) {
      throw new IOException("The js-debug parent session failed: " + e.getMessage(), e);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IOException("Interrupted while starting the js-debug session", e);
    }
    launchConfig = childConfig;
    DapClient process = BrowserDebugBackend.connectWithRetry(launched.port());
    JsDebugSessionMux mux = new JsDebugSessionMux(parent, process, launched.port());
    sessionMux = mux;
    return mux;
  }

  private Map<String, Object> attachConfig() {
    Map<String, Object> config = new LinkedHashMap<>();
    config.put("type", "pwa-node");
    config.put("request", "attach");
    config.put("name", "IntelliJ Haxe node tests");
    config.put("port", inspectorPort);
    // the debuggee holds at --inspect-brk; resume once configuration is done
    config.put("continueOnAttach", true);
    // js-debug's default restricts source-map resolution to the workspace
    // folder; a gutter single run's artifact lives under the temp root, so
    // the restriction must be lifted for its map (and the .hx originals) to
    // resolve. The match-all glob rather than null: the DAP encoder drops
    // null values (NON_NULL), and an absent field keeps the default.
    config.put("resolveSourceMapLocations", List.of("**"));
    if (workDirectory != null) {
      config.put("cwd", workDirectory);
    }
    return config;
  }

  @Override
  public void onConnected(DapDebugProcess process) {
    BufferedReader reader = adapterStdout;
    adapterStdout = null;
    BrowserDebugBackend.startBackgroundOutput(reader, sessionMux, line -> process.printSystem(line + "\n"));
  }

  @Override
  public boolean requiresLaunchRequest() {
    return true;
  }

  @Override
  public Request launchRequest() {
    // the CHILD configuration handed over by startDebugging (__pendingTargetId)
    return ConfiguredLaunchRequest.of(launchConfig);
  }

  @Override
  public boolean initializedEventAfterLaunch() {
    return true;
  }

  @Override
  public boolean awaitsLaunchResponse() {
    // js-debug answers launch only AFTER configurationDone (see BrowserDebugBackend)
    return false;
  }

  @Override
  public boolean supportsExceptionFilters() {
    return true;
  }

  @Override
  public String anyThrowFilterId() {
    return "all";
  }

  @Override
  public String criticalFilterId() {
    return "uncaught";
  }

  @Override
  public String breakpointSourcePath(String vfsPath) {
    return vfsPath.replace('/', File.separatorChar);
  }

  @Override
  public boolean supportsSmartStepInto() {
    return true;
  }

  @Override
  public XSmartStepIntoHandler<?> createSmartStepIntoHandler(DapDebugProcess process) {
    return new AdapterTargetsSmartStepHandler(process);
  }

  @Override
  public boolean threadsPauseIndependently() {
    return true;
  }

  @Override
  public boolean evaluatesAgainstForeignRuntime() {
    return true;
  }

  @Override
  public boolean supportsToStringRendering() {
    return false;
  }

  @Override
  public String startupHint() {
    return HaxeDebuggerBundle.message("node.test.runner.startup.hint");
  }

  @Override
  public void close() {
    JsDebugSessionMux mux = sessionMux;
    sessionMux = null;
    DapClient parent = parentClient;
    parentClient = null;
    Process adapter = adapterProcess;
    adapterProcess = null;
    BrowserDebugBackend.closeSession(mux, parent, adapter);
  }
}
