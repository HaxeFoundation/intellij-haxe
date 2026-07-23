package com.intellij.plugins.haxe.runner.debugger.browser;

import com.intellij.plugins.haxe.runner.debugger.dap.client.DapClient;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.intellij.plugins.haxe.runner.debugger.dap.protocol.*;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.events.*;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.requests.*;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.responses.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.After;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;

/**
 * M0 wire probe for the vscode-firefox-debug adapter (web-debugger project):
 * spawns the PINNED adapter bundle (Open VSX 2.15.0, sha256 f72f7443...) on the
 * local portable node, in {@code --server=<port>} TCP mode, and drives it with
 * the SAME DapClient the IDE backends use. Verifies the plugin's framing/decoding against
 * a foreign adapter and records the initialize capabilities + event ordering.
 *
 * Skips (does not fail) when node or the adapter are not provisioned — they are
 * user-provisioned under {@code <project>/node/} (git-ignored), see the
 * web-debugger plan.
 */
public class FirefoxAdapterLiveProbe {
  private static final long TIMEOUT = 15_000;

  private Process adapter;
  private int adapterPort;
  private DapClient client;

  /** {@code <root>/node} — provided by the gradle test task; falls back for IDE runs. */
  private static Path nodeRoot() {
    String override = System.getProperty("web.debug.node.root");
    return override != null ? Path.of(override) : Path.of("../../node").toAbsolutePath().normalize();
  }

  private static Path nodeExe() {
    // the compat-matrix web lanes point each cell at a provisioned node
    String override = System.getProperty("web.debug.node.exe");
    return override != null ? Path.of(override)
                            : nodeRoot().resolve("node-v24.18.0-win-x64/node.exe");
  }

  private static Path adapterBundle() {
    return nodeRoot().resolve("adapters/vscode-firefox-debug-2.15.0/extension/dist/adapter.bundle.js");
  }

  @Before
  public void spawnAdapter() throws IOException {
    Assume.assumeTrue("portable node not provisioned - skipping", Files.isRegularFile(nodeExe()));
    Assume.assumeTrue("firefox adapter not provisioned - skipping", Files.isRegularFile(adapterBundle()));

    int port = LiveProbeUtil.freePort();
    adapter = new ProcessBuilder(nodeExe().toString(), adapterBundle().toString(), "--server=" + port)
      // cwd = dist so the bundle finds mappings.wasm however it resolves it
      .directory(adapterBundle().getParent().toFile())
      .redirectErrorStream(true)
      .start();

    // wait for its "waiting for debug protocol on port N" announcement
    BufferedReader stdout = new BufferedReader(
      new InputStreamReader(adapter.getInputStream(), StandardCharsets.UTF_8));
    String line = stdout.readLine();
    System.out.println("[adapter] " + line);
    assertNotNull("adapter announced nothing (died?)", line);
    assertTrue("unexpected announcement: " + line, line.contains("waiting for debug protocol"));

    // keep draining in the background so the adapter can't block on a full pipe
    Thread gobbler = new Thread(() -> {
      try {
        String out;
        while ((out = stdout.readLine()) != null) {
          System.out.println("[adapter] " + out);
        }
      } catch (IOException ignored) {
      }
    }, "adapter-gobbler");
    gobbler.setDaemon(true);
    gobbler.start();

    adapterPort = port;
    client = connectWithRetry(port);
  }

  // The adapter prints its "waiting for debug protocol" line slightly
  // BEFORE the TCP listener accepts, so an immediate connect can be
  // refused - retry briefly (the production launcher must do the same).
  private static DapClient connectWithRetry(int port) throws IOException {
    return LiveProbeUtil.connectWithRetry(port, (int)TIMEOUT);
  }

  @After
  public void tearDown() throws Exception {
    if (client != null) {
      try {
        client.close();
      } catch (IOException ignored) {
      }
    }
    if (adapter != null) {
      killTree(adapter);
    }
  }

  // Killing node does NOT kill the Firefox it spawned - reap the whole tree,
  // or every probe run leaks a headless browser.
  private static void killTree(Process process) throws InterruptedException {
    LiveProbeUtil.killTree(process);
  }

  @Test(timeout = 30_000)
  public void initializeHandshakeAndCapabilities() throws Exception {
    InitializeRequest initialize = new InitializeRequest();
    InitializeRequestArguments arguments = new InitializeRequestArguments();
    arguments.setClientID("intellij");
    arguments.setAdapterID("firefox");
    // the firefox adapter REJECTS initialize unless pathFormat=="path"
    // ("debug adapter only supports native paths")
    arguments.setPathFormat("path");
    arguments.setLinesStartAt1(true);
    arguments.setColumnsStartAt1(true);
    initialize.setArguments(arguments);

    Response response = client.sendRequest(initialize, TIMEOUT);
    System.out.println("[probe] initialize success=" + response.isSuccess()
                       + " class=" + response.getClass().getSimpleName());
    assertTrue("initialize failed: " + response.getMessage(), response.isSuccess());
    if (response instanceof InitializeResponse init && init.getBody() != null) {
      System.out.println("[probe] capabilities: supportsConfigurationDone="
                         + init.getBody().getSupportsConfigurationDoneRequest()
                         + " supportsSetVariable=" + init.getBody().getSupportsSetVariable()
                         + " supportsConditionalBreakpoints=" + init.getBody().getSupportsConditionalBreakpoints());
    }

    // event-ordering observation: does an initialized event arrive BEFORE any
    // launch (some adapters), or only later? poll briefly and report.
    long deadline = System.currentTimeMillis() + 3_000;
    while (System.currentTimeMillis() < deadline) {
      Event event = client.pollEvent(250);
      if (event != null) {
        System.out.println("[probe] event before launch: " + event.getClass().getSimpleName());
      }
    }

    Response disconnect = client.sendRequest(new DisconnectRequest(), TIMEOUT);
    System.out.println("[probe] disconnect success=" + disconnect.isSuccess());
  }

  // ---------------------------------------------------------- full session

  /** A launch request whose arguments are the firefox adapter's own vocabulary. */
  static final class FirefoxLaunchRequest extends Request {
    @SuppressWarnings("unused") // serialized by jackson
    private final Map<String, Object> arguments;

    FirefoxLaunchRequest(Map<String, Object> arguments) {
      setCommand("launch");
      this.arguments = arguments;
    }

