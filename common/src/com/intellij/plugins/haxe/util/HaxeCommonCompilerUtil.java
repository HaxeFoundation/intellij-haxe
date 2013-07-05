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
package com.intellij.plugins.haxe.util;

import com.intellij.execution.process.BaseOSProcessHandler;
import com.intellij.execution.process.ProcessAdapter;
import com.intellij.execution.process.ProcessEvent;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.plugins.haxe.HaxeCommonBundle;
import com.intellij.plugins.haxe.config.HaxeTarget;
import com.intellij.plugins.haxe.config.NMETarget;
import com.intellij.plugins.haxe.config.OpenFLTarget;
import com.intellij.plugins.haxe.module.HaxeModuleSettingsBase;
import com.intellij.util.BooleanValueHolder;
import com.intellij.util.PathUtil;
import com.intellij.util.text.StringTokenizer;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

/**
 * @author: Fedor.Korotkov
 */
public class HaxeCommonCompilerUtil {
  public interface CompilationContext {
    @NotNull
    HaxeModuleSettingsBase getModuleSettings();

    String getModuleName();

    void errorHandler(String message);

    void log(String message);

    String getSdkHomePath();

    public String getHaxelibPath();

    boolean isDebug();

    String getSdkName();

    List<String> getSourceRoots();

    String getCompileOutputPath();

    void setErrorRoot(String root);

    String getErrorRoot();

    void handleOutput(String[] lines);
  }

  public static boolean compile(final CompilationContext context) {
    HaxeModuleSettingsBase settings = context.getModuleSettings();
    if (settings.isExcludeFromCompilation()) {
      context.log("Module " + context.getModuleName() + " is excluded from compilation.");
      return true;
    }
    final String mainClass = settings.getMainClass();
    final String fileName = settings.getOutputFileName();

    if (settings.isUseUserPropertiesToBuild()) {
      if (mainClass == null || mainClass.length() == 0) {
        context.errorHandler(HaxeCommonBundle.message("no.main.class.for.module", context.getModuleName()));
        return false;
      }
      if (fileName == null || fileName.length() == 0) {
        context.errorHandler(HaxeCommonBundle.message("no.output.file.name.for.module", context.getModuleName()));
        return false;
      }
    }

    final HaxeTarget target = settings.getHaxeTarget();
    final NMETarget nmeTarget = settings.getNmeTarget();
    final OpenFLTarget openFlTarget = settings.getOpenFLTarget();

    if (target == null && !settings.isUseNmmlToBuild()) {
      context.errorHandler(HaxeCommonBundle.message("no.target.for.module", context.getModuleName()));
      return false;
    }
    if (nmeTarget == null && settings.isUseNmmlToBuild()) {
      context.errorHandler(HaxeCommonBundle.message("no.target.for.module", context.getModuleName()));
      return false;
    }
    if (openFlTarget == null && settings.isUseOpenFlToBuild()) {
      context.errorHandler(HaxeCommonBundle.message("no.target.for.module", context.getModuleName()));
      return false;
    }

    if (context.getSdkHomePath() == null) {
      context.errorHandler(HaxeCommonBundle.message("no.sdk.for.module", context.getModuleName()));
      return false;
    }

    final String sdkExePath = HaxeSdkUtilBase.getCompilerPathByFolderPath(context.getSdkHomePath());

    if (sdkExePath == null || sdkExePath.isEmpty()) {
      context.errorHandler(HaxeCommonBundle.message("invalid.haxe.sdk.for.module", context.getModuleName()));
      return false;
    }

    final String haxelibPath = context.getHaxelibPath();
    if ((settings.isUseOpenFlToBuild() || settings.isUseNmmlToBuild()) && (haxelibPath == null || haxelibPath.isEmpty())) {
      context.errorHandler(HaxeCommonBundle.message("no.haxelib.for.sdk", context.getSdkName()));
      return false;
    }

    final List<String> commandLine = new ArrayList<String>();

    if (settings.isUseOpenFlToBuild() || settings.isUseNmmlToBuild()) {
      commandLine.add(haxelibPath);
    }
    else {
      commandLine.add(sdkExePath);
    }

    String workingPath = context.getCompileOutputPath() + "/" + (context.isDebug() ? "debug" : "release");

    if (settings.isUseOpenFlToBuild()) {
      setupOpenFL(commandLine, context);
      String openFlPath = settings.getOpenFlPath();
      String openFlDir = PathUtil.getParentPath(openFlPath);
      if (!StringUtil.isEmpty(openFlDir)) {
        context.setErrorRoot(openFlDir);
      }
    }
    else if (settings.isUseNmmlToBuild()) {
      setupNME(commandLine, context);
      String nmmlPath = settings.getNmmlPath();
      String nmmlDir = PathUtil.getParentPath(nmmlPath);
      if (!StringUtil.isEmpty(nmmlDir)) {
        context.setErrorRoot(nmmlDir);
      }
    }
    else if (settings.isUseHxmlToBuild()) {
      String hxmlPath = settings.getHxmlPath();
      commandLine.add(FileUtil.toSystemDependentName(hxmlPath));
      final int endIndex = hxmlPath.lastIndexOf('/');
      if (endIndex > 0) {
        workingPath = hxmlPath.substring(0, endIndex);
      }
      if (context.isDebug() && settings.getHaxeTarget() == HaxeTarget.FLASH) {
        commandLine.add("-D");
        commandLine.add("fdb");
        commandLine.add("-debug");
      }
    }
    else {
      setupUserProperties(commandLine, context);
    }

    final BooleanValueHolder hasErrors = new BooleanValueHolder(false);

    try {
      final File workingDirectory = new File(FileUtil.toSystemDependentName(workingPath));
      if (!workingDirectory.exists()) {
        workingDirectory.mkdir();
      }
      BaseOSProcessHandler handler = new BaseOSProcessHandler(
        new ProcessBuilder(commandLine).directory(workingDirectory).start(),
        null,
        Charset.defaultCharset()
      );

      handler.addProcessListener(new ProcessAdapter() {
        @Override
        public void onTextAvailable(ProcessEvent event, Key outputType) {
          context.handleOutput(event.getText().split("\\n"));
        }

        @Override
        public void processTerminated(ProcessEvent event) {
          hasErrors.setValue(event.getExitCode() != 0);
          super.processTerminated(event);
        }
      });

      handler.startNotify();
      handler.waitFor();
    }
    catch (IOException e) {
      context.errorHandler("process throw exception: " + e.getMessage());
      return false;
    }

    return !hasErrors.getValue();
  }

