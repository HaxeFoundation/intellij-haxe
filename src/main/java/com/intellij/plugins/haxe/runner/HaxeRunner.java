/*
 * Copyright 2000-2013 JetBrains s.r.o.
 * Copyright 2014-2014 AS3Boyan
 * Copyright 2014-2014 Elias Ku
 * Copyright 2017-2018 Eric Bishton
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
package com.intellij.plugins.haxe.runner;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.ExecutionResult;
import com.intellij.execution.configurations.CommandLineState;
import com.intellij.execution.configurations.RunProfile;
import com.intellij.execution.configurations.RunProfileState;
import com.intellij.execution.configurations.RunnerSettings;
import com.intellij.execution.executors.DefaultRunExecutor;
import com.intellij.execution.filters.TextConsoleBuilder;
import com.intellij.execution.filters.TextConsoleBuilderFactory;
import com.intellij.execution.process.ColoredProcessHandler;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.execution.runners.GenericProgramRunner;
import com.intellij.execution.runners.ProgramRunner;
import com.intellij.execution.ui.ExecutionUiService;
import com.intellij.execution.ui.RunContentDescriptor;
import com.intellij.ide.BrowserUtil;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.ProjectUtil;
import com.intellij.openapi.util.io.FileUtilRt;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.plugins.haxe.HaxeBundle;
import com.intellij.plugins.haxe.compilation.HaxeCompilerUtil;
import com.intellij.plugins.haxe.config.HaxeTarget;
import com.intellij.plugins.haxe.ide.module.HaxeModuleSettings;
import com.intellij.plugins.haxe.util.HaxeCommandLine;
import org.jetbrains.annotations.NotNull;

/**
 * @author: Fedor.Korotkov
 */
public class HaxeRunner extends GenericProgramRunner<RunnerSettings> {
  public static final String HAXE_RUNNER_ID = "HaxeRunner";

  @NotNull
  @Override
  public String getRunnerId() {
    return HAXE_RUNNER_ID;
  }
  @Override
  protected RunContentDescriptor doExecute(@NotNull RunProfileState state, @NotNull ExecutionEnvironment environment) throws ExecutionException {
    final HaxeApplicationConfiguration configuration = (HaxeApplicationConfiguration)environment.getRunProfile();
    final Module module = configuration.getConfigurationModule().getModule();

    if (module == null) {
      throw new ExecutionException(HaxeBundle.message("no.module.for.run.configuration", configuration.getName()));
    }

    final HaxeModuleSettings settings = HaxeModuleSettings.getInstance(module);

    if (settings.isUseNmmlToBuild()) {
      final NMERunningState nmeRunningState = new NMERunningState(environment, module, false);
      return executeState(state, environment, this);
    }

    if (settings.isUseOpenFLToBuild()) {
      final OpenFLRunningState openflRunningState = new OpenFLRunningState(environment, module, false);
      return executeState(openflRunningState, environment, this);
    }

    if (configuration.isCustomFileToLaunch() && FileUtilRt.extensionEquals(configuration.getCustomFileToLaunchPath(), "n")) {
      final NekoRunningState nekoRunningState = new NekoRunningState(environment, module, configuration.getCustomFileToLaunchPath());
      return executeState(nekoRunningState, environment, this);
    }

    if (configuration.isCustomExecutable()) {
      final String filePath = configuration.isCustomFileToLaunch()
                              ? configuration.getCustomFileToLaunchPath()
                              : getOutputFilePath(module, settings);
      return executeState(new CommandLineState(environment) {
        @NotNull
        @Override
        protected ProcessHandler startProcess() throws ExecutionException {
          final HaxeCommandLine commandLine = new HaxeCommandLine(module);
          VirtualFile workDir = ProjectUtil.guessModuleDir(module);
          if(workDir == null) {
            throw new ExecutionException("Unable to to determine workdirectory");
          }
          commandLine.withWorkDirectory(workDir.getCanonicalPath());
          commandLine.setExePath(configuration.getCustomExecutablePath());
          commandLine.addParameter(filePath);

          final TextConsoleBuilder consoleBuilder = TextConsoleBuilderFactory.getInstance().createBuilder(module.getProject());
          setConsoleBuilder(consoleBuilder);

          return new ColoredProcessHandler(commandLine.createProcess(), commandLine.getCommandLineString());
        }
      }, environment, this);
    }

    if (configuration.isCustomFileToLaunch()) {
      BrowserUtil.open(configuration.getCustomFileToLaunchPath());
      return null;
    }

    if (settings.getHaxeTarget() == HaxeTarget.FLASH) {
      BrowserUtil.open(getOutputFilePath(module, settings));
      return null;
    }

    if(settings.getHaxeTarget() == HaxeTarget.INTERP) {
      return null;
    }

    if (settings.getHaxeTarget() != HaxeTarget.NEKO) {
      throw new ExecutionException(HaxeBundle.message("haxe.run.wrong.target", settings.getHaxeTarget()));
    }

    final NekoRunningState nekoRunningState = new NekoRunningState(environment, module, null);
    return executeState(nekoRunningState, environment, this);
  }

  @Override
  public boolean canRun(@NotNull String executorId, @NotNull RunProfile profile) {
    return DefaultRunExecutor.EXECUTOR_ID.equals(executorId) && profile instanceof HaxeApplicationConfiguration;
  }



  static String getOutputFilePath(Module module, HaxeModuleSettings settings) {
    FileDocumentManager.getInstance().saveAllDocuments();
    return HaxeCompilerUtil.calculateCompilerOutput(module);
  }

  private RunContentDescriptor executeState( RunProfileState state, ExecutionEnvironment environment,  ProgramRunner<RunnerSettings> runner)
    throws ExecutionException {
    FileDocumentManager.getInstance().saveAllDocuments();
    return showRunContent(state.execute(environment.getExecutor(), runner), environment);
  }

  private RunContentDescriptor showRunContent(ExecutionResult executionResult, ExecutionEnvironment environment) {
    if (executionResult != null) {
      return ExecutionUiService.getInstance().showRunContent(executionResult, environment);
    }else {
      return null;
    }
  }
}
