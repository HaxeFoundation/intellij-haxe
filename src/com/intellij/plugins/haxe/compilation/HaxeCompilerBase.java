package com.intellij.plugins.haxe.compilation;

import com.intellij.execution.RunManager;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.openapi.compiler.CompileContext;
import com.intellij.openapi.compiler.CompileScope;
import com.intellij.openapi.compiler.TranslatingCompiler;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.plugins.haxe.HaxeBundle;
import com.intellij.plugins.haxe.config.sdk.HaxeSdkData;
import com.intellij.plugins.haxe.config.sdk.HaxeSdkUtil;
import com.intellij.plugins.haxe.runner.HaxeApplicationConfiguration;
import com.intellij.plugins.haxe.runner.HaxeRunConfigurationType;
import com.intellij.util.Chunk;
import org.jetbrains.annotations.NotNull;

public abstract class HaxeCompilerBase implements TranslatingCompiler {
  private static final Logger LOG = Logger.getInstance("#com.intellij.plugins.haxe.compilation.HaxeCompilerBase");

  @NotNull
  public String getDescription() {
    return HaxeBundle.message("haxe.compiler.description");
  }

  public boolean validateConfiguration(CompileScope scope) {
    return true;
  }

  public boolean isCompilableFile(VirtualFile file, CompileContext context) {
    return true;
  }

  public void compile(CompileContext context, Chunk<Module> moduleChunk, VirtualFile[] files, OutputSink sink) {
    final Sdk sdk = getSdk(moduleChunk);
    final HaxeSdkData sdkData = HaxeSdkUtil.testHaxeSdk(sdk.getHomePath());

    HaxeApplicationConfiguration applicationConfiguration = getApplicationConfiguration(context.getProject());

    compileImpl(context, sdkData, applicationConfiguration);
  }

  abstract void compileImpl(CompileContext context, HaxeSdkData sdkData, HaxeApplicationConfiguration applicationConfiguration);

  /**
   * @return the jdk. Assumes that the jdk is the same for all modules
   */
  private Sdk getSdk(Chunk<Module> moduleChunk) {
    final Module module = moduleChunk.getNodes().iterator().next();
    return ModuleRootManager.getInstance(module).getSdk();
  }

  private HaxeApplicationConfiguration getApplicationConfiguration(Project project) {
    RunManager runManager = RunManager.getInstance(project);
    RunConfiguration[] configurations = runManager.getConfigurations(HaxeRunConfigurationType.getInstance());
    if (configurations.length > 0) {
      return (HaxeApplicationConfiguration)configurations[0];
    }
    return null;
  }
}
