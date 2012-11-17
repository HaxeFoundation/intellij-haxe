package com.intellij.plugins.haxe.util;

import com.intellij.execution.process.BaseOSProcessHandler;
import com.intellij.execution.process.ProcessAdapter;
import com.intellij.execution.process.ProcessEvent;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.plugins.haxe.HaxeCommonBundle;
import com.intellij.plugins.haxe.config.HaxeTarget;
import com.intellij.plugins.haxe.config.NMETarget;
import com.intellij.plugins.haxe.module.HaxeModuleSettingsBase;
import com.intellij.util.BooleanValueHolder;
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

    boolean isDebug();

    String getSdkName();

    List<String> getSourceRoots();

    String getCompileOutputPath();

    boolean handleOutput(String[] lines);
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
    if (target == null && !settings.isUseNmmlToBuild()) {
      context.errorHandler(HaxeCommonBundle.message("no.target.for.module", context.getModuleName()));
      return false;
    }
    if (nmeTarget == null && settings.isUseNmmlToBuild()) {
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

    final String haxelibPath = HaxeSdkUtilBase.getHaxelibPathByFolderPath(context.getSdkHomePath());
    if (settings.isUseNmmlToBuild() && (haxelibPath == null || haxelibPath.isEmpty())) {
      context.errorHandler(HaxeCommonBundle.message("no.haxelib.for.sdk", context.getSdkName()));
      return false;
    }

    final List<String> commandLine = new ArrayList<String>();

    if (settings.isUseNmmlToBuild()) {
      commandLine.add(haxelibPath);
    }
    else {
      commandLine.add(sdkExePath);
    }

    String workingPath = context.getCompileOutputPath() + "/" + (context.isDebug() ? "debug" : "release");
    if (settings.isUseNmmlToBuild()) {
      setupNME(commandLine, context);
    }
    else if (settings.isUseHxmlToBuild()) {
      String hxmlPath = FileUtil.toSystemDependentName(settings.getHxmlPath());
      commandLine.add(hxmlPath);
      workingPath = hxmlPath.substring(0, hxmlPath.lastIndexOf('/'));
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
          final boolean messageHasErrors = context.handleOutput(event.getText().split("\\n"));
          hasErrors.setValue(hasErrors.getValue() || messageHasErrors);
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
      commandLine.add("-Ddebug");
    }
    if (settings.getNmeTarget() == NMETarget.FLASH && context.isDebug()) {
      commandLine.add("-Dfdb");
    }
  }
}
