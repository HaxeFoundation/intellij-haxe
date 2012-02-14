package com.intellij.plugins.haxe.runner.ui;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.project.Project;
import com.intellij.plugins.haxe.runner.HaxeApplicationConfiguration;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;

public class HaxeRunConfigurationEditorForm extends SettingsEditor<HaxeApplicationConfiguration> {
  private JPanel component;
  private JComboBox myComboModules;

  private final Project project;

  public HaxeRunConfigurationEditorForm(Project project) {
    this.project = project;
  }

  @Override
  protected void resetEditorFrom(HaxeApplicationConfiguration configuration) {
    myComboModules.removeAllItems();

    final Module[] modules = ModuleManager.getInstance(configuration.getProject()).getModules();
    for (final Module module : modules) {
      myComboModules.addItem(module);
    }
    myComboModules.setSelectedItem(configuration.getConfigurationModule().getModule());

    myComboModules.setRenderer(new DefaultListCellRenderer() {
      @Override
      public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
        final Component comp = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
        if (value instanceof Module) {
          final Module module = (Module)value;
          setText(module.getName());
        }
        return comp;
      }
    });
  }

  @Override
  protected void applyEditorTo(HaxeApplicationConfiguration configuration) throws ConfigurationException {
    configuration.setModule((Module)myComboModules.getSelectedItem());
  }

  @NotNull
  @Override
  protected JComponent createEditor() {
    return component;
  }

  @Override
  protected void disposeEditor() {
    component.setVisible(false);
  }
}
