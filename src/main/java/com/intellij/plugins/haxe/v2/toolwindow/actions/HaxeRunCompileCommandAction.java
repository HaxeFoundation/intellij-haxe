package com.intellij.plugins.haxe.v2.toolwindow.actions;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.project.Project;
import com.intellij.plugins.haxe.HaxeBundle;
import com.intellij.plugins.haxe.v2.toolwindow.HaxeToolWindowLaunches;
import com.intellij.plugins.haxe.v2.toolwindow.HaxeToolWindowPanel;
import com.intellij.plugins.haxe.v2.toolwindow.tree.HaxeToolWindowNodes.EnvCompileCommandNode;
import org.jetbrains.annotations.NotNull;

/** Tree context menu on a configured Compile command row: runs it. */
public final class HaxeRunCompileCommandAction extends HaxeSelectedNodeAction<EnvCompileCommandNode> {

  public HaxeRunCompileCommandAction(@NotNull HaxeToolWindowPanel panel) {
    super(panel, EnvCompileCommandNode.class, HaxeBundle.message("haxe.toolwindow.run.compile.command"), AllIcons.Actions.Compile);
  }

  @Override
  protected boolean applies(@NotNull EnvCompileCommandNode compileCommand) {
    return compileCommand.command() != null;
  }

  @Override
  protected void perform(@NotNull Project project, @NotNull EnvCompileCommandNode compileCommand) {
    HaxeToolWindowLaunches.runCompileCommand(project, compileCommand);
  }
}
