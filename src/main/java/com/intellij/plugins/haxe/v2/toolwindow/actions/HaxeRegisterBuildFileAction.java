package com.intellij.plugins.haxe.v2.toolwindow.actions;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.plugins.haxe.HaxeBundle;
import com.intellij.plugins.haxe.v2.buildsystem.HaxeBuildFileScanner;
import com.intellij.plugins.haxe.v2.buildtools.HaxeContainers;
import com.intellij.plugins.haxe.v2.buildtools.HaxeKnownBuildFiles;
import com.intellij.ui.dsl.listCellRenderer.BuilderKt;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Project-view context menu: registers the selected hxml/project-xml file as a
 * Haxe build file of a container — the counterpart of the tool window's Add
 * Build File, reachable from where users actually see the file. Offered for a
 * file the tool window does not show yet. The file's own container is
 * preselected; with several containers a chooser pops up. Registration
 * triggers the sync pipeline, so libraries and the tree follow immediately.
 */
public final class HaxeRegisterBuildFileAction extends DumbAwareAction {

  @Override
  public void update(@NotNull AnActionEvent e) {
    Project project = e.getProject();
    VirtualFile file = e.getData(CommonDataKeys.VIRTUAL_FILE);
    boolean applicable = project != null
      && file != null
      && !file.isDirectory()
      && HaxeBuildFileScanner.detectType(project, file) != null
      && !HaxeKnownBuildFiles.isKnown(project, file.getPath());
    e.getPresentation().setEnabledAndVisible(applicable);
  }

  @Override
  public void actionPerformed(@NotNull AnActionEvent e) {
    Project project = e.getProject();
    VirtualFile file = e.getData(CommonDataKeys.VIRTUAL_FILE);
    if (project == null || file == null) return;

    List<String> containerIds = HaxeContainers.containerIds(project);
    String owner = HaxeContainers.containerIdFor(project, file);
    String preselected = containerIds.contains(owner) ? owner : containerIds.getFirst();
    if (containerIds.size() == 1) {
      HaxeKnownBuildFiles.registerBuildFile(project, preselected, file, null);
      return;
    }
    JBPopupFactory.getInstance()
      .createPopupChooserBuilder(containerIds)
      .setTitle(HaxeBundle.message("haxe.register.build.file.module.chooser.title"))
      .setRenderer(BuilderKt.textListCellRenderer("", id -> HaxeContainers.isProjectRoot(id) ? project.getName() : id))
      .setSelectedValue(preselected, true)
      .setItemChosenCallback(containerId -> HaxeKnownBuildFiles.registerBuildFile(project, containerId, file, null))
      .createPopup()
      .showInBestPositionFor(e.getDataContext());
  }

  @Override
  public @NotNull ActionUpdateThread getActionUpdateThread() {
    return ActionUpdateThread.BGT;
  }
}
