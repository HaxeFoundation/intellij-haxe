package com.intellij.plugins.haxe.runner.debugger.browser;

import com.intellij.plugins.haxe.HaxeDebuggerBundle;
import com.intellij.plugins.haxe.runner.debugger.browser.BrowserRunConfiguration.BrowserFamily;
import com.intellij.plugins.haxe.runner.debugger.dap.client.DapClient;
import com.intellij.plugins.haxe.runner.debugger.dap.ide.DapBackend;
import com.intellij.plugins.haxe.runner.debugger.dap.ide.DapDebugProcess;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.Request;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.requests.*;
import com.intellij.plugins.haxe.runner.debugger.dap.transport.AdapterProcess;
import com.intellij.util.net.NetUtils;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;
import com.intellij.plugins.haxe.runner.debugger.dap.client.DapEndpoint;
import com.intellij.plugins.haxe.runner.debugger.dap.ide.AdapterTargetsSmartStepHandler;
import com.intellij.xdebugger.stepping.XSmartStepIntoHandler;
import com.intellij.plugins.haxe.runner.debugger.javascript.AdapterPin;
import com.intellij.plugins.haxe.runner.debugger.javascript.AdapterStore;
import com.intellij.plugins.haxe.runner.debugger.javascript.AdapterStores;
import com.intellij.plugins.haxe.runner.debugger.javascript.JsAdapterLauncher;
import com.intellij.plugins.haxe.runner.debugger.javascript.JsDebugAdapters;
import com.intellij.plugins.haxe.runner.debugger.javascript.JsDebugSessionMux;
import com.intellij.plugins.haxe.runner.debugger.javascript.JsDebugSessions;
import com.intellij.plugins.haxe.runner.debugger.javascript.NodeLocator;

/**
 * The browser backend: resolves the family's pinned vscode debug adapter
 * through the {@link AdapterStore} (downloaded beforehand from the run
 * configuration, SHA-256-verified), spawns it on the user's node in DAP TCP
 * server mode, optionally serves the content directory over the plugin's own
 * loopback {@link ContentHttpServer}, and connects the shared DAP client. The
 * BROWSER is launched by the adapter (the launch config carries the url and
 * optional executable), so the runner spawns no debuggee.
 *
 * FIREFOX (vscode-firefox-debug, single session — wire behaviour pinned by
 * FirefoxAdapterLiveTest): initialize needs pathFormat=path, the initialized
 * event arrives only after launch, no configurationDone, lazy breakpoint
 * verification, literal native-path matching, and the serve-mode first-page
 * refresh that makes load-time breakpoints reachable.
 *
 * CHROMIUM (vscode-js-debug's dapDebugServer, parent+child sessions — wire
 * behaviour pinned by JsDebugAdapterLiveTest): {@link #connect()} runs the
 * PARENT session itself (initialize, fire-and-forget launch, configurationDone
 * on initialized, then the {@code startDebugging} reverse request hands over
 * the child configuration) and returns the CHILD connection — so the generic
 * {@link DapDebugProcess} drives a plain single session and never sees the
 * multi-session dance. The child's launch response is deferred past
 * configurationDone, hence {@link #awaitsLaunchResponse()} = false.
 *
 * All heavy work (adapter lookup, server, spawns) happens in {@link #connect()} on
 * the debug process's request thread — never the EDT.
 */
public class BrowserDebugBackend implements DapBackend {
  /** Serve mode (firefox only): delay of the one-shot first-page refresh. */
  private static final int FIRST_PAGE_REFRESH_SECONDS = 2;

  private final BrowserFamily family;
  private final String configuredNodePath;
  private final String browserExecutable;
  private final boolean serveContent;
  // The local web root: the served directory in serve mode, or (URL mode)
  // the local files the external server hosts. Sent to the adapter as webRoot
  // so source-mapped breakpoints bind to local sources; null when unset.
  private final Path contentRoot;
  private final String url;       // when not serving

  private volatile ContentHttpServer contentServer;
  private volatile Process adapterProcess;
  private volatile BufferedReader adapterStdout;
  private volatile Map<String, Object> launchConfig;
  /** Chromium only: the parent session, kept alive beside the child. */
  private volatile DapClient parentClient;
  /** Chromium only: the page+workers multiplexer (owns parent + all children). */
  private volatile JsDebugSessionMux sessionMux;

  public BrowserDebugBackend(BrowserFamily family,
                             String configuredNodePath,
                             String browserExecutable,
                             boolean serveContent,
                             Path contentRoot,
                             String url) {
    this.family = family;
    this.configuredNodePath = configuredNodePath;
    this.browserExecutable = browserExecutable;
    this.serveContent = serveContent;
    this.contentRoot = contentRoot;
    this.url = url;
  }

