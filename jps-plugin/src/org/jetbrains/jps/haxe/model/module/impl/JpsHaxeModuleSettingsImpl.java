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
package org.jetbrains.jps.haxe.model.module.impl;

import com.intellij.plugins.haxe.config.HaxeTarget;
import com.intellij.plugins.haxe.config.NMETarget;
import com.intellij.plugins.haxe.config.OpenFLTarget;
import com.intellij.plugins.haxe.module.HaxeModuleSettingsBase;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jps.haxe.model.module.JpsHaxeModuleSettings;
import org.jetbrains.jps.model.ex.JpsElementBase;

/**
 * @author: Fedor.Korotkov
 */
public class JpsHaxeModuleSettingsImpl extends JpsElementBase<JpsHaxeModuleSettingsImpl> implements JpsHaxeModuleSettings {
  private HaxeModuleSettingsBase mySettingsBase;

  public JpsHaxeModuleSettingsImpl(HaxeModuleSettingsBase base) {
    mySettingsBase = base;
  }

  @Override
  public HaxeModuleSettingsBase getProperties() {
    return mySettingsBase;
  }

  public String getMainClass() {
    return mySettingsBase.getMainClass();
  }

  public void setMainClass(String mainClass) {
    mySettingsBase.setMainClass(mainClass);
  }

  @Override
  public void setNmeTarget(NMETarget nmeTarget) {
    mySettingsBase.setNmeTarget(nmeTarget);
  }

  @Override
  public void setOpenFLTarget(OpenFLTarget openFLTarget) {
    mySettingsBase.setOpenFLTarget(openFLTarget);
  }


  @Override
  public String getArguments() {
    return mySettingsBase.getArguments();
  }

  @Override
  public void setArguments(String arguments) {
    mySettingsBase.setArguments(arguments);
  }

  @Override
  public String getNmeFlags() {
    return mySettingsBase.getNmeFlags();
  }

  @Override
  public void setNmeFlags(String flags) {
    mySettingsBase.setNmeFlags(flags);
  }

  @Override
  public String getOpenFLFlags() {
    return mySettingsBase.getOpenFLFlags();
  }

  @Override
  public void setOpenFLFlags(String flags) {
    mySettingsBase.setOpenFLFlags(flags);
  }


  @Override
  public HaxeTarget getHaxeTarget() {
    return mySettingsBase.getHaxeTarget();
  }

  @Override
  public NMETarget getNmeTarget() {
    return mySettingsBase.getNmeTarget();
  }

  @Override
  public OpenFLTarget getOpenFLTarget() {
    return mySettingsBase.getOpenFLTarget();
  }


  @Override
  public void setHaxeTarget(HaxeTarget haxeTarget) {
    mySettingsBase.setHaxeTarget(haxeTarget);
  }

  @Override
  public boolean isExcludeFromCompilation() {
    return mySettingsBase.isExcludeFromCompilation();
  }

  @Override
  public void setExcludeFromCompilation(boolean excludeFromCompilation) {
    mySettingsBase.setExcludeFromCompilation(excludeFromCompilation);
  }

  @Override
  public String getOutputFileName() {
    return mySettingsBase.getOutputFileName();
  }

  @Override
  public void setOutputFileName(String outputFileName) {
    mySettingsBase.setOutputFileName(outputFileName);
  }

  @Override
  public String getHxmlPath() {
    return mySettingsBase.getHxmlPath();
  }

  @Override
  public String getNmmlPath() {
    return mySettingsBase.getNmmlPath();
  }

  @Override
  public String getOpenFLXmlPath() {
    return mySettingsBase.getOpenFLXmlPath();
  }

  @Override
  public void setHxmlPath(String hxmlPath) {
    mySettingsBase.setHxmlPath(hxmlPath);
  }

  @Override
  public boolean isUseHxmlToBuild() {
    return mySettingsBase.isUseHxmlToBuild();
  }

  @Override
  public boolean isUseNmmlToBuild() {
    return mySettingsBase.isUseNmmlToBuild();
  }

  @Override
  public boolean isUseOpenFLToBuild() {
    return mySettingsBase.isUseOpenFLToBuild();
  }

  @Override
  public boolean isUseUserPropertiesToBuild() {
    return mySettingsBase.isUseUserPropertiesToBuild();
  }

  @Override
  public void setNmmlPath(String nmmlPath) {
    mySettingsBase.setNmmlPath(nmmlPath);
  }

  @Override
  public int getBuildConfig() {
    return mySettingsBase.getBuildConfig();
  }

  @Override
  public void setBuildConfig(int buildConfig) {
    mySettingsBase.setBuildConfig(buildConfig);
  }

  @NotNull
  @Override
  public JpsHaxeModuleSettingsImpl createCopy() {
    return new JpsHaxeModuleSettingsImpl(mySettingsBase);
  }

  @Override
  public void applyChanges(@NotNull JpsHaxeModuleSettingsImpl modified) {
    mySettingsBase = modified.mySettingsBase;
  }
}
