package com.intellij.plugins.haxe.runner.debugger.hxcpp.vshaxe;

import com.intellij.openapi.project.Project;
import com.intellij.plugins.haxe.HaxeDebuggerBundle;
import com.intellij.plugins.haxe.runner.debugger.dap.ide.DapExecutableRunConfigurationEditorBase;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.ui.FormBuilder;
import javax.swing.JComponent;
import javax.swing.JPanel;
import org.jetbrains.annotations.NotNull;

/**
 * Settings UI for an HXCPP (vshaxe debug server) run configuration: the
 * shared executable fields plus the debug host/port (prefilled with the
 * protocol defaults; only relevant when the build overrides
 * HXCPP_DEBUG_HOST/HXCPP_DEBUG_PORT).
 */
public class HxcppVshaxeRunConfigurationEditor extends DapExecutableRunConfigurationEditorBase<HxcppVshaxeRunConfiguration> {
  private final JBTextField debugHostField = new JBTextField(HxcppVshaxeRunConfiguration.DEFAULT_DEBUG_HOST);
  private final JBTextField debugPortField = new JBTextField(Integer.toString(HxcppVshaxeRunConfiguration.DEFAULT_DEBUG_PORT));
  private final JPanel panel;

  public HxcppVshaxeRunConfigurationEditor(Project project) {
    super(project);
    panel = FormBuilder.createFormBuilder()
      .addLabeledComponent(HaxeDebuggerBundle.message("hxcpp.runner.editor.module"), moduleCombo)
      .addLabeledComponent(HaxeDebuggerBundle.message("hxcpp.runner.editor.executable"), executableField)
      .addLabeledComponent(HaxeDebuggerBundle.message("hxcpp.runner.editor.working.directory"), workingDirectoryField)
      .addLabeledComponent(HaxeDebuggerBundle.message("hxcpp.runner.editor.program.arguments"), programArgumentsField)
      .addLabeledComponent(HaxeDebuggerBundle.message("hxcpp.runner.editor.debug.host"), debugHostField)
      .addLabeledComponent(HaxeDebuggerBundle.message("hxcpp.runner.editor.debug.port"), debugPortField)
      .addComponent(hint(HaxeDebuggerBundle.message("hxcpp.runner.debug.hint")))
      .addComponentFillVertically(new JPanel(), 0)
      .getPanel();
  }

  @Override
  protected void resetEditorFrom(@NotNull HxcppVshaxeRunConfiguration configuration) {
    resetCommon(configuration);
    debugHostField.setText(configuration.getDebugHost());
    debugPortField.setText(configuration.getDebugPort());
  }

  @Override
  protected void applyEditorTo(@NotNull HxcppVshaxeRunConfiguration configuration) {
    applyCommon(configuration);
    configuration.setDebugHost(debugHostField.getText());
    configuration.setDebugPort(debugPortField.getText());
  }

  @Override
  protected @NotNull JComponent createEditor() {
    return panel;
  }
}
