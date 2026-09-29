package com.intellij.plugins.haxe.v2.toolwindow.actions;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
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
 * default an hxml anchors at its content root (the vshaxe convention its
 * relative paths usually follow); the override serves files whose paths are
 * relative to some other folder.
 */
public final class HaxeSetWorkDirectoryAction extends DumbAwareAction {

  private final HaxeToolWindowPanel panel;

  public HaxeSetWorkDirectoryAction(@NotNull HaxeToolWindowPanel panel) {
    super(HaxeBundle.message("haxe.toolwindow.set.work.directory"));
    this.panel = panel;
  }

  @Override
  public void actionPerformed(@NotNull AnActionEvent e) {
    Project project = e.getProject();
    if (project == null || !(panel.getSelectedUserObject() instanceof BuildFileRow row)) return;
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

  @Override
  public void update(@NotNull AnActionEvent e) {
    // TODO: offer for the xml-family types once their commands embed anchored file arguments
    boolean onHxmlRow = panel.getSelectedUserObject() instanceof BuildFileRow row
                        && row.buildFile().type() == HaxeBuildFileType.HXML;
    e.getPresentation().setEnabledAndVisible(onHxmlRow);
  }

  @Override
  public @NotNull ActionUpdateThread getActionUpdateThread() {
    return ActionUpdateThread.EDT;
  }
}
