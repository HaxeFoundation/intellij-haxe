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
