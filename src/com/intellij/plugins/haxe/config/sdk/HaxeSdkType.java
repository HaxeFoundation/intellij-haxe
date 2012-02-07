package com.intellij.plugins.haxe.config.sdk;

import com.intellij.openapi.projectRoots.*;
import com.intellij.plugins.haxe.HaxeBundle;
import com.intellij.util.xmlb.XmlSerializer;
import org.jdom.Element;

public class HaxeSdkType extends SdkType {
  private HaxeSdkData sdkData;

  public HaxeSdkType() {
    super(HaxeBundle.message("haxe.sdk.name"));
  }

  public static HaxeSdkType getInstance() {
    return SdkType.findInstance(HaxeSdkType.class);
  }

  public HaxeSdkData getSdkData() {
    return sdkData;
  }

  @Override
  public String getPresentableName() {
    return HaxeBundle.message("haxe.sdk.name.presentable");
  }

  @Override
  public String suggestSdkName(String currentSdkName, String sdkHome) {
    return "haXe SDK";
  }

  @Override
  public String getVersionString(String sdkHome) {
    return getSdkData() != null ? getSdkData().getVersion() : super.getVersionString(sdkHome);
  }

  @Override
  public String suggestHomePath() {
    return System.getenv("HAXEPATH");
  }

  @Override
  public boolean isValidSdkHome(String path) {
    sdkData = HaxeSdkUtil.testHaxeSdk(path);
    return sdkData != null;
  }

  @Override
  public AdditionalDataConfigurable createAdditionalDataConfigurable(SdkModel sdkModel, SdkModificator sdkModificator) {
    return new NekoConfigurable();
  }

  @Override
  public SdkAdditionalData loadAdditionalData(Element additional) {
    return XmlSerializer.deserialize(additional, HaxeSdkData.class);
  }

  @Override
  public void saveAdditionalData(SdkAdditionalData additionalData, Element additional) {
    if (additionalData instanceof HaxeSdkData) {
      XmlSerializer.serializeInto(additionalData, additional);
    }
  }
}
