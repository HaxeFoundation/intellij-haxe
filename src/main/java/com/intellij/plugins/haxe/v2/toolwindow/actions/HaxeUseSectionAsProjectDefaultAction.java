package com.intellij.plugins.haxe.v2.toolwindow.actions;

import com.intellij.openapi.project.Project;
import com.intellij.plugins.haxe.HaxeBundle;
import com.intellij.plugins.haxe.v2.buildtools.settings.HaxeSectionSelectionStore;
import com.intellij.plugins.haxe.v2.toolwindow.HaxeToolWindowPanel;
import com.intellij.plugins.haxe.v2.toolwindow.tree.HaxeToolWindowNodes.SectionNode;
import org.jetbrains.annotations.NotNull;

/**
 * Tree context menu: makes the section selected on a multi-section hxml's
 * section row the project's default, which everyone starts with until they
 * pick another one.
 */
public final class HaxeUseSectionAsProjectDefaultAction extends HaxeSelectedNodeAction<SectionNode> {

  public HaxeUseSectionAsProjectDefaultAction(@NotNull HaxeToolWindowPanel panel) {
    super(panel, SectionNode.class, HaxeBundle.message("haxe.toolwindow.use.as.project.default"));
  }

  @Override
  protected boolean applies(@NotNull SectionNode sectionNode) {
    return !sectionNode.projectDefault();
  }

  @Override
  protected void perform(@NotNull Project project, @NotNull SectionNode sectionNode) {
    String sectionId = sectionNode.ids().get(sectionNode.selected());
    HaxeSectionSelectionStore.getInstance(project).setProjectDefault(sectionNode.buildFile().file(), sectionId);
  }
}
