package com.intellij.plugins.haxe.runner.debugger.browser;

import com.intellij.openapi.application.PathManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.plugins.haxe.HaxeDebuggerBundle;
import com.intellij.plugins.haxe.runner.debugger.browser.BrowserRunConfiguration.BrowserFamily;
import com.intellij.plugins.haxe.runner.debugger.dap.client.DapClient;
import com.intellij.plugins.haxe.runner.debugger.dap.ide.DapBackend;
import com.intellij.plugins.haxe.runner.debugger.dap.ide.DapDebugProcess;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.Event;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.Request;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.events.InitializedEvent;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.requests.*;
import com.intellij.util.net.NetUtils;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import com.intellij.plugins.haxe.runner.debugger.dap.client.DapEndpoint;
import com.intellij.plugins.haxe.runner.debugger.dap.ide.AdapterTargetsSmartStepHandler;
import com.intellij.xdebugger.stepping.XSmartStepIntoHandler;

/**
 * The browser backend: resolves the family's pinned vscode debug adapter
 * through the {@link AdapterStore} (download on first use, SHA-256-verified),
 * spawns it on the user's node in DAP TCP server mode, optionally serves the
 * content directory over the plugin's own loopback {@link ContentHttpServer},
 * and connects the shared DAP client. The BROWSER is launched by the adapter
 * (the launch config carries the url and optional executable), so the runner
 * spawns no debuggee.
 *
 * FIREFOX (vscode-firefox-debug, single session — wire behaviour pinned by
 * FirefoxAdapterLiveProbe): initialize needs pathFormat=path, the initialized
 * event arrives only after launch, no configurationDone, lazy breakpoint
 * verification, literal native-path matching, and the serve-mode first-page
 * refresh that makes load-time breakpoints reachable.
 *
 * CHROMIUM (vscode-js-debug's dapDebugServer, parent+child sessions — wire
 * behaviour pinned by JsDebugAdapterLiveProbe): {@link #connect()} runs the
 * PARENT session itself (initialize, fire-and-forget launch, configurationDone
 * on initialized, then the {@code startDebugging} reverse request hands over
 * the child configuration) and returns the CHILD connection — so the generic
 * {@link DapDebugProcess} drives a plain single session and never sees the
 * multi-session dance. The child's launch response is deferred past
 * configurationDone, hence {@link #awaitsLaunchResponse()} = false.
 *
 * All heavy work (download, server, spawns) happens in {@link #connect()} on
 * the debug process's request thread — never the EDT.
 */
public class BrowserDebugBackend implements DapBackend {
  private static final Logger LOG = Logger.getInstance(BrowserDebugBackend.class);
  private static final int CONNECT_TIMEOUT_MILLIS = 15_000;
  private static final long CONNECT_RETRY_WINDOW_MILLIS = 10_000;
  private static final long PARENT_HANDSHAKE_TIMEOUT_MILLIS = 60_000;
  private static final long ADAPTER_KILL_WAIT_SECONDS = 2;
  /** Serve mode (firefox only): delay of the one-shot first-page refresh. */
  private static final int FIRST_PAGE_REFRESH_SECONDS = 2;

  private final BrowserFamily family;
  private final String configuredNodePath;
  private final String configuredBrowserExecutable;
  private final boolean serveContent;
  private final Path contentRoot; // when serving
  private final String url;       // when not serving

  private volatile ContentHttpServer contentServer;
  private volatile Process adapterProcess;
  private volatile BufferedReader adapterStdout;
  private volatile Map<String, Object> launchConfig;
  /** Chromium only: the parent session, kept alive beside the child. */
  private volatile DapClient parentClient;
  /** Chromium only: the page+workers multiplexer (owns parent + all children). */
  private volatile JsDebugSessionMux sessionMux;
  private volatile boolean closed;

  public BrowserDebugBackend(BrowserFamily family,
                             String configuredNodePath,
                             String configuredBrowserExecutable,
                             boolean serveContent,
                             Path contentRoot,
                             String url) {
    this.family = family;
    this.configuredNodePath = configuredNodePath;
    this.configuredBrowserExecutable = configuredBrowserExecutable;
    this.serveContent = serveContent;
    this.contentRoot = contentRoot;
    this.url = url;
  }

