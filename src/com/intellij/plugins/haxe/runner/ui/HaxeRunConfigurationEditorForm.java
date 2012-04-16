package com.intellij.plugins.haxe.runner.ui;

import com.intellij.ide.ui.ListCellRendererWrapper;
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
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class HaxeRunConfigurationEditorForm extends SettingsEditor<HaxeApplicationConfiguration> {
  private JPanel component;
  private JComboBox myComboModules;
  private JCheckBox myCustomPathCheckBox;
  private TextFieldWithBrowseButton myPathToFileTextField;

  private String customPathToFile = "";

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

    myComboModules.setRenderer(new ListCellRendererWrapper(myComboModules.getRenderer()) {
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
        updateCustomFilePath();
      }
    });

    myPathToFileTextField.getButton().addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        final FileChooserDescriptor descriptor = new FileChooserDescriptor(true, false, false, true, false, false);
        final VirtualFile file = FileChooser.chooseFile(descriptor, component, null, null);
        if (file != null) {
          customPathToFile = file.getPath();
          updateCustomFilePath();
        }
      }
    });

    myCustomPathCheckBox.setSelected(configuration.isCustomFileToLaunch());
    String launchPath = configuration.getCustomFileToLaunchPath();
    launchPath = launchPath.indexOf("://") == -1 ? FileUtil.toSystemDependentName(launchPath) : launchPath;
    customPathToFile = launchPath;


    updateCustomFilePath();
  }

  private void updateCustomFilePath() {
    if (myCustomPathCheckBox.isSelected()) {
      myPathToFileTextField.setText(customPathToFile);
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

  @Override
  protected void applyEditorTo(HaxeApplicationConfiguration configuration) throws ConfigurationException {
    configuration.setModule(getSelectedModule());
    configuration.setCustomFileToLaunch(myCustomPathCheckBox.isSelected());
    if (myCustomPathCheckBox.isSelected()) {
      String fileName = myPathToFileTextField.getText();
      fileName = !fileName.contains("://") ? FileUtil.toSystemIndependentName(fileName) : fileName;
      configuration.setCustomFileToLaunchPath(fileName);
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
