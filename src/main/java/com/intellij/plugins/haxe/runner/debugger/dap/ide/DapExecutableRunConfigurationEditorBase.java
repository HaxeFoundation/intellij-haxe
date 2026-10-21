package com.intellij.plugins.haxe.runner.debugger.dap.ide;

import com.intellij.application.options.ModulesComboBox;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.plugins.haxe.util.ui.HaxePathFieldChoosers;
import com.intellij.ui.components.JBTextField;
import org.jetbrains.annotations.NotNull;

/**
 * Base editor for {@link DapExecutableRunConfigurationBase} configurations:
 * owns the reset/apply of the shared module / executable / working-directory /
 * program-arguments fields.
 *
 * The COMPONENTS themselves belong to the subclass, which declares them in its
 * .form — the GUI Designer instantiates bound fields in the class named by
 * {@code bind-to-class}, and does so at the start of THAT class's constructor,
 * after this one has run. Subclasses therefore expose their components through
 * the accessors below and call {@link #wireCommonChoosers()} from their own
 * constructor.
 */
public abstract class DapExecutableRunConfigurationEditorBase<C extends DapExecutableRunConfigurationBase>
  extends SettingsEditor<C> {

  protected final Project project;

  protected DapExecutableRunConfigurationEditorBase(Project project) {
    this.project = project;
  }

  protected abstract ModulesComboBox moduleCombo();

  protected abstract TextFieldWithBrowseButton executableField();

  protected abstract TextFieldWithBrowseButton workingDirectoryField();

  protected abstract JBTextField programArgumentsField();

  /**
   * Browse-at-current-path wiring for the shared choosers. Call from the
   * subclass constructor, where the form-built components exist.
   */
  protected void wireCommonChoosers() {
    HaxePathFieldChoosers.browseInto(project, executableField(),
                                              FileChooserDescriptorFactory.singleFile());
    HaxePathFieldChoosers.browseInto(project, workingDirectoryField(),
                                              FileChooserDescriptorFactory.singleDir());
  }

  protected void resetCommon(@NotNull C configuration) {
    moduleCombo().fillModules(project);
    moduleCombo().setSelectedModule(configuration.getConfigurationModule().getModule());
    executableField().setText(FileUtil.toSystemDependentName(configuration.getExecutablePath()));
    workingDirectoryField().setText(FileUtil.toSystemDependentName(configuration.getWorkingDirectory()));
    programArgumentsField().setText(configuration.getProgramArguments());
  }

  protected void applyCommon(@NotNull C configuration) {
    configuration.setModule(moduleCombo().getSelectedModule());
    configuration.setExecutablePath(FileUtil.toSystemIndependentName(executableField().getText().trim()));
    configuration.setWorkingDirectory(FileUtil.toSystemIndependentName(workingDirectoryField().getText().trim()));
    configuration.setProgramArguments(programArgumentsField().getText().trim());
  }
}