    public Map<String, Object> getArguments() {
      return arguments;
    }
  }

  /**
   * The browser under test: the {@code WEB_DEBUG_FIREFOX_EXE} environment
   * variable when set (e.g. an ESR install), else the standard installation
   * paths. A set-but-invalid path SKIPS rather than silently testing a
   * different browser than the one asked for.
   */
  private static Path firefoxExe() {
    String env = System.getenv("WEB_DEBUG_FIREFOX_EXE");
    if (env != null && !env.isBlank()) {
      Path fromEnv = Path.of(env);
      return Files.isRegularFile(fromEnv) ? fromEnv : null;
    }
    for (String candidate : new String[]{
      "C:/Program Files/Mozilla Firefox/firefox.exe",
      "C:/Program Files (x86)/Mozilla Firefox/firefox.exe",
      "/usr/bin/firefox",
      "/usr/bin/firefox-esr",
      "/snap/bin/firefox"}) {
      Path path = Path.of(candidate);
      if (Files.isRegularFile(path)) {
        return path;
      }
    }
    return null;
  }

  private static boolean haxeOnPath() {
    return LiveProbeUtil.haxeOnPath();
  }

  // WebMain.hx line numbers are load-bearing: BP_LINE is `counter++;`
  private static final int BP_LINE = 5;
  private static final String WEB_MAIN_HX = """
    class WebMain {
    	static var counter = 0;

    	static function tick() {
    		counter++; // BP_LINE = 5
    		var label = "tick-" + counter;
    		js.Browser.console.log(label);
    	}

    	static function main() {
    		js.Browser.window.setInterval(tick, 250);
    	}
    }
    """;

  /** Writes + compiles the fixture, returns its directory (app.js/app.js.map/index.html/WebMain.hx). */
  private static Path buildFixture() throws Exception {
    Path dir = Files.createTempDirectory("haxe-web-probe");
    Files.writeString(dir.resolve("WebMain.hx"), WEB_MAIN_HX);
    Files.writeString(dir.resolve("index.html"),
                      "<!DOCTYPE html><html><head><meta charset='utf-8'></head>"
                      + "<body><script src='app.js'></script></body></html>");
    LiveProbeUtil.compileHaxeJs(dir, "WebMain", "app.js");
    return dir;
  }

  @Test(timeout = 60_000)
  public void fullSessionBreakpointInHxSourceViaFileUrl() throws Exception {
    Assume.assumeTrue("haxe not on PATH - skipping", haxeOnPath());
    Path firefox = firefoxExe();
    Assume.assumeTrue("firefox not found (set WEB_DEBUG_FIREFOX_EXE or install Firefox) - skipping", firefox != null);
    Path fixture = buildFixture();
    System.out.println("[probe] fixture at " + fixture);

    InitializeRequest initialize = new InitializeRequest();
    InitializeRequestArguments initArgs = new InitializeRequestArguments();
    initArgs.setClientID("intellij");
    initArgs.setAdapterID("firefox");
    initArgs.setPathFormat("path");
    initArgs.setLinesStartAt1(true);
    initArgs.setColumnsStartAt1(true);
    initialize.setArguments(initArgs);
    assertTrue("initialize", client.sendRequest(initialize, TIMEOUT).isSuccess());

    Map<String, Object> launchConfig = new LinkedHashMap<>();
    launchConfig.put("request", "launch");
    launchConfig.put("file", fixture.resolve("index.html").toString());
    launchConfig.put("firefoxExecutable", firefox.toString());
    launchConfig.put("firefoxArgs", List.of("-headless"));
    launchConfig.put("port", LiveProbeUtil.freePort()); // never the shared default 6000
    Response launch = client.sendRequest(new FirefoxLaunchRequest(launchConfig), 20_000);
    System.out.println("[probe] launch success=" + launch.isSuccess()
                       + (launch.isSuccess() ? "" : " message=" + launch.getMessage()));
    assertTrue("launch failed: " + launch.getMessage(), launch.isSuccess());

    // event order observation: wait for the initialized event (post-launch here)
    boolean initialized = false;
    long deadline = System.currentTimeMillis() + 15_000;
    while (System.currentTimeMillis() < deadline && !initialized) {
      Event event = client.pollEvent(250);
      if (event != null) {
        System.out.println("[probe] event: " + event.getClass().getSimpleName());
        initialized = event instanceof InitializedEvent;
      }
    }
    assertTrue("no initialized event after launch", initialized);

    // breakpoint by the ORIGINAL .hx path (native absolute path)
    SetBreakpointsRequest setBreakpoints = new SetBreakpointsRequest();
    SetBreakpointsArguments bpArgs = new SetBreakpointsArguments();
    Source source = new Source();
    source.setPath(fixture.resolve("WebMain.hx").toString());
    source.setName("WebMain.hx");
    bpArgs.setSource(source);
    SourceBreakpoint bp = new SourceBreakpoint();
    bp.setLine(BP_LINE);
    bpArgs.setBreakpoints(List.of(bp));
    setBreakpoints.setArguments(bpArgs);
    Response bpResponse = client.sendRequest(setBreakpoints, TIMEOUT);
    if (bpResponse instanceof SetBreakpointsResponse ok && ok.getBody() != null) {
      for (var b : ok.getBody().getBreakpoints()) {
        System.out.println("[probe] breakpoint verified=" + b.isVerified() + " line=" + b.getLine());
      }
    }
    assertTrue("setBreakpoints failed", bpResponse.isSuccess());

    // the ticking fixture must hit the breakpoint soon
    StoppedEvent stopped = null;
    deadline = System.currentTimeMillis() + 15_000;
    while (System.currentTimeMillis() < deadline && stopped == null) {
      Event event = client.pollEvent(250);
      if (event != null) {
        System.out.println("[probe] event: " + event.getClass().getSimpleName()
                           + (event instanceof StoppedEvent s ? " reason=" + s.getBody().getReason() : ""));
        if (event instanceof StoppedEvent s) {
          stopped = s;
        }
      }
    }
    assertNotNull("breakpoint never hit", stopped);
    int threadId = stopped.getBody().getThreadId() != null ? stopped.getBody().getThreadId() : 1;

    assertTrue("threads", client.sendRequest(new ThreadsRequest(), TIMEOUT).isSuccess());

    StackTraceRequest stackTrace = new StackTraceRequest();
    StackTraceArguments stArgs = new StackTraceArguments();
    stArgs.setThreadId(threadId);
    stackTrace.setArguments(stArgs);
    Response stResponse = client.sendRequest(stackTrace, TIMEOUT);
    assertTrue("stackTrace", stResponse.isSuccess());
    List<StackFrame> frames = ((StackTraceResponse)stResponse).getBody().getStackFrames();
    for (int i = 0; i < Math.min(3, frames.size()); i++) {
      StackFrame frame = frames.get(i);
      System.out.println("[probe] frame " + i + ": " + frame.getName()
                         + " @ " + (frame.getSource() != null ? frame.getSource().getPath() : "?")
                         + ":" + frame.getLine());
    }
    assertTrue("no frames", !frames.isEmpty());
    StackFrame top = frames.get(0);
    assertNotNull("top frame has no source", top.getSource());
    assertTrue("top frame is not the .hx original: " + top.getSource().getPath(),
               top.getSource().getPath() != null && top.getSource().getPath().endsWith("WebMain.hx"));
    assertTrue("wrong line: " + top.getLine(), top.getLine() == BP_LINE);

    // scopes + a few variables of the top frame
    ScopesRequest scopes = new ScopesRequest();
    ScopesArguments scArgs = new ScopesArguments();
    scArgs.setFrameId(top.getId());
    scopes.setArguments(scArgs);
    Response scResponse = client.sendRequest(scopes, TIMEOUT);
    assertTrue("scopes", scResponse.isSuccess());
    for (var scope : ((ScopesResponse)scResponse).getBody().getScopes()) {
      VariablesRequest variables = new VariablesRequest();
      VariablesArguments vArgs = new VariablesArguments();
      vArgs.setVariablesReference(scope.getVariablesReference());
      variables.setArguments(vArgs);
      Response vResponse = client.sendRequest(variables, TIMEOUT);
      if (vResponse instanceof VariablesResponse vars && vars.isSuccess() && vars.getBody() != null) {
        List<Variable> list = vars.getBody().getVariables();
        System.out.println("[probe] scope '" + scope.getName() + "': "
                           + list.stream().limit(5).map(v -> v.getName() + "=" + v.getValue()).toList());
      }
    }

    Response disconnect = client.sendRequest(new DisconnectRequest(), TIMEOUT);
    System.out.println("[probe] disconnect success=" + disconnect.isSuccess());
  }

