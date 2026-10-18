package com.intellij.plugins.haxe.v2.buildtools;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.project.ProjectUtil;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.plugins.haxe.util.HaxeReadActions;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

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
   * The container's root directory: the module's first content root; the
   * project base dir for the project-root container (or when the id names no
   * module).
   */
  @Nullable
  public static VirtualFile containerRootDir(@NotNull Project project, @NotNull String containerId) {
    Module module = ModuleManager.getInstance(project).findModuleByName(containerId);
    if (module != null) {
      VirtualFile[] contentRoots = ModuleRootManager.getInstance(module).getContentRoots();
      if (contentRoots.length > 0) return contentRoots[0];
    }
    return ProjectUtil.guessProjectDir(project);
  }

  /** {@link #containerRootDir} as a path. */
  @Nullable
  public static String containerRootPath(@NotNull Project project, @NotNull String containerId) {
    VirtualFile root = containerRootDir(project, containerId);
    return root == null ? null : root.getPath();
  }

  /** The project base directory's path. */
  @Nullable
  public static String projectRootPath(@NotNull Project project) {
    VirtualFile baseDir = ProjectUtil.guessProjectDir(project);
    return baseDir == null ? null : baseDir.getPath();
  }

  /**
   * The ids of the containers the tool window can show: the project node's
   * container first, then every other module.
   */
  @NotNull
  public static List<String> containerIds(@NotNull Project project) {
    String projectNode = projectNodeContainerId(project);
    List<String> ids = new ArrayList<>();
    ids.add(projectNode);
    for (Module module : ModuleManager.getInstance(project).getModules()) {
      if (!module.getName().equals(projectNode)) ids.add(module.getName());
    }
    return ids;
  }

  /**
   * The id of the container the tool window's project node stands for: the
   * module owning the project base dir, else the project-root container.
   * Project-wide tool runs (installs, the installed-library list) use it, so
   * they follow that container's environment SDK. Takes the read lock when
   * the caller holds none.
   */
  @NotNull
  public static String projectNodeContainerId(@NotNull Project project) {
    Module rootModule = HaxeReadActions.compute(() -> projectRootModule(project));
    return rootModule != null ? rootModule.getName() : PROJECT_ROOT_CONTAINER;
  }

  /**
   * The module whose content root is the project base dir, or null: that
   * module IS the project, its build files belong to the project node and it
   * gets no module row of its own.
   */
  @Nullable
  public static Module projectRootModule(@NotNull Project project) {
    VirtualFile baseDir = ProjectUtil.guessProjectDir(project);
    return baseDir == null ? null : ProjectFileIndex.getInstance(project).getModuleForFile(baseDir);
  }
}
