package com.intellij.plugins.haxe.ide.actions.haxelib;

import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.module.ModuleType;
import com.intellij.openapi.module.ModuleUtil;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.plugins.haxe.buildsystem.hxml.psi.HXMLFile;
import com.intellij.plugins.haxe.buildsystem.lime.LimeOpenFlUtil;
import com.intellij.plugins.haxe.haxelib.HaxelibCacheManager;
import com.intellij.plugins.haxe.haxelib.HaxelibProjectUpdater;
import com.intellij.plugins.haxe.haxelib.HaxelibUtil;
import com.intellij.plugins.haxe.haxelib.ModuleLibraryCache;
import com.intellij.plugins.haxe.ide.module.HaxeModuleType;
import com.intellij.psi.xml.XmlFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Objects;

public class UpdateHaxeLibCacheAction extends AnAction implements DumbAware {
  @Override
  public @NotNull ActionUpdateThread getActionUpdateThread() {
    return  ActionUpdateThread.BGT;
  }

  @Override
  public void update(@NotNull AnActionEvent event) {
    Presentation p = event.getPresentation();
    p.setEnabled(isAvailable(event));
    p.setVisible(isVisible(event));
  }

  @Override
  public void actionPerformed(@NotNull AnActionEvent event) {
    HaxelibUtil.clearCache();
    Project project = event.getProject();

    Collection<Module> modules = ModuleUtil.getModulesOfType(project, HaxeModuleType.getInstance());
    modules.stream()
      .map(HaxelibProjectUpdater::getLibraryCache)
      .filter(Objects::nonNull)
      .forEach(ModuleLibraryCache::reload);

    HaxelibCacheManager.getAllInstances().forEach(HaxelibCacheManager::reload);
  }

  protected boolean isAvailable(AnActionEvent e) {
    Project project = getProject(e);
    return hasHaxeModules(project) && isVisible(e);
  }

  private boolean hasHaxeModules(Project project) {
    if (project == null) return false;
    Module[] modules = ModuleManager.getInstance(project).getModules();
    for (Module module : modules) {
      if (ModuleType.get(module) == HaxeModuleType.getInstance()) return true;
    }
    return false;
  }

  protected boolean isVisible(AnActionEvent e) {
    Project project = getProject(e);
    return switch (e.getPlace()) {
      case "EditorPopup" -> isProjectFile(e);
      case "ProjectViewPopup" ->  hasHaxeModules(project);
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
