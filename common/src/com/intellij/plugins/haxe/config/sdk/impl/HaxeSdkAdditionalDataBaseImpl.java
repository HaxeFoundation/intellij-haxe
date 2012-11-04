package com.intellij.plugins.haxe.config.sdk.impl;

import com.intellij.plugins.haxe.config.sdk.HaxeSdkAdditionalDataBase;

/**
 * @author: Fedor.Korotkov
 */
public class HaxeSdkAdditionalDataBaseImpl implements HaxeSdkAdditionalDataBase {
  private String homePath = "";
  private String version = "";

  private String nekoBinPath = "";

  private String haxelibPath = "";

  public HaxeSdkAdditionalDataBaseImpl() {
  }

  public HaxeSdkAdditionalDataBaseImpl(String homePath, String version) {
    this.homePath = homePath;
    this.version = version;
  }

  public String getHomePath() {
    return homePath;
  }

  public String getVersion() {
    return version;
  }

  public String getNekoBinPath() {
    return nekoBinPath;
  }

  public void setNekoBinPath(String nekoBinPath) {
    this.nekoBinPath = nekoBinPath;
  }

  public String getHaxelibPath() {
    return haxelibPath;
  }

  public void setHaxelibPath(String haxelibPath) {
    this.haxelibPath = haxelibPath;
  }
}
