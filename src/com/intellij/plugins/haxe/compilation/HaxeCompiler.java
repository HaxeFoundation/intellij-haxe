package com.intellij.plugins.haxe.compilation;

import com.intellij.compiler.options.CompileStepBeforeRun;
import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.execution.configurations.RunConfigurationModule;
import com.intellij.execution.process.CapturingProcessHandler;
import com.intellij.execution.process.ProcessOutput;
import com.intellij.openapi.compiler.*;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleType;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.roots.CompilerModuleExtension;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.roots.OrderEnumerator;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.plugins.haxe.HaxeBundle;
import com.intellij.plugins.haxe.config.HaxeTarget;
import com.intellij.plugins.haxe.config.NMETarget;
import com.intellij.plugins.haxe.config.sdk.HaxeSdkData;
import com.intellij.plugins.haxe.config.sdk.HaxeSdkType;
import com.intellij.plugins.haxe.config.sdk.HaxeSdkUtil;
import com.intellij.plugins.haxe.ide.module.HaxeModuleSettings;
import com.intellij.plugins.haxe.ide.module.HaxeModuleType;
import com.intellij.plugins.haxe.runner.HaxeApplicationConfiguration;
import com.intellij.util.PathUtil;
import com.intellij.util.text.StringTokenizer;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import java.io.DataInput;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