  /**
   * The SERVE mode the IDE backend uses: the fixture is hosted by the module's own
   * ContentHttpServer and the browser navigates to the http url (webRoot maps
   * served urls back to the content directory for the source maps). Mirrors
   * BrowserDebugBackend's launch config; a stop must still land in the .hx.
   */
  @Test(timeout = 60_000)
  public void fullSessionBreakpointInHxSourceViaContentServer() throws Exception {
    Assume.assumeTrue("haxe not on PATH - skipping", haxeOnPath());
    Path firefox = firefoxExe();
    Assume.assumeTrue("firefox not found (set WEB_DEBUG_FIREFOX_EXE or install Firefox) - skipping", firefox != null);
    Path fixture = buildFixture();

    try (ContentHttpServer content = new ContentHttpServer(fixture)) {
      System.out.println("[probe] serving " + fixture + " at " + content.getBaseUrl());

      InitializeRequest initialize = new InitializeRequest();
      InitializeRequestArguments initArgs = new InitializeRequestArguments();
      initArgs.setClientID("intellij");
      initArgs.setAdapterID("firefox");
      initArgs.setPathFormat("path");
      initArgs.setLinesStartAt1(true);
      initArgs.setColumnsStartAt1(true);
      initialize.setArguments(initArgs);
      assertTrue("initialize", client.sendRequest(initialize, TIMEOUT).isSuccess());

      Map<String, Object> launchConfig = new LinkedHashMap<>();
      launchConfig.put("request", "launch");
      launchConfig.put("url", content.getBaseUrl());
      launchConfig.put("webRoot", fixture.toString());
      launchConfig.put("firefoxExecutable", firefox.toString());
      launchConfig.put("firefoxArgs", List.of("-headless"));
    launchConfig.put("port", LiveProbeUtil.freePort()); // never the shared default 6000
      Response launch = client.sendRequest(new FirefoxLaunchRequest(launchConfig), 20_000);
      assertTrue("launch failed: " + launch.getMessage(), launch.isSuccess());

      boolean initialized = false;
      long deadline = System.currentTimeMillis() + 15_000;
      while (System.currentTimeMillis() < deadline && !initialized) {
        initialized = client.pollEvent(250) instanceof InitializedEvent;
      }
      assertTrue("no initialized event after launch", initialized);

      SetBreakpointsRequest setBreakpoints = new SetBreakpointsRequest();
      SetBreakpointsArguments bpArgs = new SetBreakpointsArguments();
      Source source = new Source();
      source.setPath(fixture.resolve("WebMain.hx").toString());
      source.setName("WebMain.hx");
      bpArgs.setSource(source);
      SourceBreakpoint bp = new SourceBreakpoint();
      bp.setLine(BP_LINE);
      bpArgs.setBreakpoints(List.of(bp));
      setBreakpoints.setArguments(bpArgs);
      assertTrue("setBreakpoints failed", client.sendRequest(setBreakpoints, TIMEOUT).isSuccess());

      StoppedEvent stopped = null;
      deadline = System.currentTimeMillis() + 15_000;
      while (System.currentTimeMillis() < deadline && stopped == null) {
        if (client.pollEvent(250) instanceof StoppedEvent s) {
          stopped = s;
        }
      }
      assertNotNull("breakpoint never hit over http", stopped);
      int threadId = stopped.getBody().getThreadId() != null ? stopped.getBody().getThreadId() : 1;

      StackTraceRequest stackTrace = new StackTraceRequest();
      StackTraceArguments stArgs = new StackTraceArguments();
      stArgs.setThreadId(threadId);
      stackTrace.setArguments(stArgs);
      Response stResponse = client.sendRequest(stackTrace, TIMEOUT);
      assertTrue("stackTrace", stResponse.isSuccess());
      List<StackFrame> frames = ((StackTraceResponse)stResponse).getBody().getStackFrames();
      assertTrue("no frames", !frames.isEmpty());
      StackFrame top = frames.get(0);
      System.out.println("[probe] http-mode top frame: " + top.getName()
                         + " @ " + (top.getSource() != null ? top.getSource().getPath() : "?")
                         + ":" + top.getLine());
      assertNotNull("top frame has no source", top.getSource());
      assertTrue("top frame is not the .hx original: " + top.getSource().getPath(),
                 top.getSource().getPath() != null && top.getSource().getPath().endsWith("WebMain.hx"));
      assertTrue("wrong line: " + top.getLine(), top.getLine() == BP_LINE);

      Response disconnect = client.sendRequest(new DisconnectRequest(), TIMEOUT);
      System.out.println("[probe] disconnect success=" + disconnect.isSuccess());
    }
  }

