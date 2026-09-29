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
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.projectRoots.AdditionalDataConfigurable;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.projectRoots.SdkModificator;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.plugins.haxe.config.sdk.ui.HaxeAdditionalConfigurablePanel;
import com.intellij.plugins.haxe.util.HaxeSdkUtilBase;
import com.intellij.plugins.haxe.v2.buildtools.HaxeToolPathResolver;
import com.intellij.util.concurrency.AppExecutorUtil;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;

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
           !myHaxeAdditionalConfigurablePanel.getHlBinPath().equals(haxeSdkData.getHlBinPath()) ||
           !myHaxeAdditionalConfigurablePanel.getHaxelibPath().equals(haxeSdkData.getHaxelibPath()) ||
           !myHaxeAdditionalConfigurablePanel.getNodeBinPath().equals(haxeSdkData.getNodeBinPath()) ||
           !myHaxeAdditionalConfigurablePanel.getFlashPlayerPath().equals(haxeSdkData.getFlashPlayerPath()) ||
           !myHaxeAdditionalConfigurablePanel.getFlexSdkName().equals(haxeSdkData.getFlexSdkName()) ||
           myHaxeAdditionalConfigurablePanel.getRemoveCompletionDuplicatesFlag() ^ haxeSdkData.getRemoveCompletionDuplicatesFlag();
  }

  @Override
  public void apply() throws ConfigurationException {
    final HaxeSdkData haxeSdkData = getHaxeSdkData();
    // An SDK entry can lack HaxeSdkData (created by an old plugin version, or a
    // deferred setup write that never ran). Returning here would silently drop
    // everything typed in this panel, with no way to ever configure the SDK
    // short of deleting and recreating it — so create the data instead.
    final HaxeSdkData newData = haxeSdkData != null
                                ? new HaxeSdkData(haxeSdkData.getHomePath(), haxeSdkData.getVersion())
                                : new HaxeSdkData(mySdk.getHomePath(), mySdk.getVersionString());

    newData.setNekoBinPath(FileUtil.toSystemIndependentName(myHaxeAdditionalConfigurablePanel.getNekoBinPath()));
    newData.setHlBinPath(FileUtil.toSystemIndependentName(myHaxeAdditionalConfigurablePanel.getHlBinPath()));
    newData.setHaxelibPath(FileUtil.toSystemIndependentName(myHaxeAdditionalConfigurablePanel.getHaxelibPath()));
    newData.setNodeBinPath(FileUtil.toSystemIndependentName(myHaxeAdditionalConfigurablePanel.getNodeBinPath()));
    newData.setFlashPlayerPath(FileUtil.toSystemIndependentName(myHaxeAdditionalConfigurablePanel.getFlashPlayerPath()));
    newData.setFlexSdkName(myHaxeAdditionalConfigurablePanel.getFlexSdkName());
    newData.setRemoveCompletionDuplicatesFlag(myHaxeAdditionalConfigurablePanel.getRemoveCompletionDuplicatesFlag());

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
    updateInheritedDefaults();

    final HaxeSdkData haxeSdkData = getHaxeSdkData();
    if (haxeSdkData != null) {
      final String nekoBinPath = haxeSdkData.getNekoBinPath();
      myHaxeAdditionalConfigurablePanel.setNekoBinPath(toSystemDependentName(nekoBinPath));

      final String hlBinPath = haxeSdkData.getHlBinPath();
      myHaxeAdditionalConfigurablePanel.setHlBinPath(toSystemDependentName(hlBinPath));

      final String haxelibPath = haxeSdkData.getHaxelibPath();
      myHaxeAdditionalConfigurablePanel.setHaxelibPath(toSystemDependentName(haxelibPath));

      myHaxeAdditionalConfigurablePanel.setNodeBinPath(toSystemDependentName(haxeSdkData.getNodeBinPath()));
      myHaxeAdditionalConfigurablePanel.setFlashPlayerPath(toSystemDependentName(haxeSdkData.getFlashPlayerPath()));
      myHaxeAdditionalConfigurablePanel.setFlexSdkName(haxeSdkData.getFlexSdkName());

      final boolean bRemoveDuplicates = haxeSdkData.getRemoveCompletionDuplicatesFlag();
      myHaxeAdditionalConfigurablePanel.setRemoveCompletionDuplicatesFlag(bRemoveDuplicates);
    }
    myHaxeAdditionalConfigurablePanel.getPanel().repaint();
  }

  private static @NonNull String toSystemDependentName(String nekoBinPath) {
    return FileUtil.toSystemDependentName(nekoBinPath == null ? "" : nekoBinPath);
  }

  /** The grayed haxelib/neko/hl/node defaults for a runtime field left empty. */
  private record InheritedDefaults(@Nullable String haxelib,
                                   @Nullable String neko,
                                   @Nullable String hl,
                                   @Nullable String node) {
  }

  // the defaults probe every PATH directory on disk - computed off the EDT,
  // or a slow/network PATH freezes the SDK editor on every reset
  private void updateInheritedDefaults() {
    String homePath = mySdk.getHomePath();
    ReadAction.nonBlocking(() -> computeInheritedDefaults(homePath))
      .finishOnUiThread(ModalityState.defaultModalityState(), this::applyInheritedDefaults)
      .submit(AppExecutorUtil.getAppExecutorService());
  }

  @NonNull
  private static InheritedDefaults computeInheritedDefaults(@Nullable String homePath) {
    String bundledHaxelib = homePath != null ? HaxeSdkUtilBase.getHaxelibPathByFolderPath(homePath) : null;
    String haxelib = bundledHaxelib != null ? bundledHaxelib : HaxeToolPathResolver.pathDetectedExecutable("haxelib");
    return new InheritedDefaults(haxelib,
                                 HaxeToolPathResolver.pathDetectedExecutable("neko"),
                                 HaxeToolPathResolver.pathDetectedExecutable("hl"),
                                 HaxeToolPathResolver.pathDetectedExecutable("node"));
  }

  private void applyInheritedDefaults(@NonNull InheritedDefaults defaults) {
    myHaxeAdditionalConfigurablePanel.setInheritedDefaults(defaults.haxelib(), defaults.neko(), defaults.hl(), defaults.node());
  }

  @Override
  public void disposeUIResources() {
  }
}
