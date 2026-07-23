package com.intellij.plugins.haxe.runner.debugger.browser;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.intellij.plugins.haxe.runner.debugger.dap.client.DapClient;
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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import org.junit.After;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;

/**
 * M2 wire probe for vscode-js-debug's standalone DAP server (pinned
 * js-debug-dap v1.117.0, sha256 ad8d04ed..., from the GitHub release), driving
 * an UNGOOGLED-CHROMIUM fork — the user's chosen first Chromium target. Pins
 * the parts that differ from the firefox adapter:
 *
 * <ul>
 *   <li>launch ordering (does the launch response wait for configurationDone?);</li>
 *   <li>the {@code startDebugging} REVERSE request and the child-session
 *       handshake via {@code __pendingTargetId} on a SECOND connection;</li>
 *   <li>breakpoint-by-.hx-path over source maps in the child session.</li>
 * </ul>
 *
 * Skips when node/adapter/chromium are not provisioned under {@code <root>/node}.
 */
public class JsDebugAdapterLiveProbe {
  private static final long TIMEOUT = 15_000;
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

  private Process adapter;
  private int adapterPort;
  private DapClient parent;

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

  private static Path dapServerJs() {
    return nodeRoot().resolve("adapters/js-debug-1.117.0/js-debug/src/dapDebugServer.js");
  }

  /**
   * The browser under test: the {@code WEB_DEBUG_CHROMIUM_EXE} environment
   * variable when set (any chromium-family build — e.g. a provisioned
   * ungoogled-chromium), else an installed Chrome/Edge — mirroring the IDE
   * behaviour, where a blank executable lets js-debug find the default
   * installation. A set-but-invalid path SKIPS rather than silently testing
   * a different browser than the one asked for.
   */
  private static Path chromiumExe() {
    String env = System.getenv("WEB_DEBUG_CHROMIUM_EXE");
    if (env != null && !env.isBlank()) {
      Path fromEnv = Path.of(env);
      return Files.isRegularFile(fromEnv) ? fromEnv : null;
    }
    List<Path> candidates = new ArrayList<>();
    String localAppData = System.getenv("LOCALAPPDATA");
    if (localAppData != null && !localAppData.isBlank()) {
      // per-user installs; plain Chromium (e.g. ungoogled-chromium, the
      // reference browser of this module) ahead of the branded ones
      candidates.add(Path.of(localAppData, "Chromium/Application/chrome.exe"));
      candidates.add(Path.of(localAppData, "Google/Chrome/Application/chrome.exe"));
    }
    for (String candidate : new String[]{
      "C:/Program Files/Google/Chrome/Application/chrome.exe",
      "C:/Program Files (x86)/Google/Chrome/Application/chrome.exe",
      "C:/Program Files (x86)/Microsoft/Edge/Application/msedge.exe",
      "C:/Program Files/Microsoft/Edge/Application/msedge.exe",
      "/usr/bin/chromium",
      "/usr/bin/chromium-browser",
      "/usr/bin/google-chrome",
      "/snap/bin/chromium"}) {
      candidates.add(Path.of(candidate));
    }
    for (Path path : candidates) {
      if (Files.isRegularFile(path)) {
        return path;
      }
    }
    return null;
  }

  private static boolean haxeOnPath() {
    return LiveProbeUtil.haxeOnPath();
  }

  @Before
  public void spawnAdapter() throws IOException {
    Assume.assumeTrue("portable node not provisioned - skipping", Files.isRegularFile(nodeExe()));
    Assume.assumeTrue("js-debug adapter not provisioned - skipping", Files.isRegularFile(dapServerJs()));
    Assume.assumeTrue("no chromium-family browser found (set WEB_DEBUG_CHROMIUM_EXE or install Chrome/Edge) - skipping",
                      chromiumExe() != null);

    adapterPort = LiveProbeUtil.freePort();
    adapter = new ProcessBuilder(nodeExe().toString(), dapServerJs().toString(),
                                 String.valueOf(adapterPort), "127.0.0.1")
      .directory(dapServerJs().getParent().toFile())
      .redirectErrorStream(true)
      .start();
    BufferedReader stdout = new BufferedReader(
      new InputStreamReader(adapter.getInputStream(), StandardCharsets.UTF_8));
    String line = stdout.readLine();
    System.out.println("[adapter] " + line);
    assertNotNull("adapter announced nothing (died?)", line);
    assertTrue("unexpected announcement: " + line, line.contains("Debug server listening"));
    Thread gobbler = new Thread(() -> {
      try {
        String out;
        while ((out = stdout.readLine()) != null) {
          System.out.println("[adapter] " + out);
        }
      } catch (IOException ignored) {
      }
    }, "js-debug-gobbler");
    gobbler.setDaemon(true);
    gobbler.start();

    parent = connectWithRetry(adapterPort);
  }

  private static DapClient connectWithRetry(int port) throws IOException {
    return LiveProbeUtil.connectWithRetry(port, (int)TIMEOUT);
  }

  @After
  public void tearDown() throws Exception {
    if (parent != null) {
      try {
        parent.close();
      } catch (IOException ignored) {
      }
    }
    if (adapter != null) {
      LiveProbeUtil.killTree(adapter);
    }
  }

  private static Path buildFixture() throws Exception {
    Path dir = Files.createTempDirectory("haxe-jsdbg-probe");
    Files.writeString(dir.resolve("WebMain.hx"), WEB_MAIN_HX);
    Files.writeString(dir.resolve("index.html"),
                      "<!DOCTYPE html><html><head><meta charset='utf-8'></head>"
                      + "<body><script src='app.js'></script></body></html>");
    LiveProbeUtil.compileHaxeJs(dir, "WebMain", "app.js");
    return dir;
  }

  private static InitializeRequest initializeRequest() {
    InitializeRequest initialize = new InitializeRequest();
    InitializeRequestArguments arguments = new InitializeRequestArguments();
    arguments.setClientID("intellij");
    arguments.setClientName("IntelliJ Haxe");
    arguments.setAdapterID("chrome");
    arguments.setPathFormat("path");
    arguments.setLinesStartAt1(true);
    arguments.setColumnsStartAt1(true);
    arguments.setSupportsStartDebuggingRequest(true);
    initialize.setArguments(arguments);
    return initialize;
  }

  private SetBreakpointsRequest breakpointsRequest(Path fixture) {
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
    return setBreakpoints;
  }

  private static final int LOAD_BP_LINE = 3;
  private static final String WEB_LOAD_HX = """
    class WebLoad {
    	static function main() {
    		var marker = "before"; // LOAD_BP_LINE = 3
    		js.Browser.console.log(marker + "-loaded");
    	}
    }
    """;