  /**
   * The IDE's breakpoint paths come from IntelliJ's VFS, which uses FORWARD
   * slashes on Windows (C:/Users/...). Does the adapter bind those, or only
   * native backslash paths? (The IDE smoke test showed breakpoints never
   * binding; my earlier probes all sent native paths and worked.)
   */
  @Test(timeout = 60_000)
  public void breakpointPathSeparatorSensitivity() throws Exception {
    Assume.assumeTrue("haxe not on PATH - skipping", haxeOnPath());
    Path firefox = firefoxExe();
    Assume.assumeTrue("firefox not found (set WEB_DEBUG_FIREFOX_EXE or install Firefox) - skipping", firefox != null);
    Path fixture = buildFixture(); // the ticking fixture: no load race involved

    boolean forwardBound;
    boolean nativeBound;
    try (ContentHttpServer content = new ContentHttpServer(fixture)) {
      forwardBound = runSeparatorVariant("forward", fixture, firefox, content,
                                         fixture.resolve("WebMain.hx").toString().replace('\\', '/'));
    }
    try (ContentHttpServer content = new ContentHttpServer(fixture)) {
      nativeBound = runSeparatorVariant("native", fixture, firefox, content,
                                        fixture.resolve("WebMain.hx").toString());
    }
    System.out.println("[probe] separator sensitivity: forward=" + forwardBound + " native=" + nativeBound);
    assertTrue("native breakpoint path must bind", nativeBound);
    // no assert on forwardBound: this test RECORDS the adapter's behaviour;
    // the IDE-side fix (DapBreakpointManager normalization) covers either way
  }

  private boolean runSeparatorVariant(String label, Path fixture, Path firefox,
                                      ContentHttpServer content, String breakpointPath) throws Exception {
    int ownPort = LiveProbeUtil.freePort();
    Process ownAdapter = new ProcessBuilder(nodeExe().toString(), adapterBundle().toString(), "--server=" + ownPort)
      .directory(adapterBundle().getParent().toFile())
      .redirectErrorStream(true)
      .start();
    try {
      DapClient session = connectWithRetry(ownPort);
      try {
        InitializeRequest initialize = new InitializeRequest();
        InitializeRequestArguments initArgs = new InitializeRequestArguments();
        initArgs.setClientID("intellij");
        initArgs.setAdapterID("firefox");
        initArgs.setPathFormat("path");
        initArgs.setLinesStartAt1(true);
        initArgs.setColumnsStartAt1(true);
        initialize.setArguments(initArgs);
        if (!session.sendRequest(initialize, TIMEOUT).isSuccess()) {
          return false;
        }
        Map<String, Object> launchConfig = new LinkedHashMap<>();
        launchConfig.put("request", "launch");
        launchConfig.put("url", content.getBaseUrl());
        launchConfig.put("webRoot", fixture.toString());
        launchConfig.put("firefoxExecutable", firefox.toString());
        launchConfig.put("firefoxArgs", List.of("-headless"));
    launchConfig.put("port", LiveProbeUtil.freePort()); // never the shared default 6000
        if (!session.sendRequest(new FirefoxLaunchRequest(launchConfig), 20_000).isSuccess()) {
          return false;
        }
        long deadline = System.currentTimeMillis() + 15_000;
        boolean initialized = false;
        while (System.currentTimeMillis() < deadline && !initialized) {
          initialized = session.pollEvent(250) instanceof InitializedEvent;
        }
        if (!initialized) {
          return false;
        }
        SetBreakpointsRequest setBreakpoints = new SetBreakpointsRequest();
        SetBreakpointsArguments bpArgs = new SetBreakpointsArguments();
        Source source = new Source();
        source.setPath(breakpointPath);
        source.setName("WebMain.hx");
        bpArgs.setSource(source);
        SourceBreakpoint bp = new SourceBreakpoint();
        bp.setLine(BP_LINE);
        bpArgs.setBreakpoints(List.of(bp));
        setBreakpoints.setArguments(bpArgs);
        session.sendRequest(setBreakpoints, TIMEOUT);

        StoppedEvent stopped = null;
        // the fixture ticks every 250ms and lazy verification takes 1-2s, so
        // a path that binds stops well inside this window; the forward
        // variant is EXPECTED not to stop and pays the full wait
        deadline = System.currentTimeMillis() + 8_000;
        while (System.currentTimeMillis() < deadline && stopped == null) {
          Event event = session.pollEvent(250);
          if (event instanceof StoppedEvent s) {
            stopped = s;
          } else if (event instanceof BreakpointEvent be
                     && be.getBody() != null && be.getBody().getBreakpoint() != null) {
            System.out.println("[probe]   " + label + " breakpointEvent verified="
                               + be.getBody().getBreakpoint().isVerified());
          }
        }
        System.out.println("[probe]   " + label + " path stop=" + (stopped != null));
        session.sendRequest(new DisconnectRequest(), TIMEOUT);
        return stopped != null;
      } finally {
        session.close();
      }
    } finally {
      killTree(ownAdapter);
    }
  }

  // ------------------------------------------- load-time breakpoint strategies

  // WebLoad.hx line numbers are load-bearing: LOAD_BP_LINE is `var marker...`,
  // which executes DURING PAGE LOAD - the race the ticking fixture cannot see.
  private static final int LOAD_BP_LINE = 3;
  private static final String WEB_LOAD_HX = """
    class WebLoad {
    	static function main() {
    		var marker = "before"; // LOAD_BP_LINE = 3
    		js.Browser.console.log(marker + "-loaded");
    	}
    }
    """;

  // --- worker-frame request behaviour (evaluate vs variables) ---

