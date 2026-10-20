package com.intellij.plugins.haxe.runner;

import com.intellij.application.options.ModulesComboBox;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.plugins.haxe.runner.debugger.dap.ide.DapExecutableRunConfigurationEditorBase;
import com.intellij.plugins.haxe.runner.debugger.javascript.AdapterPin;
import com.intellij.plugins.haxe.runner.debugger.javascript.DapAdapterStatusRow;
import com.intellij.ui.components.ActionLink;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 * Settings UI for a runtime configuration: the shared executable fields with
 * the runtime's own executable hint and, for a runtime whose Debug needs a
 * DAP adapter, the adapter's status with its Download link - the only
 * acquisition path, as in the browser configuration. The layout lives in the
 * matching .form (labels bind their bundle keys there).
 */
public class HaxeRuntimeRunConfigurationEditor extends DapExecutableRunConfigurationEditorBase<HaxeRuntimeRunConfiguration> {
  private JPanel panel;
  private ModulesComboBox moduleCombo;
  private TextFieldWithBrowseButton executableField;
  private JBLabel executableHintLabel;
  private TextFieldWithBrowseButton workingDirectoryField;
  private JBTextField programArgumentsField;
  private JLabel adapterLabel;
  private JPanel adapterRow;
  private JBLabel adapterStatusLabel;
  /** Text link fetching the pinned adapter; the only acquisition path. */
  private ActionLink downloadLink;
  /** Icon-only link revealing the adapter store directory in the system file manager. */
  private ActionLink openStoreLink;
  private final DapAdapterStatusRow adapterStatus;

  public HaxeRuntimeRunConfigurationEditor(@NotNull Project project, @NotNull String executableHint, @Nullable AdapterPin debugAdapter) {
    super(project);
    wireCommonChoosers();
    // secondary information, styled like the platform's context help text
    executableHintLabel.setText(executableHint);
    executableHintLabel.setForeground(UIUtil.getContextHelpForeground());
    executableHintLabel.setFont(UIUtil.getLabelFont(UIUtil.FontSize.SMALL));
    adapterLabel.setVisible(debugAdapter != null);
    adapterRow.setVisible(debugAdapter != null);
    adapterStatus = new DapAdapterStatusRow(adapterStatusLabel, downloadLink, openStoreLink, () -> debugAdapter, this::fireEditorStateChanged);
    adapterStatus.update();
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
  protected void resetEditorFrom(@NotNull HaxeRuntimeRunConfiguration configuration) {
    resetCommon(configuration);
    adapterStatus.update();
  }

  @Override
  protected void applyEditorTo(@NotNull HaxeRuntimeRunConfiguration configuration) {
    applyCommon(configuration);
  }

  @Override
  protected @NotNull JComponent createEditor() {
    return panel;
  }
}
