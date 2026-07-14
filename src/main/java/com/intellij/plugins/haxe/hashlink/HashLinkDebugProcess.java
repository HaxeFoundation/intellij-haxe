package com.intellij.plugins.haxe.hashlink;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.filters.TextConsoleBuilderFactory;
import com.intellij.execution.process.ProcessEvent;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.process.ProcessListener;
import com.intellij.execution.process.ProcessOutputTypes;
import com.intellij.execution.ui.ConsoleView;
import com.intellij.execution.ui.ConsoleViewContentType;
import com.intellij.execution.ui.ExecutionConsole;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.Module;
import com.intellij.plugins.haxe.runner.debugger.HaxeBreakpointType;
import com.intellij.plugins.haxe.runner.debugger.HaxeDebuggerEditorsProvider;
import com.intellij.plugins.haxe.runner.debugger.dap.client.DapClient;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.Event;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.Request;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.Response;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.Scope;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.Variable;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.events.ContinuedEvent;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.events.ExitedEvent;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.events.OutputEvent;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.events.StoppedEvent;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.events.TerminatedEvent;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.requests.ConfigurationDoneRequest;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.requests.ContinueArguments;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.requests.ContinueRequest;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.requests.PauseArguments;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.requests.PauseRequest;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.requests.DisconnectRequest;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.requests.InitializeRequest;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.requests.InitializeRequestArguments;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.requests.LaunchRequest;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.requests.LaunchRequestArguments;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.requests.NextArguments;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.requests.NextRequest;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.requests.ScopesArguments;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.requests.ScopesRequest;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.requests.SetExceptionBreakpointsArguments;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.requests.SetExceptionBreakpointsRequest;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.requests.StackTraceArguments;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.DapThread;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.StackFrame;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.requests.StackTraceRequest;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.requests.ThreadsRequest;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.responses.ThreadsResponse;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.requests.StepInArguments;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.requests.StepInRequest;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.requests.StepOutArguments;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.requests.StepOutRequest;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.requests.VariablesArguments;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.requests.SetVariableArguments;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.requests.SetVariableRequest;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.requests.VariablesRequest;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.responses.ScopesResponse;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.responses.StackTraceResponse;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.responses.SetVariableResponse;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.responses.VariablesResponse;
import com.intellij.execution.ui.RunnerLayoutUi;
import com.intellij.execution.ui.layout.PlaceInGrid;
import com.intellij.icons.AllIcons;
import com.intellij.ui.content.Content;
import com.intellij.xdebugger.XDebugProcess;
import com.intellij.xdebugger.XDebugSession;
import com.intellij.xdebugger.XSourcePosition;
import com.intellij.xdebugger.XDebuggerManager;
import com.intellij.xdebugger.XDebuggerUtil;
import com.intellij.xdebugger.breakpoints.XBreakpoint;
import com.intellij.xdebugger.breakpoints.XBreakpointHandler;
import com.intellij.xdebugger.breakpoints.XBreakpointManager;
import com.intellij.xdebugger.breakpoints.XBreakpointProperties;
import com.intellij.xdebugger.breakpoints.XBreakpointType;
import com.intellij.xdebugger.breakpoints.XLineBreakpoint;
import com.intellij.xdebugger.evaluation.XDebuggerEditorsProvider;
import com.intellij.xdebugger.frame.XSuspendContext;
import com.intellij.xdebugger.ui.XDebugTabLayouter;
import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * XDebugger process for HashLink (experimental): spawns the bundled DAP
 * adapter, drives it as a DAP client, and bridges its events into the IDE.
 *
 * The debuggee itself is spawned by {@link HashLinkDebugRunner} (never by the
 * adapter — HL's process.c would force SW_HIDE onto its first window); the
 * adapter attaches to it by pid. Its {@link ProcessHandler} is the session's
 * process handler, so console output, stdin and the exit code flow through
 * the normal run machinery, and the session ends when the debuggee does.
 *
 * Threading: the IDE calls resume/step/stop on the EDT — those only submit
 * work to a single-thread request executor. A dedicated event-pump thread is
 * the sole {@code pollEvent} caller and issues its own follow-up requests
 * (stackTrace on stop); DapClient correlates concurrent requests by seq.
 */
public class HashLinkDebugProcess extends XDebugProcess {
  private static final Logger LOG = Logger.getInstance(HashLinkDebugProcess.class);
  private static final long REQUEST_TIMEOUT_MILLIS = 15_000;
  private static final long DISCONNECT_TIMEOUT_MILLIS = 3_000;
  private static final long EVENT_POLL_MILLIS = 250;

  private final Module module;
  private final Path hlExecutable;
  private final Path hlProgram;
  private final ProcessHandler processHandler;
  private final int debugPort;
  private final long debuggeePid;
  private final HashLinkBreakpointManager breakpoints = new HashLinkBreakpointManager(this);
  private final ExecutorService requestExecutor =
    Executors.newSingleThreadExecutor(r -> daemon(r, "HashLink DAP requests"));

  private volatile Process adapterProcess;
  private volatile DapClient client;
  private volatile int currentThreadId = 1;
  private volatile boolean shuttingDown = false;
  private volatile HashLinkRegistersPanel registersPanel;
  // which exception breakpoints are enabled (union sent to the adapter)

  public HashLinkDebugProcess(@NotNull XDebugSession session, Module module,
                              Path hlExecutable, Path hlProgram,
                              ProcessHandler debuggeeHandler, int debugPort, long debuggeePid) {
    super(session);
    this.module = module;
    this.hlExecutable = hlExecutable;
    this.hlProgram = hlProgram;
    this.processHandler = debuggeeHandler;
    this.debugPort = debugPort;
    this.debuggeePid = debuggeePid;
    // "Terminate" (destroy, not detach) kills the debuggee handler. But the debuggee is
    // debug-attached by the adapter and suspended at a breakpoint — Windows cannot
    // TerminateProcess it from a third party while a debugger holds it, so destroyProcess
    // hangs on "waiting for process detach". Killing the adapter (the debugger) instead
    // ends the debug session; DebugActiveProcess's kill-on-exit then tears the debuggee
    // down. Gated on client != null so our own graceful teardown (which nulls client
    // before destroying the handler) and Disconnect (detach, willBeDestroyed=false) are
    // left to the normal disconnect path.
    processHandler.addProcessListener(new ProcessListener() {
      @Override
      public void processWillTerminate(@NotNull ProcessEvent event, boolean willBeDestroyed) {
        if (willBeDestroyed && client != null) {
          Process adapter = adapterProcess;
          if (adapter != null) {
            adapter.destroyForcibly();
            // wait until the adapter is gone so the debug session is fully torn down
            // before the debuggee handler's own destroy runs, avoiding a race where it
            // TerminateProcess-es a still-attached debuggee
            try {
              adapter.waitFor(2, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
              Thread.currentThread().interrupt();
            }
          }
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
      .createBuilder(getSession().getProject()).getConsole();
    console.attachToProcess(processHandler);
    return console;
  }

  @Override
  public @NotNull XDebugTabLayouter createTabLayouter() {
    return new XDebugTabLayouter() {
      @Override
      public void registerAdditionalContent(@NotNull RunnerLayoutUi ui) {
        HashLinkRegistersPanel panel = new HashLinkRegistersPanel(HashLinkDebugProcess.this);
        registersPanel = panel;
        getSession().addSessionListener(panel);
        Content content = ui.createContent("HashLinkRegisters", panel, "Registers",
                                           AllIcons.Debugger.Value, null);
        content.setCloseable(false);
        ui.addContent(content, 0, PlaceInGrid.center, false);
      }
    };
  }

  private void initializeSession() {
    try {
      HashLinkAdapterLauncher.LaunchedAdapter launched = HashLinkAdapterLauncher.launch(hlExecutable);
      adapterProcess = launched.process();
      drainAdapterOutput(launched.stdout());
      client = DapClient.connect("127.0.0.1", launched.port(), (int)REQUEST_TIMEOUT_MILLIS);

      InitializeRequest initialize = new InitializeRequest();
      InitializeRequestArguments initializeArguments = new InitializeRequestArguments();
      initializeArguments.setAdapterID("intellij-haxe");
      initializeArguments.setClientID("intellij");
      initialize.setArguments(initializeArguments);
      client.sendRequest(initialize, REQUEST_TIMEOUT_MILLIS);
      client.pollEvent(REQUEST_TIMEOUT_MILLIS); // the initialized event

      LaunchRequest launch = new LaunchRequest();
      LaunchRequestArguments launchArguments = new LaunchRequestArguments();
      // attach mode: the runner already spawned the debuggee (program is still
      // needed for the adapter's bytecode/debug-info parse)
      launchArguments.setProgram(hlProgram.toString());
      launchArguments.setAttachPid((int)debuggeePid);
      launchArguments.setDebugPort(debugPort);
      launch.setArguments(launchArguments);
      Response launchResponse = client.sendRequest(launch, REQUEST_TIMEOUT_MILLIS);
      if (!launchResponse.isSuccess()) {
        fail("Cannot launch the HashLink program: " + launchResponse.getMessage());
        return;
      }

      breakpoints.flushAll();
      // Send the exception filters IN-PHASE (before configurationDone). The request
      // is built by reading the breakpoint manager, so a breakpoint already enabled
      // from a previous IDE run arms here — the registerBreakpoint callbacks alone
      // fire on the EDT and land after configurationDone (racing startup), which is
      // why a persisted breakpoint used to appear armed but did nothing until
      // toggled. This mirrors how line breakpoints flush.
      sendRequest(exceptionFiltersRequest());
      client.sendRequest(new ConfigurationDoneRequest(), REQUEST_TIMEOUT_MILLIS);

      Thread pump = daemon(this::pumpEvents, "HashLink DAP events");
      pump.start();
    } catch (ExecutionException | IOException e) {
      fail("Cannot start the HashLink debug session: " + e.getMessage());
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }

  // --- event pump (sole pollEvent caller) ---

  private void pumpEvents() {
    try {
      while (!shuttingDown) {
        Event event = client.pollEvent(EVENT_POLL_MILLIS);
        switch (event) {
          case null -> { /* poll again */ }
          case StoppedEvent stopped -> handleStopped(stopped);
          case ContinuedEvent ignored ->
            // a step with no user-code landing (e.g. past a thread entry's last
            // statement) was downgraded to a continue: reflect that we are running
            getSession().sessionResumed();
          case OutputEvent output -> handleOutput(output);
          case ExitedEvent ignored -> {
            // the debuggee's own ProcessHandler reports termination (with the
            // real exit code); the adapter's attach-mode code would be a guess
          }
          case TerminatedEvent ignored -> {
            terminateSession();
            return;
          }
          default -> { /* breakpoint re-verification etc.: nothing to do yet */ }
        }
      }
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    } catch (RuntimeException e) {
      LOG.warn("HashLink event pump failed", e);
      terminateSession();
    }
  }

  private void handleStopped(StoppedEvent stopped) {
    currentThreadId = stopped.getBody().getThreadId();
    // stopped on a thrown exception → mark the throw line in the gutter, tooltip = the value
    String exceptionText = null;
    if ("exception".equals(stopped.getBody().getReason())) {
      String description = stopped.getBody().getDescription();
      exceptionText = description != null ? description : "Exception thrown";
    }
    reportStopped(currentThreadId, exceptionText);
    // run-to-cursor is one-shot: any stop (including a breakpoint reached before the
    // cursor) cancels a pending run-to breakpoint.
    breakpoints.clearRunToBreakpoint();
  }

  // Reports the current stop to the session (all threads suspended, the given one active).
  private void reportStopped(int threadId, String exceptionText) {
    List<DapThread> threads = requestThreads();
    List<StackFrame> activeFrames = requestStackTrace(threadId);
    getSession().positionReached(
      new HashLinkSuspendContext(this, threads, threadId, activeFrames, exceptionText));
  }

  List<DapThread> requestThreads() {
    return sendRequest(new ThreadsRequest()) instanceof ThreadsResponse response && response.isSuccess()
           ? response.getBody().getThreads() : List.of();
  }

  List<StackFrame> requestStackTrace(int threadId) {
    StackTraceRequest request = new StackTraceRequest();
    StackTraceArguments arguments = new StackTraceArguments();
    arguments.setThreadId(threadId);
    request.setArguments(arguments);
    return sendRequest(request) instanceof StackTraceResponse response && response.isSuccess()
           ? response.getBody().getStackFrames() : List.of();
  }

  private void handleOutput(OutputEvent output) {
    String text = output.getBody().getOutput();
    if (text != null) {
      print(text, "stderr".equals(output.getBody().getCategory()));
    }
  }

  // The debuggee's stdout/stderr flow through its own ProcessHandler; DAP
  // output events only carry adapter-side messages (eval warnings, errors).
  // Print those straight into the session's Console view (with stdout/stderr
  // colouring); fall back to the process handler until the console exists.
  private void print(String text, boolean stderr) {
    ConsoleView console = getSession().getConsoleView();
    if (console != null) {
      console.print(text, stderr ? ConsoleViewContentType.ERROR_OUTPUT : ConsoleViewContentType.NORMAL_OUTPUT);
    } else {
      processHandler.notifyTextAvailable(text, stderr ? ProcessOutputTypes.STDERR : ProcessOutputTypes.STDOUT);
    }
  }

  private void printSystem(String text) {
    ConsoleView console = getSession().getConsoleView();
    if (console != null) {
      console.print(text, ConsoleViewContentType.SYSTEM_OUTPUT);
    } else {
      processHandler.notifyTextAvailable(text, ProcessOutputTypes.SYSTEM);
    }
  }

  // Keeps the adapter's own stdout/stderr drained after the port announcement:
  // otherwise a chatty adapter (crash traces, DAP_ADAPTER_TRACE) would fill the
  // OS pipe and block, and its error output would be invisible. Shown as grey
  // system output, prefixed so it cannot be mistaken for program output.
  private void drainAdapterOutput(BufferedReader adapterStdout) {
    Thread gobbler = daemon(() -> {
      try (BufferedReader reader = adapterStdout) {
        String line;
        while ((line = reader.readLine()) != null) {
          printSystem("[adapter] " + line + "\n");
        }
      } catch (IOException ignored) {
        // adapter ended
      }
    }, "HashLink adapter output");
    gobbler.start();
  }

  private void fail(String message) {
    print(message + "\n", true);
    terminateSession();
  }

  private void terminateSession() {
    teardown();
    getSession().stop();
  }

  // --- XDebugProcess callbacks (EDT: only submit, never block) ---

  @Override
  public void resume(@Nullable XSuspendContext context) {
    ContinueRequest request = new ContinueRequest();
    ContinueArguments arguments = new ContinueArguments();
    arguments.setThreadId(currentThreadId);
    request.setArguments(arguments);
    onRequestThread(() -> sendRequest(request));
  }

  // Run to cursor: plant a transient breakpoint at the target line (alongside the
  // existing breakpoints) and resume. Any stop clears it (handleStopped). If the line
  // has no executable code, don't resume — that would run away with no place to
  // stop — and re-assert the current position so the UI leaves the "running" state.
  @Override
  public void runToPosition(@NotNull XSourcePosition position, @Nullable XSuspendContext context) {
    String path = position.getFile().getPath();
    int line = position.getLine() + 1; // XSourcePosition is 0-based; DAP is 1-based
    int threadId = currentThreadId;
    onRequestThread(() -> {
      if (breakpoints.setRunToBreakpoint(path, line)) {
        ContinueRequest request = new ContinueRequest();
        ContinueArguments arguments = new ContinueArguments();
        arguments.setThreadId(threadId);
        request.setArguments(arguments);
        sendRequest(request);
      } else {
        breakpoints.clearRunToBreakpoint();
        reportStopped(threadId, null);
      }
    });
  }

  @Override
  public void startPausing() {
    // interrupt the running debuggee; the adapter replies, then sends a
    // stopped(reason:"pause") event that the existing stop handling renders
    PauseRequest request = new PauseRequest();
    PauseArguments arguments = new PauseArguments();
    arguments.setThreadId(currentThreadId);
    request.setArguments(arguments);
    onRequestThread(() -> sendRequest(request));
  }

  @Override
  public void startStepOver(@Nullable XSuspendContext context) {
    NextRequest request = new NextRequest();
    NextArguments arguments = new NextArguments();
    arguments.setThreadId(currentThreadId);
    request.setArguments(arguments);
    onRequestThread(() -> sendRequest(request));
  }

  @Override
  public void startStepInto(@Nullable XSuspendContext context) {
    StepInRequest request = new StepInRequest();
    StepInArguments arguments = new StepInArguments();
    arguments.setThreadId(currentThreadId);
    request.setArguments(arguments);
    onRequestThread(() -> sendRequest(request));
  }

  @Override
  public void startStepOut(@Nullable XSuspendContext context) {
    StepOutRequest request = new StepOutRequest();
    StepOutArguments arguments = new StepOutArguments();
    arguments.setThreadId(currentThreadId);
    request.setArguments(arguments);
    onRequestThread(() -> sendRequest(request));
  }

  @Override
  public void stop() {
    shuttingDown = true;
    requestExecutor.execute(() -> {
      DapClient dapClient = client;
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
    DapClient dapClient = client;
    client = null;
    if (dapClient != null) {
      try {
        dapClient.close();
      } catch (IOException ignored) {
      }
    }
    Process process = adapterProcess;
    adapterProcess = null;
    if (process != null) {
      process.destroy();
    }
    if (!processHandler.isProcessTerminated()) {
      processHandler.destroyProcess();
    }
  }

  // --- plumbing for breakpoints/frames/values ---

  /** Runs work on the single-thread DAP request executor. */
  void onRequestThread(Runnable work) {
    if (!requestExecutor.isShutdown()) {
      requestExecutor.execute(work);
    }
  }

  /** Blocking request; only call on the request executor or the event pump. */
  @Nullable Response sendRequest(Request request) {
    DapClient dapClient = client;
    if (dapClient == null) {
      return null;
    }
    try {
      return dapClient.sendRequest(request, REQUEST_TIMEOUT_MILLIS);
    } catch (IOException e) {
      if (!shuttingDown) {
        LOG.warn("DAP request '" + request.getCommand() + "' failed", e);
      }
      return null;
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      return null;
    }
  }

  List<Scope> requestScopes(int frameId) {
    ScopesRequest request = new ScopesRequest();
    ScopesArguments arguments = new ScopesArguments();
    arguments.setFrameId(frameId);
    request.setArguments(arguments);
    return sendRequest(request) instanceof ScopesResponse response && response.isSuccess()
           ? response.getBody().getScopes() : List.of();
  }

  List<Variable> requestVariables(int variablesReference) {
    VariablesRequest request = new VariablesRequest();
    VariablesArguments arguments = new VariablesArguments();
    arguments.setVariablesReference(variablesReference);
    request.setArguments(arguments);
    return sendRequest(request) instanceof VariablesResponse response && response.isSuccess()
           ? response.getBody().getVariables() : List.of();
  }

  /** Reloads the Registers tab (after a value write; register rows may have changed). */
  void refreshRegistersTab() {
    HashLinkRegistersPanel panel = registersPanel;
    if (panel != null) {
      panel.refresh();
    }
  }

  /**
   * Sets the named child of a container reference to `value` (a literal or
   * another variable path). Returns the new rendered value; throws with the
   * adapter's message on failure (invalid type, allocation needed, ...).
   */
  String requestSetVariable(int containerReference, String name, String value) {
    SetVariableRequest request = new SetVariableRequest();
    SetVariableArguments arguments = new SetVariableArguments();
    arguments.setVariablesReference(containerReference);
    arguments.setName(name);
    arguments.setValue(value);
    request.setArguments(arguments);
    Response response = sendRequest(request);
    if (response instanceof SetVariableResponse ok && response.isSuccess()) {
      return ok.getBody() != null ? ok.getBody().getValue() : value;
    }
    throw new IllegalStateException(response != null && response.getMessage() != null
                                    ? response.getMessage() : "the adapter rejected the change");
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
    return new XBreakpointHandler<?>[]{
      new XBreakpointHandler<XLineBreakpoint<XBreakpointProperties>>(HaxeBreakpointType.class) {
        @Override
        public void registerBreakpoint(@NotNull XLineBreakpoint<XBreakpointProperties> breakpoint) {
          breakpoints.register(breakpoint);
        }

        @Override
        public void unregisterBreakpoint(@NotNull XLineBreakpoint<XBreakpointProperties> breakpoint, boolean temporary) {
          breakpoints.unregister(breakpoint);
        }
      },
      // "Any HashLink exception": stop on every thrown exception
      new XBreakpointHandler<XBreakpoint<XBreakpointProperties>>(HashLinkExceptionBreakpointType.class) {
        @Override
        public void registerBreakpoint(@NotNull XBreakpoint<XBreakpointProperties> breakpoint) {
          updateExceptionFilters();
        }

        @Override
        public void unregisterBreakpoint(@NotNull XBreakpoint<XBreakpointProperties> breakpoint, boolean temporary) {
          updateExceptionFilters();
        }
      },
      // "Uncaught HashLink exception": stop only where no live try/catch handles it
      new XBreakpointHandler<XBreakpoint<XBreakpointProperties>>(HashLinkUncaughtExceptionBreakpointType.class) {
        @Override
        public void registerBreakpoint(@NotNull XBreakpoint<XBreakpointProperties> breakpoint) {
          updateExceptionFilters();
        }

        @Override
        public void unregisterBreakpoint(@NotNull XBreakpoint<XBreakpointProperties> breakpoint, boolean temporary) {
          updateExceptionFilters();
        }
      },
      // Per-class exception breakpoints: stop on throws of a specific class (+ subclasses)
      new XBreakpointHandler<XBreakpoint<HashLinkExceptionBreakpointProperties>>(HashLinkTypedExceptionBreakpointType.class) {
        @Override
        public void registerBreakpoint(@NotNull XBreakpoint<HashLinkExceptionBreakpointProperties> breakpoint) {
          updateExceptionFilters();
        }

        @Override
        public void unregisterBreakpoint(@NotNull XBreakpoint<HashLinkExceptionBreakpointProperties> breakpoint, boolean temporary) {
          updateExceptionFilters();
        }
      }
    };
  }

  // Recomputed whenever any exception breakpoint toggles (live path — posted to
  // the request thread). See exceptionFiltersRequest for how the union is built.
  private void updateExceptionFilters() {
    onRequestThread(() -> sendRequest(exceptionFiltersRequest()));
  }

  // Builds the setExceptionBreakpoints request by reading the CURRENT state of the
  // three exception-breakpoint types straight from the breakpoint manager: the
  // "all"/"uncaught" filters plus the class names of every enabled per-class
  // breakpoint. Reading the manager (rather than tracking volatile flags) is the
  // single source of truth, so a session started with breakpoints already enabled
  // (e.g. after an IDE restart) arms them the same as a live toggle.
  private SetExceptionBreakpointsRequest exceptionFiltersRequest() {
    List<String> filters = new ArrayList<>();
    List<String> filterTypes = new ArrayList<>();
    ReadAction.run(() -> {
      XBreakpointManager manager =
        XDebuggerManager.getInstance(getSession().getProject()).getBreakpointManager();
      XDebuggerUtil util = XDebuggerUtil.getInstance();
      if (anyEnabled(manager, util.findBreakpointType(HashLinkExceptionBreakpointType.class))) {
        filters.add("all");
      }
      if (anyEnabled(manager, util.findBreakpointType(HashLinkUncaughtExceptionBreakpointType.class))) {
        filters.add("uncaught");
      }
      XBreakpointType<?, ?> typedType = util.findBreakpointType(HashLinkTypedExceptionBreakpointType.class);
      if (typedType != null) {
        for (XBreakpoint<?> breakpoint : manager.getBreakpoints(typedType)) {
          if (breakpoint.isEnabled()
              && breakpoint.getProperties() instanceof HashLinkExceptionBreakpointProperties properties
              && properties.className != null && !properties.className.isBlank()) {
            filterTypes.add(properties.className.trim());
          }
        }
      }
    });
    SetExceptionBreakpointsRequest request = new SetExceptionBreakpointsRequest();
    SetExceptionBreakpointsArguments arguments = new SetExceptionBreakpointsArguments();
    arguments.setFilters(filters);
    arguments.setFilterTypes(filterTypes);
    request.setArguments(arguments);
    return request;
  }

  private static boolean anyEnabled(XBreakpointManager manager, XBreakpointType<?, ?> type) {
    if (type == null) {
      return false;
    }
    for (XBreakpoint<?> breakpoint : manager.getBreakpoints(type)) {
      if (breakpoint.isEnabled()) {
        return true;
      }
    }
    return false;
  }

  private static Thread daemon(Runnable work, String name) {
    Thread thread = new Thread(work, name);
    thread.setDaemon(true);
    return thread;
  }
}
