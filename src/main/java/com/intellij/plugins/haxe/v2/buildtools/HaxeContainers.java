package com.intellij.plugins.haxe.v2.buildtools;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectUtil;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Container identity for the v2 build model. A container is what environment
 * settings, compile commands and tool-window sections attach to: a module,
 * or the project root for files outside every module.
 */
public final class HaxeContainers {

  /** Container id for the project root when no module owns the project base dir. */
  public static final String PROJECT_ROOT_CONTAINER = "/project-root";

  private HaxeContainers() {
  }

  /** The container a build file belongs to: its module's name, or the project-root container. */
  @NotNull
  public static String containerIdFor(@NotNull Project project, @NotNull VirtualFile file) {
    Module module = ModuleUtilCore.findModuleForFile(file, project);
    return module != null ? module.getName() : PROJECT_ROOT_CONTAINER;
  }

  /** True for the project-root container (which has no backing module). */
  public static boolean isProjectRoot(@NotNull String containerId) {
    return PROJECT_ROOT_CONTAINER.equals(containerId);
  }

  /**
   * The container's root directory path: the module's first content root;
   * the project base dir for the project-root container (or when the id
   * names no module).
   */
  @Nullable
  public static String containerRootPath(@NotNull Project project, @NotNull String containerId) {
    Module module = ModuleManager.getInstance(project).findModuleByName(containerId);
    if (module != null) {
      VirtualFile[] contentRoots = ModuleRootManager.getInstance(module).getContentRoots();
      if (contentRoots.length > 0) return contentRoots[0].getPath();
    }
    return projectRootPath(project);
  }

  /** The project base directory's path. */
  @Nullable
  public static String projectRootPath(@NotNull Project project) {
    VirtualFile baseDir = ProjectUtil.guessProjectDir(project);
    return baseDir == null ? null : baseDir.getPath();
  }
}
