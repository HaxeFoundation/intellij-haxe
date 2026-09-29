package com.intellij.plugins.haxe.v2.buildtools;

import com.intellij.openapi.project.Project;
import com.intellij.plugins.haxe.config.HaxeTarget;
import com.intellij.plugins.haxe.v2.buildsystem.HaxeBuildFile;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** Plain hxml builds: one build action, the target read from the selected section. */
final class HxmlBuildSystem implements HaxeBuildSystem {
  static final HxmlBuildSystem INSTANCE = new HxmlBuildSystem();

  private HxmlBuildSystem() {
  }

  @Override
  public @NotNull String defaultBuildActionName() {
    return HxmlProjects.BUILD_ACTION;
  }

  @Override
  public @NotNull List<String> defaultActionNames() {
    return List.of(HxmlProjects.BUILD_ACTION);
  }

  @Override
  public @Nullable List<String> actionCommand(@NotNull Project project, @Nullable String environmentSdk,
                                              @NotNull HaxeBuildFile buildFile, @NotNull String actionName) {
    if (!HxmlProjects.isBuildAction(actionName)) return null;
    return HxmlProjects.buildCommand(project, environmentSdk, buildFile.file());
  }

  @Override
  public @NotNull String presentableCommand(@NotNull Project project, @NotNull HaxeBuildFile buildFile, @NotNull String actionName) {
    return "haxe " + buildFile.file().getName();
  }

  @Override
  public @Nullable HaxeTarget launchTarget(@NotNull Project project, @NotNull HaxeBuildFile buildFile) {
    return HaxeBuildSections.inspectSelected(project, buildFile).target();
  }

  @Override
  public @Nullable List<String> debugCompileAdditions(@NotNull Project project, @NotNull HaxeBuildFile buildFile) {
    HaxeTarget target = launchTarget(project, buildFile);
    return target != null ? HaxeDebugAdditions.forTarget(target) : null;
  }
}
