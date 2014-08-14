/*
 * Copyright 2000-2013 JetBrains s.r.o.
 * Copyright 2014-2014 AS3Boyan
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
