package com.intellij.plugins.haxe.runner.debugger.flash;

import com.intellij.application.options.ModulesComboBox;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.plugins.haxe.HaxeDebuggerBundle;
import com.intellij.plugins.haxe.config.sdk.ui.FlexSdkSelector;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.ui.FormBuilder;
import javax.swing.JComponent;
import org.jetbrains.annotations.NotNull;

/**
 * Editor for {@link AirRunConfiguration}: module, the application descriptor
 * and content root adl launches, the Flex/AIR SDK supplying adl, and the
 * pass-through option/argument lists.
 */
public final class AirRunConfigurationEditor extends SettingsEditor<AirRunConfiguration> {

  private final ModulesComboBox moduleCombo = new ModulesComboBox();
  private final TextFieldWithBrowseButton descriptorField = new TextFieldWithBrowseButton();
  private final TextFieldWithBrowseButton contentRootField = new TextFieldWithBrowseButton();
  private final FlexSdkSelector flexSdkSelector = new FlexSdkSelector();
  private final JBTextField adlOptionsField = new JBTextField();
  private final JBTextField programParametersField = new JBTextField();

  public AirRunConfigurationEditor(@NotNull Project project) {
    FileChooserDescriptor descriptorChooser = FileChooserDescriptorFactory.singleFile()
      .withTitle(HaxeDebuggerBundle.message("air.runner.editor.descriptor.chooser"))
      .withFileFilter(file -> "xml".equalsIgnoreCase(StringUtil.notNullize(file.getExtension())));
    descriptorField.addBrowseFolderListener(project, descriptorChooser);
    contentRootField.addBrowseFolderListener(project, FileChooserDescriptorFactory.singleDir()
      .withTitle(HaxeDebuggerBundle.message("air.runner.editor.content.root.chooser")));
  }

  @Override
  protected void resetEditorFrom(@NotNull AirRunConfiguration configuration) {
    moduleCombo.fillModules(configuration.getProject());
    moduleCombo.setSelectedModule(configuration.getConfigurationModule().getModule());
    descriptorField.setText(configuration.getDescriptorPath());
    contentRootField.setText(configuration.getContentRootPath());
    flexSdkSelector.setSelectedName(configuration.getFlexSdkName());
    adlOptionsField.setText(configuration.getAdlOptions());
    programParametersField.setText(configuration.getProgramParameters());
  }

  @Override
  protected void applyEditorTo(@NotNull AirRunConfiguration configuration) {
    configuration.setModule(moduleCombo.getSelectedModule());
    configuration.setDescriptorPath(descriptorField.getText().trim());
    configuration.setContentRootPath(contentRootField.getText().trim());
    configuration.setFlexSdkName(flexSdkSelector.getSelectedName());
    configuration.setAdlOptions(adlOptionsField.getText().trim());
    configuration.setProgramParameters(programParametersField.getText().trim());
  }

  @Override
  protected @NotNull JComponent createEditor() {
    return FormBuilder.createFormBuilder()
      .addLabeledComponent(HaxeDebuggerBundle.message("air.runner.editor.module"), moduleCombo)
      .addLabeledComponent(HaxeDebuggerBundle.message("air.runner.editor.descriptor"), descriptorField)
      .addLabeledComponent(HaxeDebuggerBundle.message("air.runner.editor.content.root"), contentRootField)
      .addTooltip(HaxeDebuggerBundle.message("air.runner.editor.content.root.hint"))
      .addLabeledComponent(HaxeDebuggerBundle.message("air.runner.editor.flex.sdk"), flexSdkSelector.getComponent())
      .addLabeledComponent(HaxeDebuggerBundle.message("air.runner.editor.adl.options"), adlOptionsField)
      .addLabeledComponent(HaxeDebuggerBundle.message("air.runner.editor.program.parameters"), programParametersField)
      .getPanel();
  }
}
