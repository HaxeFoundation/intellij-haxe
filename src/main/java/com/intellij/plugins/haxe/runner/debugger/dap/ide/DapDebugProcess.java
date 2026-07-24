package com.intellij.plugins.haxe.runner.debugger.dap.ide;

import com.intellij.execution.filters.TextConsoleBuilderFactory;
import com.intellij.execution.process.ProcessEvent;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.process.ProcessListener;
import com.intellij.execution.process.ProcessOutputTypes;
import com.intellij.execution.ui.ConsoleView;
import com.intellij.execution.ui.ConsoleViewContentType;
import com.intellij.execution.ui.ExecutionConsole;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.plugins.haxe.runner.debugger.exceptions.HaxeExceptionBreakpointProperties;
import com.intellij.plugins.haxe.runner.debugger.exceptions.HaxeExceptionBreakpointType;
import com.intellij.plugins.haxe.runner.debugger.*;
import com.intellij.plugins.haxe.runner.debugger.dap.client.DapEndpoint;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.*;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.events.*;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.requests.*;
import com.intellij.xdebugger.XDebugSessionListener;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.responses.*;
import com.intellij.xdebugger.XDebugProcess;
import com.intellij.xdebugger.XDebugSession;
import com.intellij.xdebugger.XSourcePosition;
import com.intellij.xdebugger.breakpoints.XBreakpoint;
import com.intellij.xdebugger.breakpoints.XBreakpointHandler;
import com.intellij.xdebugger.breakpoints.XBreakpointProperties;
import com.intellij.xdebugger.breakpoints.XLineBreakpoint;
import com.intellij.xdebugger.evaluation.XDebuggerEditorsProvider;
import com.intellij.xdebugger.frame.XExecutionStack;
import com.intellij.xdebugger.frame.XStackFrame;
import com.intellij.xdebugger.frame.XSuspendContext;
import com.intellij.xdebugger.stepping.XSmartStepIntoHandler;
import com.intellij.xdebugger.ui.XDebugTabLayouter;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import com.intellij.xdebugger.DefaultDebugProcessHandler;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * The shared XDebugger process for every DAP-based debugger: a DAP client
 * over a {@link DapBackend} (the in-process vshaxe adapter, the debuggee's
 * embedded intellij-hxcpp-debug-server, or the eval adapter), bridging its
 * events into the IDE. Mirrors the HashLink debug process, but simpler in two
 * ways: the peer is reached over a loopback socket (no external process to
 * manage), and the debuggee is a plain child process (no OS-level debug
 * attachment, so killing it needs no special ceremony).
 *
 * The debuggee is spawned by the debug runner; its {@link ProcessHandler} is
 * the session's process handler, so console output, stdin and the exit code
 * flow through the normal run machinery, and the session ends when the
 * debuggee does.
 *
 * Threading: the IDE calls resume/step/stop on the EDT — those only submit
 * work to a single-thread request executor. A dedicated event-pump thread is
 * the sole {@code pollEvent} caller; DapClient correlates concurrent requests
 * by seq.
 */
public class DapDebugProcess extends XDebugProcess {
  private static final Logger LOG = Logger.getInstance(DapDebugProcess.class);
  private static final long REQUEST_TIMEOUT_MILLIS = 15_000;
  private static final long DISCONNECT_TIMEOUT_MILLIS = 3_000;
  private static final long EVENT_POLL_MILLIS = 250;

  private final DapBackend backend;
  private final ProcessHandler processHandler;
  private final DapBreakpointManager breakpoints = new DapBreakpointManager(this);
  private final ExecutorService requestExecutor =
    Executors.newSingleThreadExecutor(r -> daemon(r, "DAP requests"));

  private volatile DapEndpoint client;
  /** The adapter's declared capabilities (from the initialize response). */
  private volatile Capabilities capabilities;
  // the DAP frame id of the newest frame at the current stop (-1 before the
  // first); smart-step handlers that query the adapter need it
  private volatile int topFrameId = -1;
  /** Eval-only: raw sub-expression steps + expression-span highlight. Session-scoped. */
  private volatile boolean expressionStepping = false;
  private HaxeExpressionPointHighlighter expressionHighlighter;
  private boolean resumeListenerInstalled = false;
  private volatile int currentThreadId = 0;
  private volatile boolean shuttingDown = false;
  private volatile boolean launched = false;
  /**
   * Whether a pause is currently presented to the user. Tracked here and NOT
   * via XDebugSession.isSuspended(): the platform reflects the suspended
   * state asynchronously, so two near-simultaneous stops (both workers'
   * ticking breakpoints) could each see "not suspended" and fight over the
   * views (duplicated thread combo entries, a frames list stuck on
   * "Loading..."). While a pause is on screen, another thread's stop leaves
   * that thread paused but does not touch the views — it is inspectable
   * through the thread list, and Resume releases it.
   */
  private volatile boolean pauseOnScreen = false;

