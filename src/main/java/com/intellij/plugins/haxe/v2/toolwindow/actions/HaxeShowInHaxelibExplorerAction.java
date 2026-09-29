package com.intellij.plugins.haxe.v2.toolwindow.actions;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.plugins.haxe.HaxeBundle;
import com.intellij.plugins.haxe.haxelib.HaxelibGitSpec;
import com.intellij.plugins.haxe.haxelib.HaxelibSemVer;
import com.intellij.plugins.haxe.ide.toolWindow.haxelib.HaxelibExplorerPanel;
import com.intellij.plugins.haxe.v2.toolwindow.HaxeToolWindowPanel;
import com.intellij.plugins.haxe.v2.toolwindow.tree.HaxeToolWindowNodes.LibraryNode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** Tree context menu on a library row: reveals the library in the Haxelib tool window's Explorer tab. */
public final class HaxeShowInHaxelibExplorerAction extends DumbAwareAction {

  private final HaxeToolWindowPanel panel;

  public HaxeShowInHaxelibExplorerAction(@NotNull HaxeToolWindowPanel panel) {
    super(HaxeBundle.message("haxe.toolwindow.show.in.haxelib.explorer"));
    this.panel = panel;
  }

  @Override
  public void actionPerformed(@NotNull AnActionEvent e) {
    Project project = e.getProject();
    if (project != null && panel.getSelectedUserObject() instanceof LibraryNode library) {
      HaxelibExplorerPanel.reveal(project, library.name(), explorerVersionOf(library));
    }
  }

  /** The version entry to land on: a git pin lives under the "git" pseudo-version, a release pin under itself. */
  @Nullable
  private static String explorerVersionOf(@NotNull LibraryNode library) {
    if (HaxelibGitSpec.parse(library.version()) != null) return HaxelibSemVer.GIT_SCM;
    return library.version() != null ? library.version() : library.resolvedVersion();
  }

  @Override
  public void update(@NotNull AnActionEvent e) {
    e.getPresentation().setEnabledAndVisible(panel.getSelectedUserObject() instanceof LibraryNode);
  }

  @Override
  public @NotNull ActionUpdateThread getActionUpdateThread() {
    return ActionUpdateThread.EDT;
  }
}
