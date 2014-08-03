/*
 * Copyright 2000-2013 JetBrains s.r.o.
 * Copyright 2014-2014 AS3Boyan
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
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.projectRoots.AdditionalDataConfigurable;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.projectRoots.SdkModificator;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.plugins.haxe.config.sdk.ui.HaxeAdditionalConfigurablePanel;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

/**
 * @author: Fedor.Korotkov
 */
public class HaxeAdditionalConfigurable implements AdditionalDataConfigurable {
  private final HaxeAdditionalConfigurablePanel myHaxeAdditionalConfigurablePanel;
  private Sdk mySdk;

  public HaxeAdditionalConfigurable() {
    myHaxeAdditionalConfigurablePanel = new HaxeAdditionalConfigurablePanel();
  }

  @Override
  public void setSdk(Sdk sdk) {
    mySdk = sdk;
  }

  @Override
  public JComponent createComponent() {
    return myHaxeAdditionalConfigurablePanel.getPanel();
  }

  @Override
  public boolean isModified() {
    final HaxeSdkData haxeSdkData = getHaxeSdkData();
    return haxeSdkData == null ||
           !myHaxeAdditionalConfigurablePanel.getNekoBinPath().equals(haxeSdkData.getNekoBinPath()) ||
           !myHaxeAdditionalConfigurablePanel.getHaxelibPath().equals(haxeSdkData.getHaxelibPath());
  }

  @Override
  public void apply() throws ConfigurationException {
    final HaxeSdkData haxeSdkData = getHaxeSdkData();
    if (haxeSdkData == null) {
      return;
    }

    final HaxeSdkData newData = new HaxeSdkData(haxeSdkData.getHomePath(), haxeSdkData.getVersion());
    newData.setNekoBinPath(FileUtil.toSystemIndependentName(myHaxeAdditionalConfigurablePanel.getNekoBinPath()));
    newData.setHaxelibPath(FileUtil.toSystemIndependentName(myHaxeAdditionalConfigurablePanel.getHaxelibPath()));

    final SdkModificator modificator = mySdk.getSdkModificator();
    modificator.setSdkAdditionalData(newData);
    ApplicationManager.getApplication().runWriteAction(new Runnable() {
      public void run() {
        modificator.commitChanges();
      }
    });
  }

  @Nullable
  private HaxeSdkData getHaxeSdkData() {
    return mySdk.getSdkAdditionalData() instanceof HaxeSdkData ? (HaxeSdkData)mySdk.getSdkAdditionalData() : null;
  }

  @Override
  public void reset() {
    final HaxeSdkData haxeSdkData = getHaxeSdkData();
    if (haxeSdkData != null) {
      final String nekoBinPath = haxeSdkData.getNekoBinPath();
      myHaxeAdditionalConfigurablePanel.setNekoBinPath(FileUtil.toSystemDependentName(nekoBinPath == null ? "" : nekoBinPath));
      final String haxelibPath = haxeSdkData.getHaxelibPath();
      myHaxeAdditionalConfigurablePanel.setHaxelibPath(FileUtil.toSystemDependentName(haxelibPath == null ? "" : haxelibPath));
    }
    myHaxeAdditionalConfigurablePanel.getPanel().repaint();
  }

  @Override
  public void disposeUIResources() {
  }
}
