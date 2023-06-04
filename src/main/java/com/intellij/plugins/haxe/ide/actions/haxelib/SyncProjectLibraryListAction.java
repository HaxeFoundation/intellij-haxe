package com.intellij.plugins.haxe.ide.actions.haxelib;

import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtil;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.plugins.haxe.buildsystem.hxml.psi.HXMLFile;
import com.intellij.plugins.haxe.buildsystem.lime.LimeOpenFlUtil;
import com.intellij.plugins.haxe.haxelib.HaxelibProjectUpdater;
import com.intellij.plugins.haxe.ide.module.HaxeModuleType;
import com.intellij.psi.xml.XmlFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

public class SyncProjectLibraryListAction extends AnAction implements DumbAware {

  @Override
  public void update(@NotNull AnActionEvent event) {
    super.update(event);
    Presentation p = event.getPresentation();
    p.setEnabled(isAvailable(event));
    p.setVisible(isVisible(event));
  }

  @Override
  public void actionPerformed(@NotNull AnActionEvent e) {
    HaxelibProjectUpdater instance = HaxelibProjectUpdater.INSTANCE;
    Project project = getProject(e);
    HaxelibProjectUpdater.ProjectTracker tracker = instance.findProjectTracker(project);

    clearHaxelibCaches(project);

    if(tracker!= null){
      tracker.getCache().clear();
      instance.synchronizeClasspaths(tracker);
    }
  }

  private static void clearHaxelibCaches(Project project) {
    Collection<Module> modules = ModuleUtil.getModulesOfType(project, HaxeModuleType.getInstance());
    modules.forEach(module -> HaxelibProjectUpdater.getLibraryCache(module).reload());
  }

  protected boolean isAvailable(AnActionEvent e) {
    return getProject(e) != null && isVisible(e);
  }

  protected boolean isVisible(AnActionEvent e) {
    return switch (e.getPlace()) {
      case "EditorPopup" -> isProjectFile(e);
      case "ProjectViewPopup" -> true;
      default -> false;
    };
  }

  private static boolean isProjectFile(AnActionEvent e) {
    Object file = e.getDataContext().getData("psi.File");
    if (file instanceof  XmlFile xmlFile) {
      return LimeOpenFlUtil.isLimeFile(xmlFile) || LimeOpenFlUtil.isOpenFlFile(xmlFile);
    }
    return file instanceof HXMLFile;
  }

  @Nullable
  private static Project getProject(AnActionEvent e) {
    DataContext context = e.getDataContext();
    return CommonDataKeys.PROJECT.getData(context);
  }

}
