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
package com.intellij.plugins.haxe.runner.debugger;

import com.intellij.lang.javascript.flex.sdk.FlexSdkComboBoxWithBrowseButton;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.UnnamedConfigurable;
import com.intellij.openapi.ui.LabeledComponent;
import com.intellij.plugins.haxe.HaxeBundle;
import com.intellij.plugins.haxe.ide.module.HaxeModuleSettings;

import javax.swing.*;

/**
 * @author: Fedor.Korotkov
 */
public class HaxeFlexSDKConfigurable implements UnnamedConfigurable {
  private final FlexSdkComboBoxWithBrowseButton flexSdkCombo = new FlexSdkComboBoxWithBrowseButton();
  private final HaxeModuleSettings mySettings;

  public HaxeFlexSDKConfigurable(HaxeModuleSettings settings) {
    mySettings = settings;
  }

  @Override
  public JComponent createComponent() {
    return LabeledComponent.create(flexSdkCombo, HaxeBundle.message("flex.sdk.label"));
  }

  @Override
  public boolean isModified() {
    return !flexSdkCombo.getSelectedSdkRaw().equals(mySettings.getFlexSdkName());
  }

  @Override
  public void apply() throws ConfigurationException {
    mySettings.setFlexSdkName(flexSdkCombo.getSelectedSdkRaw());
  }

  @Override
  public void reset() {
    flexSdkCombo.setSelectedSdkRaw(mySettings.getFlexSdkName());
  }

  @Override
  public void disposeUIResources() {
  }
}