  public DapDebugProcess(@NotNull XDebugSession session,
                           DapBackend backend, @Nullable ProcessHandler debuggeeHandler) {
    super(session);
    this.backend = backend;
    // A backend whose ADAPTER owns the debuggee (the web adapters launch the
    // browser themselves) has no runner-spawned process; a no-op handler keeps
    // the session/console plumbing uniform (Stop still goes through stop()).
    this.processHandler = debuggeeHandler != null
                          ? debuggeeHandler
                          : new DefaultDebugProcessHandler();
    if (debuggeeHandler == null) {
      return;
    }
    // A debuggee dying BEFORE the session is up is always a startup failure
    // (not compiled with the debug server, or its port is poisoned by a
    // leftover instance) — fail immediately with the exit code instead of
    // letting the launch request run into its timeout.
    processHandler.addProcessListener(new ProcessListener() {
      @Override
      public void processTerminated(@NotNull ProcessEvent event) {
        if (!shuttingDown && !launched) {
          fail("The program exited (code " + event.getExitCode() + ") before the debugger could attach.\n"
               + backend.startupHint());
        }
      }

      // "Terminate" is about to destroy the debuggee; a backend holding an OS
      // debug attachment must release it first or the destroy hangs. Gated on
      // client != null so the graceful teardown (which nulls client before
      // destroying the handler) and Disconnect skip it.
      @Override
      public void processWillTerminate(@NotNull ProcessEvent event, boolean willBeDestroyed) {
        if (willBeDestroyed && client != null) {
          backend.beforeDebuggeeDestroyed();
        }
      }
    });
  }

  // --- lifecycle ---

  @Override
  public void sessionInitialized() {
    getSession().setPauseActionSupported(true);
    requestExecutor.execute(this::initializeSession);
  }

  // The default XDebugProcess.createConsole() builds a console but never
  // attaches it to the process handler (unlike CommandLineState, which does) —
  // without this override the debuggee's stdout/stderr go nowhere.
  @Override
  public @NotNull ExecutionConsole createConsole() {
    ConsoleView console = TextConsoleBuilderFactory.getInstance()
      .createBuilder(getSession().getProject())
      .getConsole();
    console.attachToProcess(processHandler);
    return console;
  }

