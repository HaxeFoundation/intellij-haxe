package com.intellij.plugins.haxe.runner.debugger.flash;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.Executor;
import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.execution.configurations.RunProfileState;
import com.intellij.execution.configurations.RuntimeConfigurationError;
import com.intellij.execution.configurations.RuntimeConfigurationException;
import com.intellij.execution.configurations.RuntimeConfigurationWarning;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.InvalidDataException;
import com.intellij.openapi.util.JDOMExternalizerUtil;
import com.intellij.openapi.util.WriteExternalException;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.plugins.haxe.HaxeDebuggerBundle;
import com.intellij.plugins.haxe.runner.debugger.dap.ide.DapCommandLineRunningState;
import com.intellij.plugins.haxe.runner.debugger.dap.ide.DapRunConfigurationBase;
import com.intellij.plugins.haxe.v2.buildtools.HaxeToolPathResolver;
import java.nio.file.Files;
import java.nio.file.Path;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A Flash run/debug configuration: the compiled {@code .swf} plus the Flex SDK
 * whose debugger drives the session (the Flash/Flex plugin must be installed
 * for debugging). Plain Run launches the swf with the configured player
 * executable.
 */
public class FlashRunConfiguration extends DapRunConfigurationBase {
  private static final String SWF_FILE = "swfFile";
  private static final String FLEX_SDK = "flexSdk";
  private static final String PLAYER = "player";

  private String swfFilePath = "";
  private String flexSdkName = "";
  private String flashPlayerPath = "";

  public FlashRunConfiguration(String name, Project project, ConfigurationFactory factory) {
    super(name, project, factory);
  }

  // --- settings ---

  public String getSwfFilePath() {
    return swfFilePath;
  }

  public void setSwfFilePath(@Nullable String path) {
    swfFilePath = orEmpty(path);
  }

  public String getFlexSdkName() {
    return flexSdkName;
  }

  public void setFlexSdkName(@Nullable String name) {
    flexSdkName = orEmpty(name);
  }

  public String getFlashPlayerPath() {
    return flashPlayerPath;
  }

  public void setFlashPlayerPath(@Nullable String path) {
    flashPlayerPath = orEmpty(path);
  }

  /** The Flex SDK the debug session uses: this configuration's own selection, else the Haxe SDK's runtimes entry. */
  @NotNull
  public String effectiveFlexSdkName() {
    return HaxeToolPathResolver.flexSdkNameOrEmpty(getProject(), flexSdkName);
  }

  /** The standalone player launching the swf: this configuration's own path, else the Build Tools/SDK runtimes chain. */
  @NotNull
  public String effectiveFlashPlayerPath() {
    if (!flashPlayerPath.isBlank()) {
      return flashPlayerPath;
    }
    return StringUtil.notNullize(HaxeToolPathResolver.resolveFlashPlayerExecutable(getProject(), null));
  }

  @Override
  public @NotNull SettingsEditor<? extends RunConfiguration> getConfigurationEditor() {
    return new FlashRunConfigurationEditor(getProject());
  }

  @Override
  public void checkConfiguration() throws RuntimeConfigurationException {
    if (getConfigurationModule().getModule() == null) {
      throw new RuntimeConfigurationError(HaxeDebuggerBundle.message("flash.runner.no.module"));
    }
    if (swfFilePath.isBlank()) {
      throw new RuntimeConfigurationError(HaxeDebuggerBundle.message("flash.runner.no.swf"));
    }
    Path swf = resolveSwfOrNull();
    if (swf != null && !Files.isRegularFile(swf)) {
      throw new RuntimeConfigurationWarning(HaxeDebuggerBundle.message("flash.runner.swf.missing", swfFilePath));
    }
    if (effectiveFlexSdkName().isBlank()) {
      throw new RuntimeConfigurationWarning(HaxeDebuggerBundle.message("flash.runner.no.flex.sdk"));
    }
  }

  // Plain Run: <player> <swf>. Resolution stays inside the supplier - getState
  // runs before before-launch tasks, so the swf a build step produces may not
  // exist yet.
  @Override
  public RunProfileState getState(@NotNull Executor executor, @NotNull ExecutionEnvironment env) throws ExecutionException {
    requireModule();
    return new DapCommandLineRunningState(env, getProject(), this::createRunCommandLine);
  }

  private GeneralCommandLine createRunCommandLine() throws ExecutionException {
    String player = effectiveFlashPlayerPath();
    if (player.isBlank()) {
      throw new ExecutionException(HaxeDebuggerBundle.message("flash.runner.no.player"));
    }
    Path swf = resolveSwf();
    return new GeneralCommandLine()
      .withExePath(player)
      .withParameters(swf.toString())
      .withWorkDirectory(swf.getParent() != null ? swf.getParent().toString() : null);
  }

  // --- resolution ---

  /** The .swf to launch/debug: the setting, project-relative when not absolute. */
  @NotNull
  public Path resolveSwf() throws ExecutionException {
    Path swf = resolveSwfOrNull();
    if (swf == null) {
      throw new ExecutionException(HaxeDebuggerBundle.message("flash.runner.no.swf"));
    }
    if (!Files.isRegularFile(swf)) {
      throw new ExecutionException(HaxeDebuggerBundle.message("flash.runner.swf.missing", swf.toString()));
    }
    return swf;
  }

  @Nullable
  private Path resolveSwfOrNull() {
    return swfFilePath.isBlank() ? null : resolveAgainstProject(swfFilePath);
  }

  // --- persistence ---

  @Override
  public void readExternal(@NotNull Element element) throws InvalidDataException {
    super.readExternal(element);
    readModule(element);
    swfFilePath = orEmpty(JDOMExternalizerUtil.readField(element, SWF_FILE));
    flexSdkName = orEmpty(JDOMExternalizerUtil.readField(element, FLEX_SDK));
    flashPlayerPath = orEmpty(JDOMExternalizerUtil.readField(element, PLAYER));
  }

  @Override
  public void writeExternal(@NotNull Element element) throws WriteExternalException {
    super.writeExternal(element); // also serializes the module
    JDOMExternalizerUtil.writeField(element, SWF_FILE, swfFilePath);
    JDOMExternalizerUtil.writeField(element, FLEX_SDK, flexSdkName);
    JDOMExternalizerUtil.writeField(element, PLAYER, flashPlayerPath);
  }
}
