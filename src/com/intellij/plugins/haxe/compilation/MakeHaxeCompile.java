package com.intellij.plugins.haxe.compilation;

import com.intellij.compiler.impl.CompilerUtil;
import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.process.CapturingProcessHandler;
import com.intellij.execution.process.ProcessOutput;
import com.intellij.openapi.compiler.CompileContext;
import com.intellij.openapi.compiler.CompilerMessageCategory;
import com.intellij.openapi.module.Module;
import com.intellij.plugins.haxe.config.sdk.HaxeSdkData;
import com.intellij.plugins.haxe.config.sdk.HaxeSdkUtil;
import com.intellij.plugins.haxe.runner.HaxeApplicationConfiguration;
import com.intellij.plugins.haxe.util.CompilationUtil;

import java.nio.charset.Charset;

public class MakeHaxeCompile extends HaxeCompilerBase {
  @Override
  protected void compileImpl(CompileContext context, HaxeSdkData sdkData, HaxeApplicationConfiguration applicationConfiguration) {
    final String homePath = sdkData.getHomePath();
    final Module module = applicationConfiguration.getConfigurationModule().getModule();

    GeneralCommandLine commandLine = new GeneralCommandLine();

    commandLine.setExePath(HaxeSdkUtil.getCompilerPathByFolderPath(homePath));

    commandLine.addParameter("-main");
    commandLine.addParameter(CompilationUtil.getClassNameByPath(applicationConfiguration.getMainClass()));

    commandLine.addParameter("-neko");
    commandLine.addParameter(CompilationUtil.getNekoBinPathForModule(module));

    commandLine.addParameter("-cp");
    String sourceFolderPath = CompilationUtil.getSourceFolderByModule(module);
    commandLine.addParameter(CompilerUtil.quotePath(sourceFolderPath));

    ProcessOutput output = null;
    try {
      output = new CapturingProcessHandler(
        commandLine.createProcess(),
        Charset.defaultCharset(),
        commandLine.getCommandLineString()).runProcess();
    }
    catch (ExecutionException e) {
      context.addMessage(CompilerMessageCategory.ERROR, "process throw exception: " + e.getMessage(), null, -1, -1);
      return;
    }

    if (output.getExitCode() != 0) {
      context.addMessage(CompilerMessageCategory.WARNING, "process exited with code: " + output.getExitCode(), null, -1, -1);
      context.addMessage(CompilerMessageCategory.WARNING, "process exited with output: " + output.getStdout(), null, -1, -1);
      context.addMessage(CompilerMessageCategory.WARNING, "process exited with error: " + output.getStderr(), null, -1, -1);
    }
  }
}
