package com.intellij.plugins.haxe.runner.debugger.hxcpp.frame;

import com.intellij.plugins.haxe.runner.debugger.hxcpp.HXCPPDebugProcess;
import com.intellij.xdebugger.frame.XExecutionStack;
import com.intellij.xdebugger.frame.XSuspendContext;

import java.util.List;

public class HXCPPSuspendContext extends XSuspendContext {
  private final HXCPPExecutionStack myExecutionStack;

  public HXCPPSuspendContext(HXCPPDebugProcess debugProcess, List<HXCPPStackFrame> frames) {
    myExecutionStack = new HXCPPExecutionStack(debugProcess, frames);
  }

  @Override
  public XExecutionStack getActiveExecutionStack() {
    return myExecutionStack;
  }
}
