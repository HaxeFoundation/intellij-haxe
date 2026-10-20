package com.intellij.plugins.haxe.v2.toolwindow.actions;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.plugins.haxe.HaxeBundle;
import com.intellij.plugins.haxe.v2.buildsystem.HaxeBuildFileType;
import com.intellij.plugins.haxe.v2.buildtools.HaxeBuildWorkDirectories;
import com.intellij.plugins.haxe.v2.buildtools.settings.HaxeWorkDirectoryStore;
import com.intellij.plugins.haxe.v2.toolwindow.HaxeToolWindowPanel;
import com.intellij.plugins.haxe.v2.toolwindow.tree.HaxeToolWindowNodes.BuildFileRow;
import com.intellij.plugins.haxe.v2.toolwindow.ui.HaxeWorkDirectoryDialog;
import org.jetbrains.annotations.NotNull;

/**
 * Tree context menu: overrides an hxml build file's working directory. By
 * default an hxml runs in its own folder, or in its content root when its
 * classpaths resolve only there (the vshaxe convention); the override serves
 * files whose paths are relative to some other folder.
 */
public final class HaxeSetWorkDirectoryAction extends HaxeSelectedNodeAction<BuildFileRow> {

  public HaxeSetWorkDirectoryAction(@NotNull HaxeToolWindowPanel panel) {
    super(panel, BuildFileRow.class, HaxeBundle.message("haxe.toolwindow.set.work.directory"));
  }

  // TODO: offer for the xml-family types once their commands embed anchored file arguments
  @Override
  protected boolean applies(@NotNull BuildFileRow row) {
    return row.buildFile().type() == HaxeBuildFileType.HXML;
  }

  @Override
  protected void perform(@NotNull Project project, @NotNull BuildFileRow row) {
    VirtualFile file = row.buildFile().file();
    HaxeWorkDirectoryStore store = HaxeWorkDirectoryStore.getInstance(project);
    String currentOverride = store.getWorkDirectory(file.getPath());
    VirtualFile defaultAnchor = HaxeBuildWorkDirectories.defaultAnchor(project, file);
    String defaultDirectory = defaultAnchor != null ? defaultAnchor.getPath() : null;

    HaxeWorkDirectoryDialog dialog = new HaxeWorkDirectoryDialog(project, currentOverride, defaultDirectory);
    if (dialog.showAndGet()) {
      store.setWorkDirectory(file.getPath(), dialog.getWorkDirectory());
    }
  }
}
