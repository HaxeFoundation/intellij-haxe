package com.intellij.plugins.haxe.runner;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.CommandLineState;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.filters.TextConsoleBuilder;
import com.intellij.execution.filters.TextConsoleBuilderFactory;
import com.intellij.execution.process.OSProcessHandler;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.openapi.compiler.CompilerPaths;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.plugins.haxe.config.HaxeTarget;
import com.intellij.plugins.haxe.config.sdk.HaxeSdkData;
import org.jetbrains.annotations.NotNull;

public class HaxeRunningState extends CommandLineState {
  private final Module module;
  private final HaxeTarget haxeTarget;

  public HaxeRunningState(ExecutionEnvironment env, HaxeTarget haxeTarget, Module module) {
    super(env);
    this.haxeTarget = haxeTarget;
    this.module = module;
  }

  @NotNull
  @Override
  protected ProcessHandler startProcess() throws ExecutionException {
    final Sdk sdk = ModuleRootManager.getInstance(module).getSdk();
    final HaxeSdkData sdkData = sdk.getSdkAdditionalData() instanceof HaxeSdkData ? (HaxeSdkData)sdk.getSdkAdditionalData() : null;

    GeneralCommandLine commandLine = new GeneralCommandLine();

    //todo: all targets
    if (sdkData != null && sdkData.getNekoBinPath() != null && haxeTarget == HaxeTarget.NEKO) {
      commandLine.setExePath(sdkData.getNekoBinPath());
    }
    commandLine.addParameter(haxeTarget.getTargetOutput(module));

    final VirtualFile outputDirectory = CompilerPaths.getModuleOutputDirectory(module, false);
    if (outputDirectory != null) {
      commandLine.setWorkDirectory(outputDirectory.getPath());
    }

    final TextConsoleBuilder consoleBuilder = TextConsoleBuilderFactory.getInstance().createBuilder(module.getProject());
    setConsoleBuilder(consoleBuilder);

    return new OSProcessHandler(commandLine.createProcess(), commandLine.getCommandLineString());
  }
}
