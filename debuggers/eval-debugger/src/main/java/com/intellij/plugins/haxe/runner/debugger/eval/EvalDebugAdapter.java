package com.intellij.plugins.haxe.runner.debugger.eval;

import com.intellij.plugins.haxe.runner.debugger.dap.DapPaths;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.Breakpoint;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.Capabilities;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.DapThread;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.Event;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.ProtocolMessage;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.Request;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.Response;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.Scope;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.Source;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.SourceBreakpoint;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.StackFrame;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.Variable;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.events.*;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.requests.*;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.responses.*;
import com.intellij.plugins.haxe.runner.debugger.dap.transport.DapConnection;
import java.io.Closeable;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import tools.jackson.databind.JsonNode;

/**
 * The in-process DAP adapter for the EVAL debugger: presents a DAP server to
 * the IDE-side {@code DapClient} and translates every request into the haxe
 * compiler's eval-debugger JSON-RPC protocol (and its notifications back into
 * DAP events). Reference: vshaxe/eval-debugger's Main.hx; the wire behaviour
 * this relies on is live-verified in {@code EvalLiveTest} / docs.
 *
 * Lifecycle: construct (binds the VM listener socket), let the caller spawn
 * {@code haxe <args> -D eval-debugger=127.0.0.1:<port> --interp} (or any
 * compilation whose MACROS should be debugged), then {@link #start} with the
 * DAP connection. The eval VM connects to our listener and WAITS before
 * running main; configurationDone releases it with the protocol's continue.
 *
 * Simpler than the hxcpp sibling in two load-bearing ways: the VM assigns a
 * single id space for scopes and variables (so variablesReference IS the
 * VM id — no path registry), and stops always carry the thread id.
 */
public class EvalDebugAdapter implements Closeable {
  /** Ceiling on sub-expression step-in coalescing; a normal line needs a handful. */
  private static final int MAX_STEP_IN_SUBSTEPS = 64;
  /**
   * Ceiling for the smart-step walk: skipped callees are traversed
   * sub-expression by sub-expression (stepOut would overshoot the line), so
   * allow plenty; each sub-step is one fast local RPC.
   */
  private static final int MAX_SMART_STEP_SUBSTEPS = 4096;

  private final ServerSocket vmListener;
  private final long vmConnectTimeoutMillis;
  private final CompletableFuture<EvalProtocol> protocolFuture = new CompletableFuture<>();
  private final AtomicInteger nextSeq = new AtomicInteger(1);

  private DapConnection dap;
  private EvalConnection vmConnection;
  private Thread acceptThread;
  private Thread requestThread;
  private volatile boolean closed = false;
  /** ON = raw sub-expression steps + exact expression spans in stack frames. */
  private volatile boolean expressionStepping = false;
  /** Thread the VM last reported stopped; null while running. */
  private volatile Integer stoppedThreadId;
  /**
   * User breakpoint lines by normalized source path, mirroring what was sent
   * to the VM. The step-emulation loops consult this so a landing on a
   * breakpoint line ends the step there (breakpoints WIN over steps, like
   * hxcpp's HandleBreakpoints and the hashlink adapter's temp-vs-user hits).
   */
  private final Map<String, Set<Integer>> breakpointLines =
    new ConcurrentHashMap<>();
  /**
   * Runtime type by variablesReference, remembered as values are handed out.
   * Consulted before a setVariable: writing a String's derived rows
   * (length/byteLength) CRASHES the eval VM ("Cannot run Haxe code in a
   * non-Haxe thread" assert, live-reproduced), so those are refused here.
   * References die with each resume.
   */
  private final Map<Integer, String> referenceTypes =
    new ConcurrentHashMap<>();
  /** Thread whose step loop is running on the request thread; null otherwise. */
  private volatile Integer steppingThreadId;
  /**
   * Set by the reader thread when the VM pushes breakpointStop DURING a step:
   * the VM answers the step verb normally and ADDITIONALLY notifies that the
   * landing is a user breakpoint (live-verified). The loop consumes this and
   * reports the stop as "breakpoint" instead of stepping onward past it.
   */
  private volatile Integer breakpointHitDuringStep;
  /** Like {@link #breakpointHitDuringStep} for an exceptionStop pushed mid-step. */
  private volatile String exceptionDuringStepText;
  /**
   * True from an exception stop until the next resume: the debuggee is inside
   * exception dispatch, where the step-coalescing heuristics are meaningless
   * (positions barely move, stacks unwind or wedge) — a coalescing loop there
   * can spin its whole cap in futile RPCs, freezing the IDE for tens of
   * seconds (reported live). While unwinding, every step press is ONE raw
   * verb: it visibly walks the unwind chain one position at a time and is
   * bounded by construction.
   */
  private volatile boolean unwindingException;
  /** Ensures exactly one terminated event however the session ends. */
  private final AtomicBoolean terminatedSent =
    new AtomicBoolean();
  /**
   * True while the "all"-throws exception option is armed on the VM. Without
   * it, every exceptionStop is by construction an UNCAUGHT exception — the
   * state the VM can never leave (see {@link #letUncaughtExceptionKillTheProgram}).
   */
  private volatile boolean caughtExceptionFilterActive;
  /** Whether the IDE ever configured exception options this session. */
  private volatile boolean exceptionOptionsConfigured;

  public EvalDebugAdapter(long vmConnectTimeoutMillis) throws IOException {
    this.vmConnectTimeoutMillis = vmConnectTimeoutMillis;
    vmListener = new ServerSocket();
    vmListener.setReuseAddress(false);
    vmListener.bind(new InetSocketAddress("127.0.0.1", 0));
  }

  /** The port for the debuggee's {@code -D eval-debugger=127.0.0.1:<port>}. */
  public int getVmPort() {
    return vmListener.getLocalPort();
  }

  /** Starts the adapter threads against an established DAP connection. */
  public void start(DapConnection dapConnection) {
    this.dap = dapConnection;
    acceptThread = daemon("eval-vm-accept", this::acceptVm);
    requestThread = daemon("eval-dap-requests", this::requestLoop);
  }

