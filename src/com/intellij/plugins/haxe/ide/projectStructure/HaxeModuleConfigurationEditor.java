package com.intellij.plugins.haxe.ide.projectStructure;

import com.intellij.openapi.module.ModuleConfigurationEditor;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.roots.ui.configuration.ModuleConfigurationState;
import com.intellij.plugins.haxe.HaxeBundle;
import com.intellij.plugins.haxe.HaxeIcons;
import com.intellij.plugins.haxe.ide.projectStructure.ui.HaxeConfigurationEditor;
import org.jetbrains.annotations.Nls;

import javax.swing.*;

/**
 * @author: Fedor.Korotkov
 */
public class HaxeModuleConfigurationEditor implements ModuleConfigurationEditor {
  private final HaxeConfigurationEditor haxeConfigurationEditor;

  public HaxeModuleConfigurationEditor(ModuleConfigurationState state) {
    haxeConfigurationEditor = new HaxeConfigurationEditor(state.getRootModel().getModule());
  }

  @Override
  public void saveData() {
  }

  @Override
  public void moduleStateChanged() {
  }

  @Nls
  @Override
  public String getDisplayName() {
    return HaxeBundle.message("haxe.module.editor.haxe");
  }

  @Override
  public Icon getIcon() {
    return HaxeIcons.HAXE_ICON_16x16;
  }

  @Override
  public String getHelpTopic() {
    return null;
  }

  @Override
  public JComponent createComponent() {
    return haxeConfigurationEditor.getMainPanel();
  }

  @Override
  public boolean isModified() {
    return haxeConfigurationEditor.isModified();
  }

  @Override
  public void apply() throws ConfigurationException {
    haxeConfigurationEditor.apply();
  }

  @Override
  public void reset() {
    haxeConfigurationEditor.reset();
  }

  @Override
  public void disposeUIResources() {
  }
}
