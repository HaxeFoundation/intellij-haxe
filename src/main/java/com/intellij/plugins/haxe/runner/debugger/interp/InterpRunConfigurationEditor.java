package com.intellij.plugins.haxe.runner.debugger.interp;

import com.intellij.application.options.ModulesComboBox;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.plugins.haxe.HaxeDebuggerBundle;
import com.intellij.plugins.haxe.runner.debugger.HaxeRunConfigurationEditorUtil;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.ui.FormBuilder;
import com.intellij.util.ui.UIUtil;
import javax.swing.JComponent;
import javax.swing.JPanel;
import org.jetbrains.annotations.NotNull;

/**
 * Settings UI for a Haxe interpreter run configuration: module, the compiler
 * arguments (hxml file or plain arguments), working directory, and whether to
 * append {@code --interp} (off = the arguments are a regular build and the
 * debug target is its MACROS).
 */
public class InterpRunConfigurationEditor extends SettingsEditor<InterpRunConfiguration> {
  private final Project project;
  private final ModulesComboBox moduleCombo = new ModulesComboBox();
  private final JBTextField compilerArgumentsField = new JBTextField();
  private final TextFieldWithBrowseButton workingDirectoryField = new TextFieldWithBrowseButton();
  private final JBCheckBox interpretCheckBox = new JBCheckBox(HaxeDebuggerBundle.message("interp.runner.editor.interpret"));
  private final JPanel panel;

  public InterpRunConfigurationEditor(Project project) {
    this.project = project;
    HaxeRunConfigurationEditorUtil.browseInto(project, workingDirectoryField,
                                              FileChooserDescriptorFactory.createSingleFolderDescriptor());
    JBLabel hint = new JBLabel(HaxeDebuggerBundle.message("interp.runner.editor.hint"));
    hint.setComponentStyle(UIUtil.ComponentStyle.SMALL);
    hint.setForeground(UIUtil.getContextHelpForeground());
    panel = FormBuilder.createFormBuilder()
      .addLabeledComponent(HaxeDebuggerBundle.message("interp.runner.editor.module"), moduleCombo)
      .addLabeledComponent(HaxeDebuggerBundle.message("interp.runner.editor.arguments"), compilerArgumentsField)
      .addLabeledComponent(HaxeDebuggerBundle.message("interp.runner.editor.working.directory"), workingDirectoryField)
      .addComponent(interpretCheckBox)
      .addComponent(hint)
      .addComponentFillVertically(new JPanel(), 0)
      .getPanel();
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
