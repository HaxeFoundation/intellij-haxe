package com.intellij.plugins.haxe.v2.toolwindow.actions;

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
public final class HaxeShowInHaxelibExplorerAction extends HaxeSelectedNodeAction<LibraryNode> {

  public HaxeShowInHaxelibExplorerAction(@NotNull HaxeToolWindowPanel panel) {
    super(panel, LibraryNode.class, HaxeBundle.message("haxe.toolwindow.show.in.haxelib.explorer"));
  }

  @Override
  protected void perform(@NotNull Project project, @NotNull LibraryNode library) {
    HaxelibExplorerPanel.reveal(project, library.name(), explorerVersionOf(library));
  }

  /** The version entry to land on: a git pin lives under the "git" pseudo-version, a release pin under itself. */
  @Nullable
  private static String explorerVersionOf(@NotNull LibraryNode library) {
    if (HaxelibGitSpec.parse(library.version()) != null) return HaxelibSemVer.GIT_SCM;
    return library.version() != null ? library.version() : library.resolvedVersion();
  }
}