  private void initializeSession() {
    try {
      // blocks until the DAP peer is up (the adapter's loopback pair, or the
      // debuggee's embedded server connecting to the runner's listener)
      client = backend.connect();
      backend.onConnected(this);

      performInitializeHandshake();
      if (!performLaunch()) {
        return;
      }
      sendStartupConfiguration();

      Thread pump = daemon(this::pumpEvents, "DAP events");
      pump.start();
    } catch (IOException e) {
      fail("Cannot start the debug session: " + e.getMessage());
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }

  /** initialize + capability capture (and, for pre-launch adapters, the initialized event). */
  private void performInitializeHandshake() throws IOException, InterruptedException {
    Response initializeResponse =
      client.sendRequest(InitializeRequest.standard("intellij-haxe", false), REQUEST_TIMEOUT_MILLIS);
    if (initializeResponse instanceof InitializeResponse ok && ok.getBody() != null) {
      capabilities = ok.getBody();
    }
    if (!backend.initializedEventAfterLaunch()) {
      awaitInitializedEvent();
    }
  }

  /**
   * The launch request, in the backend's flavour. False = the session failed
   * and is already being torn down.
   */
  private boolean performLaunch() throws IOException, InterruptedException {
    if (backend.requiresLaunchRequest()) {
      // launch = "the debuggee's server connected" (haxe-side servers hold
      // the program before main) or "the adapter started the debuggee"
      // (web adapters launch the browser here)
      if (backend.awaitsLaunchResponse()) {
        Response launchResponse = client.sendRequest(backend.launchRequest(), REQUEST_TIMEOUT_MILLIS);
        if (!launchResponse.isSuccess()) {
          fail("Cannot start the debug session: " + launchResponse.getMessage());
          return false;
        }
      } else {
        // js-debug answers launch only after configurationDone; a launch
        // failure surfaces as an error output/terminated event instead
        client.sendRequestNoWait(backend.launchRequest());
      }
    }
    launched = true;
    if (backend.initializedEventAfterLaunch()) {
      // the vscode web adapters signal readiness for breakpoints only once
      // the browser side is up — after the launch response
      awaitInitializedEvent();
    }
    return true;
  }

  /** Breakpoints, exception filters and settings, then configurationDone. */
  private void sendStartupConfiguration() throws IOException, InterruptedException {
    breakpoints.flushAll();
    // Exception filters go IN-PHASE (before configurationDone), built by
    // reading the breakpoint manager: a breakpoint already enabled from a
    // previous IDE run arms here — the registerBreakpoint callbacks alone
    // fire on the EDT and would race startup (the HashLink lesson).
    if (backend.supportsExceptionFilters()) {
      sendRequest(exceptionFiltersRequest());
    }
    // object labels via toString: an off-default project setting; only a
    // non-default needs announcing (the server starts with it off)
    if (backend.supportsToStringRendering()
        && HaxeDebuggerSettings.getInstance(getSession().getProject()).isRenderObjectsWithToString()) {
      sendRequest(SetToStringRenderingRequest.of(true));
    }
    if (backend.sendsConfigurationDone()) {
      // releases the debuggee held by the server's startup break
      client.sendRequest(new ConfigurationDoneRequest(), REQUEST_TIMEOUT_MILLIS);
    }
  }

  /**
   * Waits for the adapter's {@code initialized} event, forwarding any output
   * events that precede it (the web adapters chatter during browser startup)
   * and ignoring the rest — the pump is not running yet, so events consumed
   * here would otherwise be lost.
   */
  private void awaitInitializedEvent() throws InterruptedException {
    long deadline = System.currentTimeMillis() + REQUEST_TIMEOUT_MILLIS;
    while (System.currentTimeMillis() < deadline) {
      Event event = client.pollEvent(EVENT_POLL_MILLIS);
      if (event instanceof InitializedEvent) {
        return;
      }
      if (event instanceof OutputEvent output) {
        handleOutput(output);
      }
    }
    LOG.warn("No DAP initialized event within " + REQUEST_TIMEOUT_MILLIS + "ms; continuing anyway");
  }

  // --- event pump (sole pollEvent caller) ---

  private void pumpEvents() {
    try {
      while (!shuttingDown) {
        Event event = client.pollEvent(EVENT_POLL_MILLIS);
        if (event != null && DapConsoleTracer.ENABLED && !(event instanceof OutputEvent)) {
          trace("ev " + DapConsoleTracer.describeEvent(event));
        }
        switch (event) {
          case null -> { /* poll again */ }
          case StoppedEvent stopped -> handleStopped(stopped);
          case ContinuedEvent continued -> handleContinued(continued);
          case OutputEvent output -> handleOutput(output);
          // adapters with source-map-lazy verification (the web adapters)
          // upgrade breakpoints asynchronously once the mapping loads
          case BreakpointEvent breakpointEvent -> breakpoints.onBreakpointEvent(breakpointEvent);
          case ExitedEvent ignored -> {
            // the debuggee's own ProcessHandler reports termination (with the
            // real exit code); an adapter-reported code would be a guess
          }
          case TerminatedEvent ignored -> {
            terminateSession();
            return;
          }
          default -> { /* thread start/exit etc.: the views refresh on the next stop */ }
        }
      }
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    } catch (RuntimeException e) {
      LOG.warn("DAP event pump failed", e);
      terminateSession();
    }
  }

  // A continue for some OTHER thread (a worker resuming in a multi-target
  // session) must not clear the pause the user is inspecting; without a
  // pause on screen it just confirms the running state.
  private void handleContinued(ContinuedEvent continued) {
    if (continued.getBody() != null
        && continued.getBody().getThreadId() != currentThreadId
        && pauseOnScreen) {
      return;
    }
    pauseOnScreen = false;
    getSession().sessionResumed();
  }

  private void handleStopped(StoppedEvent stopped) {
    Integer threadId = stopped.getBody().getThreadId();
    String exceptionText = null;
    if ("exception".equals(stopped.getBody().getReason())) {
      // prefer the runtime's own message (text) over the category (description)
      String text = stopped.getBody().getText();
      String description = stopped.getBody().getDescription();
      exceptionText = text != null ? text : (description != null ? description : "Exception thrown");
      // the gutter icon's tooltip is easy to miss - put the text where the
      // user is already looking
      print(exceptionText + "\n", true);
    }
    if (pauseOnScreen && (threadId == null || threadId != currentThreadId)) {
      // another thread stopped while a pause is on screen: it stays paused
      // (inspectable through the thread list) but must not yank the views
      // mid-inspection; Resume releases it together with everything else
      return;
    }
    currentThreadId = threadId != null ? threadId : 0;
    presentStop(currentThreadId, exceptionText);
    // NO "freeze the world" here for the per-thread-pausing backends, however
    // tempting: the firefox adapter's actor proxies answer requests through a
    // per-thread FIFO queue, and firefox never answers an interrupt that
    // RACES a breakpoint pause - the unanswered request wedges that thread's
    // queue FOREVER (every later stackTrace/evaluate for it times out;
    // verified in the adapter's base actor proxy source).
    // Threads that pause independently therefore also keep RUNNING
    // independently while one of them is on screen.
  }

  private void presentStop(int threadId, @Nullable String exceptionText) {
    pauseOnScreen = true;
    unresponsiveVariableRefs.clear(); // variablesReferences are pause-lifetime
    reportStopped(threadId, exceptionText);
    // run-to-cursor is one-shot: any stop (including a breakpoint reached before
    // the cursor) cancels a pending run-to breakpoint
    breakpoints.clearRunToBreakpoint();
  }

  // Reports the current stop to the session (all threads suspended, the given one active).
  private void reportStopped(int threadId, String exceptionText) {
    List<DapThread> threads = requestThreads();
    List<StackFrame> activeFrames = requestStackTrace(threadId);
    if (activeFrames.isEmpty()) {
      // an empty pause is a bug's symptom, not a state worth silently showing
      LOG.warn("stop for thread " + threadId + " presented WITHOUT frames"
               + " (threads=" + threads.size() + ") - the adapter did not consider it paused?");
    }
    topFrameId = activeFrames.isEmpty() ? -1 : activeFrames.get(0).getId();
    DapSuspendContext context =
      new DapSuspendContext(this, threads, threadId, activeFrames, exceptionText);
    // resolve the top frame's source position HERE, on the pump thread:
    // positionReached's sessionPaused listeners read getCurrentPosition on the
    // EDT, where the resolver's index lookups are prohibited slow operations
    XExecutionStack activeStack = context.getActiveExecutionStack();
    XStackFrame topFrame = activeStack != null ? activeStack.getTopFrame() : null;
    if (topFrame != null) {
      topFrame.getSourcePosition();
    }
    getSession().positionReached(context);
    updateExpressionHighlight(activeFrames);
  }

  // Expression-stepping visualization: highlight the exact span of the
  // expression the interpreter will run next (top frame's column..endColumn).
  private void updateExpressionHighlight(List<StackFrame> activeFrames) {
    if (expressionHighlighter == null) {
      expressionHighlighter = new HaxeExpressionPointHighlighter(getSession().getProject());
    }
    if (!resumeListenerInstalled) {
      resumeListenerInstalled = true;
      getSession().addSessionListener(new XDebugSessionListener() {
        @Override
        public void sessionResumed() {
          expressionHighlighter.clear();
        }

        @Override
        public void sessionStopped() {
          expressionHighlighter.clear();
        }

        // The user picked a frame (possibly of ANOTHER thread) in the frames
        // view: stepping/resume must target the selected frame's thread, not
        // whichever thread happened to report the latest stop.
        @Override
        public void stackFrameChanged() {
          if (getSession().getCurrentStackFrame() instanceof DapStackFrame selected) {
            currentThreadId = selected.threadId();
          }
        }
      });
    }
    if (!expressionStepping || activeFrames.isEmpty()) {
      expressionHighlighter.clear();
      return;
    }
    StackFrame top = activeFrames.get(0);
    String path = top.getSource() != null ? top.getSource().getPath() : null;
    expressionHighlighter.show(path, top.getLine(), top.getColumn(), top.getEndLine(), top.getEndColumn());
  }

  List<DapThread> requestThreads() {
    return sendRequest(new ThreadsRequest()) instanceof ThreadsResponse response && response.isSuccess()
           ? response.getBody().getThreads() : List.of();
  }

  List<StackFrame> requestStackTrace(int threadId) {
    return sendRequest(StackTraceRequest.of(threadId)) instanceof StackTraceResponse response && response.isSuccess()
           ? response.getBody().getStackFrames() : List.of();
  }

  private void handleOutput(OutputEvent output) {
    String text = output.getBody().getOutput();
    if (text != null) {
      print(text, "stderr".equals(output.getBody().getCategory()));
    }
  }

  /** Grey system-output line (e.g. an external adapter's own chatter). */
  public void printSystem(String text) {
    ConsoleView console = getSession().getConsoleView();
    if (console != null) {
      console.print(text, ConsoleViewContentType.SYSTEM_OUTPUT);
    } else {
      processHandler.notifyTextAvailable(text, ProcessOutputTypes.SYSTEM);
    }
  }

  private void print(String text, boolean stderr) {
    ConsoleView console = getSession().getConsoleView();
    if (console != null) {
      console.print(text, stderr ? ConsoleViewContentType.ERROR_OUTPUT : ConsoleViewContentType.NORMAL_OUTPUT);
    } else {
      // startup failures can precede the console; the process handler's
      // listeners (the Console tab once built) still deliver the text
      processHandler.notifyTextAvailable(text, stderr ? ProcessOutputTypes.STDERR : ProcessOutputTypes.STDOUT);
    }
  }

  private void fail(String message) {
    print(message + "\n", true);
    terminateSession();
  }

  private void terminateSession() {
    pauseOnScreen = false;
    teardown();
    getSession().stop();
  }

  // --- XDebugProcess callbacks (EDT: only submit, never block) ---

  @Override
  public void resume(@Nullable XSuspendContext context) {
    onRequestThread(() -> {
      pauseOnScreen = false;
      if (backend.threadsPauseIndependently()) {
        // Resume means "let the PROGRAM run": browser threads pause
        // independently, so every listed thread gets its continue - the one
        // on screen, workers paused at their own breakpoints in the
        // background, and any zombie a page reload left behind. This also
        // self-heals a paused-in-browser/running-in-IDE desync. The
        // continues are FIRE-AND-FORGET: a running thread answers with a
        // harmless error, but a zombie's actor never answers at all, and
        // awaiting it would block Resume for a full timeout per zombie.
        List<DapThread> threads = requestThreads();
        if (threads.isEmpty()) {
          sendRequest(ContinueRequest.of(currentThreadId));
        }
        for (DapThread thread : threads) {
          sendRequestNoWait(ContinueRequest.of(thread.getId()));
        }
      } else {
        // suspend-all servers resume everything on the one continue
        sendRequest(ContinueRequest.of(currentThreadId));
      }
    });
  }

  // Run to cursor: plant a transient breakpoint at the target line and resume.
  // Any stop clears it (handleStopped). If the line has no executable code,
  // don't resume — re-assert the current position so the UI leaves "running".
  @Override
  public void runToPosition(@NotNull XSourcePosition position, @Nullable XSuspendContext context) {
    String path = position.getFile().getPath();
    int line = position.getLine() + 1; // XSourcePosition is 0-based; DAP is 1-based
    int threadId = currentThreadId;
    onRequestThread(() -> {
      if (breakpoints.setRunToBreakpoint(path, line)) {
        pauseOnScreen = false;
        sendRequest(ContinueRequest.of(threadId));
      } else {
        breakpoints.clearRunToBreakpoint();
        reportStopped(threadId, null);
      }
    });
  }

  @Override
  public void startPausing() {
    // the server interrupts the debuggee and reports a stopped(reason:"pause")
    // event; the thread id rides along for servers that pause per-thread
    PauseRequest request = PauseRequest.of(currentThreadId);
    onRequestThread(() -> sendRequest(request));
  }

  @Override
  public void startStepOver(@Nullable XSuspendContext context) {
    NextRequest request = NextRequest.of(currentThreadId);
    onRequestThread(() -> {
      pauseOnScreen = false; // the step's landing must present, not queue
      sendRequest(request);
    });
  }

  @Override
  public void startStepInto(@Nullable XSuspendContext context) {
    StepInRequest request = StepInRequest.of(currentThreadId);
    onRequestThread(() -> {
      pauseOnScreen = false;
      sendRequest(request);
    });
  }

  @Override
  public void startStepOut(@Nullable XSuspendContext context) {
    StepOutRequest request = StepOutRequest.of(currentThreadId);
    onRequestThread(() -> {
      pauseOnScreen = false;
      sendRequest(request);
    });
  }

  @Override
  public @Nullable XSmartStepIntoHandler<?> getSmartStepIntoHandler() {
    return backend.createSmartStepIntoHandler(this);
  }

  @Override
  public @NotNull XDebugTabLayouter createTabLayouter() {
    XDebugTabLayouter layouter = backend.createTabLayouter(this);
    return layouter != null ? layouter : super.createTabLayouter();
  }

  // The Variables view's settings (gear) menu gets the toString-label toggle
  // when the backend can honor it (the vshaxe server cannot — it always
  // stringifies and offers no control, so no toggle is shown there).
  @Override
  public void registerAdditionalActions(@NotNull DefaultActionGroup leftToolbar,
                                        @NotNull DefaultActionGroup topToolbar,
                                        @NotNull DefaultActionGroup settings) {
    super.registerAdditionalActions(leftToolbar, topToolbar, settings);
    if (backend.supportsToStringRendering()) {
      settings.add(new HaxeToStringRenderToggleAction(getSession(), this::pushToStringRendering));
    }
    if (backend.supportsExpressionStepping()) {
      settings.add(new HaxeExpressionSteppingToggleAction(() -> expressionStepping, this::pushExpressionStepping));
    }
  }

  // Live toggle (eval only): tell the adapter, then re-report the current stop
  // so the frames carry (or drop) the expression spans and the highlight
  // appears/disappears without another step.
  private void pushExpressionStepping(boolean enabled) {
    if (!backend.supportsExpressionStepping()) {
      return;
    }
    expressionStepping = enabled;
    if (!enabled && expressionHighlighter != null) {
      expressionHighlighter.clear();
    }
    onRequestThread(() -> {
      sendRequest(SetExpressionSteppingRequest.of(enabled));
      if (getSession().isSuspended()) {
        reportStopped(currentThreadId, null);
      }
    });
  }

  // Live toggle: tell the server, then rebuild the views so the CURRENT
  // stop's variables re-render with the new labels (no restart needed).
  // Public: the settings page pushes to every running session on apply; a
  // backend that cannot honor the request (vshaxe) is a no-op.
  public void pushToStringRendering(boolean enabled) {
    if (!backend.supportsToStringRendering()) {
      return;
    }
    onRequestThread(() -> {
      sendRequest(SetToStringRenderingRequest.of(enabled));
      getSession().rebuildViews();
    });
  }

  /**
   * Smart step into the chosen callee (custom custom/stepIntoFunction
   * request). {@code occurrence} picks WHICH invocation on the line when the
   * same function is called more than once (1-based).
   */
  void stepIntoFunction(String className, String functionName, int occurrence) {
    StepIntoFunctionRequest request =
      StepIntoFunctionRequest.of(currentThreadId, className, functionName, occurrence);
    onRequestThread(() -> {
      pauseOnScreen = false;
      sendRequest(request);
    });
  }

  @Override
  public void stop() {
    shuttingDown = true;
    requestExecutor.execute(() -> {
      DapEndpoint dapClient = client;
      if (dapClient != null) {
        try {
          dapClient.sendRequest(new DisconnectRequest(), DISCONNECT_TIMEOUT_MILLIS);
        } catch (IOException | InterruptedException e) {
          if (e instanceof InterruptedException) {
            Thread.currentThread().interrupt();
          }
        }
      }
      teardown();
    });
    requestExecutor.shutdown();
  }

  private synchronized void teardown() {
    shuttingDown = true;
    DapEndpoint dapClient = client;
    client = null;
    if (dapClient != null) {
      try {
        dapClient.close();
      } catch (IOException ignored) {
      }
    }
    try {
      backend.close();
    } catch (IOException ignored) {
    }
    if (!processHandler.isProcessTerminated()) {
      processHandler.destroyProcess();
    }
  }

  // --- plumbing for breakpoints/frames/values ---

  /**
   * Runs work on the single-thread DAP request executor. Fire-and-forget:
   * work submitted while the session tears down is silently dropped — fine
   * for one-way requests, WRONG for work completing a promise, tree node or
   * callback (use the two-argument overload there).
   */
  public void onRequestThread(Runnable work) {
    onRequestThread(work, () -> {
    });
  }

  /**
   * Runs work on the single-thread DAP request executor; when the session is
   * tearing down (executor already shut down), runs {@code onRejected} on the
   * calling thread instead. A caller holding a promise, tree node or callback
   * MUST complete it in {@code onRejected}, or the platform waits on it
   * forever (a hung smart-step popup, a permanent "Collecting data" node).
   */
  public void onRequestThread(Runnable work, Runnable onRejected) {
    if (!requestExecutor.isShutdown()) {
      try {
        requestExecutor.execute(work);
        return;
      } catch (RejectedExecutionException ignored) {
        // shut down between the check and the submit
      }
    }
    onRejected.run();
  }

  /** Fire-and-forget request: the response (or its absence) is ignored. */
  private void sendRequestNoWait(Request request) {
    DapEndpoint dapClient = client;
    if (dapClient == null) {
      return;
    }
    trace(">> " + DapConsoleTracer.describeRequest(request) + " (no-wait)");
    try {
      dapClient.sendRequestNoWait(request);
    } catch (IOException e) {
      if (!shuttingDown) {
        LOG.warn("DAP request '" + request.getCommand() + "' could not be sent", e);
      }
    }
  }

  /** Blocking request; only call on the request executor or the event pump. */
  @Nullable Response sendRequest(Request request) {
    DapEndpoint dapClient = client;
    if (dapClient == null) {
      return null;
    }
    trace(">> " + DapConsoleTracer.describeRequest(request));
    long start = System.currentTimeMillis();
    try {
      Response response = dapClient.sendRequest(request, backend.requestTimeoutMillis());
      trace("<< " + request.getCommand() + " seq=" + response.getRequest_seq()
            + (response.isSuccess() ? " ok" : " ERROR: " + response.getMessage())
            + " (" + (System.currentTimeMillis() - start) + "ms)");
      return response;
    } catch (IOException e) {
      trace("!! " + request.getCommand() + " FAILED after " + (System.currentTimeMillis() - start)
            + "ms: " + e.getMessage());
      if (!shuttingDown) {
        LOG.warn("DAP request '" + request.getCommand() + "' failed", e);
      }
      return null;
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      return null;
    }
  }

  // Tracing lives in DapConsoleTracer (flip its ENABLED constant in code).
  private void trace(String line) {
    if (DapConsoleTracer.ENABLED) {
      printSystem("[dap] " + line + "\n");
    }
  }

  public List<Scope> requestScopes(int frameId) {
    return sendRequest(ScopesRequest.of(frameId)) instanceof ScopesResponse response && response.isSuccess()
           ? response.getBody().getScopes() : List.of();
  }

  /**
   * References whose variables request TIMED OUT at this stop. Firefox's
   * worker devtools can crash while enumerating a specific object's
   * properties (broken getter/previewer handling) - that object's actor then
   * never answers, at all, ever. The platform re-requests
   * on every tree rebuild; without this cache each rebuild would stall the
   * request thread for a full timeout PER poisoned object. Pause-scoped:
   * cleared when a new stop is presented (references are pause-lifetime).
   */
  private final Set<Integer> unresponsiveVariableRefs = ConcurrentHashMap.newKeySet();

  public List<Variable> requestVariables(int variablesReference) {
    if (unresponsiveVariableRefs.contains(variablesReference)) {
      trace("~~ variables ref=" + variablesReference + " skipped (timed out at this stop before)");
      return List.of();
    }
    Response response = sendRequest(VariablesRequest.of(variablesReference));
    if (response == null) {
      unresponsiveVariableRefs.add(variablesReference);
      return List.of();
    }
    return response instanceof VariablesResponse ok && ok.isSuccess()
           ? ok.getBody().getVariables() : List.of();
  }

  /**
   * The calls on the stopped line the user can choose to step into (empty when
   * running, no frames, or the adapter cannot resolve any callee) - DAP
   * stepInTargets, for backends whose adapter reports targets.
   */
  public List<StepInTarget> requestStepInTargets() {
    int frameId = topFrameId;
    if (frameId < 0) {
      return List.of();
    }
    Response response = sendRequest(StepInTargetsRequest.of(frameId));
    if (response instanceof StepInTargetsResponse ok && ok.isSuccess()
        && ok.getBody() != null && ok.getBody().getTargets() != null) {
      return ok.getBody().getTargets();
    }
    LOG.warn("smart-step: stepInTargets yielded no usable response for frame " + frameId
             + " (response=" + (response == null ? "null"
                                : response.getClass().getSimpleName() + " success=" + response.isSuccess()
                                  + " message=" + response.getMessage()) + ")");
    return List.of();
  }

  /** Smart step into: enter the specific call chosen from the step-in targets. */
  public void stepIntoTarget(int targetId) {
    StepInRequest request = StepInRequest.of(currentThreadId, targetId);
    onRequestThread(() -> {
      pauseOnScreen = false;
      sendRequest(request);
    });
  }

  /** The backend driving this session (for same-package collaborators). */
  DapBackend backend() {
    return backend;
  }

  /**
   * Whether the adapter offers runtime completions (DAP {@code completions};
   * js-debug does, the firefox adapter and the haxe-side servers do not).
   */
  public boolean supportsRuntimeCompletions() {
    Capabilities caps = capabilities;
    return caps != null && Boolean.TRUE.equals(caps.getSupportsCompletionsRequest());
  }

  /**
   * Whether this session evaluates against a foreign runtime that knows more
   * than the Haxe PSI (the browser) — so the evaluate/watch views should not
   * flag "unresolved" identifiers. Independent of runtime-completion support:
   * evaluation is runtime-truth even where DAP completions are unavailable
   * (the firefox adapter).
   */
  public boolean evaluatesAgainstForeignRuntime() {
    return backend.evaluatesAgainstForeignRuntime();
  }

  /**
   * Runtime completions for the evaluate view: the ADAPTER completes the text
   * against the live runtime, which knows identifiers the Haxe PSI cannot —
   * browser globals behind incomplete externs, dynamically attached fields.
   * Safe to call from a completion thread: the request runs on the DAP
   * request executor with a short bounded wait, and an unavailable session
   * (running, busy, torn down) yields an empty list rather than blocking.
   *
   * @param text   the full expression text being edited
   * @param column 1-based caret position within {@code text}
   */
  public List<CompletionItem>
  requestRuntimeCompletions(String text, int column) {
    if (!supportsRuntimeCompletions() || !getSession().isSuspended()) {
      return List.of();
    }
    int frameId = topFrameId;
    CompletableFuture<List<CompletionItem>>
      future = new CompletableFuture<>();
    onRequestThread(() -> {
      CompletionsRequest request = CompletionsRequest.of(frameId >= 0 ? frameId : null, text, column);
      future.complete(sendRequest(request) instanceof CompletionsResponse response && response.isSuccess()
                      && response.getBody() != null && response.getBody().getTargets() != null
                      ? response.getBody().getTargets() : List.of());
    }, () -> future.complete(List.of()));
    try {
      return future.get(2, TimeUnit.SECONDS);
    } catch (Exception e) {
      if (e instanceof InterruptedException) {
        Thread.currentThread().interrupt();
      }
      return List.of(); // busy session or teardown: completion just has no extras
    }
  }

  /**
   * Sets the named child of a container reference to `value`. Returns the
   * variable's NEW state (value, type, variablesReference) — the caller must
   * adopt ALL of it: assigning a container value creates a fresh reference,
   * and keeping the old one shows the old children after the edit. Throws
   * with the server's message on failure.
   */
  SetVariableResponseBody requestSetVariable(int containerReference, String name, String value) {
    Response response = sendRequest(SetVariableRequest.of(containerReference, name, value));
    if (response instanceof SetVariableResponse ok && response.isSuccess() && ok.getBody() != null) {
      return ok.getBody();
    }
    throw new IllegalStateException(response != null && response.getMessage() != null
                                    ? response.getMessage() : "the debugger rejected the change");
  }

  // --- XDebugProcess wiring ---

  @Override
  protected @Nullable ProcessHandler doGetProcessHandler() {
    return processHandler;
  }

  @Override
  public @NotNull XDebuggerEditorsProvider getEditorsProvider() {
    return new HaxeDebuggerEditorsProvider();
  }

  @Override
  public XBreakpointHandler<?> @NotNull [] getBreakpointHandlers() {
    if (!backend.supportsExceptionFilters()) {
      return new XBreakpointHandler<?>[]{lineBreakpointHandler()};
    }
    return new XBreakpointHandler<?>[]{lineBreakpointHandler(), exceptionBreakpointHandler()};
  }

  private XBreakpointHandler<XLineBreakpoint<XBreakpointProperties>> lineBreakpointHandler() {
    return new XBreakpointHandler<>(HaxeBreakpointType.class) {
      @Override
      public void registerBreakpoint(@NotNull XLineBreakpoint<XBreakpointProperties> breakpoint) {
        breakpoints.register(breakpoint);
      }

      @Override
      public void unregisterBreakpoint(@NotNull XLineBreakpoint<XBreakpointProperties> breakpoint, boolean temporary) {
        breakpoints.unregister(breakpoint);
      }
    };
  }

  // The single shared Haxe exception category: any change (a toggle, a
  // Notifications checkbox, a per-class add) recomputes the filters.
  private XBreakpointHandler<XBreakpoint<HaxeExceptionBreakpointProperties>> exceptionBreakpointHandler() {
    return new XBreakpointHandler<>(HaxeExceptionBreakpointType.class) {
      @Override
      public void registerBreakpoint(@NotNull XBreakpoint<HaxeExceptionBreakpointProperties> breakpoint) {
        updateExceptionFilters();
      }

      @Override
      public void unregisterBreakpoint(@NotNull XBreakpoint<HaxeExceptionBreakpointProperties> breakpoint, boolean temporary) {
        updateExceptionFilters();
      }
    };
  }

  // Recomputed whenever an exception breakpoint toggles (live path — posted to
  // the request thread). See exceptionFiltersRequest for how the set is built.
  private void updateExceptionFilters() {
    onRequestThread(() -> sendRequest(exceptionFiltersRequest()));
  }

  // The Notifications checkboxes mapped onto THIS backend's filter vocabulary;
  // see HaxeExceptionBreakpointType.buildFiltersRequest for how the set is built.
  private SetExceptionBreakpointsRequest exceptionFiltersRequest() {
    return HaxeExceptionBreakpointType.buildFiltersRequest(
      getSession().getProject(), backend.anyThrowFilterId(), backend.uncaughtFilterId(),
      backend.criticalFilterId());
  }

  private static Thread daemon(Runnable work, String name) {
    Thread thread = new Thread(work, name);
    thread.setDaemon(true);
    return thread;
  }
}
