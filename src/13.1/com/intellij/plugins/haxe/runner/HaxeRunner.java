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
package com.intellij.plugins.haxe.runner;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.CommandLineState;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.configurations.RunProfile;
import com.intellij.execution.configurations.RunProfileState;
import com.intellij.execution.executors.DefaultRunExecutor;
import com.intellij.execution.filters.TextConsoleBuilder;
import com.intellij.execution.filters.TextConsoleBuilderFactory;
import com.intellij.execution.process.ColoredProcessHandler;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.runners.DefaultProgramRunner;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.execution.ui.RunContentDescriptor;
import com.intellij.ide.BrowserUtil;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.CompilerModuleExtension;
import com.intellij.openapi.util.io.FileUtilRt;
import com.intellij.plugins.haxe.HaxeBundle;
import com.intellij.plugins.haxe.config.HaxeTarget;
import com.intellij.plugins.haxe.ide.module.HaxeModuleSettings;
import com.intellij.util.PathUtil;
import org.jetbrains.annotations.NotNull;

/**
 * @author: Fedor.Korotkov
 */
public class HaxeRunner extends DefaultProgramRunner {
  public static final String HAXE_RUNNER_ID = "HaxeRunner";

  @NotNull
  @Override
  public String getRunnerId() {
    return HAXE_RUNNER_ID;
  }

  @Override
  public boolean canRun(@NotNull String executorId, @NotNull RunProfile profile) {
    return DefaultRunExecutor.EXECUTOR_ID.equals(executorId) && profile instanceof HaxeApplicationConfiguration;
  }

  @Override
  protected RunContentDescriptor doExecute(Project project,
                                           RunProfileState state,
                                           RunContentDescriptor contentToReuse,
                                           ExecutionEnvironment env) throws ExecutionException {
    final HaxeApplicationConfiguration configuration = (HaxeApplicationConfiguration)env.getRunProfile();
    final Module module = configuration.getConfigurationModule().getModule();

    if (module == null) {
      throw new ExecutionException(HaxeBundle.message("no.module.for.run.configuration", configuration.getName()));
    }

    final HaxeModuleSettings settings = HaxeModuleSettings.getInstance(module);

    if (settings.isUseNmmlToBuild()) {
      final NMERunningState nmeRunningState = new NMERunningState(env, module, false);
      return super.doExecute(project, nmeRunningState, contentToReuse, env);
    }

    if (settings.isUseOpenFLToBuild()) {
      final OpenFLRunningState openflRunningState = new OpenFLRunningState(env, module, true);
      return super.doExecute(project, openflRunningState, contentToReuse, env);
    }

    if (configuration.isCustomFileToLaunch() && FileUtilRt.extensionEquals(configuration.getCustomFileToLaunchPath(), "n")) {
      final NekoRunningState nekoRunningState = new NekoRunningState(env, module, configuration.getCustomFileToLaunchPath());
      return super.doExecute(project, nekoRunningState, contentToReuse, env);
    }

    if (configuration.isCustomExecutable()) {
      final String filePath = configuration.isCustomFileToLaunch()
                              ? configuration.getCustomFileToLaunchPath()
                              : getOutputFilePath(module, settings);
      return super.doExecute(project, new CommandLineState(env) {
        @NotNull
        @Override
        protected ProcessHandler startProcess() throws ExecutionException {
          final GeneralCommandLine commandLine = new GeneralCommandLine();
          commandLine.setWorkDirectory(PathUtil.getParentPath(module.getModuleFilePath()));
          commandLine.setExePath(configuration.getCustomExecutablePath());
          commandLine.addParameter(filePath);

          final TextConsoleBuilder consoleBuilder = TextConsoleBuilderFactory.getInstance().createBuilder(module.getProject());
          setConsoleBuilder(consoleBuilder);

          return new ColoredProcessHandler(commandLine.createProcess(), commandLine.getCommandLineString());
        }
      }, contentToReuse, env);
    }

    if (configuration.isCustomFileToLaunch()) {
      BrowserUtil.open(configuration.getCustomFileToLaunchPath());
      return null;
    }

    if (settings.getHaxeTarget() == HaxeTarget.FLASH) {
      BrowserUtil.open(getOutputFilePath(module, settings));
      return null;
    }

    if (settings.getHaxeTarget() != HaxeTarget.NEKO) {
      throw new ExecutionException(HaxeBundle.message("haxe.run.wrong.target", settings.getHaxeTarget()));
    }

    final NekoRunningState nekoRunningState = new NekoRunningState(env, module, null);
    return super.doExecute(project, nekoRunningState, contentToReuse, env);
  }

  static String getOutputFilePath(Module module, HaxeModuleSettings settings) {
    FileDocumentManager.getInstance().saveAllDocuments();
    final CompilerModuleExtension model = CompilerModuleExtension.getInstance(module);
    assert model != null;
    return model.getCompilerOutputUrl() + "/release/" + settings.getOutputFileName();
  }
}
