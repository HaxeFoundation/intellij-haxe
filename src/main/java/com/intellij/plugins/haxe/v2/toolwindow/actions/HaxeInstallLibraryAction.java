package com.intellij.plugins.haxe.v2.toolwindow.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.intellij.plugins.haxe.HaxeBundle;
import com.intellij.plugins.haxe.v2.buildtools.libraries.HaxelibMutations;
import com.intellij.plugins.haxe.v2.toolwindow.HaxeToolWindowPanel;
import com.intellij.plugins.haxe.v2.toolwindow.tree.HaxeToolWindowNodes.LibraryNode;
import org.jetbrains.annotations.NotNull;

/// Tree context menu on a missing library: runs `haxelib install <name> [version] --always`
/// in the background ([HaxelibMutations]); the tree follows the sync that ends it.
public final class HaxeInstallLibraryAction extends HaxeSelectedNodeAction<LibraryNode> {

  public HaxeInstallLibraryAction(@NotNull HaxeToolWindowPanel panel) {
    super(panel, LibraryNode.class, null);
  }

  @Override
  protected boolean applies(@NotNull LibraryNode library) {
    return !library.installed();
  }

  @Override
  protected void perform(@NotNull Project project, @NotNull LibraryNode library) {
    HaxelibMutations.install(project, library.name(), library.version(), library.resolvedVersion());
  }

  /** The entry names the library it installs. */
  @Override
  public void update(@NotNull AnActionEvent e) {
    super.update(e);
    LibraryNode library = selectedNode();
    if (library != null) {
      e.getPresentation().setText(HaxeBundle.message("haxe.toolwindow.install.library", library.name()));
    }
  }
}
