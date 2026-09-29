package com.intellij.plugins.haxe.v2.buildtools;

import com.intellij.openapi.project.Project;
import com.intellij.plugins.haxe.config.HaxeTarget;
import com.intellij.plugins.haxe.v2.buildsystem.HaxeBuildFile;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** Plain hxp scripts: they generate their compiler arguments in code, so nothing is derived statically. */
final class HxpScriptBuildSystem implements HaxeBuildSystem {
  static final HxpScriptBuildSystem INSTANCE = new HxpScriptBuildSystem();

  private HxpScriptBuildSystem() {
  }

  @Override
  public @NotNull String defaultBuildActionName() {
    return HxpScriptProjects.BUILD_ACTION;
  }

  @Override
  public @NotNull List<String> defaultActionNames() {
    return List.of(HxpScriptProjects.BUILD_ACTION);
  }

  @Override
  public @Nullable List<String> actionCommand(@NotNull Project project, @Nullable String environmentSdk,
                                              @NotNull HaxeBuildFile buildFile, @NotNull String actionName) {
    if (!HxpScriptProjects.BUILD_ACTION.equals(actionName)) return null;
    return HxpScriptProjects.buildCommand(project, environmentSdk, buildFile.file());
  }

  @Override
  public @NotNull String presentableCommand(@NotNull Project project, @NotNull HaxeBuildFile buildFile, @NotNull String actionName) {
    return "hxp " + buildFile.file().getName();
  }

  @Override
  public @Nullable HaxeTarget launchTarget(@NotNull Project project, @NotNull HaxeBuildFile buildFile) {
    return null;
  }

  @Override
  public @Nullable List<String> debugCompileAdditions(@NotNull Project project, @NotNull HaxeBuildFile buildFile) {
    return null;
  }
}