  /**
   * Load-time code (main body, runs during page load) — the case the firefox
   * adapter needed the refresh-once trick for. js-debug pre-registers
   * breakpoints through CDP before scripts execute, so the FIRST load must
   * stop, with no serving tricks. Guards the family split in the backend
   * (no refreshFirstPage for chromium).
   */
  @Test(timeout = 60_000)
  public void loadTimeBreakpointHitsOnFirstLoad() throws Exception {
    Assume.assumeTrue("haxe not on PATH - skipping", haxeOnPath());
    Path fixture = Files.createTempDirectory("haxe-jsdbg-load");
    Files.writeString(fixture.resolve("WebLoad.hx"), WEB_LOAD_HX);
    Files.writeString(fixture.resolve("index.html"),
                      "<!DOCTYPE html><html><head><meta charset='utf-8'></head>"
                      + "<body><script src='app.js'></script></body></html>");
    LiveProbeUtil.compileHaxeJs(fixture, "WebLoad", "app.js");

    StackFrame top = driveSessionToStop(fixture, "WebLoad.hx", LOAD_BP_LINE);
    assertTrue("stopped in the load-time .hx line: " + top.getSource().getPath() + ":" + top.getLine(),
               top.getSource().getPath().endsWith("WebLoad.hx") && top.getLine() == LOAD_BP_LINE);
  }

  // WebSmart.hx line numbers are load-bearing: CALLS_LINE has TWO calls, the
  // smart-step material; F2_BODY_LINE is inside f2.
  private static final int CALLS_LINE = 8;
  private static final int F2_BODY_LINE = 4;
  private static final String WEB_SMART_HX = """
    class WebSmart {
    	static function f1(v:Int):Int { return v + 1; }

    	static function f2(v:Int):Int { return v * 2; } // F2_BODY_LINE = 4

    	static var counter = 0;
    	static function tick() {
    		counter = f2(f1(counter)); // CALLS_LINE = 8
    		js.Browser.console.log("t" + counter);
    	}
    	static function main() {
    		js.Browser.window.setInterval(tick, 250);
    	}
    }
    """;

  /**
   * Smart-step material: DAP stepInTargets on a line with two calls, then
   * stepIn with a chosen targetId. Also probes the "completions" request —
   * the runtime-truth fallback for identifiers the Haxe PSI cannot resolve
   * (browser globals behind incomplete externs).
   */
  @Test(timeout = 60_000)
  public void stepInTargetsAndRuntimeCompletions() throws Exception {
    Assume.assumeTrue("haxe not on PATH - skipping", haxeOnPath());
    Path fixture = Files.createTempDirectory("haxe-jsdbg-smart");
    Files.writeString(fixture.resolve("WebSmart.hx"), WEB_SMART_HX);
    Files.writeString(fixture.resolve("index.html"),
                      "<!DOCTYPE html><html><head><meta charset='utf-8'></head>"
                      + "<body><script src='app.js'></script></body></html>");
    LiveProbeUtil.compileHaxeJs(fixture, "WebSmart", "app.js");

    atStop = (child, top) -> {
      // --- stepInTargets on the two-call line ---
      StepInTargetsRequest targetsRequest =
        new StepInTargetsRequest();
      StepInTargetsArguments targetsArgs =
        new StepInTargetsArguments();
      targetsArgs.setFrameId(top.getId());
      targetsRequest.setArguments(targetsArgs);
      Response targetsResponse = child.sendRequest(targetsRequest, TIMEOUT);
      assertTrue("stepInTargets", targetsResponse.isSuccess());
      var targets = ((StepInTargetsResponse)
                       targetsResponse).getBody().getTargets();
      for (var target : targets) {
        System.out.println("[probe] stepInTarget id=" + target.getId() + " label=" + target.getLabel());
      }
      assertTrue("expected at least 2 step-in targets, got " + targets.size(), targets.size() >= 2);
      var f2Target = targets.stream().filter(t -> t.getLabel() != null && t.getLabel().contains("f2"))
        .findFirst()
        .orElseThrow(() -> new AssertionError("no f2 target among the labels"));

      // --- runtime completions: 'docum' must complete to the browser global ---
      CompletionsRequest completions =
        new CompletionsRequest();
      CompletionsArguments completionsArgs =
        new CompletionsArguments();
      completionsArgs.setFrameId(top.getId());
      completionsArgs.setText("docum");
      completionsArgs.setColumn(6); // 1-based caret after the text
      completions.setArguments(completionsArgs);
      Response completionsResponse = child.sendRequest(completions, TIMEOUT);
      assertTrue("completions", completionsResponse.isSuccess());
      var items = ((CompletionsResponse)
                     completionsResponse).getBody().getTargets();
      System.out.println("[probe] completions for 'docum': "
                         + items.stream().limit(8).map(i -> i.getLabel()).toList());
      assertTrue("expected 'document' among runtime completions",
                 items.stream().anyMatch(i -> "document".equals(i.getLabel())));

      // --- smart step INTO f2 (the outer call) ---
      StepInRequest stepIn =
        new StepInRequest();
      StepInArguments stepInArgs =
        new StepInArguments();
      stepInArgs.setThreadId(currentThreadId);
      stepInArgs.setTargetId(f2Target.getId());
      stepIn.setArguments(stepInArgs);
      assertTrue("targeted stepIn", child.sendRequest(stepIn, TIMEOUT).isSuccess());

      StoppedEvent landed = null;
      long deadline = System.currentTimeMillis() + 20_000;
      while (System.currentTimeMillis() < deadline && landed == null) {
        if (child.pollEvent(250) instanceof StoppedEvent s) {
          landed = s;
        }
      }
      assertNotNull("no stop after targeted stepIn", landed);
      StackTraceRequest stackTrace = new StackTraceRequest();
      StackTraceArguments stArgs = new StackTraceArguments();
      stArgs.setThreadId(currentThreadId);
      stackTrace.setArguments(stArgs);
      Response stResponse = child.sendRequest(stackTrace, TIMEOUT);
      StackFrame landedTop = ((StackTraceResponse)stResponse).getBody().getStackFrames().get(0);
      System.out.println("[probe] smart-step landed: " + landedTop.getName() + " @ "
                         + (landedTop.getSource() != null ? landedTop.getSource().getPath() : "?")
                         + ":" + landedTop.getLine());
      assertTrue("landed in f2's body line, got line " + landedTop.getLine(),
                 landedTop.getLine() == F2_BODY_LINE);
    };
    try {
      driveSessionToStop(fixture, "WebSmart.hx", CALLS_LINE);
    } finally {
      atStop = null;
    }
  }

