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
package com.intellij.plugins.haxe.config.sdk;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.projectRoots.*;
import com.intellij.openapi.projectRoots.impl.ProjectJdkImpl;
import com.intellij.openapi.roots.JavadocOrderRootType;
import com.intellij.openapi.roots.OrderRootType;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.plugins.haxe.HaxeBundle;
import com.intellij.plugins.haxe.HaxeCommonBundle;
import com.intellij.util.xmlb.XmlSerializer;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

public class HaxeSdkType extends SdkType {
  public HaxeSdkType() {
    super(HaxeCommonBundle.message("haxe.sdk.name"));
  }

  @Override
  public Icon getIcon() {
    return icons.HaxeIcons.HAXE_LOGO;
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
    if (sdkHome == null) return "(Haxe version not found)";
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

  @NotNull
  @Override
  public FileChooserDescriptor getHomeChooserDescriptor() {
    final FileChooserDescriptor result = super.getHomeChooserDescriptor();
    if (SystemInfo.isMac) {
      result.withShowHiddenFiles(true); // TODO: Test on a mac: converted for v15.
    }
    return result;
  }

  /**
   * Try to automatically setup Haxe SDK if it's not configured yet.
   */
  public void ensureSdk() {
    ProjectJdkTable sdkTable = ProjectJdkTable.getInstance();
    if(sdkTable.getSdksOfType(this).isEmpty()) {
      String homePath = HaxeSdkUtil.suggestHomePath();
      if(homePath != null) {
        Sdk sdk = new ProjectJdkImpl(getName(), this, homePath, getVersionString(homePath));
        setupSdkPaths(sdk);
        ApplicationManager.getApplication().runWriteAction(() -> sdkTable.addJdk(sdk));
      }
    }
  }
}
