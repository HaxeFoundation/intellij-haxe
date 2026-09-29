package com.intellij.plugins.haxe.runner.debugger.hashlink;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.process.ColoredProcessHandler;
import com.intellij.execution.ui.RunnerLayoutUi;
import com.intellij.execution.ui.layout.PlaceInGrid;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.project.Project;
import com.intellij.plugins.haxe.runner.debugger.dap.client.DapClient;
import com.intellij.plugins.haxe.runner.debugger.dap.ide.DapBackend;
import com.intellij.plugins.haxe.runner.debugger.dap.ide.DapDebugProcess;
import com.intellij.plugins.haxe.runner.debugger.dap.ide.DapSourceScopes;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.Request;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.StackFrame;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.requests.LaunchRequest;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.requests.LaunchRequestArguments;
import com.intellij.ui.content.Content;
import com.intellij.xdebugger.XSourcePosition;
import com.intellij.xdebugger.stepping.XSmartStepIntoHandler;
import com.intellij.xdebugger.ui.XDebugTabLayouter;
import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import com.intellij.plugins.haxe.runner.debugger.dap.ide.AdapterTargetsSmartStepHandler;

/**
 * The HashLink backend: spawns the bundled external {@code hl-debug-adapter.hl}
 * (on the configured HashLink VM) and talks DAP to it over TCP. The debuggee
 * was already spawned by the runner ({@code hl --debug <port> --debug-wait});
 * the launch request tells the adapter to ATTACH to it by pid — the adapter
 * must never spawn GUI debuggees itself (HL's process.c forces SW_HIDE onto
 * the child's first window).
 */
public class HashLinkBackend implements DapBackend {
  private static final int CONNECT_TIMEOUT_MILLIS = 15_000;
  private static final long ADAPTER_KILL_WAIT_SECONDS = 2;

  private final Path hlExecutable;
  private final Path hlProgram;
  private final int debugPort;
  private final List<String> sourceDirectories;

  private volatile long debuggeePid = -1;
  private volatile Process adapterProcess;
  private volatile BufferedReader adapterStdout;
  private volatile HashLinkRegistersPanel registersPanel;

  /**
   * With the build's source directories (absolute, VFS separators): the HL
   * debug tables carry classpath-relative file names, so without a scope any
   * same-named file in the IDE project binds breakpoints into this debuggee
   * and stack frames resolve to an arbitrary same-named editor file. An empty
   * list disables the scoping.
   */
  public HashLinkBackend(Path hlExecutable, Path hlProgram, int debugPort, List<String> sourceDirectories) {
    this.hlExecutable = hlExecutable;
    this.hlProgram = hlProgram;
    this.debugPort = debugPort;
    this.sourceDirectories = List.copyOf(sourceDirectories);
  }

  public int getDebugPort() {
    return debugPort;
  }

  @Override
  public void debuggeeSpawned(ColoredProcessHandler debuggeeHandler) {
    debuggeePid = debuggeeHandler.getProcess().pid();
  }

  @Override
  public DapClient connect() throws IOException {
    try {
      HashLinkAdapterLauncher.LaunchedAdapter launched = HashLinkAdapterLauncher.launch(hlExecutable);
      adapterProcess = launched.process();
      adapterStdout = launched.stdout();
      return DapClient.connect("127.0.0.1", launched.port(), CONNECT_TIMEOUT_MILLIS);
    } catch (ExecutionException e) {
      throw new IOException(e.getMessage(), e);
    }
  }

  // Keeps the adapter's own stdout/stderr drained after the port announcement:
  // otherwise a chatty adapter (crash traces, DAP_ADAPTER_TRACE) would fill the
  // OS pipe and block, and its error output would be invisible. Shown as grey
  // system output, prefixed so it cannot be mistaken for program output.
  @Override
  public void onConnected(DapDebugProcess process) {
    BufferedReader reader = adapterStdout;
    adapterStdout = null;
    if (reader == null) {
      return;
    }
    Thread gobbler = new Thread(() -> {
      try (BufferedReader stdout = reader) {
        String line;
        while ((line = stdout.readLine()) != null) {
          process.printSystem("[adapter] " + line + "\n");
        }
      } catch (IOException ignored) {
        // adapter ended
      }
    }, "HashLink adapter output");
    gobbler.setDaemon(true);
    gobbler.start();
  }

  @Override
  public boolean requiresLaunchRequest() {
    return true;
  }

