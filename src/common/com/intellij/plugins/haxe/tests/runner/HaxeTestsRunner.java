/*
 * Copyright 2000-2013 JetBrains s.r.o.
 * Copyright 2014-2015 AS3Boyan
 * Copyright 2014-2015 Elias Ku
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
package com.intellij.plugins.haxe.tests.runner;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.CommandLineState;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.configurations.RunProfile;
import com.intellij.execution.configurations.RunProfileState;
import com.intellij.execution.executors.DefaultRunExecutor;
import com.intellij.execution.filters.TextConsoleBuilder;
import com.intellij.execution.filters.TextConsoleBuilderFactory;
import com.intellij.execution.process.OSProcessHandler;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.runners.DefaultProgramRunner;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.execution.ui.RunContentDescriptor;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.plugins.haxe.config.HaxeTarget;
import com.intellij.plugins.haxe.ide.module.HaxeModuleSettings;
import com.intellij.plugins.haxe.tests.runner.filters.ErrorFilter;
import com.intellij.util.PathUtil;
import org.jetbrains.annotations.NotNull;

;

public class HaxeTestsRunner extends DefaultProgramRunner {

  public static final String HAXE_TESTS_RUNNER_ID = "HaxeTestsRunner";

  @NotNull
  @Override
  public String getRunnerId() {
    return HAXE_TESTS_RUNNER_ID;
  }

  @Override
  public boolean canRun(@NotNull String executorId, @NotNull RunProfile profile) {
    return DefaultRunExecutor.EXECUTOR_ID.equals(executorId) && profile instanceof HaxeTestsConfiguration;
  }

  @Override
  protected RunContentDescriptor doExecute(@NotNull final Project project,
                                           @NotNull RunProfileState state,
                                           final RunContentDescriptor descriptor,
                                           @NotNull final ExecutionEnvironment environment) throws ExecutionException {

    final HaxeTestsConfiguration profile = (HaxeTestsConfiguration)environment.getRunProfile();
    final Module module = profile.getConfigurationModule().getModule();

    ModuleRootManager rootManager = ModuleRootManager.getInstance(module);
    VirtualFile[] roots = rootManager.getContentRoots();

    return super.doExecute(project, new CommandLineState(environment) {
      @NotNull
      @Override
      protected ProcessHandler startProcess() throws ExecutionException {
        //actually only neko target is supported for tests
        HaxeTarget currentTarget = HaxeTarget.NEKO;
        final GeneralCommandLine commandLine = new GeneralCommandLine();
        commandLine.setWorkDirectory(PathUtil.getParentPath(module.getModuleFilePath()));
        commandLine.setExePath(currentTarget.getFlag());
        final HaxeModuleSettings settings = HaxeModuleSettings.getInstance(module);
        String folder = settings.getOutputFolder() != null ? (settings.getOutputFolder() + "/release/") : "";
        commandLine.addParameter(getFileNameWithCurrentExtension(currentTarget, folder + settings.getOutputFileName()));

        final TextConsoleBuilder consoleBuilder = TextConsoleBuilderFactory.getInstance().createBuilder(module.getProject());
        consoleBuilder.addFilter(new ErrorFilter(module));
        setConsoleBuilder(consoleBuilder);

        return new OSProcessHandler(commandLine.createProcess(), commandLine.getCommandLineString());
      }

      private String getFileNameWithCurrentExtension(HaxeTarget haxeTarget, String fileName) {
        if (haxeTarget != null) {
          return haxeTarget.getTargetFileNameWithExtension(FileUtil.getNameWithoutExtension(fileName));
        }
        return fileName;
      }
    }, descriptor, environment);
  }
}
