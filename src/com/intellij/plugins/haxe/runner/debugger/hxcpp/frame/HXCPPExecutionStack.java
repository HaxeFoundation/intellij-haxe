package com.intellij.plugins.haxe.runner.debugger.hxcpp.frame;

import com.intellij.plugins.haxe.runner.debugger.hxcpp.HXCPPDebugProcess;
import com.intellij.plugins.haxe.runner.debugger.hxcpp.connection.HXCPPResponse;
import com.intellij.util.io.socketConnection.AbstractResponseToRequestHandler;
import com.intellij.xdebugger.frame.XExecutionStack;
import com.intellij.xdebugger.frame.XStackFrame;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class HXCPPExecutionStack extends XExecutionStack {
  private final HXCPPDebugProcess myDebugProcess;
  private List<HXCPPStackFrame> myStackFrames = null;

  public HXCPPExecutionStack(@NotNull HXCPPDebugProcess debugProcess) {
    this(debugProcess, new ArrayList<HXCPPStackFrame>());
  }

  public HXCPPExecutionStack(@NotNull HXCPPDebugProcess debugProcess, List<HXCPPStackFrame> stackFrames) {
    super("DartVM");
    myDebugProcess = debugProcess;
    myStackFrames = stackFrames;
  }

  @Override
  public XStackFrame getTopFrame() {
    return myStackFrames.isEmpty() ? null : myStackFrames.get(0);
  }

  @Override
  public void computeStackFrames(final int firstFrameIndex, final XStackFrameContainer container) {
    if (!myStackFrames.isEmpty()) {
      addStackFrames(container, firstFrameIndex);
      return;
    }

    myDebugProcess.sendCommand(new AbstractResponseToRequestHandler<HXCPPResponse>() {
      @Override
      public boolean processResponse(HXCPPResponse response) {
        myStackFrames = HXCPPStackFrame.parse(myDebugProcess, response.getResponseString());
        return true;
      }
    }, "where");
  }

  private void addStackFrames(XStackFrameContainer container, int firstFrameIndex) {
    final List<HXCPPStackFrame> frames = myStackFrames.size() < 2
                                         ? Collections.<HXCPPStackFrame>emptyList()
                                         : myStackFrames.subList(firstFrameIndex, myStackFrames.size());
    container.addStackFrames(frames, true);
  }
}
