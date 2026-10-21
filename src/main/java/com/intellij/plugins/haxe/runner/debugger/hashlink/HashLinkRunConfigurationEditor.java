package com.intellij.plugins.haxe.runner.debugger.hashlink;

import com.intellij.application.options.ModulesComboBox;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.plugins.haxe.HaxeDebuggerBundle;
import com.intellij.plugins.haxe.util.ui.HaxePathFieldChoosers;
import com.intellij.ui.components.JBCheckBox;
import javax.swing.JComponent;
import javax.swing.JPanel;
import org.jetbrains.annotations.NotNull;

/**
 * Settings UI for a HashLink run configuration: module, compiled HashLink
 * binary (auto-detected from the build when empty; .dat accepted so a
 * Lime/OpenFL distribution's {@code hlboot.dat} can be debugged in place),
 * working directory, and an optional custom HashLink executable that
 * overrides the SDK/environment-resolved one.
 *
 * The layout lives in the matching .form (labels bind their bundle keys
 * there); this class owns the chooser wiring and the reset/apply mapping.
 */
public class HashLinkRunConfigurationEditor extends SettingsEditor<HashLinkRunConfiguration> {
  private JPanel panel;
  private ModulesComboBox moduleCombo;
  private TextFieldWithBrowseButton hlFileField;
  private TextFieldWithBrowseButton workingDirectoryField;
  private JBCheckBox useCustomHlBinaryCheckbox;
  private TextFieldWithBrowseButton customHlBinaryField;

  private final Project project;

  public HashLinkRunConfigurationEditor(Project project) {
    this.project = project;
    // an extension filter (not withFileFilter) so the NATIVE file dialog gets a
    // real "*.hl;*.dat" dropdown entry — a Condition-based filter is invisible to it
    HaxePathFieldChoosers.browseInto(project, hlFileField,
      FileChooserDescriptorFactory.singleFile()
        .withExtensionFilter(HaxeDebuggerBundle.message("hashlink.runner.editor.file.filter"), "hl", "dat"));

    HaxePathFieldChoosers.browseInto(project, workingDirectoryField, FileChooserDescriptorFactory.singleDir());
    HaxePathFieldChoosers.browseInto(project, customHlBinaryField, FileChooserDescriptorFactory.singleFile());

    customHlBinaryField.setEnabled(false);
    useCustomHlBinaryCheckbox.addItemListener(
      e -> customHlBinaryField.setEnabled(useCustomHlBinaryCheckbox.isSelected()));
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
