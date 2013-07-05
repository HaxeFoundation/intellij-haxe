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
package com.intellij.plugins.haxe.module.impl;

import com.intellij.plugins.haxe.config.HaxeTarget;
import com.intellij.plugins.haxe.config.NMETarget;
import com.intellij.plugins.haxe.config.OpenFLTarget;
import com.intellij.plugins.haxe.module.HaxeModuleSettingsBase;

/**
 * @author: Fedor.Korotkov
 */
public class HaxeModuleSettingsBaseImpl implements HaxeModuleSettingsBase {
  public static final int USE_PROPERTIES = 0;
  public static final int USE_HXML = 1;
  public static final int USE_NMML = 2;
  public static final int USE_OPENFL = 3;

  protected String mainClass = "";
  protected String outputFileName = "";
  protected String arguments = "";
  protected String nmeFlags = "";
  protected String openFlFlags = "";
  protected boolean excludeFromCompilation = false;
  protected HaxeTarget haxeTarget = HaxeTarget.NEKO;
  protected NMETarget nmeTarget = NMETarget.FLASH;
  protected OpenFLTarget openFLTarget = OpenFLTarget.FLASH;
  protected String hxmlPath = "";
  protected String nmmlPath = "";
  protected String openFlPath = "";
  protected int buildConfig = 0;


  public HaxeModuleSettingsBaseImpl() {
  }

  public HaxeModuleSettingsBaseImpl(String mainClass,
                                    String outputFileName,
                                    String arguments,
                                    String nmeFlags,
                                    boolean excludeFromCompilation,
                                    HaxeTarget haxeTarget,
                                    NMETarget nmeTarget,
                                    OpenFLTarget openFLTarget,
                                    String hxmlPath,
                                    String nmmlPath,
                                    String openFlPath,
                                    int buildConfig) {
    this.mainClass = mainClass;
    this.outputFileName = outputFileName;
    this.arguments = arguments;
    this.nmeFlags = nmeFlags;
    this.excludeFromCompilation = excludeFromCompilation;
    this.haxeTarget = haxeTarget;
    this.nmeTarget = nmeTarget;
    this.openFLTarget = openFLTarget;
    this.hxmlPath = hxmlPath;
    this.nmmlPath = nmmlPath;
    this.openFlPath = openFlPath;
    this.buildConfig = buildConfig;
  }

  public void setNmeTarget(NMETarget nmeTarget) {
    this.nmeTarget = nmeTarget;
  }

  public void setOpenFLTarget(OpenFLTarget openFlTarget) {
    this.openFLTarget = openFlTarget;
  }

  public int getBuildConfig() {
    return buildConfig;
  }

  public String getMainClass() {
    return mainClass;
  }

  public void setMainClass(String mainClass) {
    this.mainClass = mainClass;
  }

  public String getArguments() {
    return arguments;
  }

  public void setArguments(String arguments) {
    this.arguments = arguments;
  }

  public String getNmeFlags() {
    return nmeFlags;
  }

  public void setNmeFlags(String flags) {
    this.nmeFlags = flags;
  }

  public void setOpenFLFlags(String flags) {
    this.openFlFlags = flags;
  }

  public String getOpenFLFlags() {
    return openFlFlags;
  }

  public void setOpenFlTarget(OpenFLTarget target) {
    this.openFLTarget = target;
  }

  public OpenFLTarget getOpenFLTarget() {
    return openFLTarget;
  }


  public HaxeTarget getHaxeTarget() {
    return haxeTarget;
  }

  public NMETarget getNmeTarget() {
    return nmeTarget;
  }

  public void setHaxeTarget(HaxeTarget haxeTarget) {
    this.haxeTarget = haxeTarget;
  }

  public boolean isExcludeFromCompilation() {
    return excludeFromCompilation;
  }

  public void setExcludeFromCompilation(boolean excludeFromCompilation) {
    this.excludeFromCompilation = excludeFromCompilation;
  }

  public String getOutputFileName() {
    return outputFileName;
  }

  public void setOutputFileName(String outputFileName) {
    this.outputFileName = outputFileName;
  }

  public String getHxmlPath() {
    return hxmlPath;
  }

  public String getNmmlPath() {
    return nmmlPath;
  }


  public void setHxmlPath(String hxmlPath) {
    this.hxmlPath = hxmlPath;
  }

  public boolean isUseHxmlToBuild() {
    return buildConfig == USE_HXML;
  }

  public boolean isUseNmmlToBuild() {
    return buildConfig == USE_NMML;
  }

  public boolean isUseOpenFlToBuild() {
    return buildConfig == USE_OPENFL;
  }

  public boolean isUseUserPropertiesToBuild() {
    return buildConfig == USE_PROPERTIES;
  }

  public void setNmmlPath(String nmmlPath) {
    this.nmmlPath = nmmlPath;
  }

  public void setBuildConfig(int buildConfig) {
    this.buildConfig = buildConfig;
  }
}