  private static final int FF_WORKER_TICK_LINE = 4;
  private static final String FF_WORKER_HX = """
    class WorkerMain {
    	static var ticks = 0;
    	static function tick() {
    		ticks++; // FF_WORKER_TICK_LINE = 4
    		js.Syntax.code("console.log({0})", "w" + ticks);
    	}
    	static function main() {
    		js.Syntax.code("setInterval({0}, {1})", tick, 250);
    	}
    }
    """;
  private static final int FF_PAGE_BEAT_LINE = 4;
  private static final String FF_PAGE_HX = """
    class WebPage {
    	static var beats = 0;
    	static function heartbeat() {
    		beats++; // FF_PAGE_BEAT_LINE = 4
    	}
    	static function main() {
    		var worker = new js.html.Worker("worker.js");
    		js.Browser.console.log("worker: " + (worker != null));
    		js.Browser.window.setInterval(heartbeat, 300);
    	}
    }
    """;

  /**
   * Pins whether the firefox adapter ANSWERS evaluate for a WORKER thread's
   * frame. Suspicion (IDE-observed): variables/scopes answer, evaluate never
   * does — and per the adapter's per-actor FIFO queue, one unanswered request
   * wedges that thread forever. The TAB thread's evaluate is the control.
   * Runs variables BEFORE evaluate (evaluate may poison the queue).
   */
  @Test(timeout = 60_000)
  public void workerFrameEvaluateBehaviour() throws Exception {
    Assume.assumeTrue("haxe not on PATH - skipping", haxeOnPath());
    Path firefox = firefoxExe();
    Assume.assumeTrue("firefox not found (set WEB_DEBUG_FIREFOX_EXE or install Firefox) - skipping", firefox != null);
    Path fixture = Files.createTempDirectory("haxe-ff-worker-probe");
    System.out.println("[probe] fixture dir: " + fixture);
    Files.writeString(fixture.resolve("WebPage.hx"), FF_PAGE_HX);
    Files.writeString(fixture.resolve("WorkerMain.hx"), FF_WORKER_HX);
    Files.writeString(fixture.resolve("index.html"),
                      "<!DOCTYPE html><html><head><meta charset='utf-8'></head>"
                      + "<body><script src='app.js'></script></body></html>");
    LiveProbeUtil.compileHaxeJs(fixture, "WebPage", "app.js");
    LiveProbeUtil.compileHaxeJs(fixture, "WorkerMain", "worker.js");

    try (ContentHttpServer content = new ContentHttpServer(fixture)) {
      content.setRequestListener(line -> System.out.println("[server] " + line));
      InitializeRequest initialize = new InitializeRequest();
      InitializeRequestArguments initArgs = new InitializeRequestArguments();
      initArgs.setClientID("intellij");
      initArgs.setAdapterID("firefox");
      initArgs.setPathFormat("path");
      initArgs.setLinesStartAt1(true);
      initArgs.setColumnsStartAt1(true);
      initialize.setArguments(initArgs);
      assertTrue("initialize", client.sendRequest(initialize, TIMEOUT).isSuccess());

      Map<String, Object> launchConfig = new LinkedHashMap<>();
      launchConfig.put("request", "launch");
      launchConfig.put("url", content.getBaseUrl());
      launchConfig.put("webRoot", fixture.toString());
      launchConfig.put("firefoxExecutable", firefox.toString());
      launchConfig.put("firefoxArgs", List.of("-headless"));
    launchConfig.put("port", LiveProbeUtil.freePort()); // never the shared default 6000
      // UNIQUE RDP port: the adapter's default 6000 makes it CONNECT TO A
      // LEFTOVER firefox from an earlier session/probe instead of the one it
      // just launched ("Not attaching to this thread" for
      // every worker, foreign processes' workers in the target list)
      launchConfig.put("port", LiveProbeUtil.freePort());
      Path adapterLog = fixture.resolve("adapter.log");
      launchConfig.put("log", Map.of(
        "fileName", adapterLog.toString(),
        "fileLevel", Map.of("default", "Debug")));
      assertTrue("launch", client.sendRequest(new FirefoxLaunchRequest(launchConfig), 20_000).isSuccess());
      long deadline = System.currentTimeMillis() + 15_000;
      boolean initialized = false;
      while (System.currentTimeMillis() < deadline && !initialized) {
        initialized = client.pollEvent(250) instanceof InitializedEvent;
      }
      assertTrue("initialized", initialized);

      // breakpoint in the worker only; the ticking line hits ~immediately
      sendBreakpoint(fixture.resolve("WorkerMain.hx"), FF_WORKER_TICK_LINE);
      StoppedEvent workerStop = awaitStop(15_000);
      assertNotNull("worker breakpoint never hit", workerStop);
      int workerThread = workerStop.getBody().getThreadId() != null ? workerStop.getBody().getThreadId() : 1;
      StackFrame workerFrame = topFrame(workerThread);
      assertNotNull("no worker top frame", workerFrame);
      System.out.println("[probe] worker stop: thread=" + workerThread + " frame=" + workerFrame.getId()
                         + " @ " + (workerFrame.getSource() != null ? workerFrame.getSource().getPath() : "?")
                         + ":" + workerFrame.getLine());

      // 1) scopes+variables on the worker frame (expected to answer)
      System.out.println("[probe] worker scopes/variables: " + timedScopesAndVariables(workerFrame.getId()));
      // 2) evaluate on the worker frame - the suspected never-answered request
      System.out.println("[probe] worker evaluate(watch): " + timedEvaluate("ticks", workerFrame.getId(), "watch"));
      System.out.println("[probe] worker evaluate(repl):  " + timedEvaluate("ticks", workerFrame.getId(), "repl"));

      // 3) CONTROL: the TAB thread - move the breakpoint to the page heartbeat
      sendBreakpoints(fixture.resolve("WorkerMain.hx"), List.of()); // clear worker bp
      sendBreakpoint(fixture.resolve("WebPage.hx"), FF_PAGE_BEAT_LINE);
      resumeThread(workerThread);
      StoppedEvent tabStop = awaitStop(15_000);
      assertNotNull("page heartbeat breakpoint never hit", tabStop);
      int tabThread = tabStop.getBody().getThreadId() != null ? tabStop.getBody().getThreadId() : 1;
      StackFrame tabFrame = topFrame(tabThread);
      assertNotNull("no tab top frame", tabFrame);
      System.out.println("[probe] tab stop: thread=" + tabThread + " frame=" + tabFrame.getId());
      System.out.println("[probe] tab scopes/variables: " + timedScopesAndVariables(tabFrame.getId()));
      System.out.println("[probe] tab evaluate(watch): " + timedEvaluate("beats", tabFrame.getId(), "watch"));

      client.sendRequest(new DisconnectRequest(), TIMEOUT);
    }
  }

