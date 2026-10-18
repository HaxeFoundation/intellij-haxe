package com.intellij.plugins.haxe.v2.buildtools;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.plugins.haxe.config.HaxeTarget;
import com.intellij.plugins.haxe.v2.buildsystem.HaxeBuildFile;
import com.intellij.plugins.haxe.v2.buildtools.HaxeDebugAdditions.Debugger;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** Plain hxml builds: a build action, plus Clean for a C++ build; the target read from the selected section. */
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
  public @NotNull List<String> actionNames(@NotNull Project project, @NotNull HaxeBuildFile buildFile) {
    return HxmlProjects.cppOutput(project, buildFile) != null
           ? List.of(HxmlProjects.BUILD_ACTION, HxmlProjects.CLEAN_ACTION)
           : defaultActionNames();
  }

  @Override
  public @Nullable List<String> actionCommand(@NotNull Project project, @Nullable String environmentSdk,
                                              @NotNull HaxeBuildFile buildFile, @NotNull String actionName) {
    return switch (actionName) {
      case HxmlProjects.BUILD_ACTION -> HxmlProjects.buildCommand(project, environmentSdk, buildFile.file());
      case HxmlProjects.CLEAN_ACTION -> cleanCommand(project, environmentSdk, buildFile);
      default -> null;
    };
  }

  @Override
  public @Nullable String actionWorkDirectory(@NotNull Project project, @NotNull HaxeBuildFile buildFile, @NotNull String actionName) {
    if (!HxmlProjects.CLEAN_ACTION.equals(actionName)) return null;
    String cppOutput = HxmlProjects.cppOutput(project, buildFile);
    return cppOutput == null ? null : HaxeBuildWorkDirectories.resolve(project, buildFile.file(), cppOutput).toString();
  }

  @Override
  public @NotNull String presentableCommand(@NotNull Project project, @NotNull HaxeBuildFile buildFile, @NotNull String actionName) {
    if (HxmlProjects.CLEAN_ACTION.equals(actionName)) {
      return "hxcpp clean " + StringUtil.notNullize(HxmlProjects.cppOutput(project, buildFile));
    }
    return "haxe " + buildFile.file().getName();
  }

  /** hxcpp's clean, while the selected section still builds C++. */
  @Nullable
  private static List<String> cleanCommand(@NotNull Project project, @Nullable String environmentSdk, @NotNull HaxeBuildFile buildFile) {
    if (HxmlProjects.cppOutput(project, buildFile) == null) return null;
    return HxmlProjects.cleanCommand(project, environmentSdk);
  }

  @Override
  public @Nullable HaxeTarget launchTarget(@NotNull Project project, @NotNull HaxeBuildFile buildFile) {
    return HaxeBuildSections.inspectSelected(project, buildFile).target();
  }

  @Override
  public @Nullable List<String> debugCompileAdditions(@NotNull Project project, @NotNull HaxeBuildFile buildFile,
                                                      @NotNull Debugger debugger) {
    HaxeTarget target = launchTarget(project, buildFile);
    return target != null ? HaxeDebugAdditions.forTarget(target, debugger) : null;
  }
}
