package com.intellij.plugins.haxe.runner.debugger.hxcpp.intellij;

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
 * Settings UI for an HXCPP (IntelliJ debug server) run configuration: only
 * the shared executable fields. Deliberately minimal — the debug connection
 * is fully automatic (ephemeral port via env vars), so there is nothing
 * network-ish to configure.
 *
 * The layout lives in the matching .form (labels bind their bundle keys
 * there); the shared reset/apply comes from the base editor.
 */
public class HxcppIntellijRunConfigurationEditor
  extends DapExecutableRunConfigurationEditorBase<HxcppIntellijRunConfiguration> {

  private JPanel panel;
  private ModulesComboBox moduleCombo;
  private TextFieldWithBrowseButton executableField;
  private TextFieldWithBrowseButton workingDirectoryField;
  private JTextPane workingDirectoryHintArea;
  private JBTextField programArgumentsField;
  private JTextPane debugHintArea;

  public HxcppIntellijRunConfigurationEditor(Project project) {
    super(project);
    wireCommonChoosers();
    styleHint(workingDirectoryHintArea);
    styleHint(debugHintArea);
  }

  private static void styleHint(JTextPane hint) {
    hint.setForeground(UIUtil.getContextHelpForeground());
    hint.setFont(UIUtil.getLabelFont(UIUtil.FontSize.SMALL));
    hint.setBorder(null);
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
  protected void resetEditorFrom(@NotNull HxcppIntellijRunConfiguration configuration) {
    resetCommon(configuration);
  }

  @Override
  protected void applyEditorTo(@NotNull HxcppIntellijRunConfiguration configuration) {
    applyCommon(configuration);
  }

  @Override
  protected @NotNull JComponent createEditor() {
    return panel;
  }
}