  /**
   * The ZOMBIE question, on a CLEAN instance (unique RDP port): with the
   * serve-mode refresh armed and a worker breakpoint hitting BEFORE the
   * reload, what does the thread list look like after the reload settles?
   * (The contaminated-era sessions showed doubled worker threads whose
   * actors answered nothing; this pins whether that happens without the
   * stale-instance pollution.)
   */
  @Test(timeout = 60_000)
  public void workerThreadsAcrossRefresh() throws Exception {
    Assume.assumeTrue("haxe not on PATH - skipping", haxeOnPath());
    Path firefox = firefoxExe();
    Assume.assumeTrue("firefox not found (set WEB_DEBUG_FIREFOX_EXE or install Firefox) - skipping", firefox != null);
    Path fixture = Files.createTempDirectory("haxe-ff-refresh-probe");
    Files.writeString(fixture.resolve("WebPage.hx"), FF_PAGE_HX);
    Files.writeString(fixture.resolve("WorkerMain.hx"), FF_WORKER_HX);
    Files.writeString(fixture.resolve("index.html"),
                      "<!DOCTYPE html><html><head><meta charset='utf-8'></head>"
                      + "<body><script src='app.js'></script></body></html>");
    LiveProbeUtil.compileHaxeJs(fixture, "WebPage", "app.js");
    LiveProbeUtil.compileHaxeJs(fixture, "WorkerMain", "worker.js");

    try (ContentHttpServer content = new ContentHttpServer(fixture)) {
      content.setRequestListener(line -> System.out.println("[server] " + line));
      content.refreshFirstPage(2);
      InitializeRequest initialize = new InitializeRequest();
      InitializeRequestArguments initArgs = new InitializeRequestArguments();
      initArgs.setClientID("intellij");
      initArgs.setAdapterID("firefox");
      initArgs.setPathFormat("path");
      initArgs.setLinesStartAt1(true);
      initArgs.setColumnsStartAt1(true);
      initialize.setArguments(initArgs);
      assertTrue("initialize", client.sendRequest(initialize, TIMEOUT).isSuccess());

      Map<String, Object> launchConfig = new LinkedHashMap<>();
      launchConfig.put("request", "launch");
      launchConfig.put("url", content.getBaseUrl());
      launchConfig.put("webRoot", fixture.toString());
      launchConfig.put("firefoxExecutable", firefox.toString());
      launchConfig.put("firefoxArgs", List.of("-headless"));
      launchConfig.put("port", LiveProbeUtil.freePort());
      assertTrue("launch", client.sendRequest(new FirefoxLaunchRequest(launchConfig), 20_000).isSuccess());
      long deadline = System.currentTimeMillis() + 15_000;
      boolean initialized = false;
      while (System.currentTimeMillis() < deadline && !initialized) {
        initialized = client.pollEvent(250) instanceof InitializedEvent;
      }
      assertTrue("initialized", initialized);
      sendBreakpoint(fixture.resolve("WorkerMain.hx"), FF_WORKER_TICK_LINE);

      // first stop: the first load's worker hits its ticking bp BEFORE the 2s
      // reload; then the reload fires while that worker is paused
      StoppedEvent first = awaitStop(15_000);
      assertNotNull("worker breakpoint never hit on the first load", first);
      System.out.println("[probe] first stop: thread=" + first.getBody().getThreadId());

      // let the reload happen and the second load settle (its worker re-hits)
      StoppedEvent second = awaitStop(15_000);
      System.out.println("[probe] second stop: "
                         + (second == null ? "none" : "thread=" + second.getBody().getThreadId()));

      Response threadsResponse = client.sendRequest(new ThreadsRequest(), TIMEOUT);
      var threads = ((ThreadsResponse)
                       threadsResponse).getBody().getThreads();
      for (var thread : threads) {
        System.out.println("[probe] thread id=" + thread.getId() + " name=" + thread.getName());
      }
      System.out.println("[probe] thread count after refresh cycle: " + threads.size());
      client.sendRequest(new DisconnectRequest(), TIMEOUT);
    }
  }

  private void sendBreakpoint(Path hxFile, int line) throws Exception {
    SourceBreakpoint bp = new SourceBreakpoint();
    bp.setLine(line);
    sendBreakpoints(hxFile, List.of(bp));
  }

  private void sendBreakpoints(Path hxFile, List<SourceBreakpoint> bps) throws Exception {
    SetBreakpointsRequest request = new SetBreakpointsRequest();
    SetBreakpointsArguments arguments = new SetBreakpointsArguments();
    Source source = new Source();
    source.setPath(hxFile.toString());
    source.setName(hxFile.getFileName().toString());
    arguments.setSource(source);
    arguments.setBreakpoints(bps);
    request.setArguments(arguments);
    Response response = client.sendRequest(request, TIMEOUT);
    assertTrue("setBreakpoints " + hxFile.getFileName(), response.isSuccess());
    if (response instanceof SetBreakpointsResponse ok && ok.getBody() != null && ok.getBody().getBreakpoints() != null) {
      ok.getBody().getBreakpoints().forEach(b -> System.out.println(
        "[probe] bp " + hxFile.getFileName() + " id=" + b.getId() + " verified=" + b.isVerified()));
    }
  }

  private StoppedEvent awaitStop(long timeoutMillis) throws InterruptedException {
    long deadline = System.currentTimeMillis() + timeoutMillis;
    while (System.currentTimeMillis() < deadline) {
      Event event = client.pollEvent(250);
      if (event instanceof StoppedEvent stopped) {
        return stopped;
      }
      if (event != null) {
        String detail = event instanceof BreakpointEvent be
                        && be.getBody() != null && be.getBody().getBreakpoint() != null
                        ? " id=" + be.getBody().getBreakpoint().getId()
                          + " verified=" + be.getBody().getBreakpoint().isVerified()
                        : event instanceof ThreadEvent te
                          && te.getBody() != null
                          ? " reason=" + te.getBody().getReason() + " threadId=" + te.getBody().getThreadId()
                          : "";
        System.out.println("[probe] event '" + event.getEvent() + "'" + detail);
      }
    }
    return null;
  }

