package com.intellij.plugins.haxe.config.sdk;

/**
 * @author: Fedor.Korotkov
 */
public interface HaxeSdkAdditionalDataBase {
  String getHomePath();

  String getVersion();

  String getNekoBinPath();

  void setNekoBinPath(String nekoBinPath);

  String getHaxelibPath();

  void setHaxelibPath(String haxelibPath);
}
