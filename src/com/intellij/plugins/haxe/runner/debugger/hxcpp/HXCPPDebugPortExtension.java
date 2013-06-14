/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
package com.intellij.plugins.haxe.runner.debugger.hxcpp;

import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.UnnamedConfigurable;
import com.intellij.openapi.ui.LabeledComponent;
import com.intellij.plugins.haxe.HaxeBundle;
import com.intellij.plugins.haxe.ide.module.HaxeModuleSettings;
import com.intellij.plugins.haxe.ide.projectStructure.HaxeModuleConfigurationExtensionPoint;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

/**
 * @author: Fedor.Korotkov
 */
public class HXCPPDebugPortExtension implements HaxeModuleConfigurationExtensionPoint {
  @Override
  public UnnamedConfigurable createConfigurable(@NotNull HaxeModuleSettings settings) {
    return new HXCPPPortConfigurable(settings);
  }

  private class HXCPPPortConfigurable implements UnnamedConfigurable {
    private final JTextField myPortField = new JTextField();
    private final HaxeModuleSettings mySettings;

    public HXCPPPortConfigurable(HaxeModuleSettings settings) {
      mySettings = settings;
    }

    @Override
    public JComponent createComponent() {
      return LabeledComponent.create(myPortField, HaxeBundle.message("hxcpp.port.label"));
    }

    @Override
    public boolean isModified() {
      return !myPortField.getText().equals(mySettings.getHXCPPPort());
    }

    @Override
    public void apply() throws ConfigurationException {
      mySettings.setHXCPPPort(myPortField.getText());
    }

    @Override
    public void reset() {
      myPortField.setText(mySettings.getHXCPPPort());
    }

    @Override
    public void disposeUIResources() {
    }
  }
}
