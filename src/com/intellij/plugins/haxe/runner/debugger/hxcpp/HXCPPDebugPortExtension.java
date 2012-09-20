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
