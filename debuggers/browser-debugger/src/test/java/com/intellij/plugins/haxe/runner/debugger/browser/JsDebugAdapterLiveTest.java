package com.intellij.plugins.haxe.runner.debugger.browser;

import static com.intellij.plugins.haxe.runner.debugger.browser.LiveProbeUtil.BP_LINE;
import static com.intellij.plugins.haxe.runner.debugger.browser.LiveProbeUtil.LOAD_BP_LINE;
import static com.intellij.plugins.haxe.runner.debugger.browser.LiveProbeUtil.WEB_LOAD_HX_NAME;
import static com.intellij.plugins.haxe.runner.debugger.browser.LiveProbeUtil.WEB_MAIN_HX_NAME;
import static com.intellij.plugins.haxe.runner.debugger.browser.LiveProbeUtil.WEB_LOAD_HX_SOURCE;
import static com.intellij.plugins.haxe.runner.debugger.browser.LiveProbeUtil.WEB_MAIN_HX_SOURCE;
import static com.intellij.plugins.haxe.runner.debugger.browser.LiveProbeUtil.assertStoppedInHx;
import static com.intellij.plugins.haxe.runner.debugger.browser.LiveProbeUtil.awaitStartDebugging;
import static com.intellij.plugins.haxe.runner.debugger.browser.LiveProbeUtil.awaitStopped;
import static com.intellij.plugins.haxe.runner.debugger.browser.LiveProbeUtil.baseLaunchConfig;
import static com.intellij.plugins.haxe.runner.debugger.browser.LiveProbeUtil.breakpointsRequest;
import static com.intellij.plugins.haxe.runner.debugger.browser.LiveProbeUtil.chromiumExe;
import static com.intellij.plugins.haxe.runner.debugger.browser.LiveProbeUtil.continueRequest;
import static com.intellij.plugins.haxe.runner.debugger.browser.LiveProbeUtil.dapServerJs;
import static com.intellij.plugins.haxe.runner.debugger.browser.LiveProbeUtil.exceptionBreakpointsRequest;
import static com.intellij.plugins.haxe.runner.debugger.browser.LiveProbeUtil.haxeOnPath;
import static com.intellij.plugins.haxe.runner.debugger.browser.LiveProbeUtil.initializeRequest;
import static com.intellij.plugins.haxe.runner.debugger.browser.LiveProbeUtil.nextRequest;
import static com.intellij.plugins.haxe.runner.debugger.browser.LiveProbeUtil.nodeExe;
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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

/// Wire probe for vscode-js-debug's standalone DAP server (pinned
/// js-debug-dap v1.117.0, sha256 ad8d04ed..., from the GitHub release), driving
/// a Chromium-family browser. Pins the parts that differ from the firefox
/// adapter:
///
///   - launch ordering (does the launch response wait for configurationDone?);
///   - the `startDebugging` REVERSE request and the child-session
///       handshake via `__pendingTargetId` on a SECOND connection;
///   - breakpoint-by-.hx-path over source maps in the child session.
///
/// Skips when node/adapter/chromium are not provisioned under `<root>/node`.
@DisplayName("Browser debugger: js debug adapter (live)")
public class JsDebugAdapterLiveTest {
  private static final long TIMEOUT = 15_000;

  // fixture file names; the .hx names come back in reported breakpoint source paths
  private static final String WEB_SMART_HX_NAME = "WebSmart.hx";
  private static final String WORKER_MAIN_HX_NAME = "WorkerMain.hx";

  private Process adapter;
  private int adapterPort;
  private DapClient parent;

  @BeforeEach
  public void spawnAdapter() throws IOException {
    Assumptions.assumeTrue(Files.isRegularFile(nodeExe()), "portable node not provisioned - skipping");
    Assumptions.assumeTrue(Files.isRegularFile(dapServerJs()), "js-debug adapter not provisioned - skipping");
    Assumptions.assumeTrue(chromiumExe() != null, "no chromium-family browser found (set WEB_DEBUG_CHROMIUM_EXE or install Chrome/Edge) - skipping");

    adapterPort = LiveProbeUtil.freePort();
    adapter = LiveProbeUtil.spawnAdapterServer(
      List.of(nodeExe().toString(), dapServerJs().toString(), String.valueOf(adapterPort), "127.0.0.1"),
      dapServerJs().getParent(), "Debug server listening", "adapter");

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
    Files.writeString(dir.resolve(WEB_MAIN_HX_NAME), WEB_MAIN_HX_SOURCE);
    LiveProbeUtil.writePageAndCompile(dir, "WebMain");
    return dir;
  }

