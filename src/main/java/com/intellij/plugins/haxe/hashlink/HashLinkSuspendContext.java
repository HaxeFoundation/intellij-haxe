package com.intellij.plugins.haxe.hashlink;

import com.intellij.plugins.haxe.runner.debugger.dap.protocol.DapThread;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.StackFrame;
import com.intellij.xdebugger.frame.XExecutionStack;
import com.intellij.xdebugger.frame.XSuspendContext;
import java.util.List;
import org.jetbrains.annotations.Nullable;

/**
 * One stop of the (suspend-all) HashLink debuggee: every live thread as an
 * execution stack, with the stopped thread active and its frames pre-fetched.
 * Selecting another thread lazily walks its stack.
 */
final class HashLinkSuspendContext extends XSuspendContext {
  private final HashLinkExecutionStack[] stacks;
  private final HashLinkExecutionStack active;

  HashLinkSuspendContext(HashLinkDebugProcess process, List<DapThread> threads,
                         int activeThreadId, List<StackFrame> activeFrames) {
    // fall back to a single synthetic thread if the list is somehow empty
    if (threads.isEmpty()) {
      this.active = new HashLinkExecutionStack(process, activeThreadId, "main", activeFrames);
      this.stacks = new HashLinkExecutionStack[]{active};
      return;
    }
    this.stacks = new HashLinkExecutionStack[threads.size()];
    HashLinkExecutionStack activeStack = null;
    for (int i = 0; i < threads.size(); i++) {
      DapThread thread = threads.get(i);
      boolean isActive = thread.getId() == activeThreadId;
      HashLinkExecutionStack stack = new HashLinkExecutionStack(
        process, thread.getId(), threadLabel(thread), isActive ? activeFrames : null);
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
}