  private static Thread daemon(String name, Runnable body) {
    Thread thread = new Thread(body, name);
    thread.setDaemon(true);
    thread.start();
    return thread;
  }

  // ---------------------------------------------------------------------- VM

  private void acceptVm() {
    try {
      Socket socket = vmListener.accept();
      vmConnection = new EvalConnection(socket.getInputStream(), socket.getOutputStream());
      // listener registered BEFORE start so no early notification is dropped
      vmConnection.setEventListener(this::handleVmEvent);
      vmConnection.setOnDisconnected(() -> {
        // the eval VM's socket closing means the program (or the compilation
        // being macro-debugged) finished — that IS session termination
        if (!closed) {
          sendTerminatedOnce();
        }
      });
      vmConnection.start();
      protocolFuture.complete(new EvalProtocol(vmConnection));
    } catch (IOException e) {
      if (!closed) {
        protocolFuture.completeExceptionally(e);
      } else {
        protocolFuture.cancel(false);
      }
    }
  }

  /** The connected protocol, waiting for the VM when necessary. */
  private EvalProtocol vm(long timeoutMillis) throws IOException {
    try {
      return protocolFuture.get(timeoutMillis, TimeUnit.MILLISECONDS);
    } catch (TimeoutException e) {
      throw new IOException("The haxe eval VM did not connect within " + timeoutMillis + " ms. "
                            + "Was haxe launched with -D eval-debugger=127.0.0.1:" + getVmPort() + "?");
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IOException("Interrupted while waiting for the eval VM to connect");
    } catch (ExecutionException | CancellationException e) {
      throw new IOException("Eval VM connection failed: " + e.getCause(), e.getCause());
    }
  }

  private EvalProtocol vm() throws IOException {
    return vm(vmConnectTimeoutMillis);
  }

  // ------------------------------------------------------------ DAP requests

  private void requestLoop() {
    try {
      while (true) {
        ProtocolMessage message = dap.receive();
        if (message == null) {
          return;
        }
        if (message instanceof Request request) {
          try {
            dispatch(request);
          } catch (Exception e) {
            // answer THIS request with the failure; the loop itself must survive,
            // or every later request times out and the client's views go blank
            sendErrorResponse(request, e.getMessage() != null ? e.getMessage() : e.toString());
          }
        }
      }
    } catch (IOException e) {
      if (!closed) {
        System.err.println("EvalDebugAdapter DAP request loop died: " + e);
      }
    }
  }

  private void dispatch(Request request) throws IOException {
    switch (request) {
      case InitializeRequest r -> handleInitialize(r);
      case LaunchRequest r -> handleLaunch(r);
      case SetBreakpointsRequest r -> handleSetBreakpoints(r);
      case SetExceptionBreakpointsRequest r -> handleSetExceptionBreakpoints(r);
      case ConfigurationDoneRequest r -> handleConfigurationDone(r);
      case ThreadsRequest r -> handleThreads(r);
      case StackTraceRequest r -> handleStackTrace(r);
      case ScopesRequest r -> handleScopes(r);
      case VariablesRequest r -> handleVariables(r);
      case ContinueRequest r -> handleContinue(r);
      case NextRequest r -> handleStep(r);
      case StepInRequest r -> handleStep(r);
      case StepOutRequest r -> handleStep(r);
      case StepIntoFunctionRequest r -> handleStepIntoFunction(r);
      case SetVariableRequest r -> handleSetVariable(r);
      case SetExpressionSteppingRequest r -> {
        expressionStepping = r.getArguments() != null && r.getArguments().isEnabled();
        sendResponse(r, new Response());
      }
      case PauseRequest r -> handlePause(r);
      case EvaluateRequest r -> handleEvaluate(r);
      case DisconnectRequest r -> handleDisconnect(r);
      default -> sendErrorResponse(request, "Unsupported request '" + request.getCommand() + "'");
    }
  }

  private void handleInitialize(InitializeRequest request) throws IOException {
    Capabilities capabilities = new Capabilities();
    capabilities.setSupportsConfigurationDoneRequest(true);
    capabilities.setSupportsVariableType(true);
    capabilities.setSupportsEvaluateForHovers(true);
    InitializeResponse response = new InitializeResponse();
    response.setBody(capabilities);
    sendResponse(request, response);
    sendEvent(new InitializedEvent());
  }

  private void handleLaunch(LaunchRequest request) throws IOException {
    // the haxe process is spawned by the caller (IDE runner / test); launch
    // just means "the VM connected and is held waiting before main"
    vm();
    sendResponse(request, new LaunchResponse());
  }

  private void handleSetBreakpoints(SetBreakpointsRequest request) throws IOException {
    String file = request.getArguments().getSource().getPath();
    List<SourceBreakpoint> requested = request.getArguments().getBreakpoints() != null
                                       ? request.getArguments().getBreakpoints() : List.of();
    int[] lines = new int[requested.size()];
    Set<Integer> lineSet = new HashSet<>();
    for (int i = 0; i < requested.size(); i++) {
      lines[i] = requested.get(i).getLine();
      lineSet.add(requested.get(i).getLine());
    }
    List<EvalProtocol.EvalBreakpoint> registered = vm().setBreakpoints(file, lines);
    // mirror for the step loops; setBreakpoints REPLACES the file's set
    breakpointLines.put(DapPaths.toMatchKey(file), lineSet);

    List<Breakpoint> verified = new ArrayList<>();
    for (int i = 0; i < requested.size(); i++) {
      Breakpoint breakpoint = new Breakpoint();
      breakpoint.setVerified(true);
      breakpoint.setLine(requested.get(i).getLine());
      breakpoint.setSource(request.getArguments().getSource());
      if (i < registered.size()) {
        breakpoint.setId(registered.get(i).id());
      }
      verified.add(breakpoint);
    }
    SetBreakpointsResponseBody body = new SetBreakpointsResponseBody();
    body.setBreakpoints(verified);
    SetBreakpointsResponse response = new SetBreakpointsResponse();
    response.setBody(body);
    sendResponse(request, response);
  }