  /**
   * Replicates the IDE's EXACT child-session sequence (DapDebugProcess):
   * initialize with adapterID intellij-haxe and NO supportsStartDebuggingRequest,
   * fire-and-forget launch, await initialized, setBreakpoints,
   * setExceptionBreakpoints(["uncaught"]), configurationDone — then at the stop:
   * threads, stackTrace, and the stepInTargets the smart-step handler sends.
   * Exists because the IDE reported no step-in chooser while the probe's own
   * sequence got targets fine — this pins whether the SEQUENCE is the culprit.
   */
  @Test(timeout = 60_000)
  public void stepInTargetsUnderTheIdeExactSequence() throws Exception {
    Assume.assumeTrue("haxe not on PATH - skipping", haxeOnPath());
    Path fixture = Files.createTempDirectory("haxe-jsdbg-idelike");
    Files.writeString(fixture.resolve("WebSmart.hx"), WEB_SMART_HX);
    Files.writeString(fixture.resolve("index.html"),
                      "<!DOCTYPE html><html><head><meta charset='utf-8'></head>"
                      + "<body><script src='app.js'></script></body></html>");
    LiveProbeUtil.compileHaxeJs(fixture, "WebSmart", "app.js");

    try (ContentHttpServer content = new ContentHttpServer(fixture)) {
      // --- parent exactly as BrowserDebugBackend.runParentHandshake ---
      assertTrue("parent initialize", parent.sendRequest(initializeRequest(), TIMEOUT).isSuccess());
      Map<String, Object> parentConfig = new LinkedHashMap<>();
      parentConfig.put("type", "pwa-chrome");
      parentConfig.put("request", "launch");
      parentConfig.put("name", "IntelliJ Haxe browser session");
      parentConfig.put("url", content.getBaseUrl());
      parentConfig.put("webRoot", fixture.toString());
      parentConfig.put("runtimeExecutable", chromiumExe().toString());
      parentConfig.put("runtimeArgs", List.of("--headless=new"));
      parent.sendRequestNoWait(ConfiguredLaunchRequest.of(parentConfig));
      StartDebuggingRequest startDebugging = null;
      long deadline = System.currentTimeMillis() + 20_000;
      while (System.currentTimeMillis() < deadline && startDebugging == null) {
        Event event = parent.pollEvent(100);
        if (event instanceof InitializedEvent) {
          parent.sendRequest(new ConfigurationDoneRequest(), TIMEOUT);
        }
        Request incoming = parent.pollIncomingRequest(50);
        if (incoming != null) {
          parent.respond(incoming, true);
          if (incoming instanceof StartDebuggingRequest start) {
            startDebugging = start;
          }
        }
      }
      assertNotNull("no startDebugging", startDebugging);

      try (DapClient child = connectWithRetry(adapterPort)) {
        // --- child exactly as DapDebugProcess.initializeSession ---
        InitializeRequest initialize = new InitializeRequest();
        InitializeRequestArguments initArgs = new InitializeRequestArguments();
        initArgs.setAdapterID("intellij-haxe");
        initArgs.setClientID("intellij");
        initArgs.setPathFormat("path");
        initArgs.setLinesStartAt1(true);
        initArgs.setColumnsStartAt1(true);
        initialize.setArguments(initArgs);
        assertTrue("child initialize", child.sendRequest(initialize, TIMEOUT).isSuccess());
        child.sendRequestNoWait(ConfiguredLaunchRequest.of(startDebugging.getArguments().getConfiguration()));
        // awaitInitializedEvent
        deadline = System.currentTimeMillis() + 15_000;
        boolean initialized = false;
        while (System.currentTimeMillis() < deadline && !initialized) {
          initialized = child.pollEvent(250) instanceof InitializedEvent;
        }
        assertTrue("child initialized", initialized);
        // flushAll
        SetBreakpointsRequest setBreakpoints = new SetBreakpointsRequest();
        SetBreakpointsArguments bpArgs = new SetBreakpointsArguments();
        Source source = new Source();
        source.setPath(fixture.resolve("WebSmart.hx").toString());
        source.setName("WebSmart.hx");
        bpArgs.setSource(source);
        SourceBreakpoint bp = new SourceBreakpoint();
        bp.setLine(CALLS_LINE);
        bpArgs.setBreakpoints(List.of(bp));
        setBreakpoints.setArguments(bpArgs);
        assertTrue("child setBreakpoints", child.sendRequest(setBreakpoints, TIMEOUT).isSuccess());
        // exception filters as the IDE's exceptionFiltersRequest would send
        SetExceptionBreakpointsRequest filters =
          new SetExceptionBreakpointsRequest();
        SetExceptionBreakpointsArguments filterArgs =
          new SetExceptionBreakpointsArguments();
        filterArgs.setFilters(List.of("uncaught"));
        filters.setArguments(filterArgs);
        System.out.println("[probe] ide-seq setExceptionBreakpoints success="
                           + child.sendRequest(filters, TIMEOUT).isSuccess());
        assertTrue("child configurationDone", child.sendRequest(new ConfigurationDoneRequest(), TIMEOUT).isSuccess());

        StoppedEvent stopped = null;
        deadline = System.currentTimeMillis() + 20_000;
        while (System.currentTimeMillis() < deadline && stopped == null) {
          if (child.pollEvent(250) instanceof StoppedEvent s) {
            stopped = s;
          }
        }
        assertNotNull("breakpoint never hit", stopped);
        int threadId = stopped.getBody().getThreadId() != null ? stopped.getBody().getThreadId() : 1;

        // reportStopped order: threads THEN stackTrace
        assertTrue("threads", child.sendRequest(new ThreadsRequest(), TIMEOUT).isSuccess());
        StackTraceRequest stackTrace = new StackTraceRequest();
        StackTraceArguments stArgs = new StackTraceArguments();
        stArgs.setThreadId(threadId);
        stackTrace.setArguments(stArgs);
        Response stResponse = child.sendRequest(stackTrace, TIMEOUT);
        StackFrame top = ((StackTraceResponse)stResponse).getBody().getStackFrames().get(0);

        // --- staged stepInTargets: which IDE behaviour kills the targets? ---
        System.out.println("[probe] ide-seq top frame id=" + top.getId());
        int n1 = stepInTargetsCount(child, top.getId(), "1:immediately");

        // stage 2: hydrate the views like the IDE (scopes + variables)
        ScopesRequest scopes =
          new ScopesRequest();
        ScopesArguments scArgs =
          new ScopesArguments();
        scArgs.setFrameId(top.getId());
        scopes.setArguments(scArgs);
        Response scResponse = child.sendRequest(scopes, TIMEOUT);
        if (scResponse instanceof ScopesResponse okScopes
            && okScopes.getBody() != null && !okScopes.getBody().getScopes().isEmpty()) {
          VariablesRequest variables =
            new VariablesRequest();
          VariablesArguments vArgs =
            new VariablesArguments();
          vArgs.setVariablesReference(okScopes.getBody().getScopes().get(0).getVariablesReference());
          variables.setArguments(vArgs);
          child.sendRequest(variables, TIMEOUT);
        }
        int n2 = stepInTargetsCount(child, top.getId(), "2:after scopes+variables");

        // stage 3: a SECOND stackTrace (frames view refresh) - do ids change?
        Response stResponse2 = child.sendRequest(stackTrace, TIMEOUT);
        StackFrame top2 = ((StackTraceResponse)stResponse2).getBody().getStackFrames().get(0);
        System.out.println("[probe] ide-seq second stackTrace top id=" + top2.getId()
                           + " (was " + top.getId() + ")");
        int n3 = stepInTargetsCount(child, top2.getId(), "3:fresh frame id");
        int n3old = stepInTargetsCount(child, top.getId(), "3b:old frame id after re-stackTrace");

        // stage 4: wall time
        Thread.sleep(4_000);
        int n4 = stepInTargetsCount(child, top2.getId(), "4:after 4s delay");

        System.out.println("[probe] ide-seq counts: immediate=" + n1 + " hydrated=" + n2
                           + " freshId=" + n3 + " oldIdAfterRefresh=" + n3old + " delayed=" + n4);
        assertTrue("immediate stepInTargets must find the calls", n1 >= 2);

        // stage 5: SECOND pause (the user's failing case had frameId=3 - ids
        // increment across pauses, so their stop was not the first). continue,
        // let the ticking fixture re-hit the same line, ask again.
        ContinueRequest resume =
          new ContinueRequest();
        ContinueArguments cArgs =
          new ContinueArguments();
        cArgs.setThreadId(threadId);
        resume.setArguments(cArgs);
        assertTrue("continue", child.sendRequest(resume, TIMEOUT).isSuccess());
        StoppedEvent second = null;
        deadline = System.currentTimeMillis() + 15_000;
        while (System.currentTimeMillis() < deadline && second == null) {
          if (child.pollEvent(250) instanceof StoppedEvent s) {
            second = s;
          }
        }
        assertNotNull("no second stop", second);
        int threadId2 = second.getBody().getThreadId() != null ? second.getBody().getThreadId() : threadId;
        StackTraceRequest stackTrace3 = new StackTraceRequest();
        StackTraceArguments stArgs3 = new StackTraceArguments();
        stArgs3.setThreadId(threadId2);
        stackTrace3.setArguments(stArgs3);
        Response stResponse3 = child.sendRequest(stackTrace3, TIMEOUT);
        StackFrame top3 = ((StackTraceResponse)stResponse3).getBody().getStackFrames().get(0);
        System.out.println("[probe] ide-seq SECOND PAUSE top id=" + top3.getId()
                           + " @ " + (top3.getSource() != null ? top3.getSource().getPath() : "?")
                           + ":" + top3.getLine());
        int n5 = stepInTargetsCount(child, top3.getId(), "5:second pause");
        System.out.println("[probe] ide-seq second-pause targets=" + n5);
        assertTrue("second-pause stepInTargets must find the calls too (user's failing case)", n5 >= 2);
        child.sendRequest(new DisconnectRequest(), TIMEOUT);
      }
    }
  }

