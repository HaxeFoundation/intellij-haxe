package com.intellij.plugins.haxe.v2.buildtools;

import com.intellij.openapi.project.Project;
import com.intellij.plugins.haxe.config.HaxeTarget;
import com.intellij.plugins.haxe.v2.buildsystem.HaxeBuildFile;
import java.util.ArrayList;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** NME projects: the nme tool's actions against the selected target. */
final class NmeBuildSystem implements HaxeBuildSystem {
  static final NmeBuildSystem INSTANCE = new NmeBuildSystem();

  private NmeBuildSystem() {
  }

  @Override
  public @NotNull String defaultBuildActionName() {
    return NmeProjects.BUILD_ACTION;
  }

  @Override
  public @NotNull List<String> defaultActionNames() {
    return NmeProjects.DEFAULT_ACTIONS;
  }

  @Override
  public @Nullable List<String> actionCommand(@NotNull Project project, @Nullable String environmentSdk,
                                              @NotNull HaxeBuildFile buildFile, @NotNull String actionName) {
    if (!NmeProjects.DEFAULT_ACTIONS.contains(actionName)) return null;
    return NmeProjects.actionCommand(project, environmentSdk, buildFile.file(), actionName);
  }

  @Override
  public @NotNull String presentableCommand(@NotNull Project project, @NotNull HaxeBuildFile buildFile, @NotNull String actionName) {
    return "nme " + actionName + " " + String.join(" ", NmeProjects.selectedTargetFlags(project, buildFile.file()));
  }

  @Override
  public @NotNull String selectedTargetFlag(@NotNull Project project, @NotNull HaxeBuildFile buildFile) {
    return NmeProjects.selectedTargetFlag(project, buildFile.file());
  }

  @Override
  public @Nullable HaxeTarget launchTarget(@NotNull Project project, @NotNull HaxeBuildFile buildFile) {
    return NmeProjects.targetFor(selectedTargetFlag(project, buildFile));
  }

  /**
   * nme has no lime-style --haxelib override: a single-token
   * {@code --library <lib>} haxeflag becomes one line of the generated
   * build.hxml, and haxe pulls the lib with its extraParams (the
   * server-injection macro).
   */
  @Override
  public @NotNull List<String> debugCompileAdditions(@NotNull Project project, @NotNull HaxeBuildFile buildFile) {
    List<String> additions = new ArrayList<>();
    additions.add("-debug");
    if (DESKTOP_CPP_TARGETS.contains(selectedTargetFlag(project, buildFile))) {
      additions.add("--library " + HaxeDebugAdditions.HXCPP_DEBUG_SERVER_LIB);
    }
    return additions;
  }
}