  private void handleSetExceptionBreakpoints(SetExceptionBreakpointsRequest request) throws IOException {
    List<String> filters = request.getArguments() != null && request.getArguments().getFilters() != null
                           ? request.getArguments().getFilters() : List.of();
    List<String> options = toVmExceptionOptions(filters);
    vm().setExceptionOptions(options);
    exceptionOptionsConfigured = true;
    caughtExceptionFilterActive = options.contains("all");
    sendResponse(request, new SetExceptionBreakpointsResponse());
  }

  /**
   * The IDE speaks the shared filter vocabulary ("thrown"/"uncaught"/
   * "critical", from the hxcpp server); the eval VM's setExceptionOptions
   * understands "all" and "uncaught". Translate, dropping what cannot be
   * expressed (no critical-error category exists in eval) rather than
   * failing the configure.
   */
  private static List<String> toVmExceptionOptions(List<String> filters) {
    List<String> options = new ArrayList<>();
    for (String filter : filters) {
      switch (filter) {
        case "thrown", "all" -> options.add("all"); // every throw, caught or not
        case "uncaught" -> options.add("uncaught");
        default -> { }
      }
    }
    return options;
  }

  private void handleConfigurationDone(ConfigurationDoneRequest request) throws IOException {
    if (!exceptionOptionsConfigured) {
      // the VM's DEFAULT is to stop on uncaught exceptions (live-verified) —
      // an unconfigured session must not stop where the user set nothing up
      try {
        vm().setExceptionOptions(List.of());
      } catch (IOException tolerated) {
        // an old VM without the method still debugs; it just keeps its default
      }
    }
    // release the VM, which waits before main (script) / the macro (build)
    resumed();
    resumeToleratingExit();
    sendResponse(request, new ConfigurationDoneResponse());
  }

  /**
   * Resumes the VM, treating a connection close during the request as
   * SUCCESS: the VM acks continue from a helper thread while the resumed
   * program runs, and when the program finishes the process can exit before
   * that ack is flushed — the resume happened, the terminated event (from
   * the disconnect callback) ends the session. Live-observed race. Any OTHER
   * failure (a request timeout, a socket reset mid-write) means the VM is
   * gone or wedged — the session is ended deterministically rather than
   * letting every later request burn its own timeout.
   */
  private void resumeToleratingExit() throws IOException {
    try {
      vm().resume();
    } catch (EvalConnectionClosedException ignored) {
      // program ran to completion during the resume
    } catch (IOException unresponsive) {
      endSessionWithUnresponsiveVm();
    }
  }

  private void sendTerminatedOnce() {
    if (terminatedSent.compareAndSet(false, true)) {
      sendEventQuietly(new TerminatedEvent());
    }
  }

  /**
   * The VM stopped answering (request timeout, or the socket reset under a
   * write) without closing its socket, so no disconnect — and thus no
   * terminated event — would ever come on its own. Left alone, the session
   * sits "sort of stuck" while every queued request burns a full timeout (the
   * reported 25-50s IDE freezes). Close the connection ourselves — pending
   * and future requests fail fast — and end the session with ONE terminated.
   */
  private void endSessionWithUnresponsiveVm() {
    if (vmConnection != null) {
      vmConnection.close();
    }
    sendTerminatedOnce();
  }

  /**
   * A step drove the interpreter into an UNCAUGHT exception. The eval VM does
   * NOT raise an exceptionStop for a step the way it does for continue (verified
   * live); instead the stack unwinds out from under us and further VM calls
   * report "No frame found" (an {@link EvalProtocolException}), leaving the
   * process WEDGED with the exception pending — no stop, no exit. Resuming runs
   * the exception off the end, which exits the process (its message printed to
   * the debuggee's stderr, shown in the IDE console), exactly like continuing
   * into an uncaught throw. Reports the step as program-ended so no phantom
   * stop is synthesized; the terminated event comes from the VM disconnect.
   * Without this, stepping onto a throw hangs the session (the caller loops
   * on / errors out of the vanished stack).
   */
  private StepOutcome resumeOffUncaughtException() throws IOException {
    resumed();
    try {
      vm().resume();
    } catch (EvalConnectionClosedException alreadyEnded) {
      // the process beat us to exiting; the disconnect sends terminated
      return StepOutcome.PROGRAM_ENDED;
    } catch (IOException unresponsive) {
      endSessionWithUnresponsiveVm();
      return StepOutcome.PROGRAM_ENDED;
    }
    // On haxe <= 4.3 the resumed exception runs the program off within
    // milliseconds (disconnect -> terminated). The haxe 5 preview's VM
    // instead survives as a ZOMBIE: resume acknowledged, stack permanently
    // gone, program never finishes, no event ever pushed (live-verified,
    // including that further resumes and pause change nothing). Give the
    // legitimate death a moment, then probe: a VM still answering from the
    // frame-less state is that zombie - end the session deterministically
    // (the IDE tears the process down with it).
    try {
      Thread.sleep(800);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      return StepOutcome.PROGRAM_ENDED;
    }
    try {
      vm().getThreads(); // answered = the VM is still there
      endSessionWithUnresponsiveVm();
    } catch (EvalProtocolException stillTalking) {
      endSessionWithUnresponsiveVm(); // an error REPLY is also "still there"
    } catch (IOException dyingOrDead) {
      // connection gone (or going): the normal death; disconnect terminates
    }
    return StepOutcome.PROGRAM_ENDED;
  }

  private void handleThreads(ThreadsRequest request) throws IOException {
    List<DapThread> dapThreads = new ArrayList<>();
    for (EvalProtocol.EvalThread thread : vm().getThreads()) {
      DapThread dapThread = new DapThread();
      dapThread.setId(thread.id());
      dapThread.setName(thread.name());
      dapThreads.add(dapThread);
    }
    ThreadsResponseBody body = new ThreadsResponseBody();
    body.setThreads(dapThreads);
    ThreadsResponse response = new ThreadsResponse();
    response.setBody(body);
    sendResponse(request, response);
  }

