package com.intellij.plugins.haxe.v2.toolwindow.actions;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.plugins.haxe.HaxeBundle;
import com.intellij.plugins.haxe.v2.buildsystem.HaxeBuildFileScanner;
import com.intellij.plugins.haxe.v2.buildtools.HaxeKnownBuildFiles;
import com.intellij.plugins.haxe.v2.buildtools.settings.HaxeBuildFilesStore;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;

/**
 * Project-view context menu: registers the selected hxml/project-xml file as a
 * Haxe build file of a module's container — the counterpart of the tool
 * window's Add Build File, reachable from where users actually see the file.
 * The containing module is preselected; with several modules a chooser pops
 * up. Registration triggers the sync pipeline, so libraries and the tree
 * follow immediately.
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
      && !isRegistered(project, file);
    e.getPresentation().setEnabledAndVisible(applicable);
  }

  @Override
  public void actionPerformed(@NotNull AnActionEvent e) {
    Project project = e.getProject();
    VirtualFile file = e.getData(CommonDataKeys.VIRTUAL_FILE);
    if (project == null || file == null) return;

    List<String> moduleNames = Arrays.stream(ModuleManager.getInstance(project).getModules())
      .map(Module::getName)
      .toList();
    if (moduleNames.isEmpty()) return;
    if (moduleNames.size() == 1) {
      HaxeKnownBuildFiles.registerBuildFile(project, moduleNames.getFirst(), file, null);
      return;
    }

    Module owner = ModuleUtilCore.findModuleForFile(file, project);
    String preselected = owner != null ? owner.getName() : moduleNames.getFirst();
    JBPopupFactory.getInstance()
      .createPopupChooserBuilder(moduleNames)
      .setTitle(HaxeBundle.message("haxe.register.build.file.module.chooser.title"))
      .setSelectedValue(preselected, true)
      .setItemChosenCallback(moduleName -> HaxeKnownBuildFiles.registerBuildFile(project, moduleName, file, null))
      .createPopup()
      .showInBestPositionFor(e.getDataContext());
  }

  private static boolean isRegistered(@NotNull Project project, @Nullable VirtualFile file) {
    if (file == null) return false;
    String path = file.getPath();
    HaxeBuildFilesStore store = HaxeBuildFilesStore.getInstance(project);
    for (Module module : ModuleManager.getInstance(project).getModules()) {
      if (store.getAddedPaths(module.getName()).contains(path)) return true;
    }
    return false;
  }

  @Override
  public @NotNull ActionUpdateThread getActionUpdateThread() {
    return ActionUpdateThread.BGT;
  }
}