public class HaxeCompiler implements SourceProcessingCompiler {
  private static final Logger LOG = Logger.getInstance("#com.intellij.plugins.haxe.compilation.HaxeCompiler");

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
    if (runConfiguration instanceof HaxeApplicationConfiguration) {
      return run(context, items, (HaxeApplicationConfiguration)runConfiguration);
    }
    return make(context, items);
  }

  private static ProcessingItem[] run(CompileContext context,
                                      ProcessingItem[] items,
                                      HaxeApplicationConfiguration haxeApplicationConfiguration) {
    final Module module = haxeApplicationConfiguration.getConfigurationModule().getModule();
    if (module == null) {
      context.addMessage(CompilerMessageCategory.ERROR,
                         HaxeBundle.message("no.module.for.run.configuration", haxeApplicationConfiguration.getName()), null, -1, -1);
      return ProcessingItem.EMPTY_ARRAY;
    }
    if (compileModule(context, module)) {
      final int index = findProcessingItemIndexByModule(items, haxeApplicationConfiguration.getConfigurationModule());
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

      if (compileModule(context, myProcessingItem.myModule)) {
        result.add(processingItem);
      }
    }
    return result.toArray(new ProcessingItem[result.size()]);
  }

  private static boolean compileModule(CompileContext context, @NotNull Module module) {
    final HaxeModuleSettings settings = HaxeModuleSettings.getInstance(module);
    if (settings.isExcludeFromCompilation()) {
      return true;
    }
    final String mainClass = settings.getMainClass();
    final String fileName = settings.getOutputFileName();

    if (settings.isUseUserPropertiesToBuild()) {
      if (mainClass == null || mainClass.length() == 0) {
        context.addMessage(CompilerMessageCategory.ERROR, HaxeBundle.message("no.main.class.for.module", module.getName()), null, -1, -1);
        return false;
      }
      if (fileName == null || fileName.length() == 0) {
        context.addMessage(CompilerMessageCategory.ERROR, HaxeBundle.message("no.output.file.name.for.module", module.getName()), null, -1,
                           -1);
        return false;
      }
    }

    final HaxeTarget target = settings.getHaxeTarget();
    if (target == null) {
      context.addMessage(CompilerMessageCategory.ERROR, HaxeBundle.message("no.target.for.module", module.getName()), null, -1, -1);
      return false;
    }
    return compileModule(context, module, mainClass, fileName, target);
  }

  private static boolean compileModule(CompileContext context,
                                       @NotNull Module module,
                                       @NonNls String mainClass,
                                       @NonNls String fileName,
                                       @NotNull HaxeTarget target) {
    final HaxeModuleSettings settings = HaxeModuleSettings.getInstance(module);
    final ModuleRootManager moduleRootManager = ModuleRootManager.getInstance(module);
    final Sdk sdk = moduleRootManager.getSdk();

    if (sdk == null) {
      context.addMessage(CompilerMessageCategory.ERROR, HaxeBundle.message("no.sdk.for.module", module.getName()), null, -1, -1);
      return false;
    }

    if (sdk.getSdkType() != HaxeSdkType.getInstance()) {
      context.addMessage(CompilerMessageCategory.ERROR, HaxeBundle.message("not.haxe.sdk.for.module", module.getName()), null, -1, -1);
      return false;
    }

    final String sdkExePath = HaxeSdkUtil.getCompilerPathByFolderPath(sdk.getHomePath());

    if (sdkExePath == null) {
      context.addMessage(CompilerMessageCategory.ERROR, HaxeBundle.message("invalid.haxe.sdk.for.module", module.getName()), null, -1, -1);
      return false;
    }

    final GeneralCommandLine commandLine = new GeneralCommandLine();

    commandLine.setWorkDirectory(PathUtil.getParentPath(module.getModuleFilePath()));

    if (settings.isUseNmmlToBuild()) {
      final HaxeSdkData sdkData = sdk.getSdkAdditionalData() instanceof HaxeSdkData ? (HaxeSdkData)sdk.getSdkAdditionalData() : null;
      final String haxelibPath = sdkData == null ? null : sdkData.getHaxelibPath();
      if (haxelibPath == null) {
        context.addMessage(CompilerMessageCategory.ERROR, HaxeBundle.message("no.haxelib.for.sdk", sdk.getName()), null, -1, -1);
        return false;
      }
      setupNME(commandLine, settings, haxelibPath);
    }
    else if (settings.isUseHxmlToBuild()) {
      commandLine.setExePath(sdkExePath);
      commandLine.addParameter(FileUtil.toSystemDependentName(settings.getHxmlPath()));
      if (settings.getNmeTarget() == NMETarget.FLASH) {
        commandLine.addParameter("-debug");
        commandLine.addParameter("-D");
        commandLine.addParameter("fdb");
      }
    }
    else {
      setupUserProperties(module, mainClass, fileName, target, settings, sdkExePath, commandLine);
    }

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
      HaxeCompilerUtil.fillContext(module, context, output.getStderrLines());
      return false;
    }
    return true;
  }

  private static void setupUserProperties(Module module,
                                          String mainClass,
                                          String fileName,
                                          HaxeTarget target,
                                          HaxeModuleSettings settings,
                                          String sdkExePath, GeneralCommandLine commandLine) {
    commandLine.setExePath(sdkExePath);
    commandLine.addParameter("-main");
    commandLine.addParameter(mainClass);

    final StringTokenizer argumentsTokenizer = new StringTokenizer(settings.getArguments());
    while (argumentsTokenizer.hasMoreTokens()) {
      commandLine.addParameter(argumentsTokenizer.nextToken());
    }

    if (target == HaxeTarget.FLASH) {
      commandLine.addParameter("-debug");
      commandLine.addParameter("-D");
      commandLine.addParameter("fdb");
    }

    for (VirtualFile sourceRoot : OrderEnumerator.orderEntries(module).recursively().withoutSdk().exportedOnly().sources().getRoots()) {
      commandLine.addParameter("-cp");
      commandLine.addParameter(sourceRoot.getPath());
    }
    for (VirtualFile sourceRoot : OrderEnumerator.orderEntries(module).librariesOnly().getSourceRoots()) {
      commandLine.addParameter("-cp");
      commandLine.addParameter(sourceRoot.getPath());
    }
    commandLine.addParameter(target.getCompilerFlag());
    final String outputUrl = CompilerModuleExtension.getInstance(module).getCompilerOutputUrl();
    commandLine.addParameter(VfsUtil.urlToPath(outputUrl + "/" + fileName));
  }

  private static void setupNME(GeneralCommandLine commandLine, HaxeModuleSettings settings, String haxelibPath) {
    commandLine.setExePath(haxelibPath);
    commandLine.addParameter("run");
    commandLine.addParameter("nme");
    commandLine.addParameter("build");
    commandLine.addParameter(settings.getNmmlPath());
    commandLine.addParameter(settings.getNmeTarget().getTargetFlag());

    if (settings.getNmeTarget() == NMETarget.FLASH) {
      commandLine.addParameter("-debug");
    }
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