  private void handleStackTrace(StackTraceRequest request) throws IOException {
    int threadId = request.getArguments().getThreadId();
    List<StackFrame> stackFrames = new ArrayList<>();
    for (EvalProtocol.EvalStackFrame frame : vm().stackTrace(threadId)) {
      if (frame.artificial()) {
        continue; // interpreter-internal frames are noise to the user
      }
      StackFrame stackFrame = new StackFrame();
      stackFrame.setId(frame.id());
      stackFrame.setName(frame.name());
      stackFrame.setLine(frame.line());
      stackFrame.setColumn(frame.column());
      if (expressionStepping) {
        // the exact span of the expression about to run - the IDE highlights it
        stackFrame.setEndLine(frame.endLine());
        stackFrame.setEndColumn(frame.endColumn());
      }
      stackFrame.setSource(toSource(frame.source()));
      stackFrames.add(stackFrame);
    }
    StackTraceResponseBody body = new StackTraceResponseBody();
    body.setStackFrames(stackFrames);
    body.setTotalFrames(stackFrames.size());
    StackTraceResponse response = new StackTraceResponse();
    response.setBody(body);
    sendResponse(request, response);
  }

  private void handleScopes(ScopesRequest request) throws IOException {
    List<Scope> scopes = new ArrayList<>();
    for (EvalProtocol.EvalScope scopeInfo : vm().getScopes(request.getArguments().getFrameId())) {
      Scope scope = new Scope();
      scope.setName(scopeInfo.name());
      scope.setVariablesReference(scopeInfo.id());
      scope.setExpensive(false);
      scopes.add(scope);
    }
    ScopesResponseBody body = new ScopesResponseBody();
    body.setScopes(scopes);
    ScopesResponse response = new ScopesResponse();
    response.setBody(body);
    sendResponse(request, response);
  }

  private void handleVariables(VariablesRequest request) throws IOException {
    List<Variable> variables = new ArrayList<>();
    for (EvalProtocol.EvalVar var : vm().getVariables(request.getArguments().getVariablesReference())) {
      rememberReferenceType(var);
      variables.add(toVariable(var));
    }
    VariablesResponseBody body = new VariablesResponseBody();
    body.setVariables(variables);
    VariablesResponse response = new VariablesResponse();
    response.setBody(body);
    sendResponse(request, response);
  }

  private void handleContinue(ContinueRequest request) throws IOException {
    if (unwindingException && !caughtExceptionFilterActive) {
      letUncaughtExceptionKillTheProgram();
    } else {
      resumed();
      resumeToleratingExit();
    }
    sendResponse(request, new ContinueResponse());
  }

  /**
   * An UNCAUGHT exception stop is a state the VM can never leave forward:
   * every continue or step RE-EXECUTES the whole throw expression (the
   * exception constructor runs again, then the same exceptionStop — an
   * infinite loop, live-verified; the program cannot die while the option is
   * armed, and closing the socket wedges the process instead of freeing it).
   * The one working exit: clear the exception options FIRST, then resume —
   * the re-executed throw finally propagates for real and the program dies
   * naturally, with its own uncaught-exception stderr and exit code.
   * Only called when the caught-throws option is off, which makes every
   * exceptionStop uncaught by construction; with "all" armed a caught
   * exception's stop resumes normally instead.
   */
  private void letUncaughtExceptionKillTheProgram() throws IOException {
    try {
      vm().setExceptionOptions(List.of());
    } catch (IOException tolerated) {
      // resume regardless; worst case the VM re-stops at the same throw
    }
    resumed();
    resumeToleratingExit();
  }

  /** Step-shaped wrapper: the step "lands" in the program's death. */
  private StepOutcome letUncaughtExceptionKillTheProgramAsStep() throws IOException {
    letUncaughtExceptionKillTheProgram();
    return StepOutcome.PROGRAM_ENDED;
  }

  private enum StepOutcome { STEPPED, HIT_BREAKPOINT, PROGRAM_ENDED }

  /** The stopped thread id, or null after sending the "not stopped" error. */
  private Integer requireStoppedThread(Request request) throws IOException {
    Integer thread = stoppedThreadId;
    if (thread == null) {
      sendErrorResponse(request, "Cannot step: the debuggee is not stopped");
    }
    return thread;
  }

  /** The typed DAP response matching the step verb of {@code request}. */
  private static Response stepResponseFor(Request request) {
    return switch (request) {
      case NextRequest r -> new NextResponse();
      case StepInRequest r -> new StepInResponse();
      default -> new StepOutResponse();
    };
  }

  /**
   * Emits the DAP stopped event for a completed step: an exception landing when
   * the verb ran into one, otherwise step / breakpoint per the outcome. A
   * program that ran to completion emits nothing — the terminated event follows
   * from the process disconnect.
   */
  private void sendStepStopped(int thread, StepOutcome outcome, String vmException) throws IOException {
    if (vmException != null) {
      sendStopped("exception", thread, vmException);
    } else {
      switch (outcome) {
        case STEPPED -> sendStopped("step", thread, null);
        case HIT_BREAKPOINT -> sendStopped("breakpoint", thread, null);
        case PROGRAM_ENDED -> { } // terminated event follows from the disconnect
      }
    }
  }

