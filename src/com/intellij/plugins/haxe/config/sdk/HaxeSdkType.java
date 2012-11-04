package com.intellij.plugins.haxe.config.sdk;

import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.fileChooser.PathChooserDialog;
import com.intellij.openapi.projectRoots.*;
import com.intellij.openapi.roots.JavadocOrderRootType;
import com.intellij.openapi.roots.OrderRootType;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.plugins.haxe.HaxeBundle;
import com.intellij.plugins.haxe.HaxeCommonBundle;
import com.intellij.util.xmlb.XmlSerializer;
import org.jdom.Element;

import javax.swing.*;

public class HaxeSdkType extends SdkType {
  public HaxeSdkType() {
    super(HaxeCommonBundle.message("haxe.sdk.name"));
  }

  @Override
  public Icon getIcon() {
    return icons.HaxeIcons.HaXe_16;
  }

  @Override
  public Icon getIconForAddAction() {
    return icons.HaxeIcons.HaXe_16;
  }

  public static HaxeSdkType getInstance() {
    return SdkType.findInstance(HaxeSdkType.class);
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
    final HaxeSdkData haxeSdkData = HaxeSdkUtil.testHaxeSdk(sdkHome);
    return haxeSdkData != null ? haxeSdkData.getVersion() : super.getVersionString(sdkHome);
  }

  @Override
  public String suggestHomePath() {
    return HaxeSdkUtil.suggestHomePath();
  }

  @Override
  public boolean isValidSdkHome(String path) {
    return HaxeSdkUtil.testHaxeSdk(path) != null;
  }

  @Override
  public AdditionalDataConfigurable createAdditionalDataConfigurable(SdkModel sdkModel, SdkModificator sdkModificator) {
    return new HaxeAdditionalConfigurable();
  }

  @Override
  public boolean isRootTypeApplicable(OrderRootType type) {
    return type == OrderRootType.SOURCES || type == OrderRootType.CLASSES || type == JavadocOrderRootType.getInstance();
  }

  @Override
  public void setupSdkPaths(Sdk sdk) {
    final SdkModificator modificator = sdk.getSdkModificator();

    SdkAdditionalData data = sdk.getSdkAdditionalData();
    if (data == null) {
      data = HaxeSdkUtil.testHaxeSdk(sdk.getHomePath());
      modificator.setSdkAdditionalData(data);
    }

    HaxeSdkUtil.setupSdkPaths(sdk.getHomeDirectory(), modificator);

    modificator.commitChanges();
    super.setupSdkPaths(sdk);
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

  @Override
  public FileChooserDescriptor getHomeChooserDescriptor() {
    final FileChooserDescriptor result = super.getHomeChooserDescriptor();
    if (SystemInfo.isMac) {
      result.putUserData(PathChooserDialog.NATIVE_MAC_CHOOSER_SHOW_HIDDEN_FILES, Boolean.TRUE);
    }
    return result;
  }
}
