package com.intellij.plugins.haxe.runner.debugger.interp;

import com.intellij.application.options.ModulesComboBox;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.plugins.haxe.util.ui.HaxePathFieldChoosers;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.ui.UIUtil;
import javax.swing.JComponent;
import javax.swing.JPanel;
import org.jetbrains.annotations.NotNull;

/**
 * Settings UI for the eval (haxe --interp) run configuration: module,
 * compiler arguments, working directory, and whether to run as an
 * interpreter.
 *
 * The layout lives in the matching .form (labels bind their bundle keys
 * there); this class owns the chooser wiring and the reset/apply mapping.
 */
public class InterpRunConfigurationEditor extends SettingsEditor<InterpRunConfiguration> {
  private JPanel panel;
  private ModulesComboBox moduleCombo;
  private JBTextField compilerArgumentsField;
  private TextFieldWithBrowseButton workingDirectoryField;
  private JBCheckBox interpretCheckBox;
  private JBLabel hintLabel;

  private final Project project;

  public InterpRunConfigurationEditor(Project project) {
    this.project = project;
    HaxePathFieldChoosers.browseInto(project, workingDirectoryField, FileChooserDescriptorFactory.singleDir());
    hintLabel.setComponentStyle(UIUtil.ComponentStyle.SMALL);
    hintLabel.setForeground(UIUtil.getContextHelpForeground());
  }

  @Override
  protected void resetEditorFrom(@NotNull InterpRunConfiguration configuration) {
    moduleCombo.fillModules(project);
    moduleCombo.setSelectedModule(configuration.getConfigurationModule().getModule());
    compilerArgumentsField.setText(configuration.getCompilerArguments());
    workingDirectoryField.setText(FileUtil.toSystemDependentName(configuration.getWorkingDirectory()));
    interpretCheckBox.setSelected(configuration.isRunAsInterpreter());
  }

  @Override
  protected void applyEditorTo(@NotNull InterpRunConfiguration configuration) {
    configuration.setModule(moduleCombo.getSelectedModule());
    configuration.setCompilerArguments(compilerArgumentsField.getText().trim());
    configuration.setWorkingDirectory(FileUtil.toSystemIndependentName(workingDirectoryField.getText().trim()));
    configuration.setRunAsInterpreter(interpretCheckBox.isSelected());
  }

  @Override
  protected @NotNull JComponent createEditor() {
    return panel;
  }
}
