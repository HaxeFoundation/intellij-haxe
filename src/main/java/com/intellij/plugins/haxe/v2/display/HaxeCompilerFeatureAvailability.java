package com.intellij.plugins.haxe.v2.display;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.plugins.haxe.v2.buildtools.settings.HaxeBuildToolSettings;
import org.jetbrains.annotations.NotNull;

/**
 * Whether the compiler can serve a file, as compiler-only completion, the
 * compiler-backed Find Usages and navigation and the compiler type lookup
 * require: the project's compilation server is switched on and the file has
 * a display context. The context comes from a build file alone (its selected
 * section or target and the IDE overrides), so a module needs no build
 * command for it.
 */
final class HaxeCompilerFeatureAvailability {

  private HaxeCompilerFeatureAvailability() {
  }

  /** Call in a read action; no network. */
  static boolean isAvailable(@NotNull Project project, @NotNull VirtualFile file) {
    if (!HaxeBuildToolSettings.getInstance(project).isCompilationServerEnabled()) return false;
    return HaxeCompilerDisplayService.getInstance(project).contextFor(file) != null;
  }
}
