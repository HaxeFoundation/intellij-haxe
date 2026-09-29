package com.intellij.plugins.haxe.runner.neko;

import com.intellij.application.options.ModulesComboBox;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.plugins.haxe.HaxeDebuggerBundle;
import com.intellij.plugins.haxe.runner.debugger.dap.ide.DapExecutableRunConfigurationEditorBase;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.ui.FormBuilder;
import javax.swing.JComponent;
import javax.swing.JPanel;
import org.jetbrains.annotations.NotNull;

/** Settings UI for a Neko run configuration: only the shared executable fields, built programmatically. */
public class NekoRunConfigurationEditor extends DapExecutableRunConfigurationEditorBase<NekoRunConfiguration> {

  private final ModulesComboBox moduleCombo = new ModulesComboBox();
  private final TextFieldWithBrowseButton executableField = new TextFieldWithBrowseButton();
  private final TextFieldWithBrowseButton workingDirectoryField = new TextFieldWithBrowseButton();
  private final JBTextField programArgumentsField = new JBTextField();
  private final JPanel panel;

  public NekoRunConfigurationEditor(Project project) {
    super(project);
    wireCommonChoosers();
    panel = FormBuilder.createFormBuilder()
      .addLabeledComponent(HaxeDebuggerBundle.message("neko.runner.editor.module"), moduleCombo)
      .addLabeledComponent(HaxeDebuggerBundle.message("neko.runner.editor.executable"), executableField)
      .addTooltip(HaxeDebuggerBundle.message("neko.runner.editor.executable.hint"))
      .addLabeledComponent(HaxeDebuggerBundle.message("neko.runner.editor.working.directory"), workingDirectoryField)
      .addLabeledComponent(HaxeDebuggerBundle.message("neko.runner.editor.program.arguments"), programArgumentsField)
      .getPanel();
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
  protected void resetEditorFrom(@NotNull NekoRunConfiguration configuration) {
    resetCommon(configuration);
  }

  @Override
  protected void applyEditorTo(@NotNull NekoRunConfiguration configuration) {
    applyCommon(configuration);
  }

  @Override
  protected @NotNull JComponent createEditor() {
    return panel;
  }
}
