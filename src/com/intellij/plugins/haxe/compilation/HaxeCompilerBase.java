package com.intellij.plugins.haxe.compilation;

import com.intellij.compiler.options.CompileStepBeforeRun;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.execution.configurations.RunConfigurationModule;
import com.intellij.openapi.compiler.*;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleType;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.plugins.haxe.HaxeBundle;
import com.intellij.plugins.haxe.ide.HaxeModuleType;
import com.intellij.plugins.haxe.runner.HaxeApplicationConfiguration;
import com.intellij.util.Chunk;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.DataInput;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public abstract class HaxeCompilerBase implements SourceProcessingCompiler {
  private static final Logger LOG = Logger.getInstance("#com.intellij.plugins.haxe.compilation.HaxeCompilerBase");

  abstract boolean compileModule(CompileContext context, HaxeApplicationConfiguration applicationConfiguration);

  @NotNull
  public String getDescription() {
    return HaxeBundle.message("haxe.compiler.description");
  }

  @Override
  public boolean validateConfiguration(CompileScope scope) {
    return true;
  }

  @NotNull
  @Override
  public ProcessingItem[] getProcessingItems(CompileContext context) {
    final List<ProcessingItem> itemList = new ArrayList<ProcessingItem>();
    for (final Module module : getModulesToCompile(context.getCompileScope())) {
      itemList.add(new MyProcessingItem(module));
    }
    return itemList.toArray(new ProcessingItem[itemList.size()]);
  }

  private static List<Module> getModulesToCompile(CompileScope scope) {
    final List<Module> result = new ArrayList<Module>();
    for (final Module module : scope.getAffectedModules()) {
      if (ModuleType.get(module) != HaxeModuleType.getInstance()) continue;
      result.add(module);
    }
    return result;
  }

  @Override
  public ProcessingItem[] process(CompileContext context, ProcessingItem[] items) {
    final RunConfiguration runConfiguration = CompileStepBeforeRun.getRunConfiguration(context.getCompileScope());
    if (!(runConfiguration instanceof HaxeApplicationConfiguration)) {
      return ProcessingItem.EMPTY_ARRAY;
    }
    final HaxeApplicationConfiguration haxeApplicationConfiguration = (HaxeApplicationConfiguration)runConfiguration;
    if (compileModule(context, haxeApplicationConfiguration)) {
      final int index = findProcessingItemIndexByModule(items, haxeApplicationConfiguration.getConfigurationModule());
      if (index != -1) {
        return new ProcessingItem[]{items[index]};
      }
    }
    return ProcessingItem.EMPTY_ARRAY;
  }

  private static int findProcessingItemIndexByModule(ProcessingItem[] items, RunConfigurationModule moduleConfiguration) {
    final Module module = moduleConfiguration.getModule();
    if (module == null || module.getModuleFile() == null) {
      return -1;
    }
    for (int i = 0; i < items.length; ++i) {
      if (module.getModuleFile().equals(items[i].getFile())) {
        return i;
      }
    }
    return -1;
  }

  @Override
  public ValidityState createValidityState(DataInput in) throws IOException {
    return new EmptyValidityState();
  }

  /**
   * @return the jdk. Assumes that the jdk is the same for all modules
   */
  @Nullable
  public static Sdk getSdk(Chunk<Module> moduleChunk) {
    final Module module = moduleChunk.getNodes().iterator().next();
    return ModuleRootManager.getInstance(module).getSdk();
  }

  private static class MyProcessingItem implements ProcessingItem {
    private final Module myModule;

    private MyProcessingItem(Module module) {
      myModule = module;
    }

    @NotNull
    public VirtualFile getFile() {
      return myModule.getModuleFile();
    }

    public ValidityState getValidityState() {
      return new EmptyValidityState();
    }
  }
}
