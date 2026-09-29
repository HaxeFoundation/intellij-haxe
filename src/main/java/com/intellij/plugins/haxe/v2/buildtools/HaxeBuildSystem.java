package com.intellij.plugins.haxe.v2.buildtools;

import com.intellij.openapi.project.Project;
import com.intellij.plugins.haxe.config.HaxeTarget;
import com.intellij.plugins.haxe.v2.buildsystem.HaxeBuildFile;
import com.intellij.plugins.haxe.v2.buildsystem.HaxeBuildFileType;
import java.util.List;
import java.util.Objects;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The per-build-system traits behind one interface, so callers never switch on
 * the build file type themselves: the built-in actions and their commands, the
 * selected target, and what a debug compile adds. Each implementation resolves
 * the file's CURRENT selection itself — the tool window's target row for
 * lime/nme, the selected {@code --next} section for hxml. Whether a resolved
 * target can be DEBUGGED is not a build-system trait: that lives in
 * {@code HaxeDebugSupport}, one home shared by program and test sessions.
 * <p>
 * The dispatch lives in {@code buildtools} rather than on
 * {@link HaxeBuildFileType}: it maps types to build-tool facts, and putting it
 * on the enum would make {@code buildsystem} import {@code buildtools}.
 */
public interface HaxeBuildSystem {

  /** Target ids compiled through hxcpp whose output the HXCPP (IntelliJ) debugger can attach to ("cpp" is nme's host-desktop word; lime and nme share the rest). */
  List<String> DESKTOP_CPP_TARGETS = List.of("windows", "linux", "mac", "cpp");

  /** The build system serving the file type. */
  @NotNull
  static HaxeBuildSystem of(@NotNull HaxeBuildFileType type) {
    return switch (type) {
      case HXML -> HxmlBuildSystem.INSTANCE;
      case OPENFL, LIME, HXP_PROJECT -> LimeBuildSystem.INSTANCE;
      case NMML -> NmeBuildSystem.INSTANCE;
      case HXP_SCRIPT -> HxpScriptBuildSystem.INSTANCE;
    };
  }

  /** The default build action's NAME - the stored identifier a resolve-by-name uses (not the localized label). */
  @NotNull
  String defaultBuildActionName();

  /**
   * The built-in action names, in menu order (custom actions come on top of
   * these). Configurations store and resolve actions by these names, so they
   * are never localized.
   */
  @NotNull
  List<String> defaultActionNames();

  /** A named built-in action's command; null when the name is not one of the built-ins. */
  @Nullable
  List<String> actionCommand(@NotNull Project project, @Nullable String environmentSdk, @NotNull HaxeBuildFile buildFile,
                             @NotNull String actionName);

  /** The short form a tree row shows for a built-in action: the tool, the action and the target flags. */
  @NotNull
  String presentableCommand(@NotNull Project project, @NotNull HaxeBuildFile buildFile, @NotNull String actionName);

  /** The command compiling the file with the default build action. */
  @NotNull
  default List<String> defaultCommand(@NotNull Project project, @Nullable String environmentSdk, @NotNull HaxeBuildFile buildFile) {
    return Objects.requireNonNull(actionCommand(project, environmentSdk, buildFile, defaultBuildActionName()));
  }

  /**
   * The file's currently selected target flag (e.g. "windows", "html5"); null
   * for systems without a selectable target (hxml declares its own, a plain
   * hxp script decides in code).
   */
  @Nullable
  default String selectedTargetFlag(@NotNull Project project, @NotNull HaxeBuildFile buildFile) {
    return null;
  }

  /**
   * The haxe target the build file's current selection compiles to, or null
   * when it declares none (an hxml without a target flag, an hxp script
   * deciding targets in code, an unknown lime/nme target id).
   * Call inside a read action.
   */
  @Nullable
  HaxeTarget launchTarget(@NotNull Project project, @NotNull HaxeBuildFile buildFile);

  /**
   * The extra compile arguments that make the current selection's output
   * debuggable (applied by the before-run compile under the Debug executor
   * only), or null when the selection has no debugger support yet.
   * Call inside a read action.
   */
  @Nullable
  List<String> debugCompileAdditions(@NotNull Project project, @NotNull HaxeBuildFile buildFile);
}
