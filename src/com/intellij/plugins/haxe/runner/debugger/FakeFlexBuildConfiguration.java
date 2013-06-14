/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
package com.intellij.plugins.haxe.runner.debugger;

import com.intellij.flex.model.bc.BuildConfigurationNature;
import com.intellij.flex.model.bc.OutputType;
import com.intellij.flex.model.bc.TargetPlatform;
import com.intellij.lang.javascript.flex.projectStructure.model.*;
import com.intellij.openapi.projectRoots.Sdk;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.util.Collection;

/**
 * @author: Fedor.Korotkov
 */
public class FakeFlexBuildConfiguration implements FlexBuildConfiguration {
  private final Sdk sdk;
  private final String url;

  public FakeFlexBuildConfiguration(Sdk sdk, String url) {
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
  public String getActualOutputFilePath() {
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
  public Collection<RLMInfo> getRLMs() {
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
  public boolean isEqual(FlexBuildConfiguration other) {
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

  @Override
  public String getStatisticsEntry() {
    return null;
  }
}