  // WebClick.hx line numbers are load-bearing: CLICK_CALLS_LINE is the
  // two-call line INSIDE A DOM EVENT HANDLER - the user's exact failing case.
  private static final int CLICK_CALLS_LINE = 8;
  private static final String WEB_CLICK_HX = """
    class WebClick {
    	static function abc(i:Int):Int { return i; }

    	static function cde(i:Int):Int { return i; }

    	static function onClick(event:Dynamic) {
    		var x = 1;
    		abc(cde(1)); // CLICK_CALLS_LINE = 8
    		x++;
    	}

    	static function main() {
    		var b = js.Browser.document.createButtonElement();
    		b.onclick = onClick;
    		js.Browser.document.body.appendChild(b);
    		js.Browser.window.setTimeout(() -> b.click(), 600);
    	}
    }
    """;

  // A/B confound check: the WORKING fixture's line is an ASSIGNMENT
  // (`counter = f2(f1(...))`), the failing one a BARE expression statement
  // (`abc(cde(1));`) - same as the user's. Same timer delivery as the
  // working fixture, only the statement shape differs.
  private static final int EXPR_CALLS_LINE = 9;
  private static final String WEB_EXPR_HX = """
    class WebExpr {
    	static function f1(v:Int):Int { return v + 1; }

    	static function f2(v:Int):Int { return v * 2; }

    	static var counter = 0;
    	static function tick() {
    		var x = 1;
    		f2(f1(counter)); // EXPR_CALLS_LINE = 9
    		counter++;
    	}
    	static function main() {
    		js.Browser.window.setInterval(tick, 250);
    	}
    }
    """;

  @Test(timeout = 60_000)
  public void stepInTargetsOnABareExpressionStatement() throws Exception {
    Assume.assumeTrue("haxe not on PATH - skipping", haxeOnPath());
    Path fixture = Files.createTempDirectory("haxe-jsdbg-expr");
    Files.writeString(fixture.resolve("WebExpr.hx"), WEB_EXPR_HX);
    Files.writeString(fixture.resolve("index.html"),
                      "<!DOCTYPE html><html><head><meta charset='utf-8'></head>"
                      + "<body><script src='app.js'></script></body></html>");
    LiveProbeUtil.compileHaxeJs(fixture, "WebExpr", "app.js");

    final int[] targetsAtStop = {-2};
    atStop = (child, top) -> targetsAtStop[0] = stepInTargetsCount(child, top.getId(), "bare-expression stop");
    try {
      driveSessionToStop(fixture, "WebExpr.hx", EXPR_CALLS_LINE);
    } finally {
      atStop = null;
    }
    System.out.println("[probe] bare-expression (timer) stepInTargets count=" + targetsAtStop[0]);
    // diagnostic: no assert on the count - the pairing with the event-handler
    // test tells whether the trigger is the statement shape or the delivery
  }

  // Same fixture with the call line as the LAST statement (next line is the
  // unmapped closing brace) - the shape that breaks js-debug's target lookup.
  private static final String WEB_CLICK_LAST_HX = WEB_CLICK_HX.replace("\tx++;\n", "");

  /**
   * UPSTREAM LIMITATION, pinned: when the multi-call line is the LAST
   * statement of its function, js-debug's getStepInTargets reverse-maps the
   * line AND line+1; the closing-brace line has no source-map entries, the
   * sibling counts differ, and it bails to [] with the internal warning
   * "Expected to have the same number of start and end locations" (no CDP is
   * even consulted). Diagnosed via trace logs; the IDE then falls back to a
   * plain step into. If a future js-debug pin fixes this, THIS TEST FAILS -
   * celebrate and delete it.
   */
  @Test(timeout = 60_000)
  public void stepInTargetsKnownLimitationOnLastStatementOfFunction() throws Exception {
    Assume.assumeTrue("haxe not on PATH - skipping", haxeOnPath());
    Path fixture = Files.createTempDirectory("haxe-jsdbg-lastline");
    Files.writeString(fixture.resolve("WebClick.hx"), WEB_CLICK_LAST_HX);
    Files.writeString(fixture.resolve("index.html"),
                      "<!DOCTYPE html><html><head><meta charset='utf-8'></head>"
                      + "<body><script src='app.js'></script></body></html>");
    LiveProbeUtil.compileHaxeJs(fixture, "WebClick", "app.js");

    final int[] targetsAtStop = {-2};
    atStop = (child, top) -> targetsAtStop[0] = stepInTargetsCount(child, top.getId(), "last-statement stop");
    try {
      driveSessionToStop(fixture, "WebClick.hx", CLICK_CALLS_LINE);
    } finally {
      atStop = null;
    }
    System.out.println("[probe] last-statement stepInTargets count=" + targetsAtStop[0]);
    assertTrue("js-debug currently yields NO targets for a last-statement line (upstream limitation);"
               + " if this failed with a count >= 2, the pin fixed it - remove the limitation",
               targetsAtStop[0] == 0);
  }

  /**
   * Smart-step INSIDE A DOM EVENT HANDLER works when the call line is
   * followed by another mapped statement (the general case).
   */
  @Test(timeout = 60_000)
  public void stepInTargetsInsideADomEventHandler() throws Exception {
    Assume.assumeTrue("haxe not on PATH - skipping", haxeOnPath());
    Path fixture = Files.createTempDirectory("haxe-jsdbg-click");
    Files.writeString(fixture.resolve("WebClick.hx"), WEB_CLICK_HX);
    Files.writeString(fixture.resolve("index.html"),
                      "<!DOCTYPE html><html><head><meta charset='utf-8'></head>"
                      + "<body><script src='app.js'></script></body></html>");
    LiveProbeUtil.compileHaxeJs(fixture, "WebClick", "app.js");

    final int[] targetsAtStop = {-2};
    atStop = (child, top) -> targetsAtStop[0] = stepInTargetsCount(child, top.getId(), "event-handler stop");
    try {
      StackFrame top = driveSessionToStop(fixture, "WebClick.hx", CLICK_CALLS_LINE);
      assertTrue("stopped on the handler's call line", top.getLine() == CLICK_CALLS_LINE);
    } finally {
      atStop = null;
    }
    System.out.println("[probe] event-handler stepInTargets count=" + targetsAtStop[0]);
    assertTrue("stepInTargets inside a DOM event handler must find the calls (user's case), got "
               + targetsAtStop[0], targetsAtStop[0] >= 2);
  }