  private void handleStep(Request request) throws IOException {
    Integer thread = requireStoppedThread(request);
    if (thread == null) {
      return;
    }
    if (unwindingException && !caughtExceptionFilterActive) {
      // stepping at an uncaught exception stop only re-runs the throw
      // expression forever (the user reported stepping in circles through the
      // exception constructor); the sole way forward is the program's death
      letUncaughtExceptionKillTheProgram();
      sendResponse(request, stepResponseFor(request));
      return; // the terminated event follows from the process's disconnect
    }
    // the VM's step request is SYNCHRONOUS: its response arrives when the
    // step has LANDED, and the only notification that can follow is a
    // breakpointStop when the landing is a user breakpoint — the adapter must
    // synthesize the DAP stopped itself (vshaxe's Main.hx does the same). The
    // thread stays logically stopped through the whole step.
    // A step over the program's LAST line races process exit like continue
    // does: connection close during the step means it ran off the end.
    StepOutcome outcome;
    String vmException = null;
    steppingThreadId = thread;
    breakpointHitDuringStep = null;
    exceptionDuringStepText = null;
    try {
      Verb verb = switch (request) {
        case StepInRequest r -> Verb.STEP_IN;
        case NextRequest r -> Verb.NEXT;
        default -> Verb.STEP_OUT;
      };
      // raw single verbs when the user asked for expression stepping, and
      // ALWAYS inside exception dispatch — the coalescing heuristics are
      // meaningless mid-unwind and can spin their caps for tens of seconds
      outcome = (expressionStepping || unwindingException) ? rawStep(verb, thread) : switch (request) {
        case StepInRequest r -> coalescedStepIn(thread);
        case NextRequest r -> coalescedNext(thread);
        default -> emulatedStepOut(thread);
      };
    } catch (EvalConnectionClosedException ignored) {
      outcome = StepOutcome.PROGRAM_ENDED;
    } finally {
      steppingThreadId = null;
      vmException = exceptionDuringStepText;
      exceptionDuringStepText = null;
      breakpointHitDuringStep = null;
    }
    sendResponse(request, stepResponseFor(request));
    sendStepStopped(thread, outcome, vmException);
  }

  /**
   * The eval VM's {@code stepIn} is SUB-EXPRESSION granular: on a line like
   * {@code outer(inner(x))} it stops at each sub-expression position before
   * finally entering a callee, so a plain DAP step-into would need several
   * presses to enter a function. DAP step-into means "advance to a different
   * line, or enter a function" — so keep single-stepping while the top frame
   * stays on the SAME function AND the SAME line (pure column moves), and stop
   * the moment the function or line changes. Bounded so a pathological program
   * cannot spin. Returns true when the program ran to completion mid-step.
   */
  private StepOutcome coalescedStepIn(int thread) throws IOException {
    try {
      FrameSignature start = topFrame(thread);
      for (int step = 0; step < MAX_STEP_IN_SUBSTEPS; step++) {
        vm().stepIn();
        EvalProtocol.EvalStackFrame top = topStackFrame(thread);
        FrameSignature now = signatureOf(top);
        if (exceptionDuringStepText != null) {
          return StepOutcome.STEPPED; // the caller reports the exception stop
        }
        if (landedOnBreakpoint(top, start)) {
          return StepOutcome.HIT_BREAKPOINT;
        }
        if (now == null || start == null || !now.sameStop(start)) {
          return StepOutcome.STEPPED; // entered/left a function, or reached a new line
        }
      }
      return StepOutcome.STEPPED; // safety cap hit: stop rather than loop forever
    } catch (EvalProtocolException unwound) {
      return resumeOffUncaughtException(); // stepped onto a throw: the stack unwound
    } catch (EvalConnectionClosedException ended) {
      return StepOutcome.PROGRAM_ENDED; // stepped off the program's end
    } catch (IOException unresponsive) {
      endSessionWithUnresponsiveVm();
      return StepOutcome.PROGRAM_ENDED;
    }
  }

  /**
   * Smart step into the NAMED callee (custom intellij/stepIntoFunction, sent
   * by the IDE with PSI-resolved (className, functionName, occurrence)). The
   * eval protocol has no such method, but its sub-expression stepIn makes an
   * exact emulation possible while STAYING ON THE LINE: single-step through
   * the line's sub-expressions; when a callee is entered, either it is the
   * chosen one (report the landing) or it is stepped OUT of and the walk
   * continues. Leaving the line means the target was not (or no longer)
   * callable there — like the hxcpp server, that landing is reported as the
   * step stop rather than an error.
   */
  private void handleStepIntoFunction(StepIntoFunctionRequest request) throws IOException {
    Integer thread = requireStoppedThread(request);
    if (thread == null) {
      return;
    }
    StepIntoFunctionArguments arguments = request.getArguments();
    String className = arguments != null ? arguments.getClassName() : null;
    String functionName = arguments != null ? arguments.getFunctionName() : null;
    int occurrence = arguments != null ? Math.max(1, arguments.getOccurrence()) : 1;

    StepOutcome outcome = StepOutcome.STEPPED;
    String vmException = null;
    int matched = 0;
    int startDepth = 0;
    FrameSignature start = null;
    steppingThreadId = thread;
    breakpointHitDuringStep = null;
    exceptionDuringStepText = null;
    try {
      if (unwindingException && !caughtExceptionFilterActive) {
        // same as plain steps: at an uncaught stop the only way is death
        outcome = letUncaughtExceptionKillTheProgramAsStep();
      } else if (unwindingException) {
        // inside exception dispatch a smart-step target cannot exist; a raw
        // step (bounded by construction) walks the unwind one position on
        outcome = rawStep(Verb.STEP_IN, thread);
      } else {
        try {
          startDepth = vm().stackTrace(thread).size();
          start = topFrame(thread);
        } catch (EvalProtocolException unwound) {
          outcome = resumeOffUncaughtException();
        } catch (EvalConnectionClosedException ended) {
          outcome = StepOutcome.PROGRAM_ENDED;
        } catch (IOException unresponsive) {
          endSessionWithUnresponsiveVm();
          outcome = StepOutcome.PROGRAM_ENDED;
        }
        // NOTE: a non-target callee is skipped by WALKING THROUGH it with raw
        // sub-steps, never stepOut — eval's stepOut resumes at the caller's NEXT
        // LINE, which abandons the line the target still sits on (observed live).
        for (int step = 0; step < MAX_SMART_STEP_SUBSTEPS && outcome == StepOutcome.STEPPED; step++) {
          try {
            vm().stepIn();
            List<EvalProtocol.EvalStackFrame> frames = vm().stackTrace(thread);
            if (exceptionDuringStepText != null || frames.isEmpty()) {
              break;
            }
            EvalProtocol.EvalStackFrame top = frames.get(0);
            // even a smart-step walk yields to a user breakpoint on its way
            if (landedOnBreakpoint(top, start)) {
              outcome = StepOutcome.HIT_BREAKPOINT;
              break;
            }
            if (frames.size() > startDepth) {
              // entered SOME callee: the chosen one ends the walk, anything else
              // (including deeper calls it makes) is stepped through
              if (frames.size() == startDepth + 1
                  && isTargetFrame(top.name(), className, functionName) && ++matched >= occurrence) {
                break; // landed in the chosen callee
              }
              continue;
            }
            if (reachedNewPosition(top, start)) {
              break; // line finished without the target: report the landing as the stop
            }
          } catch (EvalProtocolException unwound) {
            outcome = resumeOffUncaughtException(); // a callee threw uncaught
          } catch (EvalConnectionClosedException ended) {
            outcome = StepOutcome.PROGRAM_ENDED;
          } catch (IOException unresponsive) {
            endSessionWithUnresponsiveVm();
            outcome = StepOutcome.PROGRAM_ENDED;
          }
        }
      }
    } finally {
      steppingThreadId = null;
      vmException = exceptionDuringStepText;
      exceptionDuringStepText = null;
      breakpointHitDuringStep = null;
    }
    sendResponse(request, new Response());
    sendStepStopped(thread, outcome, vmException);
  }