  /// As {@link #awaitStopped}, but only a stop owned by a worker session.
  private static StoppedEvent awaitWorkerStop(DapEndpoint endpoint, long millis) throws Exception {
    long deadline = System.currentTimeMillis() + millis;
    while (System.currentTimeMillis() < deadline) {
      if (endpoint.pollEvent(250) instanceof StoppedEvent stopped && isWorkerStop(stopped)) {
        return stopped;
      }
    }
    return null;
  }

  /// Worker sessions are multiplexed behind composite ids; the page keeps raw ones.
  private static boolean isWorkerStop(StoppedEvent stopped) {
    Integer threadId = stopped.getBody().getThreadId();
    return threadId != null && threadId >= COMPOSITE_FLOOR;
  }

  private static boolean isPageStop(StoppedEvent stopped) {
    Integer threadId = stopped.getBody().getThreadId();
    return threadId != null && threadId < COMPOSITE_FLOOR;
  }

  /// The mux names a worker thread after the script it runs.
  private static boolean isWorkerThread(DapThread thread) {
    return thread.getId() >= COMPOSITE_FLOOR
           && thread.getName() != null && thread.getName().contains("worker.js");
  }

  /// Load-time code (main body, runs during page load) — the case the firefox
  /// adapter needed the refresh-once trick for. js-debug pre-registers
  /// breakpoints through CDP before scripts execute, so the FIRST load must
  /// stop, with no serving tricks. Guards the family split in the backend
  /// (no refreshFirstPage for chromium).
  @Test
  @Timeout(60)
  @DisplayName("load time breakpoint hits on first load")
  public void loadTimeBreakpointHitsOnFirstLoad() throws Exception {
    Assumptions.assumeTrue(haxeOnPath(), "haxe not on PATH - skipping");
    Path fixture = Files.createTempDirectory("haxe-jsdbg-load");
    Files.writeString(fixture.resolve(WEB_LOAD_HX_NAME), WEB_LOAD_HX_SOURCE);
    LiveProbeUtil.writePageAndCompile(fixture, "WebLoad");

    StackFrame top = driveSessionToStop(fixture, WEB_LOAD_HX_NAME, LOAD_BP_LINE);
    assertStoppedInHx(top, WEB_LOAD_HX_NAME, LOAD_BP_LINE);
  }

