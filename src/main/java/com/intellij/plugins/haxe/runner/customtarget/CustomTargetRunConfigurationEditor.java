package com.intellij.plugins.haxe.runner.customtarget;

import com.intellij.application.options.ModulesComboBox;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.plugins.haxe.runner.debugger.dap.ide.DapExecutableRunConfigurationEditorBase;
import com.intellij.plugins.haxe.util.ui.HaxeDialogHints;
import com.intellij.ui.components.JBTextField;
import org.jetbrains.annotations.NotNull;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTextPane;

/**
 * Settings editor for {@link CustomTargetRunConfiguration}: module, executable,
 * working directory and arguments. The layout lives in the matching .form
 * (labels bind their bundle keys there).
 */
public class CustomTargetRunConfigurationEditor extends DapExecutableRunConfigurationEditorBase<CustomTargetRunConfiguration> {

  private JPanel panel;
  private ModulesComboBox moduleCombo;
  private TextFieldWithBrowseButton executableField;
  private TextFieldWithBrowseButton workingDirectoryField;
  private JTextPane workingDirectoryHintArea;
  private JBTextField programArgumentsField;
  private JTextPane pathsHintArea;

  public CustomTargetRunConfigurationEditor(Project project) {
    super(project);
    wireCommonChoosers();
    HaxeDialogHints.style(workingDirectoryHintArea);
    HaxeDialogHints.style(pathsHintArea);
  }

  @Override
  protected ModulesComboBox moduleCombo() {
    return moduleCombo;
  }

  @Override
  protected TextFieldWithBrowseButton executableField() {
    return executableField;
  }

  @Override
  protected TextFieldWithBrowseButton workingDirectoryField() {
    return workingDirectoryField;
  }

  @Override
  protected JBTextField programArgumentsField() {
    return programArgumentsField;
  }

  @Override
  protected void resetEditorFrom(@NotNull CustomTargetRunConfiguration configuration) {
    resetCommon(configuration);
  }

  @Override
  protected void applyEditorTo(@NotNull CustomTargetRunConfiguration configuration) {
    applyCommon(configuration);
  }

  @Override
  protected @NotNull JComponent createEditor() {
    return panel;
  }
}
