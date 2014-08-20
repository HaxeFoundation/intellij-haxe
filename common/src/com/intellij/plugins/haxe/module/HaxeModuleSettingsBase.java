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
package com.intellij.plugins.haxe.module;

import com.intellij.plugins.haxe.config.HaxeTarget;
import com.intellij.plugins.haxe.config.NMETarget;
import com.intellij.plugins.haxe.config.OpenFLTarget;

/**
 * @author: Fedor.Korotkov
 */
public interface HaxeModuleSettingsBase {
  void setNmeTarget(NMETarget nmeTarget);

  void setOpenFLTarget(OpenFLTarget openFLTarget);

  String getMainClass();

  void setMainClass(String mainClass);

  String getArguments();

  void setArguments(String arguments);

  String getNmeFlags();

  String getOpenFLFlags();

  void setNmeFlags(String flags);

  void setOpenFLFlags(String flags);

  HaxeTarget getHaxeTarget();

  NMETarget getNmeTarget();

  OpenFLTarget getOpenFLTarget();

  void setHaxeTarget(HaxeTarget haxeTarget);

  boolean isExcludeFromCompilation();

  void setExcludeFromCompilation(boolean excludeFromCompilation);

  String getOutputFileName();

  void setOutputFileName(String outputFileName);

  String getOutputFolder();

  void setOutputFolder(String outputFolder);

  String getHxmlPath();

  String getNmmlPath();

  String getOpenFLXmlPath();

  void setHxmlPath(String hxmlPath);

  boolean isUseHxmlToBuild();

  boolean isUseNmmlToBuild();

  boolean isUseOpenFLToBuild();

  boolean isUseUserPropertiesToBuild();

  void setNmmlPath(String nmmlPath);

  int getBuildConfig();

  void setBuildConfig(int buildConfig);
}
