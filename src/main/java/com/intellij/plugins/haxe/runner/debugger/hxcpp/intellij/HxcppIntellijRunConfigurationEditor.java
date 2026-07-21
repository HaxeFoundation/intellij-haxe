package com.intellij.plugins.haxe.runner.debugger.hxcpp.intellij;

import com.intellij.openapi.project.Project;
import com.intellij.plugins.haxe.HaxeDebuggerBundle;
import com.intellij.plugins.haxe.runner.debugger.dap.ide.DapExecutableRunConfigurationEditorBase;
import com.intellij.util.ui.FormBuilder;
import javax.swing.JComponent;
import javax.swing.JPanel;
import org.jetbrains.annotations.NotNull;

/**
 * Settings UI for an HXCPP (IntelliJ debug server) run configuration: only
 * the shared executable fields. Deliberately minimal — the debug connection
 * is fully automatic (ephemeral port via env vars), so there is nothing
 * network-ish to configure.
 */
public class HxcppIntellijRunConfigurationEditor
  extends DapExecutableRunConfigurationEditorBase<HxcppIntellijRunConfiguration> {
  private final JPanel panel;

  public HxcppIntellijRunConfigurationEditor(Project project) {
    super(project);
    panel = FormBuilder.createFormBuilder()
      .addLabeledComponent(HaxeDebuggerBundle.message("hxcpp.intellij.runner.editor.module"), moduleCombo)
      .addLabeledComponent(HaxeDebuggerBundle.message("hxcpp.intellij.runner.editor.executable"), executableField)
      .addLabeledComponent(HaxeDebuggerBundle.message("hxcpp.intellij.runner.editor.working.directory"), workingDirectoryField)
      .addComponentToRightColumn(hint(HaxeDebuggerBundle.message("hxcpp.intellij.runner.editor.working.directory.hint")))
      .addLabeledComponent(HaxeDebuggerBundle.message("hxcpp.intellij.runner.editor.program.arguments"), programArgumentsField)
      .addComponent(hint(HaxeDebuggerBundle.message("hxcpp.intellij.runner.debug.hint")))
      .addComponentFillVertically(new JPanel(), 0)
      .getPanel();
  }

  @Override
  protected void resetEditorFrom(@NotNull HxcppIntellijRunConfiguration configuration) {
    resetCommon(configuration);
  }

  @Override
  protected void applyEditorTo(@NotNull HxcppIntellijRunConfiguration configuration) {
    applyCommon(configuration);
  }

  @Override
  protected @NotNull JComponent createEditor() {
    return panel;
  }
}
