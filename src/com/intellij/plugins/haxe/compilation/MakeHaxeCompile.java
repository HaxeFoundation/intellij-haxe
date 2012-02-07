package com.intellij.plugins.haxe.compilation;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.process.CapturingProcessHandler;
import com.intellij.execution.process.ProcessOutput;
import com.intellij.openapi.compiler.CompileContext;
import com.intellij.openapi.compiler.CompilerMessageCategory;
import com.intellij.openapi.compiler.CompilerPaths;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.plugins.haxe.config.HaxeTarget;
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
    if (module == null) {
      return;
    }

    final GeneralCommandLine commandLine = new GeneralCommandLine();

    commandLine.setExePath(HaxeSdkUtil.getCompilerPathByFolderPath(homePath));

    commandLine.addParameter("-main");
    commandLine.addParameter(CompilationUtil.getClassNameByPath(applicationConfiguration.getMainClass()));

    for (VirtualFile sourceRoot : ModuleRootManager.getInstance(module).getSourceRoots()) {
      commandLine.addParameter("-cp");
      commandLine.addParameter(sourceRoot.getPath());
    }

    final VirtualFile outputDirectory = CompilerPaths.getModuleOutputDirectory(module, false);
    if (outputDirectory != null) {
      commandLine.setWorkDirectory(outputDirectory.getPath());
    }

    final HaxeTarget target = applicationConfiguration.getHaxeTarget();
    commandLine.addParameter(target.getCompilerFlag());
    commandLine.addParameter(target.getTargetOutput(module));

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
