package com.intellij.plugins.haxe.runner;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.CommandLineState;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.filters.TextConsoleBuilder;
import com.intellij.execution.filters.TextConsoleBuilderFactory;
import com.intellij.execution.process.OSProcessHandler;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.execution.util.ExecUtil;
import com.intellij.ide.BrowserUtil;
import com.intellij.ide.IdeBundle;
import com.intellij.openapi.compiler.CompilerPaths;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.roots.CompilerModuleExtension;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.plugins.haxe.HaxeBundle;
import com.intellij.plugins.haxe.config.HaxeTarget;
import com.intellij.plugins.haxe.config.sdk.HaxeSdkData;
import com.intellij.plugins.haxe.ide.module.HaxeModuleSettings;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.MalformedURLException;
import java.net.URL;

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

    if (haxeTarget != HaxeTarget.NEKO && haxeTarget != HaxeTarget.FLASH) {
      throw new ExecutionException(HaxeBundle.message("haxe.run.wrong.target", haxeTarget));
    }

    final CompilerModuleExtension model = CompilerModuleExtension.getInstance(module);
    assert model != null;
    final String url = model.getCompilerOutputUrl() + "/" + settings.getOutputFileName();
    GeneralCommandLine commandLine = haxeTarget == HaxeTarget.NEKO ? getCommandForNeko(sdkData, settings) : getCommandForSwf(url);

    return new OSProcessHandler(commandLine.createProcess(), commandLine.getCommandLineString());
  }

  private GeneralCommandLine getCommandForNeko(@Nullable HaxeSdkData sdkData, HaxeModuleSettings settings) throws ExecutionException {
    if (sdkData == null || sdkData.getNekoBinPath() == null || sdkData.getNekoBinPath().isEmpty()) {
      throw new ExecutionException(HaxeBundle.message("haxe.run.bad.neko.bin.path"));
    }

    GeneralCommandLine commandLine = new GeneralCommandLine();

    commandLine.setExePath(sdkData.getNekoBinPath());
    commandLine.addParameter(FileUtil.getNameWithoutExtension(settings.getOutputFileName()));

    final VirtualFile outputDirectory = CompilerPaths.getModuleOutputDirectory(module, false);
    if (outputDirectory != null) {
      commandLine.setWorkDirectory(outputDirectory.getPath());
    }

    final TextConsoleBuilder consoleBuilder = TextConsoleBuilderFactory.getInstance().createBuilder(module.getProject());
    setConsoleBuilder(consoleBuilder);
    return commandLine;
  }

  private static GeneralCommandLine getCommandForSwf(String url) throws ExecutionException {
    final String[] command = getDefaultBrowserCommand();
    if (command == null) {
      throw new ExecutionException("Cannot launch browser");
    }
    return launchBrowserByCommand(url, command);
  }

  //todo refactor
  //quick hack

  @Nullable
  @NonNls
  private static String[] getDefaultBrowserCommand() {
    if (SystemInfo.isWindows9x) {
      return new String[]{"command.com", "/c", "start"};
    }
    else if (SystemInfo.isWindows) {
      return new String[]{"cmd.exe", "/c", "start"};
    }
    else if (SystemInfo.isMac) {
      return new String[]{ExecUtil.getOpenCommandPath()};
    }
    else if (SystemInfo.isUnix && SystemInfo.hasXdgOpen) {
      return new String[]{"xdg-open"};
    }

    return null;
  }

  private static GeneralCommandLine launchBrowserByCommand(final String url, @NotNull final String[] command) throws ExecutionException {
    URL curl;
    try {
      curl = BrowserUtil.getURL(url);
    }
    catch (MalformedURLException ignored) {
      curl = null;
    }
    if (curl == null) {
      throw new ExecutionException(IdeBundle.message("error.malformed.url", url));
    }

    final GeneralCommandLine commandLine = new GeneralCommandLine(command);
    commandLine.addParameter(BrowserUtil.escapeUrl(curl.toString()));
    return commandLine;
  }
}
