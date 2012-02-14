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
import com.intellij.plugins.haxe.HaxeBundle;
import com.intellij.plugins.haxe.config.HaxeTarget;
import com.intellij.plugins.haxe.config.sdk.HaxeSdkData;
import com.intellij.plugins.haxe.ide.module.HaxeModuleSettings;
import org.jetbrains.annotations.NotNull;

public class HaxeRunningState extends CommandLineState {
  private final Module module;

  public HaxeRunningState(ExecutionEnvironment env, Module module) {
    super(env);
    this.module = module;
  }

  @NotNull
  @Override
  protected ProcessHandler startProcess() throws ExecutionException {
    final HaxeModuleSettings settings = HaxeModuleSettings.getInstance(module);
    final HaxeTarget haxeTarget = settings.getTarget();
    final Sdk sdk = ModuleRootManager.getInstance(module).getSdk();
    assert sdk != null;
    final HaxeSdkData sdkData = sdk.getSdkAdditionalData() instanceof HaxeSdkData ? (HaxeSdkData)sdk.getSdkAdditionalData() : null;

    GeneralCommandLine commandLine = new GeneralCommandLine();

    //todo: all targets
    if (haxeTarget != HaxeTarget.NEKO) {
      throw new ExecutionException(HaxeBundle.message("haxe.run.wrong.target", haxeTarget));
    }
    if (sdkData == null || sdkData.getNekoBinPath() == null || sdkData.getNekoBinPath().isEmpty()) {
      throw new ExecutionException(HaxeBundle.message("haxe.run.bad.neko.bin.path"));
    }

    commandLine.setExePath(sdkData.getNekoBinPath());
    commandLine.addParameter(module.getName());

    final VirtualFile outputDirectory = CompilerPaths.getModuleOutputDirectory(module, false);
    if (outputDirectory != null) {
      commandLine.setWorkDirectory(outputDirectory.getPath());
    }

    final TextConsoleBuilder consoleBuilder = TextConsoleBuilderFactory.getInstance().createBuilder(module.getProject());
    setConsoleBuilder(consoleBuilder);

    return new OSProcessHandler(commandLine.createProcess(), commandLine.getCommandLineString());
  }
}
