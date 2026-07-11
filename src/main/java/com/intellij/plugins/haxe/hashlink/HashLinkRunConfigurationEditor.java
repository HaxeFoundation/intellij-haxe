package com.intellij.plugins.haxe.hashlink;

import com.intellij.application.options.ModulesComboBox;
import com.intellij.openapi.fileChooser.FileChooser;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.plugins.haxe.HaxeBundle;
import com.intellij.util.ui.FormBuilder;
import javax.swing.JComponent;
import javax.swing.JPanel;
import org.jetbrains.annotations.NotNull;

/**
 * Settings UI for a HashLink run configuration: module, compiled .hl file
 * (optional — auto-detected from the build when empty) and working directory.
 */
public class HashLinkRunConfigurationEditor extends SettingsEditor<HashLinkRunConfiguration> {
  private final Project project;
  private final ModulesComboBox moduleCombo = new ModulesComboBox();
  private final TextFieldWithBrowseButton hlFileField = new TextFieldWithBrowseButton();
  private final TextFieldWithBrowseButton workingDirectoryField = new TextFieldWithBrowseButton();
  private final JPanel panel;

  public HashLinkRunConfigurationEditor(Project project) {
    this.project = project;
    browseInto(hlFileField, FileChooserDescriptorFactory.createSingleFileDescriptor("hl"));
    browseInto(workingDirectoryField, FileChooserDescriptorFactory.createSingleFolderDescriptor());
    panel = FormBuilder.createFormBuilder()
      .addLabeledComponent(HaxeBundle.message("hashlink.runner.editor.module"), moduleCombo)
      .addLabeledComponent(HaxeBundle.message("hashlink.runner.editor.hl.file"), hlFileField)
      .addLabeledComponent(HaxeBundle.message("hashlink.runner.editor.working.directory"), workingDirectoryField)
      .addComponentFillVertically(new JPanel(), 0)
      .getPanel();
  }

  private void browseInto(TextFieldWithBrowseButton field, FileChooserDescriptor descriptor) {
    field.addActionListener(e -> {
      VirtualFile file = FileChooser.chooseFile(descriptor, project, null);
      if (file != null) {
        field.setText(FileUtil.toSystemDependentName(file.getPath()));
      }
    });
  }

  @Override
  protected void resetEditorFrom(@NotNull HashLinkRunConfiguration configuration) {
    moduleCombo.fillModules(project);
    moduleCombo.setSelectedModule(configuration.getConfigurationModule().getModule());
    hlFileField.setText(FileUtil.toSystemDependentName(configuration.getHlFilePath()));
    workingDirectoryField.setText(FileUtil.toSystemDependentName(configuration.getWorkingDirectory()));
  }

  @Override
  protected void applyEditorTo(@NotNull HashLinkRunConfiguration configuration) {
    configuration.setModule(moduleCombo.getSelectedModule());
    configuration.setHlFilePath(FileUtil.toSystemIndependentName(hlFileField.getText().trim()));
    configuration.setWorkingDirectory(FileUtil.toSystemIndependentName(workingDirectoryField.getText().trim()));
  }

  @Override
  protected @NotNull JComponent createEditor() {
    return panel;
  }
}