  // Eval frame names are "pack.Class.method"; the IDE sends the class path
  // ("pack.Class" or "Class") plus the bare function name.
  private static boolean isTargetFrame(String frameName, String className, String functionName) {
    if (frameName == null || functionName == null) {
      return false;
    }
    if (className == null || className.isBlank()) {
      return frameName.equals(functionName) || frameName.endsWith("." + functionName);
    }
    String qualified = className + "." + functionName;
    if (frameName.equals(qualified) || frameName.endsWith("." + qualified)) {
      return true;
    }
    String simpleClass = className.substring(className.lastIndexOf('.') + 1);
    String simpleQualified = simpleClass + "." + functionName;
    return frameName.equals(simpleQualified) || frameName.endsWith("." + simpleQualified);
  }

  /**
   * The VM's {@code next} is sub-expression granular like its stepIn: on a
   * chained line ({@code a.f().g().h()}) each press would stop at the next
   * chain element while the IDE's line display shows nothing moving. Coalesce
   * to DAP semantics: keep stepping until the position leaves the line at the
   * starting depth (or above it, when the line ended the function).
   */
  private StepOutcome coalescedNext(int thread) throws IOException {
    try {
      int startDepth = vm().stackTrace(thread).size();
      FrameSignature start = topFrame(thread);
      for (int step = 0; step < MAX_SMART_STEP_SUBSTEPS; step++) {
        vm().next();
        List<EvalProtocol.EvalStackFrame> frames = vm().stackTrace(thread);
        if (exceptionDuringStepText != null) {
          return StepOutcome.STEPPED; // the caller reports the exception stop
        }
        if (frames.isEmpty()) {
          return StepOutcome.STEPPED;
        }
        // BEFORE the depth logic: the VM's own next stops mid-verb at a user
        // breakpoint even INSIDE the call being stepped over — honour it there
        if (landedOnBreakpoint(frames.get(0), start)) {
          return StepOutcome.HIT_BREAKPOINT;
        }
        if (frames.size() > startDepth) {
          continue; // mid-call bookkeeping frame; keep going
        }
        EvalProtocol.EvalStackFrame top = frames.get(0);
        if (frames.size() < startDepth || reachedNewPosition(top, start)) {
          return StepOutcome.STEPPED; // reached a new line (or returned out of the function)
        }
      }
      return StepOutcome.STEPPED;
    } catch (EvalProtocolException unwound) {
      return resumeOffUncaughtException(); // stepped onto a throw: the stack unwound
    } catch (EvalConnectionClosedException ended) {
      return StepOutcome.PROGRAM_ENDED; // stepped off the program's end
    } catch (IOException unresponsive) {
      endSessionWithUnresponsiveVm();
      return StepOutcome.PROGRAM_ENDED;
    }
  }

  /**
   * Step out with CHAIN-HOPPING: on {@code a.f().g().h()} the interpreter has
   * NO caller-position stop between the calls (after f returns, the very next
   * stop is inside g — verified live), so a literal "back to the caller
   * mid-line" cannot exist. Instead, step-out stops at the ENTRY of the next
   * sibling call — one press per chain element (f -> g -> h -> next line),
   * never silently running the rest of the chain like the VM's raw stepOut
   * does. Coalesced next still finishes the whole line from anywhere.
   */
  private StepOutcome emulatedStepOut(int thread) throws IOException {
    try {
      List<EvalProtocol.EvalStackFrame> startFrames = vm().stackTrace(thread);
      int startDepth = startFrames.size();
      String startFunction = startFrames.isEmpty() ? null : startFrames.get(0).name();
      FrameSignature start = startFrames.isEmpty() ? null : signatureOf(startFrames.get(0));
      for (int step = 0; step < MAX_SMART_STEP_SUBSTEPS; step++) {
        vm().stepIn();
        List<EvalProtocol.EvalStackFrame> frames = vm().stackTrace(thread);
        if (exceptionDuringStepText != null) {
          return StepOutcome.STEPPED; // the caller reports the exception stop
        }
        // breakpoints WIN over the walk to the caller, wherever they sit
        if (!frames.isEmpty() && landedOnBreakpoint(frames.get(0), start)) {
          return StepOutcome.HIT_BREAKPOINT;
        }
        if (frames.isEmpty() || frames.size() < startDepth) {
          return StepOutcome.STEPPED; // back in the caller (the line finished)
        }
        // a SIBLING call of a chained line (a.f().g()) runs at the SAME depth
        // with no caller-position stop in between — stopping at its entry is
        // what makes step-out hop chain element to chain element instead of
        // silently walking through the rest of the chain
        if (frames.size() == startDepth
            && !Objects.equals(frames.get(0).name(), startFunction)) {
          return StepOutcome.STEPPED;
        }
      }
      return StepOutcome.STEPPED;
    } catch (EvalProtocolException unwound) {
      return resumeOffUncaughtException(); // stepped onto a throw: the stack unwound
    } catch (EvalConnectionClosedException ended) {
      return StepOutcome.PROGRAM_ENDED; // stepped off the program's end
    } catch (IOException unresponsive) {
      endSessionWithUnresponsiveVm();
      return StepOutcome.PROGRAM_ENDED;
    }
  }

