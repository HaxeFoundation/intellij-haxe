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
import com.intellij.execution.configurations.CommandLineState;
import com.intellij.execution.filters.TextConsoleBuilder;
import com.intellij.execution.filters.TextConsoleBuilderFactory;
import com.intellij.execution.process.ColoredProcessHandler;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.ProjectUtil;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.plugins.haxe.HaxeCommonBundle;
import com.intellij.plugins.haxe.config.OpenFLTarget;
import com.intellij.plugins.haxe.config.sdk.HaxeSdkData;
import com.intellij.plugins.haxe.ide.module.HaxeModuleSettings;
import com.intellij.plugins.haxe.util.HaxeCommandLine;
import com.intellij.util.text.StringTokenizer;
import org.jetbrains.annotations.NotNull;

/**
 * @author: Fedor.Korotkov
 */
public class OpenFLRunningState extends CommandLineState {
  private final Module module;
  private final boolean myRunInTest;
  private final boolean myDebug;
  private final int myDebugPort;

  public OpenFLRunningState(ExecutionEnvironment env, Module module, boolean runInTest) {
    this(env, module, runInTest, false, 0);
  }

  public OpenFLRunningState(ExecutionEnvironment env, Module module, boolean runInTest, boolean debug) {
    this(env, module, runInTest, debug, 6972);
  }

  public OpenFLRunningState(ExecutionEnvironment env, Module module, boolean runInTest, boolean debug, int debugPort) {
    super(env);
    this.module = module;
    myRunInTest = runInTest;
    myDebug = debug;
    myDebugPort = debugPort;
  }

  @NotNull
  @Override
  protected ProcessHandler startProcess() throws ExecutionException {
    final HaxeModuleSettings settings = HaxeModuleSettings.getInstance(module);
    final Sdk sdk = ModuleRootManager.getInstance(module).getSdk();
    assert sdk != null;

    HaxeCommandLine commandLine = getCommandForOpenFL(sdk, settings);

    return new ColoredProcessHandler(commandLine.createProcess(), commandLine.getCommandLineString());
  }

  private HaxeCommandLine getCommandForOpenFL(Sdk sdk, HaxeModuleSettings settings) throws ExecutionException {
    final HaxeSdkData sdkData = sdk.getSdkAdditionalData() instanceof HaxeSdkData ? (HaxeSdkData)sdk.getSdkAdditionalData() : null;
    if (sdkData == null) {
      throw new ExecutionException(HaxeCommonBundle.message("invalid.haxe.sdk"));
    }
    final HaxeCommandLine commandLine = new HaxeCommandLine(module);

    VirtualFile workDir = ProjectUtil.guessModuleDir(module);
    if(workDir == null) {
      throw new ExecutionException("Unable to to determine workdirectory");
    }
    commandLine.setWorkDirectory(workDir.getCanonicalPath());
    final String haxelibPath = sdkData.getHaxelibPath();
    if (haxelibPath == null || haxelibPath.isEmpty()) {
      throw new ExecutionException(HaxeCommonBundle.message("no.haxelib.for.sdk", sdk.getName()));
    }

    commandLine.setExePath(haxelibPath);
    commandLine.addParameter("run");
    commandLine.addParameter("lime");
    commandLine.addParameter(myRunInTest ? "test" : "run");

    if(!StringUtil.isEmpty(settings.getOpenFLPath())) {
      commandLine.addParameter(settings.getOpenFLPath());
    }

    for (String flag : settings.getOpenFLTarget().getFlags()) {
      commandLine.addParameter(flag);
    }

    commandLine.addParameter("-verbose");

    if (myDebug) {
      commandLine.addParameter("-Ddebug");
      commandLine.addParameter("-debug");

      if (settings.getOpenFLTarget() == OpenFLTarget.FLASH) {
        commandLine.addParameter("-Dfdb");
      }
      else {
        commandLine.addParameter("-args");
        commandLine.addParameter("-start_debugger");
        commandLine.addParameter("-debugger_host=localhost:" + myDebugPort);
      }
    }

    final StringTokenizer flagsTokenizer = new StringTokenizer(settings.getOpenFLFlags());
    while (flagsTokenizer.hasMoreTokens()) {
      commandLine.addParameter(flagsTokenizer.nextToken());
    }

    final TextConsoleBuilder consoleBuilder = TextConsoleBuilderFactory.getInstance().createBuilder(module.getProject());
    setConsoleBuilder(consoleBuilder);
    return commandLine;
  }
}
