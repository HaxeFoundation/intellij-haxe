package com.intellij.plugins.haxe.config.sdk;

import com.intellij.openapi.projectRoots.*;
import com.intellij.plugins.haxe.HaxeBundle;
import com.intellij.plugins.haxe.HaxeIcons;
import com.intellij.util.xmlb.XmlSerializer;
import org.jdom.Element;

import javax.swing.*;

public class HaxeSdkType extends SdkType {
  private HaxeSdkData sdkData;

  public HaxeSdkType() {
    super(HaxeBundle.message("haxe.sdk.name"));
  }

  @Override
  public Icon getIcon() {
    return HaxeIcons.HAXE_ICON_16x16;
  }

  @Override
  public Icon getIconForAddAction() {
    return HaxeIcons.HAXE_ICON_16x16;
  }

  public static HaxeSdkType getInstance() {
    return SdkType.findInstance(HaxeSdkType.class);
  }

  protected HaxeSdkData getSdkData() {
    return sdkData;
  }

  @Override
  public String getPresentableName() {
    return HaxeBundle.message("haxe.sdk.name.presentable");
  }

  @Override
  public String suggestSdkName(String currentSdkName, String sdkHome) {
    return HaxeBundle.message("haxe.sdk.name.suggest", getVersionString(sdkHome));
  }

  @Override
  public String getVersionString(String sdkHome) {
    return getSdkData() != null ? getSdkData().getVersion() : super.getVersionString(sdkHome);
  }

  @Override
  public String suggestHomePath() {
    return HaxeSdkUtil.suggestHomePath();
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
