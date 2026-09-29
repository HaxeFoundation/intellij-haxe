package com.intellij.plugins.haxe.v2.display;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.plugins.haxe.v2.buildtools.HaxeContainers;
import com.intellij.plugins.haxe.v2.buildtools.settings.HaxeBuildToolSettings;
import com.intellij.plugins.haxe.v2.buildtools.settings.HaxeEnvironmentStore;
import org.jetbrains.annotations.NotNull;

/**
 * Whether the compiler can serve a file, as compiler-only completion and the
 * compiler-backed Find Usages and navigation require: the project's
 * compilation server is switched on, the file's container has a build
 * command, and the file has a display context. Without a build command the
 * display context can still come from the project's active build file, but
 * that file alone lacks the setup the command adds, so the server's answers
 * would be of no use.
 */
final class HaxeCompilerFeatureAvailability {

  private HaxeCompilerFeatureAvailability() {
  }

  /** Call in a read action; no network. */
  static boolean isAvailable(@NotNull Project project, @NotNull VirtualFile file) {
    boolean serverEnabled = HaxeBuildToolSettings.getInstance(project).isCompilationServerEnabled();
    return serverEnabled && hasBuildCommand(project, file) && HaxeCompilerDisplayService.getInstance(project).contextFor(file) != null;
  }

  private static boolean hasBuildCommand(@NotNull Project project, @NotNull VirtualFile file) {
    String containerId = HaxeContainers.containerIdFor(project, file);
    return HaxeEnvironmentStore.getInstance(project).getCompileCommand(containerId) != null;
  }
}