  private enum Verb { STEP_IN, NEXT, STEP_OUT }

  /** One raw VM step (expression-stepping mode) and its outcome. */
  private StepOutcome rawStep(Verb verb, int thread) throws IOException {
    try {
      FrameSignature start = topFrame(thread);
      switch (verb) {
        case STEP_IN -> vm().stepIn();
        case NEXT -> vm().next();
        case STEP_OUT -> vm().stepOut();
      }
      // the follow-up stackTrace doubles as an ordering barrier: the VM emits
      // the verb's breakpointStop/exceptionStop right after the verb's
      // response, so it precedes this stackTrace's response on the wire and
      // the single reader thread has dispatched it before this returns
      EvalProtocol.EvalStackFrame top = topStackFrame(thread);
      if (exceptionDuringStepText != null) {
        return StepOutcome.STEPPED; // the caller reports the exception stop
      }
      return landedOnBreakpoint(top, start) ? StepOutcome.HIT_BREAKPOINT : StepOutcome.STEPPED;
    } catch (EvalProtocolException unwound) {
      return resumeOffUncaughtException(); // stepping onto a throw unwound the stack
    } catch (EvalConnectionClosedException ended) {
      return StepOutcome.PROGRAM_ENDED; // stepped off the program's end
    } catch (IOException unresponsive) {
      endSessionWithUnresponsiveVm();
      return StepOutcome.PROGRAM_ENDED;
    }
  }

  /** The stopped thread's top frame, or null if the stack is unavailable. */
  private EvalProtocol.EvalStackFrame topStackFrame(int thread) throws IOException {
    List<EvalProtocol.EvalStackFrame> frames = vm().stackTrace(thread);
    return frames.isEmpty() ? null : frames.get(0);
  }

  /** The stopped thread's top frame as (function, line), or null if unavailable. */
  private FrameSignature topFrame(int thread) throws IOException {
    return signatureOf(topStackFrame(thread));
  }

  private static FrameSignature signatureOf(EvalProtocol.EvalStackFrame frame) {
    return frame == null ? null : new FrameSignature(frame.name(), frame.line());
  }

  private record FrameSignature(String function, int line) {
    boolean sameStop(FrameSignature other) {
      return line == other.line && Objects.equals(function, other.function);
    }
  }

  /**
   * True when the step's top frame has reached a position DIFFERENT from where
   * the step started — a new line, or a return into a different function — or
   * there is no start baseline. The shared "the step arrived somewhere new,
   * report the landing" test used by the step-over / step-into loops and by the
   * breakpoint-landing check (which exempts the step's own starting line).
   */
  private static boolean reachedNewPosition(EvalProtocol.EvalStackFrame top, FrameSignature start) {
    return start == null || top == null || !start.sameStop(signatureOf(top));
  }

  /**
   * True when a step landing must end the step as a BREAKPOINT stop: the VM
   * pushed breakpointStop for the step's thread mid-verb (authoritative — it
   * evaluates its own breakpoint state), or the landing sits on a line the IDE
   * registered a breakpoint on (belt and braces for landings the VM does not
   * flag). The step's own STARTING line is exempt either way, so stepping off
   * a line whose breakpoint we are already parked on cannot insta-stop.
   */
  private boolean landedOnBreakpoint(EvalProtocol.EvalStackFrame top, FrameSignature start) {
    if (top == null) {
      return false;
    }
    boolean vmFlagged = breakpointHitDuringStep != null;
    boolean onRegisteredLine = isBreakpointLine(top.source(), top.line());
    if (!vmFlagged && !onRegisteredLine) {
      return false;
    }
    breakpointHitDuringStep = null; // consumed (or a same-line duplicate)
    return reachedNewPosition(top, start);
  }

  private boolean isBreakpointLine(String source, int line) {
    if (source == null) {
      return false;
    }
    Set<Integer> lines = breakpointLines.get(DapPaths.toMatchKey(source));
    return lines != null && lines.contains(line);
  }

  /**
   * Variables-view inline editing (Set Value): DAP's (variablesReference,
   * name, value) maps 1:1 onto the VM's setVariable (id, name, value) — the
   * VM's single id space IS variablesReference, and its response is the
   * variable's new state (value parsed with the haxe expression parser, so
   * the same trailing-semicolon cleanup as evaluate applies).
   */
  private void handleSetVariable(SetVariableRequest request) throws IOException {
    SetVariableArguments arguments = request.getArguments();
    if ("String".equals(referenceTypes.get(arguments.getVariablesReference()))) {
      // a String's rows (length/byteLength) are derived values; the VM does
      // not survive the write attempt (non-Haxe-thread assert), so refuse
      sendErrorResponse(request, "String contents are read-only");
      return;
    }
    EvalProtocol.EvalVar updated = vm().setVariable(
      arguments.getVariablesReference(), arguments.getName(),
      stripTrailingSemicolons(arguments.getValue()));
    rememberReferenceType(updated);
    SetVariableResponseBody body = new SetVariableResponseBody();
    body.setValue(updated.value());
    body.setType(updated.type());
    body.setVariablesReference(updated.numChildren() > 0 ? updated.id() : 0);
    SetVariableResponse response = new SetVariableResponse();
    response.setBody(body);
    sendResponse(request, response);
  }

  private void handlePause(PauseRequest request) throws IOException {
    vm().pause();
    sendResponse(request, new PauseResponse());
  }

  private void handleEvaluate(EvaluateRequest request) throws IOException {
    Integer frameId = request.getArguments().getFrameId();
    if (frameId == null) {
      sendErrorResponse(request, "evaluate requires a frameId (no frame context without a stopped stack)");
      return;
    }
    EvalProtocol.EvalVar result = vm().evaluate(stripTrailingSemicolons(request.getArguments().getExpression()), frameId);
    rememberReferenceType(result);
    EvaluateResponseBody body = new EvaluateResponseBody();
    body.setResult(result.value());
    body.setType(result.type());
    body.setVariablesReference(result.numChildren() > 0 ? result.id() : 0);
    EvaluateResponse response = new EvaluateResponse();
    response.setBody(body);
    sendResponse(request, response);
  }

