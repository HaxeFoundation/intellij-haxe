package com.intellij.plugins.haxe.runner.debugger.hxcpp.legacy;

import com.intellij.application.options.ModulesComboBox;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.plugins.haxe.HaxeDebuggerBundle;
import com.intellij.ui.RawCommandLineEditor;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.fields.IntegerField;
import com.intellij.util.ui.FormBuilder;
import com.intellij.util.ui.UIUtil;
import javax.swing.JComponent;
import org.jetbrains.annotations.NotNull;

/**
 * Editor for {@link LegacyHxcppRunConfiguration}: module, the hxcpp executable
 * with its parameters and working directory, the debugger port, and the
 * remote-debugging mode (listen only, the user launches the debuggee).
 */
public final class LegacyHxcppRunConfigurationEditor extends SettingsEditor<LegacyHxcppRunConfiguration> {

  private final ModulesComboBox moduleCombo = new ModulesComboBox();
  private final TextFieldWithBrowseButton executableField = new TextFieldWithBrowseButton();
  private final RawCommandLineEditor parametersField = new RawCommandLineEditor();
  private final TextFieldWithBrowseButton workingDirectoryField = new TextFieldWithBrowseButton();
  private final IntegerField portField = new IntegerField(null, 1, 65535);
  private final JBCheckBox remoteCheckBox =
    new JBCheckBox(HaxeDebuggerBundle.message("legacy.hxcpp.runner.editor.remote"));

  public LegacyHxcppRunConfigurationEditor(@NotNull Project project) {
    executableField.addBrowseFolderListener(project, FileChooserDescriptorFactory.singleFile()
      .withTitle(HaxeDebuggerBundle.message("legacy.hxcpp.runner.editor.executable.chooser")));
    workingDirectoryField.addBrowseFolderListener(project, FileChooserDescriptorFactory.singleDir()
      .withTitle(HaxeDebuggerBundle.message("legacy.hxcpp.runner.editor.workdir.chooser")));
    portField.setDefaultValue(LegacyHxcppRunConfiguration.DEFAULT_PORT);
    remoteCheckBox.addActionListener(e -> updateEnabledState());
  }

  private void updateEnabledState() {
    boolean local = !remoteCheckBox.isSelected();
    executableField.setEnabled(local);
    parametersField.setEnabled(local);
    workingDirectoryField.setEnabled(local);
  }

  @Override
  protected void resetEditorFrom(@NotNull LegacyHxcppRunConfiguration configuration) {
    moduleCombo.fillModules(configuration.getProject());
    moduleCombo.setSelectedModule(configuration.getConfigurationModule().getModule());
    executableField.setText(configuration.getExecutablePath());
    parametersField.setText(configuration.getProgramParameters());
    workingDirectoryField.setText(configuration.getWorkingDirectory());
    portField.setValue(configuration.getPort());
    remoteCheckBox.setSelected(configuration.isRemoteDebugging());
    updateEnabledState();
  }

  @Override
  protected void applyEditorTo(@NotNull LegacyHxcppRunConfiguration configuration) {
    configuration.setModule(moduleCombo.getSelectedModule());
    configuration.setExecutablePath(executableField.getText().trim());
    configuration.setProgramParameters(parametersField.getText().trim());
    configuration.setWorkingDirectory(workingDirectoryField.getText().trim());
    configuration.setPort(portField.getValue());
    configuration.setRemoteDebugging(remoteCheckBox.isSelected());
  }

  @Override
  protected @NotNull JComponent createEditor() {
    JBLabel hint = new JBLabel(HaxeDebuggerBundle.message("legacy.hxcpp.runner.editor.debug.hint"));
    hint.setForeground(UIUtil.getContextHelpForeground());
    return FormBuilder.createFormBuilder()
      .addLabeledComponent(HaxeDebuggerBundle.message("legacy.hxcpp.runner.editor.module"), moduleCombo)
      .addLabeledComponent(HaxeDebuggerBundle.message("legacy.hxcpp.runner.editor.executable"), executableField)
      .addLabeledComponent(HaxeDebuggerBundle.message("legacy.hxcpp.runner.editor.parameters"), parametersField)
      .addLabeledComponent(HaxeDebuggerBundle.message("legacy.hxcpp.runner.editor.workdir"), workingDirectoryField)
      .addLabeledComponent(HaxeDebuggerBundle.message("legacy.hxcpp.runner.editor.port"), portField)
      .addComponent(remoteCheckBox)
      .addComponent(hint)
      .getPanel();
  }
}
