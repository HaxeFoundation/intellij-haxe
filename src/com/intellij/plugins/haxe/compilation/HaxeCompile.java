package com.intellij.plugins.haxe.compilation;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.process.CapturingProcessHandler;
import com.intellij.execution.process.ProcessOutput;
import com.intellij.openapi.compiler.CompileContext;
import com.intellij.openapi.compiler.CompilerMessageCategory;
import com.intellij.openapi.compiler.CompilerPaths;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.roots.OrderEnumerator;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.plugins.haxe.HaxeBundle;
import com.intellij.plugins.haxe.config.HaxeTarget;
import com.intellij.plugins.haxe.config.sdk.HaxeSdkUtil;
import com.intellij.plugins.haxe.runner.HaxeApplicationConfiguration;
import com.intellij.plugins.haxe.runner.HaxeApplicationModuleBasedConfiguration;
import com.intellij.plugins.haxe.util.CompilationUtil;

import java.nio.charset.Charset;

public class HaxeCompile extends HaxeCompilerBase {
  @Override
  protected boolean compileModule(CompileContext context, HaxeApplicationConfiguration applicationConfiguration) {
    final HaxeApplicationModuleBasedConfiguration moduleConfiguration = applicationConfiguration.getConfigurationModule();
    final Module module = moduleConfiguration.getModule();
    if (module == null) {
      return false;
    }
    final ModuleRootManager moduleRootManager = ModuleRootManager.getInstance(module);
    final Sdk sdk = moduleRootManager.getSdk();

    if (sdk == null) {
      context.addMessage(CompilerMessageCategory.ERROR, HaxeBundle.message("no.sdk.for.module", module.getName()), null, -1, -1);
      return false;
    }

    final GeneralCommandLine commandLine = new GeneralCommandLine();

    commandLine.setExePath(HaxeSdkUtil.getCompilerPathByFolderPath(sdk.getHomePath()));

    commandLine.addParameter("-main");
    commandLine.addParameter(CompilationUtil.getClassNameByPath(applicationConfiguration.getMainClass()));

    for (VirtualFile sourceRoot : OrderEnumerator.orderEntries(module).recursively().exportedOnly().sources().getRoots()) {
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
      return false;
    }

    if (output.getExitCode() != 0) {
      context.addMessage(CompilerMessageCategory.WARNING, "process exited with code: " + output.getExitCode(), null, -1, -1);
      context.addMessage(CompilerMessageCategory.WARNING, "process exited with output: " + output.getStdout(), null, -1, -1);
      context.addMessage(CompilerMessageCategory.WARNING, "process exited with error: " + output.getStderr(), null, -1, -1);
    }
    return true;
  }
}