  // WorkerMain.hx line numbers are load-bearing: WORKER_BP_LINE ticks forever,
  // so a breakpoint replayed slightly after the worker attaches still hits.
  private static final int WORKER_BP_LINE = 4;
  private static final String WORKER_MAIN_HX = """
    class WorkerMain {
    	static var counter = 0;
    	static function tick() {
    		counter++; // WORKER_BP_LINE = 4
    		js.Syntax.code("console.log({0})", "w" + counter);
    	}
    	static function main() {
    		js.Syntax.code("setInterval({0}, {1})", tick, 250);
    	}
    }
    """;
  private static final String WEB_PAGE_HX = """
    class WebPage {
    	static var beats = 0;
    	static function heartbeat() {
    		beats++;
    	}
    	static function main() {
    		var worker = new js.html.Worker("worker.js");
    		js.Browser.console.log("worker spawned: " + (worker != null));
    		js.Browser.window.setInterval(heartbeat, 300);
    	}
    }
    """;

  /** Ids from a k>0 session carry the session index above this. */
  private static final int COMPOSITE_FLOOR = 1 << 24;

  /**
   * Workers-as-threads: the {@link JsDebugSessionMux} auto-attaches the worker
   * session js-debug announces via {@code startDebugging} on the PAGE
   * connection, replays the cached breakpoint (set through the mux BEFORE the
   * worker existed), and surfaces the worker's stop as a COMPOSITE thread id
   * in the one merged session. Pins the id round-trip the IDE relies on:
   * stackTrace by composite threadId, scopes/variables by composite
   * frameId/variablesReference, merged threads listing both targets, and
   * continue routed back to the worker.
   */
  @Test(timeout = 60_000)
  public void workerAppearsAsAdditionalThreadThroughTheMux() throws Exception {
    Assume.assumeTrue("haxe not on PATH - skipping", haxeOnPath());
    Path fixture = Files.createTempDirectory("haxe-jsdbg-worker");
    Files.writeString(fixture.resolve("WebPage.hx"), WEB_PAGE_HX);
    Files.writeString(fixture.resolve("WorkerMain.hx"), WORKER_MAIN_HX);
    Files.writeString(fixture.resolve("index.html"),
                      "<!DOCTYPE html><html><head><meta charset='utf-8'></head>"
                      + "<body><script src='app.js'></script></body></html>");
    LiveProbeUtil.compileHaxeJs(fixture, "WebPage", "app.js");
    LiveProbeUtil.compileHaxeJs(fixture, "WorkerMain", "worker.js");

    try (ContentHttpServer content = new ContentHttpServer(fixture)) {
      // --- parent handshake exactly as BrowserDebugBackend.runParentHandshake ---
      assertTrue("parent initialize", parent.sendRequest(initializeRequest(), TIMEOUT).isSuccess());
      Map<String, Object> parentConfig = new LinkedHashMap<>();
      parentConfig.put("type", "pwa-chrome");
      parentConfig.put("request", "launch");
      parentConfig.put("name", "probe");
      parentConfig.put("url", content.getBaseUrl());
      parentConfig.put("webRoot", fixture.toString());
      parentConfig.put("runtimeExecutable", chromiumExe().toString());
      parentConfig.put("runtimeArgs", List.of("--headless=new"));
      parent.sendRequestNoWait(ConfiguredLaunchRequest.of(parentConfig));
      StartDebuggingRequest startDebugging = null;
      long deadline = System.currentTimeMillis() + 20_000;
      while (System.currentTimeMillis() < deadline && startDebugging == null) {
        Event event = parent.pollEvent(100);
        if (event instanceof InitializedEvent) {
          parent.sendRequest(new ConfigurationDoneRequest(), TIMEOUT);
        }
        Request incoming = parent.pollIncomingRequest(50);
        if (incoming != null) {
          parent.respond(incoming, true);
          if (incoming instanceof StartDebuggingRequest start) {
            startDebugging = start;
          }
        }
      }
      assertNotNull("no startDebugging for the page", startDebugging);

      DapClient page = connectWithRetry(adapterPort);
      try (JsDebugSessionMux mux = new JsDebugSessionMux(parent, page, adapterPort)) {
        mux.setLogSink(line -> System.out.println("[mux] " + line));

        // --- page handshake THROUGH the mux, as DapDebugProcess drives it ---
        assertTrue("page initialize", mux.sendRequest(initializeRequest(), TIMEOUT).isSuccess());
        mux.sendRequestNoWait(ConfiguredLaunchRequest.of(startDebugging.getArguments().getConfiguration()));
        boolean initialized = false;
        deadline = System.currentTimeMillis() + 15_000;
        while (System.currentTimeMillis() < deadline && !initialized) {
          initialized = mux.pollEvent(250) instanceof InitializedEvent;
        }
        assertTrue("page initialized", initialized);

        // breakpoint in the WORKER's source while no worker session exists yet:
        // the mux must cache it and replay it into the worker as it attaches
        SetBreakpointsRequest setBreakpoints = new SetBreakpointsRequest();
        SetBreakpointsArguments bpArgs = new SetBreakpointsArguments();
        Source source = new Source();
        source.setPath(fixture.resolve("WorkerMain.hx").toString());
        source.setName("WorkerMain.hx");
        bpArgs.setSource(source);
        SourceBreakpoint bp = new SourceBreakpoint();
        bp.setLine(WORKER_BP_LINE);
        bpArgs.setBreakpoints(List.of(bp));
        setBreakpoints.setArguments(bpArgs);
        Response bpResponse = mux.sendRequest(setBreakpoints, TIMEOUT);
        assertTrue("setBreakpoints via mux", bpResponse.isSuccess());
        var bpResult = ((SetBreakpointsResponse)
                          bpResponse).getBody().getBreakpoints().get(0);
        Integer pageBreakpointId = bpResult.getId();
        System.out.println("[probe] worker-source bp before worker exists: id=" + pageBreakpointId
                           + " verified=" + bpResult.isVerified());
        assertTrue("configurationDone via mux",
                   mux.sendRequest(new ConfigurationDoneRequest(), TIMEOUT).isSuccess());

        // page loads -> spawns the worker -> js-debug announces it on the page
        // connection -> the mux attaches it -> the replayed breakpoint stops it.
        // On the way, the mux must announce the MERGED verification upgrade
        // under the page's breakpoint id (the gutter's lazy checkmark).
        StoppedEvent stopped = null;
        boolean verifiedUpgradeSeen = false;
        deadline = System.currentTimeMillis() + 20_000;
        while (System.currentTimeMillis() < deadline && stopped == null) {
          Event event = mux.pollEvent(250);
          if (event instanceof BreakpointEvent be
              && be.getBody() != null && be.getBody().getBreakpoint() != null) {
            var state = be.getBody().getBreakpoint();
            System.out.println("[probe] breakpoint event: id=" + state.getId() + " verified=" + state.isVerified());
            if (state.isVerified() && state.getId() != null && state.getId().equals(pageBreakpointId)) {
              verifiedUpgradeSeen = true;
            }
          }
          if (event instanceof StoppedEvent s) {
            stopped = s;
          }
        }
        assertNotNull("worker breakpoint never hit through the mux", stopped);
        assertTrue("the worker's verification must surface as a breakpoint event on the PAGE id "
                   + pageBreakpointId + " (gutter checkmark)", verifiedUpgradeSeen);
        Integer threadId = stopped.getBody().getThreadId();
        assertNotNull("stop without a thread id", threadId);
        assertTrue("the stop must come from a WORKER session (composite thread id), got " + threadId,
                   threadId >= COMPOSITE_FLOOR);

        // stackTrace routed by the composite thread id
        StackTraceRequest stackTrace = new StackTraceRequest();
        StackTraceArguments stArgs = new StackTraceArguments();
        stArgs.setThreadId(threadId);
        stackTrace.setArguments(stArgs);
        Response stResponse = mux.sendRequest(stackTrace, TIMEOUT);
        assertTrue("stackTrace via composite thread id", stResponse.isSuccess());
        StackFrame top = ((StackTraceResponse)stResponse).getBody().getStackFrames().get(0);
        System.out.println("[probe] worker top frame: " + top.getName() + " @ "
                           + (top.getSource() != null ? top.getSource().getPath() : "?") + ":" + top.getLine());
        assertNotNull("worker frame has no source", top.getSource());
        assertTrue("stopped in the worker's .hx line: " + top.getSource().getPath() + ":" + top.getLine(),
                   top.getSource().getPath().endsWith("WorkerMain.hx") && top.getLine() == WORKER_BP_LINE);
        assertTrue("frame id must be composited, got " + top.getId(), top.getId() >= COMPOSITE_FLOOR);

        // scopes by composite frame id -> composited variablesReference -> variables
        ScopesRequest scopes =
          new ScopesRequest();
        ScopesArguments scArgs =
          new ScopesArguments();
        scArgs.setFrameId(top.getId());
        scopes.setArguments(scArgs);
        Response scResponse = mux.sendRequest(scopes, TIMEOUT);
        assertTrue("scopes via composite frame id", scResponse.isSuccess());
        var scopeList = ((ScopesResponse)
                           scResponse).getBody().getScopes();
        assertTrue("no scopes", !scopeList.isEmpty());
        int varRef = scopeList.get(0).getVariablesReference();
        assertTrue("scope variablesReference must be composited, got " + varRef, varRef >= COMPOSITE_FLOOR);
        VariablesRequest variables =
          new VariablesRequest();
        VariablesArguments vArgs =
          new VariablesArguments();
        vArgs.setVariablesReference(varRef);
        variables.setArguments(vArgs);
        assertTrue("variables via composite reference", mux.sendRequest(variables, TIMEOUT).isSuccess());

        // the merged thread list carries the page AND the labelled worker
        Response threadsResponse = mux.sendRequest(
          new ThreadsRequest(), TIMEOUT);
        assertTrue("merged threads", threadsResponse.isSuccess());
        var threads = ((ThreadsResponse)
                         threadsResponse).getBody().getThreads();
        for (var thread : threads) {
          System.out.println("[probe] merged thread id=" + thread.getId() + " name=" + thread.getName());
        }
        assertTrue("merged threads must include the page (raw id)",
                   threads.stream().anyMatch(t -> t.getId() < COMPOSITE_FLOOR));
        assertTrue("merged threads must include the worker (composite id, named after its script)",
                   threads.stream().anyMatch(t -> t.getId() >= COMPOSITE_FLOOR
                                                  && t.getName() != null && t.getName().contains("worker.js")));

        // continue routes back to the worker's session
        ContinueRequest resume =
          new ContinueRequest();
        ContinueArguments cArgs =
          new ContinueArguments();
        cArgs.setThreadId(threadId);
        resume.setArguments(cArgs);
        assertTrue("continue via composite thread id", mux.sendRequest(resume, TIMEOUT).isSuccess());

        // the ticking worker re-hits: the composite ids are stable across stops
        StoppedEvent second = null;
        deadline = System.currentTimeMillis() + 15_000;
        while (System.currentTimeMillis() < deadline && second == null) {
          if (mux.pollEvent(250) instanceof StoppedEvent s) {
            second = s;
          }
        }
        assertNotNull("no second worker stop after continue", second);
        assertTrue("second stop must be composite too",
                   second.getBody().getThreadId() != null && second.getBody().getThreadId() >= COMPOSITE_FLOOR);

        // --- multi-pause routing: pause the PAGE while the worker stays paused ---
        int pageThreadId = threads.stream().filter(t -> t.getId() < COMPOSITE_FLOOR)
          .findFirst()
          .orElseThrow()
          .getId();
        PauseRequest pause =
          new PauseRequest();
        PauseArguments pArgs =
          new PauseArguments();
        pArgs.setThreadId(pageThreadId);
        pause.setArguments(pArgs);
        assertTrue("pause the page thread", mux.sendRequest(pause, TIMEOUT).isSuccess());
        StoppedEvent pageStop = null;
        deadline = System.currentTimeMillis() + 15_000;
        while (System.currentTimeMillis() < deadline && pageStop == null) {
          if (mux.pollEvent(250) instanceof StoppedEvent s) {
            pageStop = s;
          }
        }
        assertNotNull("page never paused", pageStop);
        assertTrue("the pause stop must be the PAGE's (raw thread id), got " + pageStop.getBody().getThreadId(),
                   pageStop.getBody().getThreadId() != null && pageStop.getBody().getThreadId() < COMPOSITE_FLOOR);

        // a step routed to the PAGE must stop in the PAGE, never the worker
        // (the IDE bug this pins: stepping after switching threads)
        NextRequest next =
          new NextRequest();
        NextArguments nArgs =
          new NextArguments();
        nArgs.setThreadId(pageThreadId);
        next.setArguments(nArgs);
        assertTrue("step the page thread", mux.sendRequest(next, TIMEOUT).isSuccess());
        StoppedEvent stepStop = null;
        deadline = System.currentTimeMillis() + 15_000;
        while (System.currentTimeMillis() < deadline && stepStop == null) {
          if (mux.pollEvent(250) instanceof StoppedEvent s) {
            stepStop = s;
          }
        }
        assertNotNull("no stop after stepping the page", stepStop);
        assertTrue("the step must land in the PAGE thread, got " + stepStop.getBody().getThreadId(),
                   stepStop.getBody().getThreadId() != null && stepStop.getBody().getThreadId() < COMPOSITE_FLOOR);

        // continue routed to the PAGE must leave the paused worker untouched
        // (the IDE holds the worker's stop back and presents it after this
        // resume - releasing it here would run it away before the user sees
        // its breakpoint). The ticking worker would re-hit within ~250ms if
        // it were resumed; observing silence pins that it stayed paused.
        ContinueRequest pageResume =
          new ContinueRequest();
        ContinueArguments caArgs =
          new ContinueArguments();
        caArgs.setThreadId(pageThreadId);
        pageResume.setArguments(caArgs);
        assertTrue("resume via the page thread", mux.sendRequest(pageResume, TIMEOUT).isSuccess());
        deadline = System.currentTimeMillis() + 4_000;
        while (System.currentTimeMillis() < deadline) {
          if (mux.pollEvent(250) instanceof StoppedEvent s && s.getBody().getThreadId() != null
              && s.getBody().getThreadId() >= COMPOSITE_FLOOR) {
            throw new AssertionError("the page-routed continue must NOT release the paused worker,"
                                     + " but its ticking breakpoint re-hit");
          }
        }

        // a continue routed to the WORKER releases it - the bp re-hits
        ContinueRequest workerResume =
          new ContinueRequest();
        ContinueArguments wcArgs =
          new ContinueArguments();
        wcArgs.setThreadId(second.getBody().getThreadId());
        workerResume.setArguments(wcArgs);
        assertTrue("resume via the worker thread", mux.sendRequest(workerResume, TIMEOUT).isSuccess());
        StoppedEvent workerAgain = null;
        deadline = System.currentTimeMillis() + 15_000;
        while (System.currentTimeMillis() < deadline && workerAgain == null) {
          if (mux.pollEvent(250) instanceof StoppedEvent s && s.getBody().getThreadId() != null
              && s.getBody().getThreadId() >= COMPOSITE_FLOOR) {
            workerAgain = s;
          }
        }
        assertNotNull("the worker-routed continue must release the worker (bp re-hit)", workerAgain);

        mux.sendRequest(new DisconnectRequest(), TIMEOUT);
      }
    }
  }

