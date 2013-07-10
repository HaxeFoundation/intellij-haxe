/*
 * Copyright 2000-2013 JetBrains s.r.o.
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

import com.intellij.execution.ExecutionException;
import com.intellij.execution.ExecutionResult;
import com.intellij.execution.Executor;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.execution.ui.RunContentDescriptor;
import com.intellij.lang.javascript.flex.projectStructure.model.FlexBuildConfiguration;
import com.intellij.lang.javascript.flex.run.BCBasedRunnerParameters;
import com.intellij.lang.javascript.flex.run.FlashRunnerParameters;
import com.intellij.lang.javascript.flex.sdk.FlexSdkUtils;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.plugins.haxe.HaxeBundle;
import com.intellij.plugins.haxe.runner.NMERunningState;
import com.intellij.plugins.haxe.runner.OpenFLRunningState;
import com.intellij.xdebugger.XDebugProcess;
import com.intellij.xdebugger.XDebugProcessStarter;
import com.intellij.xdebugger.XDebugSession;
import com.intellij.xdebugger.XDebuggerManager;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

/**
 * @author: Fedor.Korotkov
 */
public class HaxeFlashDebuggingUtil {
  public static RunContentDescriptor getDescriptor(HaxeDebugRunner runner,
                                                   final Module module,
                                                   RunContentDescriptor contentToReuse,
                                                   ExecutionEnvironment env,
                                                   String urlToLaunch,
                                                   String flexSdkName) throws ExecutionException {
    final Sdk flexSdk = FlexSdkUtils.findFlexOrFlexmojosSdk(flexSdkName);
    if (flexSdk == null) {
      throw new ExecutionException(HaxeBundle.message("flex.sdk.not.found", flexSdkName));
    }

    final FlexBuildConfiguration bc = new FakeFlexBuildConfiguration(flexSdk, urlToLaunch);

    final XDebugSession debugSession =
      XDebuggerManager.getInstance(module.getProject()).startSession(runner, env, contentToReuse, new XDebugProcessStarter() {
        @NotNull
        public XDebugProcess start(@NotNull final XDebugSession session) throws ExecutionException {
          try {
            final FlashRunnerParameters params = new FlashRunnerParameters();
            params.setModuleName(module.getName());
            return new HaxeDebugProcess(session, bc, params);
          }
          catch (IOException e) {
            throw new ExecutionException(e.getMessage(), e);
          }
        }
      });

    return debugSession.getRunContentDescriptor();
  }

  public static RunContentDescriptor getNMEDescriptor(final HaxeDebugRunner runner,
                                                      final Module module,
                                                      RunContentDescriptor contentToReuse,
                                                      final ExecutionEnvironment env,
                                                      final Executor executor, String flexSdkName) throws ExecutionException {
    final Sdk flexSdk = FlexSdkUtils.findFlexOrFlexmojosSdk(flexSdkName);
    if (flexSdk == null) {
      throw new ExecutionException(HaxeBundle.message("flex.sdk.not.found", flexSdkName));
    }

    final FlexBuildConfiguration bc = new FakeFlexBuildConfiguration(flexSdk, null);

    final XDebugSession debugSession =
      XDebuggerManager.getInstance(module.getProject()).startSession(runner, env, contentToReuse, new XDebugProcessStarter() {
        @NotNull
        public XDebugProcess start(@NotNull final XDebugSession session) throws ExecutionException {
          try {
            NMERunningState runningState = new NMERunningState(env, module, false, true);
            final ExecutionResult executionResult = runningState.execute(executor, runner);
            final BCBasedRunnerParameters params = new BCBasedRunnerParameters();
            params.setModuleName(module.getName());
            return new HaxeDebugProcess(session, bc, params);
          }
          catch (IOException e) {
            throw new ExecutionException(e.getMessage(), e);
          }
        }
      });

    return debugSession.getRunContentDescriptor();
  }

  public static RunContentDescriptor getOpenFLDescriptor(final HaxeDebugRunner runner,
                                                      final Module module,
                                                      RunContentDescriptor contentToReuse,
                                                      final ExecutionEnvironment env,
                                                      final Executor executor, String flexSdkName) throws ExecutionException {
    final Sdk flexSdk = FlexSdkUtils.findFlexOrFlexmojosSdk(flexSdkName);
    if (flexSdk == null) {
      throw new ExecutionException(HaxeBundle.message("flex.sdk.not.found", flexSdkName));
    }

    final FlexBuildConfiguration bc = new FakeFlexBuildConfiguration(flexSdk, null);

    final XDebugSession debugSession =
      XDebuggerManager.getInstance(module.getProject()).startSession(runner, env, contentToReuse, new XDebugProcessStarter() {
        @NotNull
        public XDebugProcess start(@NotNull final XDebugSession session) throws ExecutionException {
          try {
            OpenFLRunningState runningState = new OpenFLRunningState(env, module, true, true);
            final ExecutionResult executionResult = runningState.execute(executor, runner);
            final BCBasedRunnerParameters params = new BCBasedRunnerParameters();
            params.setModuleName(module.getName());
            return new HaxeDebugProcess(session, bc, params);
          }
          catch (IOException e) {
            throw new ExecutionException(e.getMessage(), e);
          }
        }
      });

    return debugSession.getRunContentDescriptor();
  }
}
