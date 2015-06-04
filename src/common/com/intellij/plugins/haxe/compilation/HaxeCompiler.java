/*
 * Copyright 2000-2013 JetBrains s.r.o.
 * Copyright 2014-2014 AS3Boyan
 * Copyright 2014-2014 Elias Ku
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
package com.intellij.plugins.haxe.compilation;

import com.intellij.compiler.options.CompileStepBeforeRun;
import com.intellij.execution.ExecutorRegistry;
import com.intellij.execution.configurations.ModuleBasedConfiguration;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.execution.configurations.RunConfigurationModule;
import com.intellij.execution.executors.DefaultDebugExecutor;
import com.intellij.openapi.compiler.*;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleType;
import com.intellij.openapi.module.ModuleUtil;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.projectRoots.SdkAdditionalData;
import com.intellij.openapi.roots.CompilerModuleExtension;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.roots.OrderEnumerator;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.plugins.haxe.HaxeBundle;
import com.intellij.plugins.haxe.config.HaxeTarget;
import com.intellij.plugins.haxe.config.sdk.HaxeSdkAdditionalDataBase;
import com.intellij.plugins.haxe.ide.module.HaxeModuleSettings;
import com.intellij.plugins.haxe.ide.module.HaxeModuleType;
import com.intellij.plugins.haxe.module.HaxeModuleSettingsBase;
import com.intellij.plugins.haxe.runner.debugger.HaxeDebugRunner;
import com.intellij.plugins.haxe.tests.runner.HaxeTestsConfiguration;
import com.intellij.plugins.haxe.util.HaxeCommonCompilerUtil;
import com.intellij.util.PathUtil;
import org.jetbrains.annotations.NotNull;

import java.io.DataInput;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class HaxeCompiler implements SourceProcessingCompiler {
  private static final Logger LOG = Logger.getInstance("#com.intellij.plugins.haxe.compilation.HaxeCompiler");

  /*
  // flag to indicate whether a module needs to be built
  private static HashMap<Module, Boolean> skipBuildMap = new HashMap<Module, Boolean>();
  // holds the change set for each module to track whether anything changed since last build
  private static HashMap<Module, Set<String>> changeSetMap = new HashMap<Module, Set<String>>();
  */

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
    for (final Module module : getModulesToCompile(context.getCompileScope() /*, context.getProject() */)) {
      itemList.add(new MyProcessingItem(module));
    }
    return itemList.toArray(new ProcessingItem[itemList.size()]);
  }

  private static List<Module> getModulesToCompile(final CompileScope scope /*, final Project project */) {
    final List<Module> result = new ArrayList<Module>();
    for (final Module module : scope.getAffectedModules()) {
      if (ModuleType.get(module) != HaxeModuleType.getInstance()) continue;
      result.add(module);
      /*
      boolean skipBuilding = false; // default: always build
      //-- are any changes since last build
      if (changeSetMap.get(module) != null) { // was built at least once, in past ...
        final Set<String> latestChangeSet = new HashSet<String>();
        Collection<VirtualFile> vFileCollection = FileTypeIndex
          .getFiles(HaxeFileType.HAXE_FILE_TYPE, module.getModuleWithDependenciesScope());
        final VirtualFile[] virtualFiles = vFileCollection.toArray(new VirtualFile[0]);
        for (VirtualFile file : virtualFiles) {
          final FileStatus fileStatus = FileStatusManager.getInstance(project).getStatus(file);
          if (fileStatus.equals(FileStatus.NOT_CHANGED) || fileStatus.equals(FileStatus.UNKNOWN)) continue;
          latestChangeSet.add(file.getPath());
        }
        skipBuilding = latestChangeSet.equals(changeSetMap.get(module));
        changeSetMap.put(module, latestChangeSet);
      }
      else {
        changeSetMap.put(module, new HashSet<String>());
      }
      skipBuildMap.put(module, new Boolean(skipBuilding));
      if (! skipBuilding) {
        result.add(module);
      }
      */
    }
    return result;
  }

  @Override
  public ProcessingItem[] process(CompileContext context, ProcessingItem[] items) {
    final RunConfiguration runConfiguration = CompileStepBeforeRun.getRunConfiguration(context.getCompileScope());
    if (runConfiguration instanceof ModuleBasedConfiguration) {
      return run(context, items, (ModuleBasedConfiguration)runConfiguration);
    }
    return make(context, items);
  }

  private static ProcessingItem[] run(CompileContext context,
                                      ProcessingItem[] items,
                                      ModuleBasedConfiguration configuration) {
    final Module module = configuration.getConfigurationModule().getModule();
    if (module == null) {
      context.addMessage(CompilerMessageCategory.ERROR,
                         HaxeBundle.message("no.module.for.run.configuration", configuration.getName()), null, -1, -1);
      return ProcessingItem.EMPTY_ARRAY;
    }
    HaxeCommonCompilerUtil.CompilationContext compilationContext = createCompilationContext(context, module, configuration);

    if (compileModule(context, module, compilationContext)) {
      final int index = findProcessingItemIndexByModule(items, configuration.getConfigurationModule());
      if (index != -1) {
        return new ProcessingItem[]{items[index]};
      }
    }
    return ProcessingItem.EMPTY_ARRAY;
  }

  private static ProcessingItem[] make(CompileContext context, ProcessingItem[] items) {
    final List<ProcessingItem> result = new ArrayList<ProcessingItem>();
    for (ProcessingItem processingItem : items) {
      if (!(processingItem instanceof MyProcessingItem)) {
        continue;
      }
      final MyProcessingItem myProcessingItem = (MyProcessingItem)processingItem;
      if (compileModule(context, myProcessingItem.myModule, createCompilationContext(context, myProcessingItem.myModule, null))) {
        result.add(processingItem);
      }
    }
    return result.toArray(new ProcessingItem[result.size()]);
  }

  private static boolean compileModule(final CompileContext context,
                                       Module module,
                                       @NotNull final HaxeCommonCompilerUtil.CompilationContext compilationContext) {

    /*
    if ((skipBuildMap.get(module) != null) && (skipBuildMap.get(module).booleanValue())) {
      return false;
    }
    */

    if (!ModuleUtil.getModuleType(module).equals(HaxeModuleType.getInstance())) {
      return true;
    }

    boolean compiled = HaxeCommonCompilerUtil.compile(compilationContext);

    if (!compiled) {
      context.addMessage(CompilerMessageCategory.ERROR, "Compilation failed", null, 0, 0);
    }

    return compiled;
  }

  private static HaxeCommonCompilerUtil.CompilationContext createCompilationContext(final CompileContext context,
                                                                                    final Module module,
                                                                                    ModuleBasedConfiguration configuration) {

    final HaxeModuleSettings settings = HaxeModuleSettings.getInstance(module);
    final boolean isDebug = ExecutorRegistry.getInstance()
      .isStarting(context.getProject(), DefaultDebugExecutor.EXECUTOR_ID, HaxeDebugRunner.HAXE_DEBUG_RUNNER_ID);
    final ModuleRootManager moduleRootManager = ModuleRootManager.getInstance(module);
    final Sdk sdk = moduleRootManager.getSdk();
    if (sdk == null) {
      context.addMessage(CompilerMessageCategory.ERROR, HaxeBundle.message("no.sdk.for.module", module.getName()), null, -1, -1);
      return null;
    }

    HaxeTestsConfiguration haxeTestsConfiguration = null;

    if(configuration != null && configuration instanceof HaxeTestsConfiguration) {
      haxeTestsConfiguration = (HaxeTestsConfiguration)configuration;
    }

    final HaxeTestsConfiguration finalHaxeTestsConfiguration = haxeTestsConfiguration;

    return new HaxeCommonCompilerUtil.CompilationContext() {
      private String myErrorRoot;

      @NotNull
      @Override
      public HaxeModuleSettingsBase getModuleSettings() {
        return settings;
      }

      @Override
      public String getModuleName() {
        return module.getName();
      }

      @Override
      public String getCompilationClass() {
        return getIsTestBuild() ? finalHaxeTestsConfiguration.getRunnerClass() : getModuleSettings().getMainClass();
      }

      @Override
      public String getOutputFileName() {
        return getFileNameWithCurrentExtension(getModuleSettings().getOutputFileName());
      }

      @Override
      public Boolean getIsTestBuild() {
        return finalHaxeTestsConfiguration != null;
      }

      @Override
      public void errorHandler(String message) {
        context.addMessage(CompilerMessageCategory.ERROR, message, null, -1, -1);
      }

      @Override
      public void warningHandler(String message) {
        context.addMessage(CompilerMessageCategory.WARNING, message, null, -1, -1);
      }

      @Override
      public void infoHandler(String message) {
        context.addMessage(CompilerMessageCategory.INFORMATION, message, null, -1, -1);
      }

      public void statisticsHandler(String message) {
        context.addMessage(CompilerMessageCategory.INFORMATION, message, null, -1, -1);
      }

      @Override
      public void log(String message) {
        LOG.debug(message);
      }

      @Override
      public String getSdkName() {
        return sdk.getName();
      }

      @Override
      public String getSdkHomePath() {
        return sdk.getHomePath();
      }

      @Override
      public String getHaxelibPath() {
        SdkAdditionalData data = sdk.getSdkAdditionalData();
        return data instanceof HaxeSdkAdditionalDataBase ? ((HaxeSdkAdditionalDataBase)data).getHaxelibPath() : null;
      }

      @Override
      public boolean isDebug() {
        return isDebug;
      }

      @Override
      public List<String> getSourceRoots() {
        final List<String> result = new ArrayList<String>();
        for (VirtualFile sourceRoot : OrderEnumerator.orderEntries(module).recursively().withoutSdk().exportedOnly().sources().getRoots()) {
          result.add(sourceRoot.getPath());
        }
        for (VirtualFile sourceRoot : OrderEnumerator.orderEntries(module).librariesOnly().getSourceRoots()) {
          result.add(sourceRoot.getPath());
        }
        return result;
      }

      @Override
      public String getCompileOutputPath() {
        final CompilerModuleExtension moduleExtension = CompilerModuleExtension.getInstance(module);
        final String outputUrl = moduleExtension != null ? moduleExtension.getCompilerOutputUrl() : null;
        return VfsUtilCore.urlToPath(outputUrl);
      }

      @Override
      public void setErrorRoot(String root) {
        myErrorRoot = root;
      }

      @Override
      public String getErrorRoot() {
        return (myErrorRoot != null) ? myErrorRoot : PathUtil.getParentPath(module.getModuleFilePath());
      }

      @Override
      public void handleOutput(String[] lines) {
        HaxeCompilerUtil.fillContext(context, getErrorRoot(), lines);
      }

      @Override
      public HaxeTarget getHaxeTarget() {
        //actually only neko target is supported for tests
        return getIsTestBuild() ? HaxeTarget.NEKO : getModuleSettings().getHaxeTarget();
      }

      private String getFileNameWithCurrentExtension(String fileName) {
        if (getHaxeTarget() != null) {
          return getHaxeTarget().getTargetFileNameWithExtension(FileUtil.getNameWithoutExtension(fileName));
        }
        return fileName;
      }

      @Override
      public String getModuleDirPath() {
        return PathUtil.getParentPath(module.getModuleFilePath());
      }
    };
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
