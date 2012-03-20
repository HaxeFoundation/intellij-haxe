package com.intellij.plugins.haxe.runner.debugger;

import com.intellij.lang.javascript.flex.projectStructure.model.*;
import com.intellij.lang.javascript.flex.projectStructure.options.BuildConfigurationNature;
import com.intellij.openapi.projectRoots.Sdk;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.util.Collection;

/**
 * @author: Fedor.Korotkov
 */
public class FakeFlexIdeBuildConfiguration implements FlexIdeBuildConfiguration {
  private final Sdk sdk;
  private final String url;

  public FakeFlexIdeBuildConfiguration(Sdk sdk, String url) {
    this.sdk = sdk;
    this.url = url;
  }

  @Override
  public Sdk getSdk() {
    return sdk;
  }

  @NotNull
  @Override
  public TargetPlatform getTargetPlatform() {
    return TargetPlatform.Web;
  }

  @Override
  public boolean isUseHtmlWrapper() {
    return false;
  }

  @Override
  public String getOutputFilePath(boolean respectAdditionalConfigFile) {
    return url;
  }

  @NotNull
  @Override
  public String getName() {
    return null;
  }

  @Override
  public boolean isPureAs() {
    return false;
  }

  @NotNull
  @Override
  public OutputType getOutputType() {
    return null;
  }

  @NotNull
  @Override
  public String getOptimizeFor() {
    return null;
  }

  @NotNull
  @Override
  public String getMainClass() {
    return null;
  }

  @NotNull
  @Override
  public String getOutputFileName() {
    return null;
  }

  @NotNull
  @Override
  public String getOutputFolder() {
    return null;
  }

  @NotNull
  @Override
  public String getWrapperTemplatePath() {
    return null;
  }

  @NotNull
  @Override
  public Collection<String> getCssFilesToCompile() {
    return null;
  }

  @Override
  public boolean isSkipCompile() {
    return false;
  }

  @NotNull
  @Override
  public Dependencies getDependencies() {
    return null;
  }

  @NotNull
  @Override
  public CompilerOptions getCompilerOptions() {
    return null;
  }

  @NotNull
  @Override
  public AirDesktopPackagingOptions getAirDesktopPackagingOptions() {
    return null;
  }

  @NotNull
  @Override
  public AndroidPackagingOptions getAndroidPackagingOptions() {
    return null;
  }

  @NotNull
  @Override
  public IosPackagingOptions getIosPackagingOptions() {
    return null;
  }

  @Override
  public Icon getIcon() {
    return null;
  }

  @Override
  public BuildConfigurationNature getNature() {
    return null;
  }

  @Override
  public boolean isTempBCForCompilation() {
    return false;
  }

  @Override
  public boolean isEqual(FlexIdeBuildConfiguration other) {
    return false;
  }

  @Override
  public String getShortText() {
    return null;
  }

  @Override
  public String getDescription() {
    return null;
  }
}