  private int stepInTargetsCount(DapClient child, int frameId, String stage) throws Exception {
    StepInTargetsRequest request =
      new StepInTargetsRequest();
    StepInTargetsArguments arguments =
      new StepInTargetsArguments();
    arguments.setFrameId(frameId);
    request.setArguments(arguments);
    Response response = child.sendRequest(request, TIMEOUT);
    int count = response instanceof StepInTargetsResponse ok
                && ok.isSuccess() && ok.getBody() != null && ok.getBody().getTargets() != null
                ? ok.getBody().getTargets().size() : -1;
    System.out.println("[probe] ide-seq stage " + stage + " frameId=" + frameId + " -> targets=" + count
                       + (response.isSuccess() ? "" : " message=" + response.getMessage()));
    return count;
  }

  /** Optional at-stop hook for probes needing extra requests before disconnect. */
  private interface AtStop {
    void run(DapClient child, StackFrame top) throws Exception;
  }

  private volatile AtStop atStop;
  /** When set, launch configs carry js-debug's "trace": true and output events are printed. */
  private volatile boolean traceAdapter;
  private volatile int currentThreadId;

  /** The parent+child flow, shared by the probes; returns the stop's top frame. */
  private StackFrame driveSessionToStop(Path fixture, String bpFileName, int bpLine) throws Exception {
    try (ContentHttpServer content = new ContentHttpServer(fixture)) {
      assertTrue("parent initialize", parent.sendRequest(initializeRequest(), TIMEOUT).isSuccess());

      Map<String, Object> launchConfig = new LinkedHashMap<>();
      launchConfig.put("type", "pwa-chrome");
      launchConfig.put("request", "launch");
      launchConfig.put("name", "probe");
      launchConfig.put("url", content.getBaseUrl());
      launchConfig.put("webRoot", fixture.toString());
      launchConfig.put("runtimeExecutable", chromiumExe().toString());
      launchConfig.put("runtimeArgs", List.of("--headless=new"));
      if (traceAdapter) {
        launchConfig.put("trace", true);
      }
      parent.sendRequestNoWait(ConfiguredLaunchRequest.of(launchConfig));

      StartDebuggingRequest startDebugging = null;
      long deadline = System.currentTimeMillis() + 20_000;
      while (System.currentTimeMillis() < deadline && startDebugging == null) {
        Event event = parent.pollEvent(100);
        if (traceAdapter && event instanceof OutputEvent o && o.getBody() != null) {
          System.out.println("[trace-out] " + String.valueOf(o.getBody().getOutput()).trim());
        }
        if (event instanceof InitializedEvent) {
          parent.sendRequest(new ConfigurationDoneRequest(), TIMEOUT);
        }
        Request incoming = parent.pollIncomingRequest(50);
        if (incoming != null) {
          parent.respond(incoming, true);
          if (incoming instanceof StartDebuggingRequest start) {
            startDebugging = start;
          }
        }
      }
      assertNotNull("no startDebugging reverse request", startDebugging);

      try (DapClient child = connectWithRetry(adapterPort)) {
        Response childInit = child.sendRequest(initializeRequest(), TIMEOUT);
        assertTrue("child initialize", childInit.isSuccess());
        if (childInit instanceof InitializeResponse ir
            && ir.getBody() != null) {
          System.out.println("[probe] CHILD caps: completions=" + ir.getBody().getSupportsCompletionsRequest()
                             + " stepInTargets=" + ir.getBody().getSupportsStepInTargetsRequest());
        }
        Map<String, Object> childConfig = new LinkedHashMap<>(startDebugging.getArguments().getConfiguration());
        if (traceAdapter) {
          childConfig.put("trace", true);
        }
        child.sendRequestNoWait(ConfiguredLaunchRequest.of(childConfig));

        boolean childConfigured = false;
        StoppedEvent stopped = null;
        deadline = System.currentTimeMillis() + 20_000;
        while (System.currentTimeMillis() < deadline && stopped == null) {
          Event event = child.pollEvent(100);
          if (traceAdapter && event instanceof OutputEvent o && o.getBody() != null) {
            System.out.println("[trace-out] " + String.valueOf(o.getBody().getOutput()).trim());
          }
          if (event instanceof InitializedEvent && !childConfigured) {
            childConfigured = true;
            SetBreakpointsRequest setBreakpoints = new SetBreakpointsRequest();
            SetBreakpointsArguments bpArgs = new SetBreakpointsArguments();
            Source source = new Source();
            source.setPath(fixture.resolve(bpFileName).toString());
            source.setName(bpFileName);
            bpArgs.setSource(source);
            SourceBreakpoint bp = new SourceBreakpoint();
            bp.setLine(bpLine);
            bpArgs.setBreakpoints(List.of(bp));
            setBreakpoints.setArguments(bpArgs);
            assertTrue("child setBreakpoints", child.sendRequest(setBreakpoints, TIMEOUT).isSuccess());
            assertTrue("child configurationDone",
                       child.sendRequest(new ConfigurationDoneRequest(), TIMEOUT).isSuccess());
          }
          if (event instanceof StoppedEvent s) {
            stopped = s;
          }
        }
        assertNotNull("breakpoint never hit", stopped);
        int threadId = stopped.getBody().getThreadId() != null ? stopped.getBody().getThreadId() : 1;
        currentThreadId = threadId;
        StackTraceRequest stackTrace = new StackTraceRequest();
        StackTraceArguments stArgs = new StackTraceArguments();
        stArgs.setThreadId(threadId);
        stackTrace.setArguments(stArgs);
        Response stResponse = child.sendRequest(stackTrace, TIMEOUT);
        assertTrue("child stackTrace", stResponse.isSuccess());
        List<StackFrame> frames = ((StackTraceResponse)stResponse).getBody().getStackFrames();
        assertTrue("no frames", !frames.isEmpty());
        StackFrame top = frames.get(0);
        System.out.println("[probe] top frame: " + top.getName() + " @ "
                           + (top.getSource() != null ? top.getSource().getPath() : "?") + ":" + top.getLine());
        assertNotNull("top frame has no source", top.getSource());
        if (atStop != null) {
          atStop.run(child, top);
        }
        child.sendRequest(new DisconnectRequest(), TIMEOUT);
        return top;
      }
    }
  }