  private void handleDisconnect(DisconnectRequest request) throws IOException {
    closed = true;
    if (vmConnection != null) {
      vmConnection.close();
    }
    vmListener.close();
    sendResponse(request, new DisconnectResponse());
  }

  // ------------------------------------------------------ eval -> DAP events

  private void handleVmEvent(String method, JsonNode params) {
    try {
      switch (method) {
        case EvalProtocol.EVENT_BREAKPOINT_STOP -> {
          int threadId = params.path("threadId").asInt(0);
          Integer stepping = steppingThreadId;
          if (stepping != null && stepping == threadId) {
            // the VM flags a user breakpoint reached DURING a step verb with
            // this notification (the verb's response arrives separately); the
            // running step loop consumes it and reports the stop itself, so
            // forwarding here would double-report and out-order the response
            breakpointHitDuringStep = threadId;
          } else {
            sendStopped("breakpoint", threadId, null);
          }
        }
        case EvalProtocol.EVENT_EXCEPTION_STOP -> {
          int threadId = params.path("threadId").asInt(0);
          String text = params.path("text").asString("");
          Integer stepping = steppingThreadId;
          if (stepping != null && stepping == threadId) {
            // a step verb ran into an exception stop: the running loop ends
            // and handleStep reports it AFTER the verb's response (ordering)
            exceptionDuringStepText = text;
          } else {
            sendStopped("exception", threadId, text);
          }
        }
        case EvalProtocol.EVENT_THREAD_EVENT ->
          sendThreadEvent(params.path("reason").asString(""), params.path("threadId").asInt(0));
        default -> System.err.println("EvalDebugAdapter: unknown notification '" + method + "'");
      }
    } catch (IOException e) {
      System.err.println("EvalDebugAdapter failed to forward '" + method + "': " + e);
    }
  }

  private void sendStopped(String reason, int threadId, String description) throws IOException {
    stoppedThreadId = threadId;
    if ("exception".equals(reason)) {
      unwindingException = true; // raw single-verb steps until the next resume
    }
    StoppedEventBody body = new StoppedEventBody();
    body.setReason(reason);
    body.setThreadId(threadId);
    body.setAllThreadsStopped(true);
    body.setDescription(description);
    StoppedEvent event = new StoppedEvent();
    event.setBody(body);
    sendEvent(event);
  }

  private void sendThreadEvent(String reason, int threadId) throws IOException {
    ThreadEventBody body = new ThreadEventBody();
    body.setReason(reason);
    body.setThreadId(threadId);
    ThreadEvent event = new ThreadEvent();
    event.setBody(body);
    sendEvent(event);
  }

  /** The debuggee is about to run again: references die with the stop. */
  private void resumed() {
    stoppedThreadId = null;
    unwindingException = false;
    referenceTypes.clear();
  }

  /** Remembers the runtime type behind a handed-out variablesReference. */
  private void rememberReferenceType(EvalProtocol.EvalVar var) {
    if (var.id() > 0 && var.numChildren() > 0 && var.type() != null) {
      referenceTypes.put(var.id(), var.type());
    }
  }

  // ------------------------------------------------------------------ helpers

  /**
   * The eval VM parses an evaluate expression with the haxe EXPRESSION parser,
   * which rejects a trailing {@code ;} ("Unexpected ;") - a statement
   * terminator, not part of an expression. IDE evaluate/watch input commonly
   * carries one (copied from source, or typed by habit), so drop trailing
   * semicolons and surrounding whitespace. Verified against the VM.
   */
  static String stripTrailingSemicolons(String expression) {
    if (expression == null) {
      return null;
    }
    String trimmed = expression.strip();
    while (trimmed.endsWith(";")) {
      trimmed = trimmed.substring(0, trimmed.length() - 1).strip();
    }
    return trimmed;
  }

  private static Source toSource(String sourcePath) {
    if (sourcePath == null || sourcePath.isEmpty() || sourcePath.equals("?")) {
      return null;
    }
    try {
      Source source = new Source();
      source.setPath(sourcePath);
      Path fileName = Path.of(sourcePath).getFileName();
      source.setName(fileName != null ? fileName.toString() : sourcePath);
      return source;
    } catch (InvalidPathException e) {
      return null;
    }
  }

  private static Variable toVariable(EvalProtocol.EvalVar var) {
    Variable variable = new Variable();
    variable.setName(var.name());
    variable.setValue(var.value());
    variable.setType(var.type());
    variable.setVariablesReference(var.numChildren() > 0 ? var.id() : 0);
    return variable;
  }

  private void sendResponse(Request request, Response response) throws IOException {
    response.setSeq(nextSeq.getAndIncrement());
    response.setRequest_seq(request.getSeq());
    response.setCommand(request.getCommand());
    response.setSuccess(true);
    dap.send(response);
  }

  private void sendErrorResponse(Request request, String message) throws IOException {
    ErrorMessage errorMessage = new ErrorMessage();
    errorMessage.setId(0);
    errorMessage.setFormat(message);
    errorMessage.setShowUser(true);
    ErrorResponseBody body = new ErrorResponseBody();
    body.setError(errorMessage);
    ErrorResponse response = new ErrorResponse();
    response.setBody(body);
    response.setSeq(nextSeq.getAndIncrement());
    response.setRequest_seq(request.getSeq());
    response.setCommand(request.getCommand());
    response.setSuccess(false);
    response.setMessage(message);
    dap.send(response);
  }

  private void sendEvent(Event event) throws IOException {
    event.setSeq(nextSeq.getAndIncrement());
    dap.send(event);
  }

  private void sendEventQuietly(Event event) {
    try {
      sendEvent(event);
    } catch (IOException ignored) {
      // DAP side already gone; nothing left to tell
    }
  }

  @Override
  public void close() throws IOException {
    closed = true;
    protocolFuture.cancel(false);
    if (vmConnection != null) {
      vmConnection.close();
    }
    vmListener.close();
  }
}
