package com.intellij.plugins.haxe.v2.buildtools;

import com.intellij.openapi.project.Project;
import com.intellij.plugins.haxe.config.HaxeTarget;
import com.intellij.plugins.haxe.v2.buildsystem.HaxeBuildFile;
import com.intellij.plugins.haxe.v2.buildtools.HaxeDebugAdditions.Debugger;
import java.util.ArrayList;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** Lime and openfl projects (xml or hxp): the lime tool's actions against the selected target. */
final class LimeBuildSystem implements HaxeBuildSystem {
  static final LimeBuildSystem INSTANCE = new LimeBuildSystem();

  private LimeBuildSystem() {
  }

  @Override
  public @NotNull String defaultBuildActionName() {
    return LimeProjects.BUILD_ACTION;
  }

  @Override
  public @NotNull List<String> defaultActionNames() {
    return LimeProjects.DEFAULT_ACTIONS;
  }

  @Override
  public @Nullable List<String> actionCommand(@NotNull Project project, @Nullable String environmentSdk,
                                              @NotNull HaxeBuildFile buildFile, @NotNull String actionName) {
    if (!LimeProjects.DEFAULT_ACTIONS.contains(actionName)) return null;
    return LimeProjects.actionCommand(project, environmentSdk, buildFile.file(), buildFile.type(), actionName);
  }

  @Override
  public @NotNull String presentableCommand(@NotNull Project project, @NotNull HaxeBuildFile buildFile, @NotNull String actionName) {
    List<String> targetFlags = LimeProjects.selectedTargetFlags(project, buildFile.type(), buildFile.file());
    return LimeProjects.toolFor(buildFile.type()) + " " + actionName + " " + String.join(" ", targetFlags);
  }

  @Override
  public @NotNull String selectedTargetFlag(@NotNull Project project, @NotNull HaxeBuildFile buildFile) {
    return LimeProjects.selectedTargetFlag(project, buildFile.type(), buildFile.file());
  }

  @Override
  public @Nullable HaxeTarget launchTarget(@NotNull Project project, @NotNull HaxeBuildFile buildFile) {
    return LimeProjects.targetFor(selectedTargetFlag(project, buildFile));
  }

  /**
   * The lime tool takes -debug itself and forwards it into the haxe build it
   * generates, so one flag covers every lime target. hxcpp debugging also
   * needs the debugger's in-debuggee server compiled in; lime's --haxelib
   * override merges the lib exactly like a project {@code <haxelib>} entry
   * (include.xml and extraParams included), so the project file stays
   * untouched. A tool define ({@code -Dfdb}) becomes a haxedef of the
   * generated build.
   */
  @Override
  public @NotNull List<String> debugCompileAdditions(@NotNull Project project, @NotNull HaxeBuildFile buildFile,
                                                     @NotNull Debugger debugger) {
    List<String> additions = new ArrayList<>();
    additions.add("-debug");
    String serverLib = debugger.hxcppServerLib();
    if (serverLib != null && DESKTOP_CPP_TARGETS.contains(selectedTargetFlag(project, buildFile))) {
      additions.add("--haxelib=" + serverLib);
    }
    if (debugger.flashDebuggerTag() && launchTarget(project, buildFile) == HaxeTarget.FLASH) {
      additions.add("-Dfdb");
    }
    return additions;
  }
}
