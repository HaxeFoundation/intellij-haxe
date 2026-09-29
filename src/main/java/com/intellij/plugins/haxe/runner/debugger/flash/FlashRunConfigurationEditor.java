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
import com.intellij.util.ui.FormBuilder;
import javax.swing.JComponent;
import org.jetbrains.annotations.NotNull;

/**
 * Editor for {@link FlashRunConfiguration}: module, the swf to launch, the
 * Flex SDK driving the debugger and the player executable used by plain Run.
 */
public final class FlashRunConfigurationEditor extends SettingsEditor<FlashRunConfiguration> {

  private final ModulesComboBox moduleCombo = new ModulesComboBox();
  private final TextFieldWithBrowseButton swfField = new TextFieldWithBrowseButton();
  private final FlexSdkSelector flexSdkSelector = new FlexSdkSelector();
  private final TextFieldWithBrowseButton playerField = new TextFieldWithBrowseButton();

  public FlashRunConfigurationEditor(@NotNull Project project) {
    FileChooserDescriptor swfDescriptor = FileChooserDescriptorFactory.singleFile()
      .withTitle(HaxeDebuggerBundle.message("flash.runner.editor.swf.chooser"))
      .withFileFilter(file -> "swf".equalsIgnoreCase(StringUtil.notNullize(file.getExtension())));
    swfField.addBrowseFolderListener(project, swfDescriptor);
    playerField.addBrowseFolderListener(project, FileChooserDescriptorFactory.singleFile()
      .withTitle(HaxeDebuggerBundle.message("flash.runner.editor.player.chooser")));
  }

  @Override
  protected void resetEditorFrom(@NotNull FlashRunConfiguration configuration) {
    moduleCombo.fillModules(configuration.getProject());
    moduleCombo.setSelectedModule(configuration.getConfigurationModule().getModule());
    swfField.setText(configuration.getSwfFilePath());
    playerField.setText(configuration.getFlashPlayerPath());
    flexSdkSelector.setSelectedName(configuration.getFlexSdkName());
  }

  @Override
  protected void applyEditorTo(@NotNull FlashRunConfiguration configuration) {
    configuration.setModule(moduleCombo.getSelectedModule());
    configuration.setSwfFilePath(swfField.getText().trim());
    configuration.setFlexSdkName(flexSdkSelector.getSelectedName());
    configuration.setFlashPlayerPath(playerField.getText().trim());
  }

  @Override
  protected @NotNull JComponent createEditor() {
    return FormBuilder.createFormBuilder()
      .addLabeledComponent(HaxeDebuggerBundle.message("flash.runner.editor.module"), moduleCombo)
      .addLabeledComponent(HaxeDebuggerBundle.message("flash.runner.editor.swf"), swfField)
      .addLabeledComponent(HaxeDebuggerBundle.message("flash.runner.editor.flex.sdk"), flexSdkSelector.getComponent())
      .addLabeledComponent(HaxeDebuggerBundle.message("flash.runner.editor.player"), playerField)
      .getPanel();
  }
}