  @Override
  public DapEndpoint connect() throws IOException {
    Path node = NodeLocator.locate(configuredNodePath);
    NodeLocator.requireModern(node);
    AdapterStore store = AdapterStores.open();

    String targetUrl = url;
    if (serveContent) {
      contentServer = new ContentHttpServer(contentRoot);
      targetUrl = contentServer.getBaseUrl();
      if (family == BrowserFamily.FIREFOX) {
        // firefox cannot pause a tab's very FIRST load (the JS thread actor
        // is only born when scripts first execute): the first load binds the
        // map + breakpoints and this one-shot refresh re-runs it armed.
        // js-debug pre-registers breakpoints through CDP and needs none.
        // Known cost (pinned by FirefoxAdapterLiveTest.workerThreadsAcrossRefresh): a WORKER breakpoint hitting
        // before the refresh leaves that paused worker behind as an inert
        // zombie thread - firefox worker debugging has deeper quirks anyway
        // and Chromium is the recommended family for worker-heavy sessions.
        contentServer.refreshFirstPage(FIRST_PAGE_REFRESH_SECONDS);
      }
    }

    return switch (family) {
      case FIREFOX -> connectFirefox(node, store, targetUrl);
      case CHROMIUM -> connectChromium(node, store, targetUrl);
    };
  }

  // ------------------------------------------------------------- firefox

  private DapClient connectFirefox(Path node, AdapterStore store, String targetUrl) throws IOException {
    Path adapterEntry = JsDebugAdapters.installedEntry(store, AdapterPin.FIREFOX);
    launchConfig = firefoxLaunchConfig(targetUrl);
    AdapterProcess launched = JsAdapterLauncher.launch(node, adapterEntry);
    adapterProcess = launched.process();
    adapterStdout = launched.stdout();
    return JsDebugSessions.connectWithRetry(launched.port());
  }

  private Map<String, Object> firefoxLaunchConfig(String targetUrl) throws IOException {
    Map<String, Object> config = new LinkedHashMap<>();
    config.put("request", "launch");
    config.put("url", targetUrl);
    // UNIQUE RDP port per session. The adapter's default is a FIXED 6000:
    // firefox launched for an earlier session survives teardown on Windows
    // (the launcher process re-parents the real firefox out of the adapter's
    // process tree, escaping the tree-kill), keeps owning 6000, and the next
    // session's adapter then debugs the STALE instance - foreign workers it
    // refuses to attach, dead actors that answer nothing, surfacing as ghost
    // threads and endless evaluate timeouts. OS-assigned rather than random:
    // a random pick can land in a Windows excluded port range (Hyper-V/
    // WinNAT reservations), where the bind fails and the launch dies with an
    // empty adapter message.
    config.put("port", NetUtils.findAvailableSocketPort());
    // webRoot maps the served files back to local sources for breakpoints; sent
    // whenever a local root is known, serving or pointing at an external server
    if (contentRoot != null) {
      config.put("webRoot", contentRoot.toString());
    }
    if (!browserExecutable.isBlank()) {
      config.put("firefoxExecutable", browserExecutable);
    }
    return config;
  }

  // ------------------------------------------------------------ chromium

  private DapEndpoint connectChromium(
    Path node, AdapterStore store, String targetUrl) throws IOException {
    Path dapServerJs = JsDebugAdapters.installedEntry(store, AdapterPin.JS_DEBUG);
    JsDebugSessions.Session session = JsDebugSessions.open(node, dapServerJs, "chrome", parentLaunchConfig(targetUrl));
    adapterProcess = session.adapter().process();
    adapterStdout = session.adapter().stdout();
    parentClient = session.parent();
    launchConfig = session.childConfiguration();
    // the mux takes over the parent (further startDebugging = new targets)
    // and presents page+workers as ONE session with workers as extra threads
    sessionMux = session.mux();
    return session.mux();
  }

  /** The parent session's launch configuration (mirrors {@link #firefoxLaunchConfig}). */
  private Map<String, Object> parentLaunchConfig(String targetUrl) {
    Map<String, Object> config = new LinkedHashMap<>();
    config.put("type", "pwa-chrome");
    config.put("request", "launch");
    config.put("name", "IntelliJ Haxe browser session");
    config.put("url", targetUrl);
    // js-debug REQUIRES webRoot (or pathMappings) for a url launch, and it maps
    // the served files back to local sources for breakpoints - send it whenever
    // a local root is known
    if (contentRoot != null) {
      config.put("webRoot", contentRoot.toString());
    }
    if (!browserExecutable.isBlank()) {
      config.put("runtimeExecutable", browserExecutable);
    }
    return config;
  }

