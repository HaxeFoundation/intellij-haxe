package com.intellij.plugins.haxe.runner.debugger.dap.ide;

import com.intellij.plugins.haxe.runner.debugger.dap.protocol.DapThread;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.StackFrame;
import com.intellij.xdebugger.frame.XExecutionStack;
import com.intellij.xdebugger.frame.XSuspendContext;
import java.util.List;
import org.jetbrains.annotations.Nullable;

/**
 * One stop of the (suspend-all) debuggee: every live thread as an
 * execution stack, with the stopped thread active and its frames pre-fetched.
 * Selecting another thread lazily walks its stack.
 */
final class DapSuspendContext extends XSuspendContext {
  private final DapExecutionStack[] stacks;
  private final DapExecutionStack active;

  DapSuspendContext(DapDebugProcess process, List<DapThread> threads,
                      int activeThreadId, List<StackFrame> activeFrames,
                      @Nullable String exceptionText) {
    // fall back to a single synthetic thread if the list is somehow empty
    if (threads.isEmpty()) {
      this.active = new DapExecutionStack(process, activeThreadId, "main", activeFrames, exceptionText);
      this.stacks = new DapExecutionStack[]{active};
      return;
    }
    this.stacks = new DapExecutionStack[threads.size()];
    DapExecutionStack activeStack = null;
    for (int i = 0; i < threads.size(); i++) {
      DapThread thread = threads.get(i);
      boolean isActive = thread.getId() == activeThreadId;
      DapExecutionStack stack = new DapExecutionStack(
        process, thread.getId(), threadLabel(thread), isActive ? activeFrames : null,
        // the exception gutter marker belongs only on the thread that threw
        isActive ? exceptionText : null);
      stacks[i] = stack;
      if (isActive) {
        activeStack = stack;
      }
    }
    // the stopped thread should always be in the list; guard just in case
    this.active = activeStack != null ? activeStack : stacks[0];
  }

  private static String threadLabel(DapThread thread) {
    String name = thread.getName();
    return (name != null ? name : "thread") + " [" + thread.getId() + "]";
  }

  @Override
  public @Nullable XExecutionStack getActiveExecutionStack() {
    return active;
  }

  @Override
  public XExecutionStack[] getExecutionStacks() {
    return stacks;
  }

  // The frames view's thread dropdown fills through this callback (lazily, on
  // first popup open). Two chunks ON PURPOSE: the view refreshes an already
  // open popup only when a delivery CHANGES the combo's item count — a single
  // final chunk removes the "Loading..." row (-1) and adds the one missing
  // thread (+1) in the same pass, the count stays equal, the refresh is
  // skipped, and the thread only appears on the SECOND open. Splitting the
  // stacks from the completion signal makes each pass change the count.
  @Override
  public void computeExecutionStacks(XExecutionStackContainer container) {
    container.addExecutionStack(List.of(stacks), false);
    container.addExecutionStack(List.of(), true);
  }
}
