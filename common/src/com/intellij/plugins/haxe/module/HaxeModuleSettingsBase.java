package com.intellij.plugins.haxe.module;

import com.intellij.plugins.haxe.config.HaxeTarget;
import com.intellij.plugins.haxe.config.NMETarget;

/**
 * @author: Fedor.Korotkov
 */
public interface HaxeModuleSettingsBase {
  void setNmeTarget(NMETarget nmeTarget);

  String getMainClass();

  void setMainClass(String mainClass);

  String getArguments();

  void setArguments(String arguments);

  String getNmeFlags();

  void setNmeFlags(String flags);

  HaxeTarget getHaxeTarget();

  NMETarget getNmeTarget();

  void setHaxeTarget(HaxeTarget haxeTarget);

  boolean isExcludeFromCompilation();

  void setExcludeFromCompilation(boolean excludeFromCompilation);

  String getOutputFileName();

  void setOutputFileName(String outputFileName);

  String getHxmlPath();

  String getNmmlPath();

  void setHxmlPath(String hxmlPath);

  boolean isUseHxmlToBuild();

  boolean isUseNmmlToBuild();

  boolean isUseUserPropertiesToBuild();

  void setNmmlPath(String nmmlPath);

  int getBuildConfig();

  void setBuildConfig(int buildConfig);
}
