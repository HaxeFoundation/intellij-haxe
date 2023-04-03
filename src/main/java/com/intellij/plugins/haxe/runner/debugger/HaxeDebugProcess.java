/*
 * Copyright 2000-2013 JetBrains s.r.o.
 * Copyright 2014-2023 AS3Boyan
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
package com.intellij.plugins.haxe.runner.debugger;

import com.intellij.lang.javascript.flex.debug.FlexDebugProcess;
import com.intellij.lang.javascript.flex.projectStructure.model.FlexBuildConfiguration;
import com.intellij.lang.javascript.flex.run.BCBasedRunnerParameters;
import com.intellij.xdebugger.XDebugSession;
import com.intellij.xdebugger.evaluation.XDebuggerEditorsProvider;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

/**
 * @author: Fedor.Korotkov
 */
public class HaxeDebugProcess extends FlexDebugProcess {
  public HaxeDebugProcess(final XDebugSession session,
                          final FlexBuildConfiguration bc,
                          final BCBasedRunnerParameters params) throws IOException {
    super(session, bc, params);
  }

  @NotNull
  @Override
  public XDebuggerEditorsProvider getEditorsProvider() {
    return new HaxeDebuggerEditorsProvider();
  }
}
