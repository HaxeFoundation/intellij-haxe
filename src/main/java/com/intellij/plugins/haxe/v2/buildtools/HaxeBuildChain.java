package com.intellij.plugins.haxe.v2.buildtools;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.process.KillableColoredProcessHandler;
import com.intellij.execution.process.ProcessEvent;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.process.ProcessListener;
import com.intellij.execution.process.ProcessOutputType;
import com.intellij.execution.process.ProcessOutputTypes;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.plugins.haxe.HaxeBundle;
import com.intellij.plugins.haxe.util.HaxeReadActions;
import com.intellij.task.ProjectTaskRunner.Result;
import com.intellij.task.TaskRunnerResults;
import com.intellij.util.concurrency.AppExecutorUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.concurrency.AsyncPromise;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Iterator;
import java.util.List;

/**
 * The process behind the Build Project console: the containers' build commands
 * run one after another, each as its own {@link KillableColoredProcessHandler}
 * whose output is forwarded here, so a console attached to this handler streams
 * every command while it runs. The first non-zero exit ends the chain; Stop
 * kills the running command and ends it too. The task's promise resolves once,
 * when the chain ends.
 */
final class HaxeBuildChain extends ProcessHandler {

  private static final int NO_COMMAND_EXIT_CODE = -1;

  private final Project project;
  private final Iterator<String> containerIds;
  private final AsyncPromise<Result> promise;
  private final Object lock = new Object();
  // both guarded by lock
  private KillableColoredProcessHandler current;
  private boolean stopped;

  HaxeBuildChain(@NotNull Project project, @NotNull List<String> containerIds, @NotNull AsyncPromise<Result> promise) {
    this.project = project;
    this.containerIds = containerIds.iterator();
    this.promise = promise;
  }

  /** Starts the chain; call after {@link #startNotify()}, from any thread. */
  void start() {
    AppExecutorUtil.getAppExecutorService().execute(() -> {
      HaxeUnsavedDocuments.saveAll();
      buildNext();
    });
  }

  /** Pooled thread: launches the next container's command, or ends the chain. */
  private void buildNext() {
    if (isStopped() || project.isDisposed()) {
      finish(TaskRunnerResults.ABORTED, NO_COMMAND_EXIT_CODE);
      return;
    }
    if (!containerIds.hasNext()) {
      printSystem(HaxeBundle.message("haxe.build.console.done"));
      finish(TaskRunnerResults.SUCCESS, 0);
      return;
    }
    String containerId = containerIds.next();
    HaxeCompileCommands.Resolved resolved = HaxeReadActions.compute(() -> HaxeCompileCommands.resolve(project, containerId));
    if (resolved == null) {
      // canRun saw a command, but it may have gone stale since - not an error
      buildNext();
      return;
    }
    List<String> command = HaxeCompileCommands.connectIfEnabled(project, containerId, resolved.connectEligible(), resolved.command());
    printSystem("[" + containerId + "] " + String.join(" ", command));
    startCommand(containerId, command, resolved.workDirectory());
  }

  private void startCommand(@NotNull String containerId, @NotNull List<String> command, @Nullable String workDirectory) {
    KillableColoredProcessHandler handler;
    try {
      GeneralCommandLine commandLine = HaxeToolCommandLines.interactive(command, workDirectory);
      handler = new KillableColoredProcessHandler(commandLine);
    }
    catch (ExecutionException e) {
      printSystem(HaxeBundle.message("haxe.build.console.start.failed", containerId, e.getMessage()));
      finish(TaskRunnerResults.FAILURE, NO_COMMAND_EXIT_CODE);
      return;
    }
    handler.addProcessListener(forwardingListener(containerId));
    synchronized (lock) {
      current = handler;
    }
    handler.startNotify();
    // Stop pressed while the command was being launched
    if (isStopped()) handler.killProcess();
  }

  @NotNull
  private ProcessListener forwardingListener(@NotNull String containerId) {
    return new ProcessListener() {
      @Override
      public void onTextAvailable(@NotNull ProcessEvent event, @NotNull Key outputType) {
        // the handler's only system-typed output is its own command-line echo
        // on startNotify; the container-prefixed line above replaces it
        if (ProcessOutputType.isSystem(outputType)) return;
        notifyTextAvailable(event.getText(), outputType);
      }

      @Override
      public void processTerminated(@NotNull ProcessEvent event) {
        onCommandExit(containerId, event.getExitCode());
      }
    };
  }

  private void onCommandExit(@NotNull String containerId, int exitCode) {
    boolean wasStopped;
    synchronized (lock) {
      current = null;
      wasStopped = stopped;
    }
    if (wasStopped) {
      printSystem(HaxeBundle.message("haxe.build.console.stopped"));
      finish(TaskRunnerResults.ABORTED, exitCode);
    }
    else if (exitCode != 0) {
      printSystem(HaxeBundle.message("haxe.build.console.failed", containerId, exitCode));
      finish(TaskRunnerResults.FAILURE, exitCode);
    }
    else {
      AppExecutorUtil.getAppExecutorService().execute(this::buildNext);
    }
  }

  private void finish(@NotNull Result result, int exitCode) {
    notifyProcessTerminated(exitCode);
    promise.setResult(result);
  }

  private void printSystem(@NotNull String line) {
    notifyTextAvailable(line + "\n", ProcessOutputTypes.SYSTEM);
  }

  private boolean isStopped() {
    synchronized (lock) {
      return stopped;
    }
  }

  /** Marks the chain stopped and hands back the running command, if any. */
  @Nullable
  private KillableColoredProcessHandler stop() {
    synchronized (lock) {
      stopped = true;
      return current;
    }
  }

  /** Stop in the console tab: the running command is killed outright and the chain ends after it. */
  @Override
  protected void destroyProcessImpl() {
    KillableColoredProcessHandler running = stop();
    if (running != null) running.killProcess();
  }

  @Override
  protected void detachProcessImpl() {
    KillableColoredProcessHandler running = stop();
    if (running != null) running.detachProcess();
    notifyProcessDetached();
    promise.setResult(TaskRunnerResults.ABORTED);
  }

  @Override
  public boolean detachIsDefault() {
    return false;
  }

  /** Console input reaches whichever command is running, so a tool's prompt can be answered. */
  @Override
  public @NotNull OutputStream getProcessInput() {
    return new OutputStream() {
      @Override
      public void write(int b) throws IOException {
        OutputStream input = runningInput();
        if (input != null) input.write(b);
      }

      @Override
      public void flush() throws IOException {
        OutputStream input = runningInput();
        if (input != null) input.flush();
      }
    };
  }

  @Nullable
  private OutputStream runningInput() {
    synchronized (lock) {
      return current == null ? null : current.getProcessInput();
    }
  }
}