  // --- session behaviour (wire facts from the live probes) ---

  @Override
  public void onConnected(DapDebugProcess process) {
    startBackgroundOutput(line -> process.printSystem(line + "\n"));
  }

  /**
   * Drains the adapter's buffered stdout (the launch reader must be consumed
   * or the adapter can block on a full pipe) and routes the mux's attach/exit
   * notes into {@code sink} — tagged {@code [adapter]}/{@code [js-debug]} lines.
   * Called once after {@link #connect()}, by the debug process or the test
   * run host.
   */
  public void startBackgroundOutput(Consumer<String> sink) {
    BufferedReader reader = adapterStdout;
    adapterStdout = null;
    JsDebugSessions.startBackgroundOutput(reader, sessionMux, sink);
  }

  @Override
  public boolean requiresLaunchRequest() {
    return true;
  }

  // a page's console has no process stdout: program output arrives as DAP
  // output events and is replayed through the session's process handler
  @Override
  public boolean programOutputViaAdapter() {
    return true;
  }

  @Override
  public Request launchRequest() {
    // firefox: the built config; chromium: the CHILD configuration handed
    // over by the parent's startDebugging (carries __pendingTargetId)
    return ConfiguredLaunchRequest.of(launchConfig);
  }

  @Override
  public boolean initializedEventAfterLaunch() {
    return true; // both adapters signal readiness after the launch request
  }

  @Override
  public boolean sendsConfigurationDone() {
    return family == BrowserFamily.CHROMIUM; // firefox reports it unsupported
  }

  @Override
  public boolean awaitsLaunchResponse() {
    // js-debug answers launch only AFTER configurationDone;
    // awaiting it would deadlock the setup sequence
    return family != BrowserFamily.CHROMIUM;
  }

  @Override
  public boolean supportsExceptionFilters() {
    return true;
  }

  // Both adapters speak the "all"/"uncaught" filter vocabulary. There is no
  // separate critical category in a JS runtime; critical maps to uncaught.
  @Override
  public String anyThrowFilterId() {
    return "all";
  }

  @Override
  public String criticalFilterId() {
    return "uncaught";
  }

  // The firefox adapter matches breakpoint paths LITERALLY against native
  // paths; the IDE's forward-slash VFS paths silently never bind. js-debug
  // is tolerant, but native is correct for both.
  @Override
  public String breakpointSourcePath(String vfsPath) {
    return vfsPath.replace('/', File.separatorChar);
  }

  // js-debug answers DAP stepInTargets (labels come mapped through the source
  // map: "f2(...)"), so the shared targets-based chooser works exactly like
  // HashLink's. The firefox adapter has no stepInTargets.
  @Override
  public boolean supportsSmartStepInto() {
    return family == BrowserFamily.CHROMIUM;
  }

  @Override
  public XSmartStepIntoHandler<?> createSmartStepIntoHandler(DapDebugProcess process) {
    return supportsSmartStepInto()
           ? new AdapterTargetsSmartStepHandler(process)
           : null;
  }

  // Both families pause per-thread (firefox thread actors, js-debug child
  // sessions): Resume releases every listed thread; see the hook's javadoc
  // for why the world is NOT frozen while one thread is paused.
  @Override
  public boolean threadsPauseIndependently() {
    return true;
  }

  // Short budget: firefox can wedge an actor queue on a request it never
  // answers (worker-devtools previewer crashes); recover the request thread
  // quickly instead of freezing every view for the full default.
  @Override
  public long requestTimeoutMillis() {
    return 8_000;
  }

  // The JS runtime knows identifiers the externs may not map; evaluation is
  // runtime-truth regardless of family, so the evaluate views must not cry
  // "unresolved". (Runtime COMPLETION additionally needs the completions
  // capability, which only js-debug has.)
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
    return HaxeDebuggerBundle.message("browser.runner.startup.hint");
  }

  @Override
  public void close() {
    ContentHttpServer server = contentServer;
    contentServer = null;
    if (server != null) {
      server.close();
    }
    JsDebugSessionMux mux = sessionMux;
    sessionMux = null;
    DapClient parent = parentClient;
    parentClient = null;
    Process adapter = adapterProcess;
    adapterProcess = null;
    JsDebugSessions.closeSession(mux, parent, adapter);
  }

}
