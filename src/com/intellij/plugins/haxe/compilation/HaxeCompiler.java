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
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.plugins.haxe.HaxeBundle;
import com.intellij.plugins.haxe.config.HaxeTarget;
import com.intellij.plugins.haxe.config.sdk.HaxeSdkType;
import com.intellij.plugins.haxe.config.sdk.HaxeSdkUtil;
import com.intellij.plugins.haxe.ide.module.HaxeModuleSettings;
import com.intellij.plugins.haxe.ide.module.HaxeModuleType;
import com.intellij.plugins.haxe.runner.HaxeApplicationConfiguration;
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
    if (mainClass == null || mainClass.length() == 0) {
      context.addMessage(CompilerMessageCategory.ERROR, HaxeBundle.message("no.main.class.for.module", module.getName()), null, -1, -1);
      return false;
    }
    final String fileName = settings.getOutputFileName();
    if (fileName == null || fileName.length() == 0) {
      context.addMessage(CompilerMessageCategory.ERROR, HaxeBundle.message("no.output.file.name.for.module", module.getName()), null, -1,
                         -1);
      return false;
    }
    final HaxeTarget target = settings.getTarget();
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

    commandLine.setExePath(sdkExePath);

    commandLine.addParameter("-main");
    commandLine.addParameter(mainClass);

    for (VirtualFile sourceRoot : OrderEnumerator.orderEntries(module).recursively().exportedOnly().sources().getRoots()) {
      commandLine.addParameter("-cp");
      commandLine.addParameter(sourceRoot.getPath());
    }

    final String url = CompilerModuleExtension.getInstance(module).getCompilerOutputUrl();
    commandLine.setWorkDirectory(VfsUtil.urlToPath(url));
    commandLine.addParameter(target.getCompilerFlag());
    commandLine.addParameter(fileName);

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
      HaxeCompilerUtil.fillContext(context, output.getStderrLines());
      return false;
    }
    return true;
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
