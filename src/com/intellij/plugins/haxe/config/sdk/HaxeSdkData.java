package com.intellij.plugins.haxe.config.sdk;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.projectRoots.SdkAdditionalData;
import com.intellij.plugins.haxe.config.sdk.impl.HaxeSdkAdditionalDataBaseImpl;
import com.intellij.util.xmlb.XmlSerializerUtil;

public class HaxeSdkData extends HaxeSdkAdditionalDataBaseImpl implements SdkAdditionalData, PersistentStateComponent<HaxeSdkData> {
  public HaxeSdkData() {
    super();
  }

  public HaxeSdkData(String homePath, String version) {
    super(homePath, version);
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