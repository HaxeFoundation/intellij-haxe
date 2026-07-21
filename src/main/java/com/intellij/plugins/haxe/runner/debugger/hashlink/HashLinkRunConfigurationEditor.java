package com.intellij.plugins.haxe.runner.debugger.hashlink;

import com.intellij.application.options.ModulesComboBox;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.plugins.haxe.HaxeDebuggerBundle;
import com.intellij.plugins.haxe.runner.debugger.HaxeRunConfigurationEditorUtil;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.util.ui.FormBuilder;
import javax.swing.JComponent;
import javax.swing.JPanel;
import org.jetbrains.annotations.NotNull;

/**
 * Settings UI for a HashLink run configuration: module, compiled HashLink
 * binary (auto-detected from the build when empty; .dat accepted so a
 * Lime/OpenFL distribution's {@code hlboot.dat} can be debugged in place),
 * working directory, and an optional custom HashLink executable that
 * overrides the SDK/environment-resolved one.
 */
public class HashLinkRunConfigurationEditor extends SettingsEditor<HashLinkRunConfiguration> {
  private final Project project;
  private final ModulesComboBox moduleCombo = new ModulesComboBox();
  private final TextFieldWithBrowseButton hlFileField = new TextFieldWithBrowseButton();
  private final TextFieldWithBrowseButton workingDirectoryField = new TextFieldWithBrowseButton();
  private final JBCheckBox useCustomHlBinaryCheckbox =
    new JBCheckBox(HaxeDebuggerBundle.message("hashlink.runner.editor.use.custom.hl"));
  private final TextFieldWithBrowseButton customHlBinaryField = new TextFieldWithBrowseButton();
  private final JPanel panel;

  public HashLinkRunConfigurationEditor(Project project) {
    this.project = project;
    // an extension filter (not withFileFilter) so the NATIVE file dialog gets a
    // real "*.hl;*.dat" dropdown entry — a Condition-based filter is invisible to it
    HaxeRunConfigurationEditorUtil.browseInto(project, hlFileField,
      FileChooserDescriptorFactory.createSingleFileDescriptor()
        .withExtensionFilter(HaxeDebuggerBundle.message("hashlink.runner.editor.file.filter"), "hl", "dat"));
    HaxeRunConfigurationEditorUtil.browseInto(project, workingDirectoryField,
      FileChooserDescriptorFactory.createSingleFolderDescriptor());
    HaxeRunConfigurationEditorUtil.browseInto(project, customHlBinaryField,
      FileChooserDescriptorFactory.createSingleFileDescriptor());
    customHlBinaryField.setEnabled(false);
    useCustomHlBinaryCheckbox.addItemListener(e -> customHlBinaryField.setEnabled(useCustomHlBinaryCheckbox.isSelected()));
    panel = FormBuilder.createFormBuilder()
      .addLabeledComponent(HaxeDebuggerBundle.message("hashlink.runner.editor.module"), moduleCombo)
      .addLabeledComponent(HaxeDebuggerBundle.message("hashlink.runner.editor.hl.file"), hlFileField)
      .addLabeledComponent(HaxeDebuggerBundle.message("hashlink.runner.editor.working.directory"), workingDirectoryField)
      .addLabeledComponent(useCustomHlBinaryCheckbox, customHlBinaryField)
      .addComponentFillVertically(new JPanel(), 0)
      .getPanel();
  }

  @Override
  protected void resetEditorFrom(@NotNull HashLinkRunConfiguration configuration) {
    moduleCombo.fillModules(project);
    moduleCombo.setSelectedModule(configuration.getConfigurationModule().getModule());
    hlFileField.setText(FileUtil.toSystemDependentName(configuration.getHlFilePath()));
    workingDirectoryField.setText(FileUtil.toSystemDependentName(configuration.getWorkingDirectory()));
    useCustomHlBinaryCheckbox.setSelected(configuration.isUseCustomHlBinary());
    customHlBinaryField.setText(FileUtil.toSystemDependentName(configuration.getCustomHlBinaryPath()));
    customHlBinaryField.setEnabled(configuration.isUseCustomHlBinary());
  }

  @Override
  protected void applyEditorTo(@NotNull HashLinkRunConfiguration configuration) {
    configuration.setModule(moduleCombo.getSelectedModule());
    configuration.setHlFilePath(FileUtil.toSystemIndependentName(hlFileField.getText().trim()));
    configuration.setWorkingDirectory(FileUtil.toSystemIndependentName(workingDirectoryField.getText().trim()));
    configuration.setUseCustomHlBinary(useCustomHlBinaryCheckbox.isSelected());
    configuration.setCustomHlBinaryPath(FileUtil.toSystemIndependentName(customHlBinaryField.getText().trim()));
  }

  @Override
  protected @NotNull JComponent createEditor() {
    return panel;
  }
}