  private StackFrame topFrame(int threadId) throws Exception {
    StackTraceRequest request = new StackTraceRequest();
    StackTraceArguments arguments = new StackTraceArguments();
    arguments.setThreadId(threadId);
    request.setArguments(arguments);
    Response response = client.sendRequest(request, TIMEOUT);
    return response instanceof StackTraceResponse st && st.isSuccess() && !st.getBody().getStackFrames().isEmpty()
           ? st.getBody().getStackFrames().get(0) : null;
  }

  private void resumeThread(int threadId) throws Exception {
    ContinueRequest resume = new ContinueRequest();
    ContinueArguments arguments = new ContinueArguments();
    arguments.setThreadId(threadId);
    resume.setArguments(arguments);
    client.sendRequest(resume, TIMEOUT);
  }

  private String timedScopesAndVariables(int frameId) {
    long start = System.currentTimeMillis();
    try {
      ScopesRequest scopes = new ScopesRequest();
      ScopesArguments scArgs = new ScopesArguments();
      scArgs.setFrameId(frameId);
      scopes.setArguments(scArgs);
      Response scResponse = client.sendRequest(scopes, 10_000);
      if (!(scResponse instanceof ScopesResponse ok) || !ok.isSuccess() || ok.getBody().getScopes().isEmpty()) {
        return "scopes FAILED after " + (System.currentTimeMillis() - start) + "ms";
      }
      VariablesRequest variables = new VariablesRequest();
      VariablesArguments vArgs = new VariablesArguments();
      vArgs.setVariablesReference(ok.getBody().getScopes().get(0).getVariablesReference());
      variables.setArguments(vArgs);
      Response vResponse = client.sendRequest(variables, 10_000);
      int count = vResponse instanceof VariablesResponse vr && vr.isSuccess() && vr.getBody().getVariables() != null
                  ? vr.getBody().getVariables().size() : -1;
      return "OK (" + count + " vars, " + (System.currentTimeMillis() - start) + "ms)";
    } catch (Exception e) {
      return "HUNG/FAILED after " + (System.currentTimeMillis() - start) + "ms: " + e.getMessage();
    }
  }

  private String timedEvaluate(String expression, int frameId, String context) {
    long start = System.currentTimeMillis();
    try {
      EvaluateRequest request = new EvaluateRequest();
      EvaluateArguments arguments = new EvaluateArguments();
      arguments.setExpression(expression);
      arguments.setFrameId(frameId);
      arguments.setContext(context);
      request.setArguments(arguments);
      Response response = client.sendRequest(request, 10_000);
      String result = response instanceof EvaluateResponse ok
                      && ok.isSuccess() ? ok.getBody().getResult() : "error: " + response.getMessage();
      return "answered in " + (System.currentTimeMillis() - start) + "ms -> " + result;
    } catch (Exception e) {
      return "NO ANSWER after " + (System.currentTimeMillis() - start) + "ms (" + e.getMessage() + ")";
    }
  }

  private static Path buildLoadFixture() throws Exception {
    Path dir = Files.createTempDirectory("haxe-web-load-probe");
    Files.writeString(dir.resolve("WebLoad.hx"), WEB_LOAD_HX);
    Files.writeString(dir.resolve("index.html"),
                      "<!DOCTYPE html><html><head><meta charset='utf-8'></head>"
                      + "<body><script src='app.js'></script></body></html>");
    LiveProbeUtil.compileHaxeJs(dir, "WebLoad", "app.js");
    return dir;
  }

  /**
   * Which strategy beats the load race: code in main() runs while the page
   * loads, likely BEFORE the standard breakpoint flow (launch -> initialized
   * -> setBreakpoints) completes. Tries, in order:
   *   A: breakpoints BEFORE launch;
   *   B: breakpoints before launch + reloadOnAttach:true;
   *   C: standard order + reloadOnAttach:true.
   * Each variant is a fresh DAP connection to the same adapter server (it
   * accepts sequential connections). Asserts at least one variant stops.
   */
  @Test(timeout = 60_000)
  public void loadTimeBreakpointStrategies() throws Exception {
    Assume.assumeTrue("haxe not on PATH - skipping", haxeOnPath());
    Path firefox = firefoxExe();
    Assume.assumeTrue("firefox not found (set WEB_DEBUG_FIREFOX_EXE or install Firefox) - skipping", firefox != null);
    Path fixture = buildLoadFixture();

    // J: plain standard flow (baseline: does the adapter attach/emit at all?)
    // K: refresh-once, NO injection (second load hits via the learned map?)
    // I: refresh-once + `debugger;` entry-pause injection
    Path appJs = fixture.resolve("app.js");
    String pristineAppJs = Files.readString(appJs);

    // Variants L/M/N do not arm load-time breakpoints and were removed (N
    // was re-checked on a clean instance with a unique RDP port): L/M served
    // a synthetic BOOTSTRAP page first (empty page + meta refresh to the app;
    // M with an inert <script>); N registered the breakpoints only when the
    // RELOADED page requested its script (server-held response, so the first
    // load ran unpaused). None armed, because the adapter applies breakpoints
    // to a load only when they were registered BEFORE the load that taught it
    // the sources. Register -> load once -> reload (K) is the single working
    // sequence; its cost is that a worker PAUSED at a breakpoint when the
    // reload fires lingers as a zombie thread (pinned by the
    // workerThreadsAcrossRefresh probe), which the module README documents.
    String worked = null;
    for (String variant : new String[]{"J", "K"}) {
      Files.writeString(appJs, pristineAppJs);
      try (ContentHttpServer content = new ContentHttpServer(fixture)) {
        content.setRequestListener(line -> System.out.println("[server] " + line));
        boolean stopped = runLoadVariant(variant, fixture, firefox, content);
        System.out.println("[probe] load-variant " + variant + ": " + (stopped ? "STOPPED" : "missed"));
        if (stopped && worked == null) {
          worked = variant;
        }
      }
    }
    assertNotNull("no strategy hit the load-time breakpoint", worked);
    System.out.println("[probe] first working load strategy: " + worked);
  }

