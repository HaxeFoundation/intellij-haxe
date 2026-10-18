package com.intellij.plugins.haxe.runner.nodejs;

import com.intellij.plugins.haxe.runner.debugger.dap.client.DapClient;
import com.intellij.plugins.haxe.runner.debugger.dap.client.DapEndpoint;
import com.intellij.plugins.haxe.runner.debugger.dap.ide.AdapterTargetsSmartStepHandler;
import com.intellij.plugins.haxe.runner.debugger.dap.ide.DapBackend;
import com.intellij.plugins.haxe.runner.debugger.dap.ide.DapDebugProcess;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.Request;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.requests.ConfiguredLaunchRequest;
import com.intellij.plugins.haxe.HaxeDebuggerBundle;
import com.intellij.xdebugger.stepping.XSmartStepIntoHandler;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import com.intellij.plugins.haxe.runner.debugger.javascript.AdapterPin;
import com.intellij.plugins.haxe.runner.debugger.javascript.AdapterStore;
import com.intellij.plugins.haxe.runner.debugger.javascript.AdapterStores;
import com.intellij.plugins.haxe.runner.debugger.javascript.JsDebugAdapters;
import com.intellij.plugins.haxe.runner.debugger.javascript.JsDebugSessionMux;
import com.intellij.plugins.haxe.runner.debugger.javascript.JsDebugSessions;
import com.intellij.plugins.haxe.runner.debugger.javascript.NodeEntryHoldRelease;
import com.intellij.plugins.haxe.runner.debugger.javascript.NodeLocator;

/**
 * The node debug backend, for programs and tests alike: the RUNNER spawns the
 * debuggee itself - {@code node --inspect-brk=<port> <script.js>} - so its
 * output stays on the real process stdout (a test run's protocol drives the
 * SM console exactly like a plain run); this backend spawns the pinned
 * vscode-js-debug adapter (the chromium lane's, shared store and pin) and
 * ATTACHES it to the inspector port. The parent/child session dance and the
 * wire quirks live in {@link JsDebugSessions} (fire-and-forget launch,
 * startDebugging hand-over, deferred launch response); the mux presents the
 * process plus any worker_threads as one session. {@link NodeEntryHoldRelease} releases
 * the {@code --inspect-brk} hold once the breakpoints are bound, so even code
 * that runs at startup stops on them.
 */
public class NodeDebugBackend implements DapBackend {

  private final Path nodeExecutable; // runs the adapter AND the debuggee
  private final int inspectorPort;
  private final String workDirectory;

  private volatile Process adapterProcess;
  private volatile BufferedReader adapterStdout;
  private volatile Map<String, Object> launchConfig;
  private volatile DapClient parentClient;
  private volatile JsDebugSessionMux sessionMux;

  public NodeDebugBackend(Path nodeExecutable, int inspectorPort, String workDirectory) {
    this.nodeExecutable = nodeExecutable;
    this.inspectorPort = inspectorPort;
    this.workDirectory = workDirectory;
  }

  public int getInspectorPort() {
    return inspectorPort;
  }

  /** The node option that holds the script at its first line on this backend's inspector port. */
  public String inspectorArgument() {
    return "--inspect-brk=" + inspectorPort;
  }

  @Override
  public DapEndpoint connect() throws IOException {
    NodeLocator.requireModern(nodeExecutable);
    AdapterStore store = AdapterStores.open();
    Path dapServerJs = JsDebugAdapters.installedEntry(store, AdapterPin.JS_DEBUG);
    JsDebugSessions.Session session = JsDebugSessions.open(nodeExecutable, dapServerJs, "node", attachConfig());
    adapterProcess = session.adapter().process();
    adapterStdout = session.adapter().stdout();
    parentClient = session.parent();
    launchConfig = session.childConfiguration();
    sessionMux = session.mux();
    return new NodeEntryHoldRelease(session.mux());
  }

  private Map<String, Object> attachConfig() {
    Map<String, Object> config = new LinkedHashMap<>();
    config.put("type", "pwa-node");
    config.put("request", "attach");
    config.put("name", "IntelliJ Haxe node");
    config.put("port", inspectorPort);
    // the hold is released by NodeEntryHoldRelease: js-debug's own release
    // comes before the breakpoints of a source-mapped file are bound
    config.put("continueOnAttach", false);
    // js-debug's default restricts source-map resolution to the workspace
    // folder; an output outside it (a gutter single run's artifact lives under
    // the temp root) needs the restriction lifted for its map (and the .hx
    // originals) to resolve. The match-all glob rather than null: the DAP encoder drops
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
    JsDebugSessions.startBackgroundOutput(reader, sessionMux, line -> process.printSystem(line + "\n"));
  }

  @Override
  public boolean requiresLaunchRequest() {
    return true;
  }

  // the program's output arrives on node's own stdout and stderr; js-debug
  // reports every console call again as an output event
  @Override
  public boolean echoesProgramOutput() {
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
    return HaxeDebuggerBundle.message("node.runner.startup.hint");
  }

  @Override
  public void close() {
    JsDebugSessionMux mux = sessionMux;
    sessionMux = null;
    DapClient parent = parentClient;
    parentClient = null;
    Process adapter = adapterProcess;
    adapterProcess = null;
    JsDebugSessions.closeSession(mux, parent, adapter);
  }
}
