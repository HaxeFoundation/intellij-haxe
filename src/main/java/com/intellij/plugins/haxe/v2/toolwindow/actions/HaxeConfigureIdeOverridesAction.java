package com.intellij.plugins.haxe.v2.toolwindow.actions;

import com.intellij.openapi.project.Project;
import com.intellij.plugins.haxe.HaxeBundle;
import com.intellij.plugins.haxe.v2.toolwindow.HaxeToolWindowPanel;
import com.intellij.plugins.haxe.v2.toolwindow.tree.HaxeToolWindowNodes.IdeOverridesNode;
import com.intellij.plugins.haxe.v2.toolwindow.ui.HaxeIdeOverridesDialog;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** Tree context menu on a build file's IDE Overrides row or any row below it: opens the IDE Overrides dialog. */
public final class HaxeConfigureIdeOverridesAction extends HaxeSelectedNodeAction<IdeOverridesNode> {

  public HaxeConfigureIdeOverridesAction(@NotNull HaxeToolWindowPanel panel) {
    super(panel, IdeOverridesNode.class, HaxeBundle.message("haxe.toolwindow.configure.ide.overrides"));
  }

  @Override
  protected @Nullable Object selection() {
    return panel.getSelectedIdeOverridesNode();
  }

  @Override
  protected void perform(@NotNull Project project, @NotNull IdeOverridesNode overrides) {
    HaxeIdeOverridesDialog dialog = new HaxeIdeOverridesDialog(project, overrides.buildFile(), overrides.buildDefineNames(),
                                                               overrides.customTargetApplicable());
    dialog.show();
  }
}
