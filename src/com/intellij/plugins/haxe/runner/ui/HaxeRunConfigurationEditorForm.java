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
package com.intellij.plugins.haxe.runner.ui;

import com.intellij.openapi.fileChooser.FileChooser;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.module.ModuleType;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.CompilerModuleExtension;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.plugins.haxe.ide.module.HaxeModuleSettings;
import com.intellij.plugins.haxe.ide.module.HaxeModuleType;
import com.intellij.plugins.haxe.runner.HaxeApplicationConfiguration;
import com.intellij.ui.ListCellRendererWrapper;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class HaxeRunConfigurationEditorForm extends SettingsEditor<HaxeApplicationConfiguration> {
  private JPanel component;
  private JComboBox myComboModules;
  private JCheckBox myCustomPathCheckBox;
  private TextFieldWithBrowseButton myPathToFileTextField;
  private JCheckBox myAlternativeExecutable;
  private TextFieldWithBrowseButton myExecutableField;

  private String customPathToFile = "";
  private String customPathToExecutable = "";

  private final Project project;

  public HaxeRunConfigurationEditorForm(Project project) {
    this.project = project;
  }

  @Override
  protected void resetEditorFrom(HaxeApplicationConfiguration configuration) {
    myComboModules.removeAllItems();

    final Module[] modules = ModuleManager.getInstance(configuration.getProject()).getModules();
    for (final Module module : modules) {
      if (ModuleType.get(module) == HaxeModuleType.getInstance()) {
        myComboModules.addItem(module);
      }
    }
    myComboModules.setSelectedItem(configuration.getConfigurationModule().getModule());

    myComboModules.setRenderer(new ListCellRendererWrapper() {
      @Override
      public void customize(JList list, Object value, int index, boolean selected, boolean hasFocus) {
        if (value instanceof Module) {
          final Module module = (Module)value;
          setText(module.getName());
        }
      }
    });

    myCustomPathCheckBox.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        if (!myCustomPathCheckBox.isSelected()) {
          customPathToFile = myPathToFileTextField.getText();
        }
        updateComponents();
      }
    });

    myPathToFileTextField.getButton().addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        final FileChooserDescriptor descriptor = new FileChooserDescriptor(true, false, false, true, false, false);
        final VirtualFile file = FileChooser.chooseFile(descriptor, component, null, null);
        if (file != null) {
          customPathToFile = FileUtil.toSystemIndependentName(file.getPath());
          updateComponents();
        }
      }
    });

    myAlternativeExecutable.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        if (!myAlternativeExecutable.isSelected()) {
          customPathToExecutable = myExecutableField.getText();
        }
        updateComponents();
      }
    });

    myExecutableField.getButton().addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        final FileChooserDescriptor descriptor = new FileChooserDescriptor(true, false, false, true, false, false);
        final VirtualFile file = FileChooser.chooseFile(descriptor, component, null, null);
        if (file != null) {
          customPathToExecutable = FileUtil.toSystemIndependentName(file.getPath());
          updateComponents();
        }
      }
    });

    myCustomPathCheckBox.setSelected(configuration.isCustomFileToLaunch());
    myAlternativeExecutable.setSelected(configuration.isCustomExecutable());

    String launchPath = configuration.getCustomFileToLaunchPath();
    launchPath = !launchPath.contains("://") ? FileUtil.toSystemDependentName(launchPath) : launchPath;
    customPathToFile = launchPath;

    launchPath = configuration.getCustomExecutablePath();
    launchPath = !launchPath.contains("://") ? FileUtil.toSystemDependentName(launchPath) : launchPath;
    customPathToExecutable = launchPath;


    updateComponents();
  }

  private void updateComponents() {
    updateCustomPathToFile();
    updateCustomPathToExecutable();
  }

  private void updateCustomPathToFile() {
    if (myCustomPathCheckBox.isSelected()) {
      myPathToFileTextField.setText(FileUtil.toSystemDependentName(customPathToFile));
    }
    else if (getSelectedModule() != null) {
      final HaxeModuleSettings settings = HaxeModuleSettings.getInstance(getSelectedModule());
      final CompilerModuleExtension model = CompilerModuleExtension.getInstance(getSelectedModule());
      assert model != null;
      final String url = model.getCompilerOutputUrl() + "/" + settings.getOutputFileName();
      myPathToFileTextField.setText(FileUtil.toSystemDependentName(VfsUtil.urlToPath(url)));
    }
    else {
      myPathToFileTextField.setText("");
    }
    myPathToFileTextField.setEnabled(myCustomPathCheckBox.isSelected());
  }

  private void updateCustomPathToExecutable() {
    myExecutableField.setText(myAlternativeExecutable.isSelected() ? FileUtil.toSystemDependentName(customPathToExecutable) : "");
    myExecutableField.setEnabled(myAlternativeExecutable.isSelected());
  }

  @Override
  protected void applyEditorTo(HaxeApplicationConfiguration configuration) throws ConfigurationException {
    configuration.setModule(getSelectedModule());
    configuration.setCustomFileToLaunch(myCustomPathCheckBox.isSelected());
    configuration.setCustomExecutable(myAlternativeExecutable.isSelected());
    if (myCustomPathCheckBox.isSelected()) {
      String fileName = myPathToFileTextField.getText();
      fileName = !fileName.contains("://") ? FileUtil.toSystemIndependentName(fileName) : fileName;
      configuration.setCustomFileToLaunchPath(fileName);
    }
    if (myAlternativeExecutable.isSelected()) {
      String fileName = myExecutableField.getText();
      fileName = !fileName.contains("://") ? FileUtil.toSystemIndependentName(fileName) : fileName;
      configuration.setCustomExecutablePath(fileName);
    }
  }

  private Module getSelectedModule() {
    return (Module)myComboModules.getSelectedItem();
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
