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
import com.intellij.execution.Executor;
import com.intellij.execution.configurations.RunProfile;
import com.intellij.execution.configurations.RunProfileState;
import com.intellij.execution.executors.DefaultDebugExecutor;
import com.intellij.execution.runners.DefaultProgramRunner;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.execution.ui.RunContentDescriptor;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.plugins.haxe.HaxeBundle;
import com.intellij.plugins.haxe.config.NMETarget;
import com.intellij.plugins.haxe.ide.module.HaxeModuleSettings;
import com.intellij.plugins.haxe.runner.debugger.hxcpp.HXCPPDebugProcess;
import com.intellij.plugins.haxe.runner.debugger.hxcpp.HXCPPRemoteDebugConfiguration;
import com.intellij.xdebugger.XDebugProcess;
import com.intellij.xdebugger.XDebugProcessStarter;
import com.intellij.xdebugger.XDebugSession;
import com.intellij.xdebugger.XDebuggerManager;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

/**
 * @author: Fedor.Korotkov
 */
public class HXCPPRemoteDebugRunner extends DefaultProgramRunner {
  public static final String HAXE_DEBUG_RUNNER_ID = "HXCPPRemoteDebugRunner";

  @NotNull
  @Override
  public String getRunnerId() {
    return HAXE_DEBUG_RUNNER_ID;
  }

  @Override
  public boolean canRun(@NotNull String executorId, @NotNull RunProfile profile) {
    return DefaultDebugExecutor.EXECUTOR_ID.equals(executorId) && profile instanceof HXCPPRemoteDebugConfiguration;
  }

  @Override
  protected RunContentDescriptor doExecute(final Project project,
                                           RunProfileState state,
                                           RunContentDescriptor contentToReuse,
                                           final ExecutionEnvironment env) throws ExecutionException {
    final HXCPPRemoteDebugConfiguration configuration = (HXCPPRemoteDebugConfiguration)env.getRunProfile();
    final Module module = configuration.getConfigurationModule().getModule();
    final Executor executor = env.getExecutor();

    if (module == null) {
      throw new ExecutionException(HaxeBundle.message("no.module.for.run.configuration", configuration.getName()));
    }

    final HaxeModuleSettings settings = HaxeModuleSettings.getInstance(module);

    final boolean notHXCPP = settings.getNmeTarget() != NMETarget.WINDOWS &&
                             settings.getNmeTarget() != NMETarget.MAC &&
                             settings.getNmeTarget() != NMETarget.LINUX &&
                             settings.getNmeTarget() != NMETarget.LINUX64 &&
                             settings.getNmeTarget() != NMETarget.ANDROID &&
                             settings.getNmeTarget() != NMETarget.IOS;
    if (notHXCPP) {
      throw new ExecutionException(HaxeBundle.message("haxe.proper.debug.targets"));
    }

    Integer port = null;
    try {
      port = Integer.parseInt(settings.getHXCPPPort());
    }
    catch (NumberFormatException e) {
      throw new ExecutionException("Bad HXCPP port \"" + settings.getHXCPPPort() + "\" in module settings");
    }

    final int finalPort = port;
    final XDebugSession debugSession =
      XDebuggerManager.getInstance(project).startSession(this, env, contentToReuse, new XDebugProcessStarter() {
        @NotNull
        public XDebugProcess start(@NotNull final XDebugSession session) throws ExecutionException {
          try {
            final HXCPPDebugProcess debugProcess = new HXCPPDebugProcess(session, module, finalPort);
            HXCPPRemoteDebugState runningState = new HXCPPRemoteDebugState(project, env);
            debugProcess.setExecutionResult(runningState.execute(executor, HXCPPRemoteDebugRunner.this));
            return debugProcess;
          }
          catch (IOException e) {
            throw new ExecutionException(e.getMessage(), e);
          }
        }
      });

    return debugSession.getRunContentDescriptor();
  }
}
