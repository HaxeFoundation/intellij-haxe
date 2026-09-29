package com.intellij.plugins.haxe.v2.toolwindow.actions;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.fileChooser.FileChooser;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectUtil;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.plugins.haxe.HaxeBundle;
import com.intellij.plugins.haxe.v2.buildtools.projectmodel.HaxeModuleWorkspace;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Toolbar action: turn a project subfolder holding a haxe project into a real
 * module, so it shows up as a container in the tree (nested content roots are
 * fine - the platform assigns files to the innermost one).
 */
public final class HaxeAddModuleAction extends DumbAwareAction {

  public HaxeAddModuleAction() {
    super(HaxeBundle.message("haxe.toolwindow.add.module"),
          HaxeBundle.message("haxe.toolwindow.add.module.description"),
          AllIcons.General.Add);
  }

  @Override
  public void actionPerformed(@NotNull AnActionEvent e) {
    Project project = e.getProject();
    if (project == null) return;

    FileChooserDescriptor descriptor = FileChooserDescriptorFactory.singleDir()
      .withTitle(HaxeBundle.message("haxe.toolwindow.add.module.chooser"));
    VirtualFile directory = FileChooser.chooseFile(descriptor, project, ProjectUtil.guessProjectDir(project));
    if (directory == null) return;

    if (ModuleManager.getInstance(project).findModuleByName(directory.getName()) != null) {
      Messages.showErrorDialog(project,
                               HaxeBundle.message("haxe.toolwindow.add.module.exists", directory.getName()),
                               HaxeBundle.message("haxe.toolwindow.add.module"));
      return;
    }
    // module-roots lookups need a read action - the EDT has no implicit read access
    Module owner = ReadAction.computeBlocking(() -> contentRootOwner(project, directory));
    if (owner != null) {
      Messages.showErrorDialog(project,
                               HaxeBundle.message("haxe.toolwindow.add.module.already.root", owner.getName()),
                               HaxeBundle.message("haxe.toolwindow.add.module"));
      return;
    }
    HaxeModuleWorkspace.getInstance(project).addModuleAsync(directory);
  }

  /** The module whose CONTENT ROOT the directory already is, or null (being nested inside a root is fine). */
  @Nullable
  private static Module contentRootOwner(@NotNull Project project, @NotNull VirtualFile directory) {
    for (Module module : ModuleManager.getInstance(project).getModules()) {
      for (VirtualFile root : ModuleRootManager.getInstance(module).getContentRoots()) {
        if (root.equals(directory)) {
          return module;
        }
      }
    }
    return null;
  }

  @Override
  public @NotNull ActionUpdateThread getActionUpdateThread() {
    return ActionUpdateThread.BGT;
  }
}
