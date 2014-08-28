/*
 * Copyright 2000-2013 JetBrains s.r.o.
 * Copyright 2014-2014 AS3Boyan
 * Copyright 2014-2014 Elias Ku
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
