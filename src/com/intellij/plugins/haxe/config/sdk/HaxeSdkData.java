package com.intellij.plugins.haxe.config.sdk;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.projectRoots.SdkAdditionalData;
import com.intellij.openapi.projectRoots.SdkModel;
import com.intellij.util.xmlb.XmlSerializerUtil;
import org.jetbrains.annotations.Nullable;

public class HaxeSdkData implements SdkAdditionalData, PersistentStateComponent<HaxeSdkData> {
  private String homePath = "";
  private String version = "";

  private String nekoBinPath = "";

  private String haxelibPath = "";

  public HaxeSdkData() {
  }

  public HaxeSdkData(String homePath, String version) {
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

  public void setNekoBinPath( String nekoBinPath) {
    this.nekoBinPath = nekoBinPath;
  }

  public String getHaxelibPath() {
    return haxelibPath;
  }

  public void setHaxelibPath(String haxelibPath) {
    this.haxelibPath = haxelibPath;
  }

  @SuppressWarnings({"CloneDoesntCallSuperClone"})
  @Override
  public Object clone() throws CloneNotSupportedException {
    return super.clone();
  }

  public HaxeSdkData getState() {
    return this;
  }

  public void loadState(HaxeSdkData state) {
    XmlSerializerUtil.copyBean(state, this);
  }
}