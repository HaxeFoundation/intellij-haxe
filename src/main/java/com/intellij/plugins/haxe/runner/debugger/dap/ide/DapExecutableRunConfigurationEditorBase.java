package com.intellij.plugins.haxe.runner.debugger.dap.ide;

import com.intellij.application.options.ModulesComboBox;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.plugins.haxe.runner.debugger.HaxeRunConfigurationEditorUtil;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;

/**
 * Base editor for {@link DapExecutableRunConfigurationBase} configurations:
 * owns the module / executable / working-directory / program-arguments fields
 * (with browse-at-current-path wiring) and their reset/apply. The subclass
 * composes the form — labels are per-debugger bundle keys, extra rows go
 * wherever they read best — and calls {@link #resetCommon}/{@link #applyCommon}
 * from its own reset/apply.
 */
public abstract class DapExecutableRunConfigurationEditorBase<C extends DapExecutableRunConfigurationBase>
  extends SettingsEditor<C> {

  protected final Project project;
  protected final ModulesComboBox moduleCombo = new ModulesComboBox();
  protected final TextFieldWithBrowseButton executableField = new TextFieldWithBrowseButton();
  protected final TextFieldWithBrowseButton workingDirectoryField = new TextFieldWithBrowseButton();
  protected final JBTextField programArgumentsField = new JBTextField();

  protected DapExecutableRunConfigurationEditorBase(Project project) {
    this.project = project;
    HaxeRunConfigurationEditorUtil.browseInto(project, executableField,
                                              FileChooserDescriptorFactory.createSingleFileDescriptor());
    HaxeRunConfigurationEditorUtil.browseInto(project, workingDirectoryField,
                                              FileChooserDescriptorFactory.createSingleFolderDescriptor());
  }

  /** A small gray helper line, styled the way every debugger editor hints. */
  protected static JBLabel hint(String text) {
    JBLabel label = new JBLabel(text);
    label.setComponentStyle(UIUtil.ComponentStyle.SMALL);
    label.setForeground(UIUtil.getContextHelpForeground());
    return label;
  }

  protected void resetCommon(@NotNull C configuration) {
    moduleCombo.fillModules(project);
    moduleCombo.setSelectedModule(configuration.getConfigurationModule().getModule());
    executableField.setText(FileUtil.toSystemDependentName(configuration.getExecutablePath()));
    workingDirectoryField.setText(FileUtil.toSystemDependentName(configuration.getWorkingDirectory()));
    programArgumentsField.setText(configuration.getProgramArguments());
  }

  protected void applyCommon(@NotNull C configuration) {
    configuration.setModule(moduleCombo.getSelectedModule());
    configuration.setExecutablePath(FileUtil.toSystemIndependentName(executableField.getText().trim()));
    configuration.setWorkingDirectory(FileUtil.toSystemIndependentName(workingDirectoryField.getText().trim()));
    configuration.setProgramArguments(programArgumentsField.getText().trim());
  }
}
