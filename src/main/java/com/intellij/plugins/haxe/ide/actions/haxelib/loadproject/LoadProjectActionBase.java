package com.intellij.plugins.haxe.ide.actions.haxelib.loadproject;

import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.plugins.haxe.ide.module.HaxeModuleType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class LoadProjectActionBase extends AnAction {

  abstract boolean isProjectFile(VirtualFile file);

  abstract void performProjectImport(Project project, VirtualFile file);

  @Override
  public void actionPerformed(@NotNull AnActionEvent e) {
    VirtualFile file = (VirtualFile)e.getDataContext().getData("virtualFile");
    performProjectImport(e.getProject(), file);
  }


  @Override
  public void update(@NotNull AnActionEvent event) {
    super.update(event);
    Presentation p = event.getPresentation();
    p.setEnabled(isAvailable(event));
    p.setVisible(isVisible(event));
  }

  protected boolean isAvailable(AnActionEvent e) {
    return getProject(e) != null && isVisible(e);
  }

  protected boolean isVisible(AnActionEvent e) {
    return switch (e.getPlace()) {
      case "ProjectViewPopup" -> isProjectFile(e) && !hasHaxeModuleLoaded(e);
      default -> false;
    };
  }


  private static boolean hasHaxeModuleLoaded(AnActionEvent e) {
    Module module = (Module)e.getDataContext().getData("module");
    return module!= null && ModuleType.get(module) == HaxeModuleType.getInstance();
  }

  private boolean isProjectFile(AnActionEvent e) {
    VirtualFile data = (VirtualFile)e.getDataContext().getData("virtualFile");
    return isProjectFile(data);
  }


  @Nullable
  private Project getProject(AnActionEvent e) {
    DataContext context = e.getDataContext();
    return CommonDataKeys.PROJECT.getData(context);
  }
}