  @Override
  public Request launchRequest() {
    LaunchRequest launch = new LaunchRequest();
    LaunchRequestArguments arguments = new LaunchRequestArguments();
    // attach mode: the runner already spawned the debuggee (program is still
    // needed for the adapter's bytecode/debug-info parse)
    arguments.setProgram(hlProgram.toString());
    arguments.setAttachPid((int)debuggeePid);
    arguments.setDebugPort(debugPort);
    launch.setArguments(arguments);
    return launch;
  }

  // "Terminate" (destroy, not detach) kills the debuggee handler. But the
  // debuggee is debug-attached by the adapter and suspended at a breakpoint —
  // Windows cannot TerminateProcess it from a third party while a debugger
  // holds it, so destroyProcess hangs on "waiting for process detach". Killing
  // the adapter (the debugger) instead ends the debug session;
  // DebugActiveProcess's kill-on-exit then tears the debuggee down.
  @Override
  public void beforeDebuggeeDestroyed() {
    Process adapter = adapterProcess;
    if (adapter != null) {
      adapter.destroyForcibly();
      // wait until the adapter is gone so the debug session is fully torn down
      // before the debuggee handler's own destroy runs, avoiding a race where
      // it TerminateProcess-es a still-attached debuggee
      try {
        adapter.waitFor(ADAPTER_KILL_WAIT_SECONDS, TimeUnit.SECONDS);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }
    }
  }

  @Override
  public boolean supportsExceptionFilters() {
    return true;
  }

  // The bundled adapter's filter vocabulary (ExceptionController):
  @Override
  public String anyThrowFilterId() {
    return "all";
  }

  @Override
  public String criticalFilterId() {
    return "vm"; // VM-raised errors: null access, out-of-bounds, cast, ...
  }

  @Override
  public boolean supportsSmartStepInto() {
    return true;
  }

  // Targets come from the adapter's DAP stepInTargets (it reads the bytecode),
  // not from the PSI-based default handler.
  @Override
  public XSmartStepIntoHandler<?> createSmartStepIntoHandler(DapDebugProcess process) {
    return new AdapterTargetsSmartStepHandler(process);
  }

  @Override
  public XDebugTabLayouter createTabLayouter(DapDebugProcess process) {
    return new XDebugTabLayouter() {
      @Override
      public void registerAdditionalContent(@NotNull RunnerLayoutUi ui) {
        HashLinkRegistersPanel panel = new HashLinkRegistersPanel(process);
        registersPanel = panel;
        process.getSession().addSessionListener(panel);

        Content content = ui.createContent("HashLinkRegisters", panel, "Registers",
                                           AllIcons.Debugger.Value, null);
        content.setCloseable(false);
        ui.addContent(content, 0, PlaceInGrid.center, false);
      }
    };
  }

  /** After a value write the register rows may have changed. */
  @Override
  public void afterSetVariable(DapDebugProcess process) {
    HashLinkRegistersPanel panel = registersPanel;
    if (panel != null) {
      panel.refresh();
    }
  }

  @Override
  public String qualifyExpression(Project project, @Nullable XSourcePosition position, @NotNull String expression) {
    return HashLinkExpressionQualifier.qualify(project, position, expression);
  }

  @Override
  public @Nullable XSourcePosition resolveSource(Project project, @Nullable String path, StackFrame frame) {
    return HashLinkSourceResolver.resolve(project, path, frame, sourceDirectories);
  }

  // The debug tables carry classpath-relative (sometimes bare) file names, so
  // the adapter can only match a breakpoint by name/suffix - a file outside
  // the build's source directories must not be offered at all, or its
  // same-named sibling inside the build binds and stops the debuggee.
  @Override
  public boolean acceptsBreakpointFile(String vfsPath) {
    return DapSourceScopes.acceptsWhenScoped(vfsPath, sourceDirectories);
  }

  // NOTE: the adapter currently only STORES the flag — labels stay class names
  // until the hl_dyn_call_safe rendering lands (a faulting toString must be
  // impossible, not merely handled; see the adapter docs).
  @Override
  public boolean supportsToStringRendering() {
    return true;
  }

  @Override
  public String startupHint() {
    return "Check that the .hl was compiled with -debug and that no other debugger holds the program.";
  }

  @Override
  public void close() {
    Process process = adapterProcess;
    adapterProcess = null;
    if (process != null) {
      process.destroy();
    }
  }
}