  private static void setupUserProperties(List<String> commandLine, CompilationContext context) {
    final HaxeModuleSettingsBase settings = context.getModuleSettings();
    commandLine.add("-main");
    commandLine.add(settings.getMainClass());

    final StringTokenizer argumentsTokenizer = new StringTokenizer(settings.getArguments());
    while (argumentsTokenizer.hasMoreTokens()) {
      commandLine.add(argumentsTokenizer.nextToken());
    }

    if (context.isDebug()) {
      commandLine.add("-debug");
    }
    if (settings.getHaxeTarget() == HaxeTarget.FLASH && context.isDebug()) {
      commandLine.add("-D");
      commandLine.add("fdb");
    }

    for (String sourceRoot : context.getSourceRoots()) {
      commandLine.add("-cp");
      commandLine.add(sourceRoot);
    }

    commandLine.add(settings.getHaxeTarget().getCompilerFlag());
    commandLine.add(settings.getOutputFileName());
  }

  private static void setupNME(List<String> commandLine, CompilationContext context) {
    final HaxeModuleSettingsBase settings = context.getModuleSettings();
    commandLine.add("run");
    commandLine.add("nme");
    commandLine.add("build");
    commandLine.add(settings.getNmmlPath());
    commandLine.add(settings.getNmeTarget().getTargetFlag());
    if (context.isDebug()) {
      commandLine.add("-debug");
      commandLine.add("-Ddebug");
    }
    if (settings.getNmeTarget() == NMETarget.FLASH && context.isDebug()) {
      commandLine.add("-Dfdb");
    }
    final StringTokenizer flagsTokenizer = new StringTokenizer(settings.getNmeFlags());
    while (flagsTokenizer.hasMoreTokens()) {
      commandLine.add(flagsTokenizer.nextToken());
    }
  }

  private static void setupOpenFL(List<String> commandLine, CompilationContext context) {
    final HaxeModuleSettingsBase settings = context.getModuleSettings();
    commandLine.add("run");
    commandLine.add("openfl");
    commandLine.add("build");
    commandLine.add(settings.getOpenFlPath());
    commandLine.add(settings.getOpenFLTarget().getTargetFlag());
    if (context.isDebug()) {
      commandLine.add("-debug");
      commandLine.add("-Ddebug");
    }
    if (settings.getOpenFLTarget() == OpenFLTarget.FLASH && context.isDebug()) {
      commandLine.add("-Dfdb");
    }
    final StringTokenizer flagsTokenizer = new StringTokenizer(settings.getOpenFLFlags());
    while (flagsTokenizer.hasMoreTokens()) {
      commandLine.add(flagsTokenizer.nextToken());
    }
  }
}