  // WebSmart.hx line numbers are load-bearing: CALLS_LINE has TWO calls, the
  // smart-step material; F2_BODY_LINE is inside f2.
  private static final int CALLS_LINE = 8;
  private static final int F2_BODY_LINE = 4;
  private static final String WEB_SMART_HX_SOURCE = """
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

  /// Smart-step material: DAP stepInTargets on a line with two calls, then
  /// stepIn with a chosen targetId. Also probes the "completions" request —
  /// the runtime-truth fallback for identifiers the Haxe PSI cannot resolve
  /// (browser globals behind incomplete externs).
  @Test
  @Timeout(60)
  @DisplayName("step in targets and runtime completions")
  public void stepInTargetsAndRuntimeCompletions() throws Exception {
    Assumptions.assumeTrue(haxeOnPath(), "haxe not on PATH - skipping");
    Path fixture = Files.createTempDirectory("haxe-jsdbg-smart");
    Files.writeString(fixture.resolve(WEB_SMART_HX_NAME), WEB_SMART_HX_SOURCE);
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
      driveSessionToStop(fixture, WEB_SMART_HX_NAME, CALLS_LINE);
    } finally {
      atStop = null;
    }
  }

  /// Replicates the IDE's EXACT child-session sequence (DapDebugProcess):
  /// initialize with adapterID intellij-haxe and NO supportsStartDebuggingRequest,
  /// fire-and-forget launch, await initialized, setBreakpoints,
  /// setExceptionBreakpoints(["uncaught"]), configurationDone — then at the stop:
  /// threads, stackTrace, and the stepInTargets the smart-step handler sends.
  /// Exists because the IDE reported no step-in chooser while the probe's own
  /// sequence got targets fine — this pins whether the SEQUENCE is the culprit.
  @Test
  @Timeout(60)
  @DisplayName("step in targets under the ide exact sequence")
  public void stepInTargetsUnderTheIdeExactSequence() throws Exception {
    Assumptions.assumeTrue(haxeOnPath(), "haxe not on PATH - skipping");
    Path fixture = Files.createTempDirectory("haxe-jsdbg-idelike");
    Files.writeString(fixture.resolve(WEB_SMART_HX_NAME), WEB_SMART_HX_SOURCE);
    LiveProbeUtil.writePageAndCompile(fixture, "WebSmart");

    try (ContentHttpServer content = new ContentHttpServer(fixture)) {
      // --- parent exactly as BrowserDebugBackend.runParentHandshake ---
      assertTrue(parent.sendRequest(initializeRequest("chrome"), TIMEOUT).isSuccess(), "parent initialize");

      Map<String, Object> parentConfig = baseLaunchConfig(content.getBaseUrl(), fixture);
      parentConfig.put("name", "IntelliJ Haxe browser session");

      parent.sendRequestNoWait(ConfiguredLaunchRequest.of(parentConfig));

      StartDebuggingRequest startDebugging = awaitStartDebugging(parent, 20_000, TIMEOUT);
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
        SetBreakpointsRequest setBreakpoints = breakpointsRequest(fixture, WEB_SMART_HX_NAME, CALLS_LINE);
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

        // hydrate the views like the IDE (scopes + variables) before stepping on
        Response scResponse = child.sendRequest(scopesRequest(top.getId()), TIMEOUT);

        int reference = LiveProbeUtil.firstScopeReference(scResponse);
        if (reference >= 0) {
          child.sendRequest(variablesRequest(reference), TIMEOUT);
        }

        // first-stop targets are the plain probe's pin; the unique pin here is
        // the SECOND pause (the user's failing case had frameId=3 - ids
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
  private static final String WEB_CLICK_HX_SOURCE = """
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
  private static final String WEB_EXPR_HX_SOURCE = """
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
  @DisplayName("step in targets on a bare expression statement")
  public void stepInTargetsOnABareExpressionStatement() throws Exception {
    Assumptions.assumeTrue(haxeOnPath(), "haxe not on PATH - skipping");
    Path fixture = Files.createTempDirectory("haxe-jsdbg-expr");
    Files.writeString(fixture.resolve("WebExpr.hx"), WEB_EXPR_HX_SOURCE);
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
  private static final String WEB_CLICK_LAST_HX_SOURCE = WEB_CLICK_HX_SOURCE.replace("\tx++;\n", "");

  /// UPSTREAM LIMITATION, pinned: when the multi-call line is the LAST
  /// statement of its function, js-debug's getStepInTargets reverse-maps the
  /// line AND line+1; the closing-brace line has no source-map entries, the
  /// sibling counts differ, and it bails to [] with the internal warning
  /// "Expected to have the same number of start and end locations" (no CDP is
  /// even consulted). Diagnosed via trace logs; the IDE then falls back to a
  /// plain step into. If a future js-debug pin fixes this, THIS TEST FAILS -
  /// celebrate and delete it.
  @Test
  @Timeout(60)
  @DisplayName("step in targets known limitation on last statement of function")
  public void stepInTargetsKnownLimitationOnLastStatementOfFunction() throws Exception {
    Assumptions.assumeTrue(haxeOnPath(), "haxe not on PATH - skipping");
    Path fixture = Files.createTempDirectory("haxe-jsdbg-lastline");
    Files.writeString(fixture.resolve("WebClick.hx"), WEB_CLICK_LAST_HX_SOURCE);
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

  /// Smart-step INSIDE A DOM EVENT HANDLER works when the call line is
  /// followed by another mapped statement (the general case).
  @Test
  @Timeout(60)
  @DisplayName("step in targets inside a dom event handler")
  public void stepInTargetsInsideADomEventHandler() throws Exception {
    Assumptions.assumeTrue(haxeOnPath(), "haxe not on PATH - skipping");
    Path fixture = Files.createTempDirectory("haxe-jsdbg-click");
    Files.writeString(fixture.resolve("WebClick.hx"), WEB_CLICK_HX_SOURCE);
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
  private static final String WORKER_MAIN_HX_SOURCE = """
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
  private static final String WEB_PAGE_HX_SOURCE = """
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

  /// Ids from a k>0 session carry the session index above this.
  private static final int COMPOSITE_FLOOR = 1 << 24;

  /// Workers-as-threads: the {@link JsDebugSessionMux} auto-attaches the worker
  /// session js-debug announces via `startDebugging` on the PAGE
  /// connection, replays the cached breakpoint (set through the mux BEFORE the
  /// worker existed), and surfaces the worker's stop as a COMPOSITE thread id
  /// in the one merged session. Pins the id round-trip the IDE relies on:
  /// stackTrace by composite threadId, scopes/variables by composite
  /// frameId/variablesReference, merged threads listing both targets, and
  /// continue routed back to the worker.
  @Test
  @Timeout(60)
  @DisplayName("worker appears as additional thread through the mux")
  public void workerAppearsAsAdditionalThreadThroughTheMux() throws Exception {
    Assumptions.assumeTrue(haxeOnPath(), "haxe not on PATH - skipping");
    Path fixture = Files.createTempDirectory("haxe-jsdbg-worker");
    Files.writeString(fixture.resolve("WebPage.hx"), WEB_PAGE_HX_SOURCE);
    Files.writeString(fixture.resolve(WORKER_MAIN_HX_NAME), WORKER_MAIN_HX_SOURCE);
    LiveProbeUtil.writePageAndCompile(fixture, "WebPage");
    LiveProbeUtil.compileHaxeJs(fixture, "WorkerMain", "worker.js");

    try (ContentHttpServer content = new ContentHttpServer(fixture)) {
      // --- parent handshake exactly as BrowserDebugBackend.runParentHandshake ---
      assertTrue(parent.sendRequest(initializeRequest("chrome"), TIMEOUT).isSuccess(), "parent initialize");

      Map<String, Object> parentConfig = baseLaunchConfig(content.getBaseUrl(), fixture);

      parent.sendRequestNoWait(ConfiguredLaunchRequest.of(parentConfig));

      StartDebuggingRequest startDebugging = awaitStartDebugging(parent, 20_000, TIMEOUT);
      assertNotNull(startDebugging, "no startDebugging for the page");

      DapClient page = connectWithRetry(adapterPort);
      try (JsDebugSessionMux mux = new JsDebugSessionMux(parent, page, adapterPort)) {
        mux.setLogSink(line -> System.out.println("[mux] " + line));

        // --- page handshake THROUGH the mux, as DapDebugProcess drives it ---
        assertTrue(mux.sendRequest(initializeRequest("chrome"), TIMEOUT).isSuccess(), "page initialize");
        mux.sendRequestNoWait(ConfiguredLaunchRequest.of(startDebugging.getArguments().getConfiguration()));
        boolean initialized = false;
        long deadline = System.currentTimeMillis() + 15_000;
        while (System.currentTimeMillis() < deadline && !initialized) {
          initialized = mux.pollEvent(250) instanceof InitializedEvent;
        }
        assertTrue(initialized, "page initialized");

        // breakpoint in the WORKER's source while no worker session exists yet:
        // the mux must cache it and replay it into the worker as it attaches
        SetBreakpointsRequest setBreakpoints = breakpointsRequest(fixture, WORKER_MAIN_HX_NAME, WORKER_BP_LINE);
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
          var state = LiveProbeUtil.breakpointOf(event);
          if (state != null) {
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
  @DisplayName("full session with child via start debugging")
  public void fullSessionWithChildViaStartDebugging() throws Exception {
    Assumptions.assumeTrue(haxeOnPath(), "haxe not on PATH - skipping");
    StackFrame top = driveSessionToStop(buildFixture(), WEB_MAIN_HX_NAME, BP_LINE);
    assertStoppedInHx(top, WEB_MAIN_HX_NAME, BP_LINE);
  }

  /// stackTrace -> scopes -> variables, every one routed by a COMPOSITE id: the
  /// mux has to map each id back to the worker session that owns it, and the
  /// ids it hands out must stay composited on the way back.
  private static void assertCompositeIdsRoundTrip(JsDebugSessionMux mux, int threadId) throws Exception {
    Response stResponse = mux.sendRequest(stackTraceRequest(threadId), TIMEOUT);
    assertTrue(stResponse.isSuccess(), "stackTrace via composite thread id");

    StackFrame top = ((StackTraceResponse)stResponse).getBody().getStackFrames().get(0);
    probe("worker top frame: " + top.getName() + " @ "
          + (top.getSource() != null ? top.getSource().getPath() : "?") + ":" + top.getLine());

    assertStoppedInHx(top, WORKER_MAIN_HX_NAME, WORKER_BP_LINE);
    assertTrue(top.getId() >= COMPOSITE_FLOOR, "frame id must be composited, got " + top.getId());

    Response scResponse = mux.sendRequest(scopesRequest(top.getId()), TIMEOUT);
    assertTrue(scResponse.isSuccess(), "scopes via composite frame id");

    var scopeList = ((ScopesResponse)scResponse).getBody().getScopes();
    assertTrue(!scopeList.isEmpty(), "no scopes");

    int varRef = scopeList.get(0).getVariablesReference();
    assertTrue(varRef >= COMPOSITE_FLOOR, "scope variablesReference must be composited, got " + varRef);
    assertTrue(mux.sendRequest(variablesRequest(varRef), TIMEOUT).isSuccess(), "variables via composite reference");
  }

  /// The merged listing carries the page under its raw id and the worker under a composite one.
  private static int pageThreadIdFromMergedThreads(JsDebugSessionMux mux) throws Exception {
    Response threadsResponse = mux.sendRequest(new ThreadsRequest(), TIMEOUT);
    assertTrue(threadsResponse.isSuccess(), "merged threads");

    var threads = ((ThreadsResponse)threadsResponse).getBody().getThreads();
    for (var thread : threads) {
      probe("merged thread id=" + thread.getId() + " name=" + thread.getName());
    }
    boolean pageListed = threads.stream().anyMatch(t -> t.getId() < COMPOSITE_FLOOR);
    boolean workerListed = threads.stream().anyMatch(JsDebugAdapterLiveTest::isWorkerThread);

    assertTrue(pageListed, "merged threads must include the page (raw id)");
    assertTrue(workerListed, "merged threads must include the worker (composite id, named after its script)");

    return threads.stream()
      .filter(t -> t.getId() < COMPOSITE_FLOOR)
      .findFirst()
      .orElseThrow()
      .getId();
  }

  /// Pausing, stepping and resuming the PAGE must never disturb a worker that is
  /// already paused. The IDE holds the worker's stop back and presents it after
  /// the page resumes, so releasing it early would run it past the breakpoint
  /// before the user ever sees it. The ticking worker re-hits within ~250ms once
  /// resumed, so silence is what pins that it stayed put.
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

    int count = targetsCountOf(response);
    probe("ide-seq stage " + stage + " frameId=" + frameId + " -> targets=" + count
          + (response.isSuccess() ? "" : " message=" + response.getMessage()));
    return count;
  }

  /// The response's step-in target count, or -1 when the request failed or answered without targets.
  private static int targetsCountOf(Response response) {
    boolean answered = response instanceof StepInTargetsResponse ok
      && ok.isSuccess() && ok.getBody() != null && ok.getBody().getTargets() != null;
    return answered ? ((StepInTargetsResponse)response).getBody().getTargets().size() : -1;
  }

  /// Optional at-stop hook for probes needing extra requests before disconnect.
  private interface AtStop {
    void run(DapClient child, StackFrame top) throws Exception;
  }

  private volatile AtStop atStop;
  private volatile int currentThreadId;

  /// The parent+child flow, shared by the probes; returns the stop's top frame.
  private StackFrame driveSessionToStop(Path fixture, String bpFileName, int bpLine) throws Exception {
    try (ContentHttpServer content = new ContentHttpServer(fixture)) {
      assertTrue(parent.sendRequest(initializeRequest("chrome"), TIMEOUT).isSuccess(), "parent initialize");

      Map<String, Object> launchConfig = baseLaunchConfig(content.getBaseUrl(), fixture);
      parent.sendRequestNoWait(ConfiguredLaunchRequest.of(launchConfig));

      StartDebuggingRequest startDebugging = awaitStartDebugging(parent, 20_000, TIMEOUT);
      assertNotNull(startDebugging, "no startDebugging reverse request");

      try (DapClient child = connectWithRetry(adapterPort)) {
        Response childInit = child.sendRequest(initializeRequest("chrome"), TIMEOUT);
        assertTrue(childInit.isSuccess(), "child initialize");
        if (childInit instanceof InitializeResponse ir
            && ir.getBody() != null) {
          probe("CHILD caps: completions=" + ir.getBody().getSupportsCompletionsRequest()
                + " stepInTargets=" + ir.getBody().getSupportsStepInTargetsRequest());
        }
        Map<String, Object> childConfig = new LinkedHashMap<>(startDebugging.getArguments().getConfiguration());
        child.sendRequestNoWait(ConfiguredLaunchRequest.of(childConfig));

        boolean childConfigured = false;
        StoppedEvent stopped = null;
        long deadline = System.currentTimeMillis() + 20_000;
        while (System.currentTimeMillis() < deadline && stopped == null) {
          Event event = child.pollEvent(100);
          if (event instanceof InitializedEvent && !childConfigured) {
            childConfigured = true;
            SetBreakpointsRequest setBreakpoints = breakpointsRequest(fixture, bpFileName, bpLine);
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
