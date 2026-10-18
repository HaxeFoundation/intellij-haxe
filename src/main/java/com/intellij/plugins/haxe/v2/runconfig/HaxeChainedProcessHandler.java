package com.intellij.plugins.haxe.v2.runconfig;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.process.ProcessEvent;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.process.ProcessListener;
import com.intellij.execution.process.ProcessOutputTypes;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.ThrowableComputable;
import com.intellij.plugins.haxe.v2.buildtools.HaxeForwardingProcessInput;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.OutputStream;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;

/// Runs commands one after another as one process for a console: each starts
/// once the previous one exited with 0, and the first non-zero exit ends the
/// chain with that code. The output of every command, after a header line
/// naming it, goes to the chain's listeners; stopping the chain stops the
/// running command and skips the rest.
public final class HaxeChainedProcessHandler extends ProcessHandler {

  /** One command of the chain: the line announcing it and how to start it. */
  public record Step(@NotNull String header, @NotNull ThrowableComputable<ProcessHandler, ExecutionException> start) {
  }

  private final Deque<Step> remaining;
  private volatile ProcessHandler current;

  public HaxeChainedProcessHandler(@NotNull List<Step> steps) {
    remaining = new ConcurrentLinkedDeque<>(steps);
  }

  @Override
  public void startNotify() {
    super.startNotify();
    startNext();
  }

  private void startNext() {
    Step step = remaining.poll();
    if (step == null) {
      notifyProcessTerminated(0);
      return;
    }
    notifyTextAvailable(step.header() + "\n", ProcessOutputTypes.SYSTEM);
    try {
      ProcessHandler handler = step.start().compute();
      current = handler;
      handler.addProcessListener(new StepListener());
      handler.startNotify();
    }
    catch (ExecutionException e) {
      notifyTextAvailable(e.getMessage() + "\n", ProcessOutputTypes.STDERR);
      notifyProcessTerminated(1);
    }
  }

  private void stepFinished(int exitCode) {
    if (exitCode != 0 || remaining.isEmpty()) {
      notifyProcessTerminated(exitCode);
      return;
    }
    startNext();
  }

  @Override
  protected void destroyProcessImpl() {
    remaining.clear();
    ProcessHandler running = current;
    if (running != null && !running.isProcessTerminated()) {
      running.destroyProcess();
    }
    else {
      notifyProcessTerminated(-1);
    }
  }

  @Override
  protected void detachProcessImpl() {
    remaining.clear();
    ProcessHandler running = current;
    if (running != null) {
      running.detachProcess();
    }
    notifyProcessDetached();
  }

  @Override
  public boolean detachIsDefault() {
    return false;
  }

  /** Console input reaches whichever step is running; the console takes this stream once, before the first step starts. */
  @Override
  public @NotNull OutputStream getProcessInput() {
    return new HaxeForwardingProcessInput(this::runningInput);
  }

  @Nullable
  private OutputStream runningInput() {
    ProcessHandler running = current;
    return running != null ? running.getProcessInput() : null;
  }

  /** Forwards a step's output and moves on when it ends. */
  private final class StepListener implements ProcessListener {
    @Override
    public void onTextAvailable(@NotNull ProcessEvent event, @NotNull Key outputType) {
      notifyTextAvailable(event.getText(), outputType);
    }

    @Override
    public void processTerminated(@NotNull ProcessEvent event) {
      stepFinished(event.getExitCode());
    }
  }
}
