package com.intellij.plugins.haxe.hashlink;

import com.intellij.plugins.haxe.runner.debugger.dap.protocol.StackFrame;
import com.intellij.xdebugger.frame.XExecutionStack;
import com.intellij.xdebugger.frame.XSuspendContext;
import java.util.List;
import org.jetbrains.annotations.Nullable;

/**
 * One stop of the (single-threaded) HashLink debuggee: a single execution
 * stack built from the adapter's stackTrace response.
 */
final class HashLinkSuspendContext extends XSuspendContext {
  private final HashLinkExecutionStack stack;

  HashLinkSuspendContext(HashLinkDebugProcess process, List<StackFrame> frames) {
    this.stack = new HashLinkExecutionStack(process, frames);
  }

  @Override
  public @Nullable XExecutionStack getActiveExecutionStack() {
    return stack;
  }

  @Override
  public XExecutionStack[] getExecutionStacks() {
    return new XExecutionStack[]{stack};
  }
}