  private boolean runLoadVariant(String variant, Path fixture, Path firefox, ContentHttpServer content)
    throws Exception {
    // every variant gets its OWN adapter process + first connection, so no
    // verdict is polluted by session-reuse behaviour of the adapter server
    int ownPort = LiveProbeUtil.freePort();
    Process ownAdapter = new ProcessBuilder(nodeExe().toString(), adapterBundle().toString(), "--server=" + ownPort)
      .directory(adapterBundle().getParent().toFile())
      .redirectErrorStream(true)
      .start();
    Thread gobbler = new Thread(() -> {
      try (BufferedReader out = new BufferedReader(
        new InputStreamReader(ownAdapter.getInputStream(), StandardCharsets.UTF_8))) {
        String line;
        while ((line = out.readLine()) != null) {
          System.out.println("[adapter-" + variant + "] " + line);
        }
      } catch (IOException ignored) {
      }
    }, "adapter-gobbler-" + variant);
    gobbler.setDaemon(true);
    gobbler.start();
    try {
      DapClient session = connectWithRetry(ownPort);
      try {
        InitializeRequest initialize = new InitializeRequest();
        InitializeRequestArguments initArgs = new InitializeRequestArguments();
        initArgs.setClientID("intellij");
        initArgs.setAdapterID("firefox");
        initArgs.setPathFormat("path");
        initArgs.setLinesStartAt1(true);
        initArgs.setColumnsStartAt1(true);
        initialize.setArguments(initArgs);
        if (!session.sendRequest(initialize, TIMEOUT).isSuccess()) {
          return false;
        }

        boolean refreshOnce = !variant.equals("J");
        if (refreshOnce) {
          content.refreshFirstPage(2);
        }

        Map<String, Object> launchConfig = new LinkedHashMap<>();
        launchConfig.put("request", "launch");
        launchConfig.put("url", content.getBaseUrl());
        launchConfig.put("webRoot", fixture.toString());
        launchConfig.put("firefoxExecutable", firefox.toString());
        launchConfig.put("firefoxArgs", List.of("-headless"));
        // unique RDP port: default 6000 would CONNECT TO A LEFTOVER firefox
        // from an earlier variant/session instead of the launched one
        launchConfig.put("port", LiveProbeUtil.freePort());
        Response launch = session.sendRequest(new FirefoxLaunchRequest(launchConfig), 20_000);
        if (!launch.isSuccess()) {
          System.out.println("[probe]   variant " + variant + " launch failed: " + launch.getMessage());
          return false;
        }

        long deadline = System.currentTimeMillis() + 15_000;
        boolean initialized = false;
        while (System.currentTimeMillis() < deadline && !initialized) {
          initialized = session.pollEvent(250) instanceof InitializedEvent;
        }
        if (!initialized) {
          System.out.println("[probe]   variant " + variant + " no initialized event");
          return false;
        }

        SetBreakpointsRequest setBreakpoints = new SetBreakpointsRequest();
        SetBreakpointsArguments bpArgs = new SetBreakpointsArguments();
        Source source = new Source();
        source.setPath(fixture.resolve("WebLoad.hx").toString());
        source.setName("WebLoad.hx");
        bpArgs.setSource(source);
        SourceBreakpoint bp = new SourceBreakpoint();
        bp.setLine(LOAD_BP_LINE);
        bpArgs.setBreakpoints(List.of(bp));
        setBreakpoints.setArguments(bpArgs);
        Response bpResponse = session.sendRequest(setBreakpoints, TIMEOUT);
        System.out.println("[probe]   variant " + variant + " setBreakpoints success=" + bpResponse.isSuccess());

        // observe everything; an entry pause (non-breakpoint stop) is resumed
        // after a beat so the map can bind; success = a stop ON the .hx line
        deadline = System.currentTimeMillis() + 15_000;
        while (System.currentTimeMillis() < deadline) {
          Event event = session.pollEvent(250);
          if (event == null) {
            continue;
          }
          String detail = event instanceof StoppedEvent s ? " reason=" + s.getBody().getReason() : "";
          System.out.println("[probe]   variant " + variant + " event '" + event.getEvent() + "' ("
                             + event.getClass().getSimpleName() + ")" + detail);
          // No-refresh variants: the fixture logs AFTER the breakpoint line,
          // so page output arriving without a stop is definitive - the load
          // ran through unpaused. Refresh variants skip this: their FIRST
          // load is expected to run through, only the reloaded one stops.
          if (!refreshOnce && event instanceof OutputEvent output
              && output.getBody() != null && output.getBody().getOutput() != null
              && output.getBody().getOutput().contains("-loaded")) {
            System.out.println("[probe]   variant " + variant + " page output without a stop - missed");
            session.sendRequest(new DisconnectRequest(), TIMEOUT);
            return false;
          }
          if (!(event instanceof StoppedEvent stopped)) {
            continue;
          }
          int threadId = stopped.getBody().getThreadId() != null ? stopped.getBody().getThreadId() : 1;
          StackTraceRequest stackTrace = new StackTraceRequest();
          StackTraceArguments stArgs = new StackTraceArguments();
          stArgs.setThreadId(threadId);
          stackTrace.setArguments(stArgs);
          Response stResponse = session.sendRequest(stackTrace, TIMEOUT);
          StackFrame top = stResponse instanceof StackTraceResponse st && st.isSuccess()
                           && !st.getBody().getStackFrames().isEmpty()
                           ? st.getBody().getStackFrames().get(0) : null;
          System.out.println("[probe]   variant " + variant + " stop frame: "
                             + (top == null ? "<none>"
                                : (top.getSource() != null ? top.getSource().getPath() : "?") + ":" + top.getLine()));
          boolean onBpLine = top != null && top.getSource() != null && top.getSource().getPath() != null
                             && top.getSource().getPath().endsWith("WebLoad.hx") && top.getLine() == LOAD_BP_LINE;
          if (onBpLine) {
            session.sendRequest(new DisconnectRequest(), TIMEOUT);
            return true;
          }
          // entry pause or foreign stop: give the adapter a beat to bind, resume
          Thread.sleep(1_500);
          ContinueRequest resume = new ContinueRequest();
          ContinueArguments cArgs = new ContinueArguments();
          cArgs.setThreadId(threadId);
          resume.setArguments(cArgs);
          session.sendRequest(resume, TIMEOUT);
        }
        session.sendRequest(new DisconnectRequest(), TIMEOUT);
        return false;
      } finally {
        session.close();
      }
    } finally {
      killTree(ownAdapter);
    }
  }
}
