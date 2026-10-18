package com.intellij.plugins.haxe.v2.toolwindow.actions;

import com.intellij.openapi.project.Project;
import com.intellij.plugins.haxe.HaxeBundle;
import com.intellij.plugins.haxe.v2.toolwindow.HaxeToolWindowEditors;
import com.intellij.plugins.haxe.v2.toolwindow.HaxeToolWindowPanel;
import com.intellij.plugins.haxe.v2.toolwindow.tree.HaxeToolWindowNodes.EnvCompileCommandNode;
import org.jetbrains.annotations.NotNull;

/** Tree context menu on the Compile command row: opens its configuration dialog. */
public final class HaxeConfigureCompileCommandAction extends HaxeSelectedNodeAction<EnvCompileCommandNode> {

  public HaxeConfigureCompileCommandAction(@NotNull HaxeToolWindowPanel panel) {
    super(panel, EnvCompileCommandNode.class, HaxeBundle.message("haxe.toolwindow.configure.compile.command"));
  }

  @Override
  protected void perform(@NotNull Project project, @NotNull EnvCompileCommandNode compileCommand) {
    HaxeToolWindowEditors.configureCompileCommand(project, compileCommand);
  }
}
