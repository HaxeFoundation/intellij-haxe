package com.intellij.plugins.haxe.runner.debugger.browser;

import static com.intellij.plugins.haxe.runner.debugger.browser.LiveProbeUtil.assertStoppedInHx;
import static com.intellij.plugins.haxe.runner.debugger.browser.LiveProbeUtil.awaitStopped;
import static com.intellij.plugins.haxe.runner.debugger.browser.LiveProbeUtil.continueRequest;
import static com.intellij.plugins.haxe.runner.debugger.browser.LiveProbeUtil.exceptionBreakpointsRequest;
import static com.intellij.plugins.haxe.runner.debugger.browser.LiveProbeUtil.haxeOnPath;
import static com.intellij.plugins.haxe.runner.debugger.browser.LiveProbeUtil.nextRequest;
import static com.intellij.plugins.haxe.runner.debugger.browser.LiveProbeUtil.nodeExe;
import static com.intellij.plugins.haxe.runner.debugger.browser.LiveProbeUtil.nodeRoot;
import static com.intellij.plugins.haxe.runner.debugger.browser.LiveProbeUtil.pauseRequest;
import static com.intellij.plugins.haxe.runner.debugger.browser.LiveProbeUtil.probe;
import static com.intellij.plugins.haxe.runner.debugger.browser.LiveProbeUtil.scopesRequest;
import static com.intellij.plugins.haxe.runner.debugger.browser.LiveProbeUtil.stackTraceRequest;
import static com.intellij.plugins.haxe.runner.debugger.browser.LiveProbeUtil.stepInRequest;
import static com.intellij.plugins.haxe.runner.debugger.browser.LiveProbeUtil.stepInTargetsRequest;
import static com.intellij.plugins.haxe.runner.debugger.browser.LiveProbeUtil.variablesRequest;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.intellij.plugins.haxe.runner.debugger.dap.client.DapClient;
import com.intellij.plugins.haxe.runner.debugger.dap.client.DapEndpoint;
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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

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

  /** Machine-wide install locations, tried after the per-user LOCALAPPDATA ones. */
  private static final List<String> CHROMIUM_PATHS = List.of(
    "C:/Program Files/Google/Chrome/Application/chrome.exe",
    "C:/Program Files (x86)/Google/Chrome/Application/chrome.exe",
    "C:/Program Files (x86)/Microsoft/Edge/Application/msedge.exe",
    "C:/Program Files/Microsoft/Edge/Application/msedge.exe",
    "/usr/bin/chromium",
    "/usr/bin/chromium-browser",
    "/usr/bin/google-chrome",
    "/snap/bin/chromium");

  // fixture file names; the .hx names come back in reported breakpoint source paths
  private static final String MAIN_HX = "WebMain.hx";
  private static final String SMART_HX = "WebSmart.hx";
  private static final String WORKER_HX = "WorkerMain.hx";

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
    for (String candidate : CHROMIUM_PATHS) {
      candidates.add(Path.of(candidate));
    }
    for (Path path : candidates) {
      if (Files.isRegularFile(path)) {
        return path;
      }
    }
    return null;
  }

  @BeforeEach
  public void spawnAdapter() throws IOException {
    Assumptions.assumeTrue(Files.isRegularFile(nodeExe()), "portable node not provisioned - skipping");
    Assumptions.assumeTrue(Files.isRegularFile(dapServerJs()), "js-debug adapter not provisioned - skipping");
    Assumptions.assumeTrue(chromiumExe() != null, "no chromium-family browser found (set WEB_DEBUG_CHROMIUM_EXE or install Chrome/Edge) - skipping");

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
    assertNotNull(line, "adapter announced nothing (died?)");
    assertTrue(line.contains("Debug server listening"), "unexpected announcement: " + line);

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

  @AfterEach
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
    Files.writeString(dir.resolve(MAIN_HX), WEB_MAIN_HX);
    LiveProbeUtil.writePageAndCompile(dir, "WebMain");
    return dir;
  }

  private static InitializeRequest initializeRequest() {
    InitializeRequest initialize = InitializeRequest.standard("chrome", true);
    initialize.getArguments().setClientName("IntelliJ Haxe");
    return initialize;
  }

  /** The parent-session launch config every probe here sends. */
  private static Map<String, Object> baseLaunchConfig(String baseUrl, Path fixture) {
    Map<String, Object> config = new LinkedHashMap<>();

    config.put("type", "pwa-chrome");
    config.put("request", "launch");
    config.put("name", "probe");
    config.put("url", baseUrl);
    config.put("webRoot", fixture.toString());
    config.put("runtimeExecutable", chromiumExe().toString());
    config.put("runtimeArgs", List.of("--headless=new"));

    return config;
  }

  private SetBreakpointsRequest breakpointsRequest(Path fixture) {
    SetBreakpointsRequest setBreakpoints = new SetBreakpointsRequest();
    SetBreakpointsArguments bpArgs = new SetBreakpointsArguments();

    Source source = new Source();
    source.setPath(fixture.resolve(MAIN_HX).toString());
    source.setName(MAIN_HX);
    bpArgs.setSource(source);

    SourceBreakpoint bp = new SourceBreakpoint();
    bp.setLine(BP_LINE);
    bpArgs.setBreakpoints(List.of(bp));

    setBreakpoints.setArguments(bpArgs);
    return setBreakpoints;
  }

  /**
   * Drives the parent session until js-debug asks for the page's child session:
   * configurationDone on the initialized event, every reverse request answered
   * as the IDE answers it. Null when no startDebugging arrives in time.
   */
  private StartDebuggingRequest awaitStartDebugging(long millis) throws Exception {
    long deadline = System.currentTimeMillis() + millis;
    while (System.currentTimeMillis() < deadline) {
      Event event = parent.pollEvent(100);
      if (traceAdapter && event instanceof OutputEvent output && output.getBody() != null) {
        System.out.println("[trace-out] " + String.valueOf(output.getBody().getOutput()).trim());
      }
      if (event instanceof InitializedEvent) {
        parent.sendRequest(new ConfigurationDoneRequest(), TIMEOUT);
      }

      Request incoming = parent.pollIncomingRequest(50);
      if (incoming != null) {
        parent.respond(incoming, true);
        if (incoming instanceof StartDebuggingRequest start) {
          return start;
        }
      }
    }
    return null;
  }


  /** As {@link #awaitStopped}, but only a stop owned by a worker session. */
  private static StoppedEvent awaitWorkerStop(DapEndpoint endpoint, long millis) throws Exception {
    long deadline = System.currentTimeMillis() + millis;
    while (System.currentTimeMillis() < deadline) {
      if (endpoint.pollEvent(250) instanceof StoppedEvent stopped && isWorkerStop(stopped)) {
        return stopped;
      }
    }
    return null;
  }

  /** Worker sessions are multiplexed behind composite ids; the page keeps raw ones. */
  private static boolean isWorkerStop(StoppedEvent stopped) {
    Integer threadId = stopped.getBody().getThreadId();
    return threadId != null && threadId >= COMPOSITE_FLOOR;
  }

  private static boolean isPageStop(StoppedEvent stopped) {
    Integer threadId = stopped.getBody().getThreadId();
    return threadId != null && threadId < COMPOSITE_FLOOR;
  }

  /** The mux names a worker thread after the script it runs. */
  private static boolean isWorkerThread(DapThread thread) {
    return thread.getId() >= COMPOSITE_FLOOR
           && thread.getName() != null && thread.getName().contains("worker.js");
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
  @Test
  @Timeout(60)
  public void loadTimeBreakpointHitsOnFirstLoad() throws Exception {
    Assumptions.assumeTrue(haxeOnPath(), "haxe not on PATH - skipping");
    Path fixture = Files.createTempDirectory("haxe-jsdbg-load");
    Files.writeString(fixture.resolve("WebLoad.hx"), WEB_LOAD_HX);
    LiveProbeUtil.writePageAndCompile(fixture, "WebLoad");

    StackFrame top = driveSessionToStop(fixture, "WebLoad.hx", LOAD_BP_LINE);
    assertTrue(top.getSource().getPath().endsWith("WebLoad.hx") && top.getLine() == LOAD_BP_LINE, "stopped in the load-time .hx line: " + top.getSource().getPath() + ":" + top.getLine());
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
  @Test
  @Timeout(60)
  public void stepInTargetsAndRuntimeCompletions() throws Exception {
    Assumptions.assumeTrue(haxeOnPath(), "haxe not on PATH - skipping");
    Path fixture = Files.createTempDirectory("haxe-jsdbg-smart");
    Files.writeString(fixture.resolve(SMART_HX), WEB_SMART_HX);
    LiveProbeUtil.writePageAndCompile(fixture, "WebSmart");

    atStop = (child, top) -> {
      // --- stepInTargets on the two-call line ---
      StepInTargetsRequest targetsRequest = stepInTargetsRequest(top.getId());

      Response targetsResponse = child.sendRequest(targetsRequest, TIMEOUT);
      assertTrue(targetsResponse.isSuccess(), "stepInTargets");
      var targets = ((StepInTargetsResponse)
                       targetsResponse).getBody().getTargets();

      for (var target : targets) {
        probe("stepInTarget id=" + target.getId() + " label=" + target.getLabel());
      }
      assertTrue(targets.size() >= 2, "expected at least 2 step-in targets, got " + targets.size());
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
      assertTrue(completionsResponse.isSuccess(), "completions");

      var items = ((CompletionsResponse)
                     completionsResponse).getBody().getTargets();

      List<String> labels = items.stream()
        .limit(8)
        .map(CompletionItem::getLabel)
        .toList();

      probe("completions for 'docum': " + labels);

      boolean anyMatch = items.stream().anyMatch(i -> "document".equals(i.getLabel()));
      assertTrue(anyMatch, "expected 'document' among runtime completions");

      // --- smart step INTO f2 (the outer call) ---
      StepInRequest stepIn = stepInRequest(currentThreadId, f2Target.getId());
      assertTrue(child.sendRequest(stepIn, TIMEOUT).isSuccess(), "targeted stepIn");

      StoppedEvent landed = null;
      long deadline = System.currentTimeMillis() + 20_000;
      while (System.currentTimeMillis() < deadline && landed == null) {
        if (child.pollEvent(250) instanceof StoppedEvent s) {
          landed = s;
        }
      }
      assertNotNull(landed, "no stop after targeted stepIn");

      Response stResponse = child.sendRequest(stackTraceRequest(currentThreadId), TIMEOUT);
      StackFrame landedTop = ((StackTraceResponse)stResponse).getBody().getStackFrames().get(0);

      probe("smart-step landed: " + landedTop.getName() + " @ "
            + (landedTop.getSource() != null ? landedTop.getSource().getPath() : "?")
            + ":" + landedTop.getLine());
      assertTrue(landedTop.getLine() == F2_BODY_LINE, "landed in f2's body line, got line " + landedTop.getLine());
    };
    try {
      driveSessionToStop(fixture, SMART_HX, CALLS_LINE);
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
  @Test
  @Timeout(60)
  public void stepInTargetsUnderTheIdeExactSequence() throws Exception {
    Assumptions.assumeTrue(haxeOnPath(), "haxe not on PATH - skipping");
    Path fixture = Files.createTempDirectory("haxe-jsdbg-idelike");
    Files.writeString(fixture.resolve(SMART_HX), WEB_SMART_HX);
    LiveProbeUtil.writePageAndCompile(fixture, "WebSmart");

    try (ContentHttpServer content = new ContentHttpServer(fixture)) {
      // --- parent exactly as BrowserDebugBackend.runParentHandshake ---
      assertTrue(parent.sendRequest(initializeRequest(), TIMEOUT).isSuccess(), "parent initialize");

      Map<String, Object> parentConfig = baseLaunchConfig(content.getBaseUrl(), fixture);
      parentConfig.put("name", "IntelliJ Haxe browser session");

      parent.sendRequestNoWait(ConfiguredLaunchRequest.of(parentConfig));

      StartDebuggingRequest startDebugging = awaitStartDebugging(20_000);
      assertNotNull(startDebugging, "no startDebugging");

      try (DapClient child = connectWithRetry(adapterPort)) {
        // --- child exactly as DapDebugProcess.initializeSession ---
        InitializeRequest initialize = InitializeRequest.standard("intellij-haxe", false);
        assertTrue(child.sendRequest(initialize, TIMEOUT).isSuccess(), "child initialize");
        child.sendRequestNoWait(ConfiguredLaunchRequest.of(startDebugging.getArguments().getConfiguration()));

        // awaitInitializedEvent
        long deadline = System.currentTimeMillis() + 15_000;
        boolean initialized = false;
        while (System.currentTimeMillis() < deadline && !initialized) {
          initialized = child.pollEvent(250) instanceof InitializedEvent;
        }
        assertTrue(initialized, "child initialized");

        // flushAll
        SetBreakpointsRequest setBreakpoints = new SetBreakpointsRequest();
        SetBreakpointsArguments bpArgs = new SetBreakpointsArguments();
        Source source = new Source();
        source.setPath(fixture.resolve(SMART_HX).toString());
        source.setName(SMART_HX);
        bpArgs.setSource(source);
        SourceBreakpoint bp = new SourceBreakpoint();
        bp.setLine(CALLS_LINE);
        bpArgs.setBreakpoints(List.of(bp));
        setBreakpoints.setArguments(bpArgs);

        assertTrue(child.sendRequest(setBreakpoints, TIMEOUT).isSuccess(), "child setBreakpoints");

        // exception filters as the IDE's exceptionFiltersRequest would send
        SetExceptionBreakpointsRequest filters = exceptionBreakpointsRequest(List.of("uncaught"));
        probe("ide-seq setExceptionBreakpoints success=" + child.sendRequest(filters, TIMEOUT).isSuccess());

        assertTrue(child.sendRequest(new ConfigurationDoneRequest(), TIMEOUT).isSuccess(), "child configurationDone");

        StoppedEvent stopped = null;
        deadline = System.currentTimeMillis() + 20_000;
        while (System.currentTimeMillis() < deadline && stopped == null) {
          if (child.pollEvent(250) instanceof StoppedEvent s) {
            stopped = s;
          }
        }
        assertNotNull(stopped, "breakpoint never hit");
        int threadId = stopped.getBody().getThreadId() != null ? stopped.getBody().getThreadId() : 1;

        // reportStopped order: threads THEN stackTrace
        assertTrue(child.sendRequest(new ThreadsRequest(), TIMEOUT).isSuccess(), "threads");
        StackTraceRequest stackTrace = stackTraceRequest(threadId);
        Response stResponse = child.sendRequest(stackTrace, TIMEOUT);
        StackFrame top = ((StackTraceResponse)stResponse).getBody().getStackFrames().get(0);

        // --- staged stepInTargets: which IDE behaviour kills the targets? ---
        probe("ide-seq top frame id=" + top.getId());
        int n1 = stepInTargetsCount(child, top.getId(), "1:immediately");

        // stage 2: hydrate the views like the IDE (scopes + variables)
        Response scResponse = child.sendRequest(scopesRequest(top.getId()), TIMEOUT);

        if (scResponse instanceof ScopesResponse okScopes
            && okScopes.getBody() != null && !okScopes.getBody().getScopes().isEmpty()) {
          int reference = okScopes.getBody().getScopes().get(0).getVariablesReference();
          child.sendRequest(variablesRequest(reference), TIMEOUT);
        }
        int n2 = stepInTargetsCount(child, top.getId(), "2:after scopes+variables");

        // stage 3: a SECOND stackTrace (frames view refresh) - do ids change?
        Response stResponse2 = child.sendRequest(stackTrace, TIMEOUT);
        StackFrame top2 = ((StackTraceResponse)stResponse2).getBody().getStackFrames().get(0);
        probe("ide-seq second stackTrace top id=" + top2.getId() + " (was " + top.getId() + ")");
        int n3 = stepInTargetsCount(child, top2.getId(), "3:fresh frame id");
        int n3old = stepInTargetsCount(child, top.getId(), "3b:old frame id after re-stackTrace");

        // stage 4: wall time
        Thread.sleep(4_000);
        int n4 = stepInTargetsCount(child, top2.getId(), "4:after 4s delay");

        probe("ide-seq counts: immediate=" + n1 + " hydrated=" + n2
              + " freshId=" + n3 + " oldIdAfterRefresh=" + n3old + " delayed=" + n4);
        assertTrue(n1 >= 2, "immediate stepInTargets must find the calls");

        // stage 5: SECOND pause (the user's failing case had frameId=3 - ids
        // increment across pauses, so their stop was not the first). continue,
        // let the ticking fixture re-hit the same line, ask again.
        assertTrue(child.sendRequest(continueRequest(threadId), TIMEOUT).isSuccess(), "continue");

        StoppedEvent second = awaitStopped(child, 15_000);
        assertNotNull(second, "no second stop");
        int threadId2 = second.getBody().getThreadId() != null ? second.getBody().getThreadId() : threadId;

        Response stResponse3 = child.sendRequest(stackTraceRequest(threadId2), TIMEOUT);
        StackFrame top3 = ((StackTraceResponse)stResponse3).getBody().getStackFrames().get(0);

        probe("ide-seq SECOND PAUSE top id=" + top3.getId()
              + " @ " + (top3.getSource() != null ? top3.getSource().getPath() : "?")
              + ":" + top3.getLine());
        int n5 = stepInTargetsCount(child, top3.getId(), "5:second pause");
        probe("ide-seq second-pause targets=" + n5);
        assertTrue(n5 >= 2, "second-pause stepInTargets must find the calls too (user's failing case)");

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

  @Test
  @Timeout(60)
  public void stepInTargetsOnABareExpressionStatement() throws Exception {
    Assumptions.assumeTrue(haxeOnPath(), "haxe not on PATH - skipping");
    Path fixture = Files.createTempDirectory("haxe-jsdbg-expr");
    Files.writeString(fixture.resolve("WebExpr.hx"), WEB_EXPR_HX);
    LiveProbeUtil.writePageAndCompile(fixture, "WebExpr");

    final int[] targetsAtStop = {-2};
    atStop = (child, top) -> targetsAtStop[0] = stepInTargetsCount(child, top.getId(), "bare-expression stop");
    try {
      driveSessionToStop(fixture, "WebExpr.hx", EXPR_CALLS_LINE);
    } finally {
      atStop = null;
    }
    probe("bare-expression (timer) stepInTargets count=" + targetsAtStop[0]);
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
  @Test
  @Timeout(60)
  public void stepInTargetsKnownLimitationOnLastStatementOfFunction() throws Exception {
    Assumptions.assumeTrue(haxeOnPath(), "haxe not on PATH - skipping");
    Path fixture = Files.createTempDirectory("haxe-jsdbg-lastline");
    Files.writeString(fixture.resolve("WebClick.hx"), WEB_CLICK_LAST_HX);
    LiveProbeUtil.writePageAndCompile(fixture, "WebClick");

    final int[] targetsAtStop = {-2};
    atStop = (child, top) -> targetsAtStop[0] = stepInTargetsCount(child, top.getId(), "last-statement stop");
    try {
      driveSessionToStop(fixture, "WebClick.hx", CLICK_CALLS_LINE);
    } finally {
      atStop = null;
    }
    probe("last-statement stepInTargets count=" + targetsAtStop[0]);
    assertTrue(targetsAtStop[0] == 0, "js-debug currently yields NO targets for a last-statement line (upstream limitation);"
               + " if this failed with a count >= 2, the pin fixed it - remove the limitation");
  }

  /**
   * Smart-step INSIDE A DOM EVENT HANDLER works when the call line is
   * followed by another mapped statement (the general case).
   */
  @Test
  @Timeout(60)
  public void stepInTargetsInsideADomEventHandler() throws Exception {
    Assumptions.assumeTrue(haxeOnPath(), "haxe not on PATH - skipping");
    Path fixture = Files.createTempDirectory("haxe-jsdbg-click");
    Files.writeString(fixture.resolve("WebClick.hx"), WEB_CLICK_HX);
    LiveProbeUtil.writePageAndCompile(fixture, "WebClick");

    final int[] targetsAtStop = {-2};
    atStop = (child, top) -> targetsAtStop[0] = stepInTargetsCount(child, top.getId(), "event-handler stop");
    try {
      StackFrame top = driveSessionToStop(fixture, "WebClick.hx", CLICK_CALLS_LINE);
      assertTrue(top.getLine() == CLICK_CALLS_LINE, "stopped on the handler's call line");
    } finally {
      atStop = null;
    }
    probe("event-handler stepInTargets count=" + targetsAtStop[0]);
    assertTrue(targetsAtStop[0] >= 2, "stepInTargets inside a DOM event handler must find the calls (user's case), got "
               + targetsAtStop[0]);
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
  @Test
  @Timeout(60)
  public void workerAppearsAsAdditionalThreadThroughTheMux() throws Exception {
    Assumptions.assumeTrue(haxeOnPath(), "haxe not on PATH - skipping");
    Path fixture = Files.createTempDirectory("haxe-jsdbg-worker");
    Files.writeString(fixture.resolve("WebPage.hx"), WEB_PAGE_HX);
    Files.writeString(fixture.resolve(WORKER_HX), WORKER_MAIN_HX);
    LiveProbeUtil.writePageAndCompile(fixture, "WebPage");
    LiveProbeUtil.compileHaxeJs(fixture, "WorkerMain", "worker.js");

    try (ContentHttpServer content = new ContentHttpServer(fixture)) {
      // --- parent handshake exactly as BrowserDebugBackend.runParentHandshake ---
      assertTrue(parent.sendRequest(initializeRequest(), TIMEOUT).isSuccess(), "parent initialize");

      Map<String, Object> parentConfig = baseLaunchConfig(content.getBaseUrl(), fixture);

      parent.sendRequestNoWait(ConfiguredLaunchRequest.of(parentConfig));

      StartDebuggingRequest startDebugging = awaitStartDebugging(20_000);
      assertNotNull(startDebugging, "no startDebugging for the page");

      DapClient page = connectWithRetry(adapterPort);
      try (JsDebugSessionMux mux = new JsDebugSessionMux(parent, page, adapterPort)) {
        mux.setLogSink(line -> System.out.println("[mux] " + line));

        // --- page handshake THROUGH the mux, as DapDebugProcess drives it ---
        assertTrue(mux.sendRequest(initializeRequest(), TIMEOUT).isSuccess(), "page initialize");
        mux.sendRequestNoWait(ConfiguredLaunchRequest.of(startDebugging.getArguments().getConfiguration()));
        boolean initialized = false;
        long deadline = System.currentTimeMillis() + 15_000;
        while (System.currentTimeMillis() < deadline && !initialized) {
          initialized = mux.pollEvent(250) instanceof InitializedEvent;
        }
        assertTrue(initialized, "page initialized");

        // breakpoint in the WORKER's source while no worker session exists yet:
        // the mux must cache it and replay it into the worker as it attaches
        SetBreakpointsRequest setBreakpoints = new SetBreakpointsRequest();
        SetBreakpointsArguments bpArgs = new SetBreakpointsArguments();

        Source source = new Source();
        source.setPath(fixture.resolve(WORKER_HX).toString());
        source.setName(WORKER_HX);
        bpArgs.setSource(source);

        SourceBreakpoint bp = new SourceBreakpoint();
        bp.setLine(WORKER_BP_LINE);
        bpArgs.setBreakpoints(List.of(bp));
        setBreakpoints.setArguments(bpArgs);

        Response bpResponse = mux.sendRequest(setBreakpoints, TIMEOUT);
        assertTrue(bpResponse.isSuccess(), "setBreakpoints via mux");
        var bpResult = ((SetBreakpointsResponse)
                          bpResponse).getBody().getBreakpoints().get(0);
        Integer pageBreakpointId = bpResult.getId();
        probe("worker-source bp before worker exists: id=" + pageBreakpointId + " verified=" + bpResult.isVerified());

        assertTrue(mux.sendRequest(new ConfigurationDoneRequest(), TIMEOUT).isSuccess(), "configurationDone via mux");

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
            probe("breakpoint event: id=" + state.getId() + " verified=" + state.isVerified());
            if (state.isVerified() && state.getId() != null && state.getId().equals(pageBreakpointId)) {
              verifiedUpgradeSeen = true;
            }
          }
          if (event instanceof StoppedEvent s) {
            stopped = s;
          }
        }
        assertNotNull(stopped, "worker breakpoint never hit through the mux");
        assertTrue(verifiedUpgradeSeen, "the worker's verification must surface as a breakpoint event on the PAGE id "
                   + pageBreakpointId + " (gutter checkmark)");
        Integer threadId = stopped.getBody().getThreadId();
        assertNotNull(threadId, "stop without a thread id");
        assertTrue(threadId >= COMPOSITE_FLOOR, "the stop must come from a WORKER session (composite thread id), got " + threadId);

        assertCompositeIdsRoundTrip(mux, threadId);
        int pageThreadId = pageThreadIdFromMergedThreads(mux);

        // continue routes back to the worker's session
        assertTrue(mux.sendRequest(continueRequest(threadId), TIMEOUT).isSuccess(), "continue via composite thread id");

        // the ticking worker re-hits: the composite ids are stable across stops
        StoppedEvent second = awaitStopped(mux, 15_000);
        assertNotNull(second, "no second worker stop after continue");
        assertTrue(isWorkerStop(second), "second stop must be composite too");

        assertPageRoutingLeavesWorkerPaused(mux, pageThreadId, second.getBody().getThreadId());

        mux.sendRequest(new DisconnectRequest(), TIMEOUT);
      }
    }
  }


  @Test
  @Timeout(60)
  public void fullSessionWithChildViaStartDebugging() throws Exception {
    Assumptions.assumeTrue(haxeOnPath(), "haxe not on PATH - skipping");
    Path fixture = buildFixture();

    try (ContentHttpServer content = new ContentHttpServer(fixture)) {
      content.setRequestListener(line -> System.out.println("[server] " + line));

      // --- parent session ---
      Response initResponse = parent.sendRequest(initializeRequest(), TIMEOUT);
      probe("parent initialize success=" + initResponse.isSuccess());
      assertTrue(initResponse.isSuccess(), "parent initialize");

      Map<String, Object> launchConfig = baseLaunchConfig(content.getBaseUrl(), fixture);

      // js-debug may hold the launch response until configurationDone, so the
      // launch goes out on a helper thread rather than blocking the sequence
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
          probe("parent event '" + event.getEvent() + "'"
                + (event instanceof OutputEvent o ? " :: " + o.getBody().getOutput() : ""));
          if (event instanceof InitializedEvent && !parentConfigured) {
            parentConfigured = true;
            probe("parent setBreakpoints success=" + parent.sendRequest(breakpointsRequest(fixture), TIMEOUT).isSuccess());
            probe("parent configurationDone success=" + parent.sendRequest(new ConfigurationDoneRequest(), TIMEOUT).isSuccess());
          }
        }
        Request incoming = parent.pollIncomingRequest(50);
        if (incoming != null) {
          probe("parent REVERSE request '" + incoming.getCommand() + "'");
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
      probe("launch settled=" + launchFuture.isDone() + " startDebugging=" + (startDebugging != null));
      assertNotNull(startDebugging, "no startDebugging reverse request from js-debug");
      Map<String, Object> childConfig = startDebugging.getArguments().getConfiguration();
      probe("child config keys=" + childConfig.keySet());

      // --- child session (second connection, config from the reverse request) ---
      try (DapClient child = connectWithRetry(adapterPort)) {
        assertTrue(child.sendRequest(initializeRequest(), TIMEOUT).isSuccess(), "child initialize");

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
          probe("child event '" + event.getEvent() + "'"
                + (event instanceof StoppedEvent s ? " reason=" + s.getBody().getReason() : ""));
          if (event instanceof InitializedEvent && !childConfigured) {
            childConfigured = true;
            probe("child launch settled BEFORE configuration: " + childLaunchFuture.isDone());
            probe("child setBreakpoints success=" + child.sendRequest(breakpointsRequest(fixture), TIMEOUT).isSuccess());
            probe("child configurationDone success=" + child.sendRequest(new ConfigurationDoneRequest(), TIMEOUT).isSuccess());
            probe("child launch settled AFTER configurationDone: " + childLaunchFuture.isDone());
          }
          if (event instanceof StoppedEvent s) {
            stopped = s;
          }
        }
        assertNotNull(stopped, "breakpoint never hit in the child session");
        probe("at stop: parent launch settled=" + launchFuture.isDone() + " child launch settled=" + childLaunchFuture.isDone());
        int threadId = stopped.getBody().getThreadId() != null ? stopped.getBody().getThreadId() : 1;

        Response stResponse = child.sendRequest(stackTraceRequest(threadId), TIMEOUT);
        assertTrue(stResponse.isSuccess(), "child stackTrace");
        List<StackFrame> frames = ((StackTraceResponse)stResponse).getBody().getStackFrames();
        assertTrue(!frames.isEmpty(), "no frames");
        StackFrame top = frames.get(0);
        probe("child top frame: " + top.getName() + " @ "
              + (top.getSource() != null ? top.getSource().getPath() : "?") + ":" + top.getLine());
        assertStoppedInHx(top, MAIN_HX, BP_LINE);

        child.sendRequest(new DisconnectRequest(), TIMEOUT);
      }
      parent.sendRequest(new DisconnectRequest(), TIMEOUT);
    }
  }

  /**
   * stackTrace -> scopes -> variables, every one routed by a COMPOSITE id: the
   * mux has to map each id back to the worker session that owns it, and the
   * ids it hands out must stay composited on the way back.
   */
  private static void assertCompositeIdsRoundTrip(JsDebugSessionMux mux, int threadId) throws Exception {
    Response stResponse = mux.sendRequest(stackTraceRequest(threadId), TIMEOUT);
    assertTrue(stResponse.isSuccess(), "stackTrace via composite thread id");

    StackFrame top = ((StackTraceResponse)stResponse).getBody().getStackFrames().get(0);
    probe("worker top frame: " + top.getName() + " @ "
          + (top.getSource() != null ? top.getSource().getPath() : "?") + ":" + top.getLine());

    assertStoppedInHx(top, WORKER_HX, WORKER_BP_LINE);
    assertTrue(top.getId() >= COMPOSITE_FLOOR, "frame id must be composited, got " + top.getId());

    Response scResponse = mux.sendRequest(scopesRequest(top.getId()), TIMEOUT);
    assertTrue(scResponse.isSuccess(), "scopes via composite frame id");

    var scopeList = ((ScopesResponse)scResponse).getBody().getScopes();
    assertTrue(!scopeList.isEmpty(), "no scopes");

    int varRef = scopeList.get(0).getVariablesReference();
    assertTrue(varRef >= COMPOSITE_FLOOR, "scope variablesReference must be composited, got " + varRef);
    assertTrue(mux.sendRequest(variablesRequest(varRef), TIMEOUT).isSuccess(), "variables via composite reference");
  }

  /** The merged listing carries the page under its raw id and the worker under a composite one. */
  private static int pageThreadIdFromMergedThreads(JsDebugSessionMux mux) throws Exception {
    Response threadsResponse = mux.sendRequest(new ThreadsRequest(), TIMEOUT);
    assertTrue(threadsResponse.isSuccess(), "merged threads");

    var threads = ((ThreadsResponse)threadsResponse).getBody().getThreads();
    for (var thread : threads) {
      probe("merged thread id=" + thread.getId() + " name=" + thread.getName());
    }
    boolean pageListed = threads.stream().anyMatch(t -> t.getId() < COMPOSITE_FLOOR);
    boolean workerListed = threads.stream().anyMatch(JsDebugAdapterLiveProbe::isWorkerThread);

    assertTrue(pageListed, "merged threads must include the page (raw id)");
    assertTrue(workerListed, "merged threads must include the worker (composite id, named after its script)");

    return threads.stream()
      .filter(t -> t.getId() < COMPOSITE_FLOOR)
      .findFirst()
      .orElseThrow()
      .getId();
  }

  /**
   * Pausing, stepping and resuming the PAGE must never disturb a worker that is
   * already paused. The IDE holds the worker's stop back and presents it after
   * the page resumes, so releasing it early would run it past the breakpoint
   * before the user ever sees it. The ticking worker re-hits within ~250ms once
   * resumed, so silence is what pins that it stayed put.
   */
  private static void assertPageRoutingLeavesWorkerPaused(JsDebugSessionMux mux, int pageThreadId,
                                                          int workerThreadId) throws Exception {
    assertTrue(mux.sendRequest(pauseRequest(pageThreadId), TIMEOUT).isSuccess(), "pause the page thread");

    StoppedEvent pageStop = awaitStopped(mux, 15_000);
    assertNotNull(pageStop, "page never paused");
    assertTrue(isPageStop(pageStop), "the pause stop must be the PAGE's (raw thread id), got "
               + pageStop.getBody().getThreadId());

    // a step routed to the PAGE must stop in the PAGE, never the worker
    // (the IDE bug this pins: stepping after switching threads)
    assertTrue(mux.sendRequest(nextRequest(pageThreadId), TIMEOUT).isSuccess(), "step the page thread");

    StoppedEvent stepStop = awaitStopped(mux, 15_000);
    assertNotNull(stepStop, "no stop after stepping the page");
    assertTrue(isPageStop(stepStop), "the step must land in the PAGE thread, got "
               + stepStop.getBody().getThreadId());

    assertTrue(mux.sendRequest(continueRequest(pageThreadId), TIMEOUT).isSuccess(), "resume via the page thread");

    assertNull(awaitWorkerStop(mux, 4_000), "the page-routed continue must NOT release the paused worker,"
               + " but its ticking breakpoint re-hit");

    // a continue routed to the WORKER releases it - the bp re-hits
    assertTrue(mux.sendRequest(continueRequest(workerThreadId), TIMEOUT).isSuccess(), "resume via the worker thread");

    assertNotNull(awaitWorkerStop(mux, 15_000), "the worker-routed continue must release the worker (bp re-hit)");
  }

  private int stepInTargetsCount(DapClient child, int frameId, String stage) throws Exception {
    StepInTargetsRequest request = stepInTargetsRequest(frameId);

    Response response = child.sendRequest(request, TIMEOUT);

    int count = response instanceof StepInTargetsResponse ok
                && ok.isSuccess() && ok.getBody() != null && ok.getBody().getTargets() != null
                ? ok.getBody().getTargets().size() : -1;
    probe("ide-seq stage " + stage + " frameId=" + frameId + " -> targets=" + count
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
      assertTrue(parent.sendRequest(initializeRequest(), TIMEOUT).isSuccess(), "parent initialize");

      Map<String, Object> launchConfig = baseLaunchConfig(content.getBaseUrl(), fixture);
      if (traceAdapter) {
        launchConfig.put("trace", true);
      }
      parent.sendRequestNoWait(ConfiguredLaunchRequest.of(launchConfig));

      StartDebuggingRequest startDebugging = awaitStartDebugging(20_000);
      assertNotNull(startDebugging, "no startDebugging reverse request");

      try (DapClient child = connectWithRetry(adapterPort)) {
        Response childInit = child.sendRequest(initializeRequest(), TIMEOUT);
        assertTrue(childInit.isSuccess(), "child initialize");
        if (childInit instanceof InitializeResponse ir
            && ir.getBody() != null) {
          probe("CHILD caps: completions=" + ir.getBody().getSupportsCompletionsRequest()
                + " stepInTargets=" + ir.getBody().getSupportsStepInTargetsRequest());
        }
        Map<String, Object> childConfig = new LinkedHashMap<>(startDebugging.getArguments().getConfiguration());
        if (traceAdapter) {
          childConfig.put("trace", true);
        }
        child.sendRequestNoWait(ConfiguredLaunchRequest.of(childConfig));

        boolean childConfigured = false;
        StoppedEvent stopped = null;
        long deadline = System.currentTimeMillis() + 20_000;
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

            assertTrue(child.sendRequest(setBreakpoints, TIMEOUT).isSuccess(), "child setBreakpoints");
            assertTrue(child.sendRequest(new ConfigurationDoneRequest(), TIMEOUT).isSuccess(), "child configurationDone");
          }
          if (event instanceof StoppedEvent s) {
            stopped = s;
          }
        }
        assertNotNull(stopped, "breakpoint never hit");
        int threadId = stopped.getBody().getThreadId() != null ? stopped.getBody().getThreadId() : 1;
        currentThreadId = threadId;

        Response stResponse = child.sendRequest(stackTraceRequest(threadId), TIMEOUT);
        assertTrue(stResponse.isSuccess(), "child stackTrace");

        List<StackFrame> frames = ((StackTraceResponse)stResponse).getBody().getStackFrames();
        assertTrue(!frames.isEmpty(), "no frames");
        StackFrame top = frames.get(0);
        probe("top frame: " + top.getName() + " @ "
              + (top.getSource() != null ? top.getSource().getPath() : "?") + ":" + top.getLine());
        assertNotNull(top.getSource(), "top frame has no source");
        if (atStop != null) {
          atStop.run(child, top);
        }
        child.sendRequest(new DisconnectRequest(), TIMEOUT);
        return top;
      }
    }
  }


}
