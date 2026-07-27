package com.intellij.plugins.haxe.runner.debugger.hxcpp.vshaxe;

import com.intellij.application.options.ModulesComboBox;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.plugins.haxe.runner.debugger.dap.ide.DapExecutableRunConfigurationEditorBase;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.ui.UIUtil;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTextPane;
import org.jetbrains.annotations.NotNull;

/**
 * Settings UI for an HXCPP (vshaxe debug server) run configuration: the
 * shared executable fields plus the debug host/port (prefilled with the
 * protocol defaults; only relevant when the build overrides
 * HXCPP_DEBUG_HOST/HXCPP_DEBUG_PORT).
 *
 * The layout lives in the matching .form (labels bind their bundle keys
 * there); the shared reset/apply comes from the base editor.
 */
public class HxcppVshaxeRunConfigurationEditor
  extends DapExecutableRunConfigurationEditorBase<HxcppVshaxeRunConfiguration> {

  private JPanel panel;
  private ModulesComboBox moduleCombo;
  private TextFieldWithBrowseButton executableField;
  private TextFieldWithBrowseButton workingDirectoryField;
  private JBTextField programArgumentsField;
  private JBTextField debugHostField;
  private JBTextField debugPortField;
  private JTextPane debugHintArea;

  public HxcppVshaxeRunConfigurationEditor(Project project) {
    super(project);
    wireCommonChoosers();
    debugHostField.setText(HxcppVshaxeRunConfiguration.DEFAULT_DEBUG_HOST);
    debugPortField.setText(Integer.toString(HxcppVshaxeRunConfiguration.DEFAULT_DEBUG_PORT));
    debugHintArea.setForeground(UIUtil.getContextHelpForeground());
    debugHintArea.setFont(UIUtil.getLabelFont(UIUtil.FontSize.SMALL));
    debugHintArea.setBorder(null);
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
  protected void resetEditorFrom(@NotNull HxcppVshaxeRunConfiguration configuration) {
    resetCommon(configuration);
    debugHostField.setText(configuration.getDebugHost());
    debugPortField.setText(configuration.getDebugPort());
  }

  @Override
  protected void applyEditorTo(@NotNull HxcppVshaxeRunConfiguration configuration) {
    applyCommon(configuration);
    configuration.setDebugHost(debugHostField.getText());
    configuration.setDebugPort(debugPortField.getText());
  }

  @Override
  protected @NotNull JComponent createEditor() {
    return panel;
  }
}