  @Test(timeout = 60_000)
  public void fullSessionWithChildViaStartDebugging() throws Exception {
    Assume.assumeTrue("haxe not on PATH - skipping", haxeOnPath());
    Path fixture = buildFixture();

    try (ContentHttpServer content = new ContentHttpServer(fixture)) {
      content.setRequestListener(line -> System.out.println("[server] " + line));

      // --- parent session ---
      Response initResponse = parent.sendRequest(initializeRequest(), TIMEOUT);
      System.out.println("[probe] parent initialize success=" + initResponse.isSuccess());
      assertTrue("parent initialize", initResponse.isSuccess());

      Map<String, Object> launchConfig = new LinkedHashMap<>();
      launchConfig.put("type", "pwa-chrome");
      launchConfig.put("request", "launch");
      launchConfig.put("name", "probe");
      launchConfig.put("url", content.getBaseUrl());
      launchConfig.put("webRoot", fixture.toString());
      launchConfig.put("runtimeExecutable", chromiumExe().toString());
      launchConfig.put("runtimeArgs", List.of("--headless=new"));

      // ORDERING QUESTION: js-debug may hold the launch response until
      // configurationDone - send launch on a helper thread and observe
      CompletableFuture<Response> launchFuture = new CompletableFuture<>();
      Thread launcher = new Thread(() -> {
        try {
          launchFuture.complete(parent.sendRequest(ConfiguredLaunchRequest.of(launchConfig), 20_000));
        } catch (Exception e) {
          launchFuture.completeExceptionally(e);
        }
      }, "probe-launch");
      launcher.setDaemon(true);
      launcher.start();

      // drive the parent: initialized -> breakpoints + configurationDone;
      // meanwhile watch for the startDebugging reverse request
      StartDebuggingRequest startDebugging = null;
      boolean parentConfigured = false;
      long deadline = System.currentTimeMillis() + 20_000;
      while (System.currentTimeMillis() < deadline && startDebugging == null) {
        Event event = parent.pollEvent(100);
        if (event != null) {
          System.out.println("[probe] parent event '" + event.getEvent() + "'"
                             + (event instanceof OutputEvent o ? " :: " + o.getBody().getOutput() : ""));
          if (event instanceof InitializedEvent && !parentConfigured) {
            parentConfigured = true;
            System.out.println("[probe] parent setBreakpoints success="
                               + parent.sendRequest(breakpointsRequest(fixture), TIMEOUT).isSuccess());
            System.out.println("[probe] parent configurationDone success="
                               + parent.sendRequest(new ConfigurationDoneRequest(), TIMEOUT).isSuccess());
          }
        }
        Request incoming = parent.pollIncomingRequest(50);
        if (incoming != null) {
          System.out.println("[probe] parent REVERSE request '" + incoming.getCommand() + "'");
          if (incoming instanceof StartDebuggingRequest start) {
            startDebugging = start;
            parent.respond(incoming, true);
          } else {
            parent.respond(incoming, true);
          }
        }
        if (launchFuture.isDone() && !launchFuture.isCompletedExceptionally()) {
          // just report once - the loop condition is the reverse request
        }
      }
      System.out.println("[probe] launch settled=" + launchFuture.isDone()
                         + " startDebugging=" + (startDebugging != null));
      assertNotNull("no startDebugging reverse request from js-debug", startDebugging);
      Map<String, Object> childConfig = startDebugging.getArguments().getConfiguration();
      System.out.println("[probe] child config keys=" + childConfig.keySet());

      // --- child session (second connection, config from the reverse request) ---
      try (DapClient child = connectWithRetry(adapterPort)) {
        assertTrue("child initialize", child.sendRequest(initializeRequest(), TIMEOUT).isSuccess());

        CompletableFuture<Response> childLaunchFuture = new CompletableFuture<>();
        Thread childLauncher = new Thread(() -> {
          try {
            childLaunchFuture.complete(child.sendRequest(ConfiguredLaunchRequest.of(childConfig), 20_000));
          } catch (Exception e) {
            childLaunchFuture.completeExceptionally(e);
          }
        }, "probe-child-launch");
        childLauncher.setDaemon(true);
        childLauncher.start();

        boolean childConfigured = false;
        StoppedEvent stopped = null;
        deadline = System.currentTimeMillis() + 20_000;
        while (System.currentTimeMillis() < deadline && stopped == null) {
          Event event = child.pollEvent(100);
          if (event == null) {
            continue;
          }
          System.out.println("[probe] child event '" + event.getEvent() + "'"
                             + (event instanceof StoppedEvent s ? " reason=" + s.getBody().getReason() : ""));
          if (event instanceof InitializedEvent && !childConfigured) {
            childConfigured = true;
            System.out.println("[probe] child launch settled BEFORE configuration: " + childLaunchFuture.isDone());
            System.out.println("[probe] child setBreakpoints success="
                               + child.sendRequest(breakpointsRequest(fixture), TIMEOUT).isSuccess());
            System.out.println("[probe] child configurationDone success="
                               + child.sendRequest(new ConfigurationDoneRequest(), TIMEOUT).isSuccess());
            System.out.println("[probe] child launch settled AFTER configurationDone: " + childLaunchFuture.isDone());
          }
          if (event instanceof StoppedEvent s) {
            stopped = s;
          }
        }
        assertNotNull("breakpoint never hit in the child session", stopped);
        System.out.println("[probe] at stop: parent launch settled=" + launchFuture.isDone()
                           + " child launch settled=" + childLaunchFuture.isDone());
        int threadId = stopped.getBody().getThreadId() != null ? stopped.getBody().getThreadId() : 1;

        StackTraceRequest stackTrace = new StackTraceRequest();
        StackTraceArguments stArgs = new StackTraceArguments();
        stArgs.setThreadId(threadId);
        stackTrace.setArguments(stArgs);
        Response stResponse = child.sendRequest(stackTrace, TIMEOUT);
        assertTrue("child stackTrace", stResponse.isSuccess());
        List<StackFrame> frames = ((StackTraceResponse)stResponse).getBody().getStackFrames();
        assertTrue("no frames", !frames.isEmpty());
        StackFrame top = frames.get(0);
        System.out.println("[probe] child top frame: " + top.getName() + " @ "
                           + (top.getSource() != null ? top.getSource().getPath() : "?") + ":" + top.getLine());
        assertNotNull("top frame has no source", top.getSource());
        assertTrue("top frame is not the .hx original: " + top.getSource().getPath(),
                   top.getSource().getPath() != null && top.getSource().getPath().endsWith("WebMain.hx"));
        assertTrue("wrong line: " + top.getLine(), top.getLine() == BP_LINE);

        child.sendRequest(new DisconnectRequest(), TIMEOUT);
      }
      parent.sendRequest(new DisconnectRequest(), TIMEOUT);
    }
  }
}
