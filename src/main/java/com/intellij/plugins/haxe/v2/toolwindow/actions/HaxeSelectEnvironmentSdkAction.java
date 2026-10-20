package com.intellij.plugins.haxe.v2.toolwindow.actions;

import com.intellij.openapi.project.Project;
import com.intellij.plugins.haxe.HaxeBundle;
import com.intellij.plugins.haxe.v2.toolwindow.HaxeToolWindowEditors;
import com.intellij.plugins.haxe.v2.toolwindow.HaxeToolWindowPanel;
import com.intellij.plugins.haxe.v2.toolwindow.tree.HaxeToolWindowNodes.EnvSdkNode;
import org.jetbrains.annotations.NotNull;

/** Tree context menu on the environment SDK row: opens the SDK dropdown. */
public final class HaxeSelectEnvironmentSdkAction extends HaxeSelectedNodeAction<EnvSdkNode> {

  public HaxeSelectEnvironmentSdkAction(@NotNull HaxeToolWindowPanel panel) {
    super(panel, EnvSdkNode.class, HaxeBundle.message("haxe.toolwindow.select.sdk.title"));
  }

  @Override
  protected void perform(@NotNull Project project, @NotNull EnvSdkNode sdkNode) {
    HaxeToolWindowEditors.showEnvironmentSdkPopup(project, sdkNode, panel.getSelectionPopupPoint());
  }
}