  @Override
  public DapEndpoint connect() throws IOException {
    Path node = NodeLocator.locate(configuredNodePath);
    NodeLocator.requireModern(node);
    AdapterStore store = new AdapterStore(adapterStoreRoot());

    String targetUrl = url;
    if (serveContent) {
      contentServer = new ContentHttpServer(contentRoot);
      targetUrl = contentServer.getBaseUrl();
      if (family == BrowserFamily.FIREFOX) {
        // firefox cannot pause a tab's very FIRST load (the JS thread actor
        // is only born when scripts first execute): the first load binds the
        // map + breakpoints and this one-shot refresh re-runs it armed.
        // js-debug pre-registers breakpoints through CDP and needs none.
        // Known cost (see the module README): a WORKER breakpoint hitting
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

  /**
   * The adapter's entry point, WITHOUT downloading: acquisition is the user's
   * explicit decision (the run configuration's Download link), and a missing
   * adapter is already a validation error — this guard only covers a session
   * forced past the configuration warning.
   */
  private Path installedAdapterEntry(AdapterStore store, AdapterPin pin) throws IOException {
    if (!store.isInstalled(pin)) {
      throw new IOException(HaxeDebuggerBundle.message(
        "browser.runner.adapter.missing",
        BrowserRunConfiguration.adapterDisplayName(family) + " " + pin.version()));
    }
    return store.resolveEntry(pin, null); // already installed: no network
  }

  private DapClient connectFirefox(Path node, AdapterStore store, String targetUrl) throws IOException {
    Path adapterEntry = installedAdapterEntry(store, AdapterPin.FIREFOX);
    launchConfig = firefoxLaunchConfig(targetUrl);
    BrowserAdapterLauncher.LaunchedAdapter launched = BrowserAdapterLauncher.launch(node, adapterEntry);
    adapterProcess = launched.process();
    adapterStdout = launched.stdout();
    return connectWithRetry(launched.port());
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
    if (serveContent) {
      config.put("webRoot", contentRoot.toString());
    }
    if (!configuredBrowserExecutable.isBlank()) {
      config.put("firefoxExecutable", configuredBrowserExecutable);
    }
    return config;
  }

  // ------------------------------------------------------------ chromium

  private DapEndpoint connectChromium(
    Path node, AdapterStore store, String targetUrl) throws IOException {
    Path dapServerJs = installedAdapterEntry(store, AdapterPin.JS_DEBUG);
    BrowserAdapterLauncher.LaunchedAdapter launched = BrowserAdapterLauncher.launchJsDebug(node, dapServerJs);
    adapterProcess = launched.process();
    adapterStdout = launched.stdout();

    DapClient parent = connectWithRetry(launched.port());
    parentClient = parent;
    Map<String, Object> childConfig;
    try {
      childConfig = runParentHandshake(parent, targetUrl);
    } catch (IOException e) {
      throw new IOException("The js-debug parent session failed: " + e.getMessage(), e);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IOException("Interrupted while starting the js-debug session", e);
    }
    launchConfig = childConfig;
    DapClient page = connectWithRetry(launched.port()); // the PAGE session's connection
    // the mux takes over the parent (further startDebugging = new targets)
    // and presents page+workers as ONE session with workers as extra threads
    JsDebugSessionMux mux = new JsDebugSessionMux(parent, page, launched.port());
    sessionMux = mux;
    return mux;
  }

  /**
   * Drives the parent session to the child hand-over: initialize,
   * fire-and-forget launch (the response is deferred past configurationDone),
   * configurationDone on the initialized event, then the startDebugging
   * reverse request carries the child configuration (__pendingTargetId).
   */
  private Map<String, Object> runParentHandshake(DapClient parent, String targetUrl)
    throws IOException, InterruptedException {
    InitializeRequest initialize = InitializeRequest.standard("chrome", true);
    initialize.getArguments().setClientName("IntelliJ Haxe");
    if (!parent.sendRequest(initialize, CONNECT_TIMEOUT_MILLIS).isSuccess()) {
      throw new IOException("initialize was rejected");
    }
    parent.sendRequestNoWait(ConfiguredLaunchRequest.of(parentLaunchConfig(targetUrl)));

    long deadline = System.currentTimeMillis() + PARENT_HANDSHAKE_TIMEOUT_MILLIS;
    while (System.currentTimeMillis() < deadline) {
      Event event = parent.pollEvent(100);
      if (event instanceof InitializedEvent) {
        parent.sendRequest(new ConfigurationDoneRequest(), CONNECT_TIMEOUT_MILLIS);
      }
      Request incoming = parent.pollIncomingRequest(50);
      if (incoming != null) {
        parent.respond(incoming, true);
        if (incoming instanceof StartDebuggingRequest start
            && start.getArguments() != null && start.getArguments().getConfiguration() != null) {
          return start.getArguments().getConfiguration();
        }
      }
    }
    throw new IOException("js-debug never requested the child debug session"
                          + " (no startDebugging within " + PARENT_HANDSHAKE_TIMEOUT_MILLIS / 1000 + "s)");
  }

  /** The parent session's launch configuration (mirrors {@link #firefoxLaunchConfig}). */
  private Map<String, Object> parentLaunchConfig(String targetUrl) {
    Map<String, Object> config = new LinkedHashMap<>();
    config.put("type", "pwa-chrome");
    config.put("request", "launch");
    config.put("name", "IntelliJ Haxe browser session");
    config.put("url", targetUrl);
    if (serveContent) {
      config.put("webRoot", contentRoot.toString());
    }
    if (!configuredBrowserExecutable.isBlank()) {
      config.put("runtimeExecutable", configuredBrowserExecutable);
    }
    return config;
  }

  // ------------------------------------------------------ shared plumbing

  // The adapters announce their port slightly BEFORE the listener accepts;
  // retry inside a short window instead of failing the session.
  private static DapClient connectWithRetry(int port) throws IOException {
    return DapClient.connectWithRetry("127.0.0.1", port, CONNECT_TIMEOUT_MILLIS, CONNECT_RETRY_WINDOW_MILLIS);
  }

  /** The pinned-adapter cache: {@code <ide-system>/haxe/debug-adapters}. */
  public static Path adapterStoreRoot() {
    return Path.of(PathManager.getSystemPath(), "haxe", "debug-adapters");
  }

  // --- session behaviour (wire facts from the live probes) ---

  @Override
  public void onConnected(DapDebugProcess process) {
    BufferedReader reader = adapterStdout;
    adapterStdout = null;
    if (reader != null) {
      Thread gobbler = new Thread(() -> {
        try (BufferedReader stdout = reader) {
          String line;
          while ((line = stdout.readLine()) != null) {
            process.printSystem("[adapter] " + line + "\n");
          }
        } catch (IOException ignored) {
          // adapter ended
        }
      }, "Browser adapter output");
      gobbler.setDaemon(true);
      gobbler.start();
    }
    JsDebugSessionMux mux = sessionMux;
    if (mux != null) {
      // the mux owns the parent pumping and worker attachment; its
      // attach/exit notes land in the console as grey system output
      mux.setLogSink(line -> process.printSystem("[js-debug] " + line + "\n"));
    }
  }

  @Override
  public boolean requiresLaunchRequest() {
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
    closed = true;
    ContentHttpServer server = contentServer;
    contentServer = null;
    if (server != null) {
      server.close();
    }
    JsDebugSessionMux mux = sessionMux;
    sessionMux = null;
    if (mux != null) {
      try {
        mux.close(); // closes page, workers AND the parent connection
      } catch (IOException ignored) {
      }
    }
    DapClient parent = parentClient;
    parentClient = null;
    if (mux == null && parent != null) {
      try {
        parent.close(); // startup failed before the mux existed
      } catch (IOException ignored) {
      }
    }
    Process adapter = adapterProcess;
    adapterProcess = null;
    if (adapter != null) {
      // reap the WHOLE tree: killing node does not kill the browser it
      // spawned, and when the graceful DAP disconnect did not happen (forced
      // teardown) every session would otherwise leak a headless browser.
      adapter.descendants().forEach(ProcessHandle::destroyForcibly);
      adapter.destroy();
      try {
        if (!adapter.waitFor(ADAPTER_KILL_WAIT_SECONDS, TimeUnit.SECONDS)) {
          adapter.destroyForcibly();
        }
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }
    }
  }
}
