package com.intellij.plugins.haxe.v2.toolwindow.actions;

import com.intellij.openapi.project.Project;
import com.intellij.plugins.haxe.HaxeBundle;
import com.intellij.plugins.haxe.v2.toolwindow.HaxeToolWindowEditors;
import com.intellij.plugins.haxe.v2.toolwindow.HaxeToolWindowPanel;
import com.intellij.plugins.haxe.v2.toolwindow.tree.HaxeToolWindowNodes.SectionNode;
import org.jetbrains.annotations.NotNull;

/** Tree context menu: opens the section dropdown for a multi-section hxml's section row. */
public final class HaxeSelectSectionAction extends HaxeSelectedNodeAction<SectionNode> {

  public HaxeSelectSectionAction(@NotNull HaxeToolWindowPanel panel) {
    super(panel, SectionNode.class, HaxeBundle.message("haxe.toolwindow.select.section.title"));
  }

  @Override
  protected void perform(@NotNull Project project, @NotNull SectionNode sectionNode) {
    HaxeToolWindowEditors.showSectionPopup(project, sectionNode, panel.getSelectionPopupPoint());
  }
}
