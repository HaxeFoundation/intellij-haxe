package com.intellij.plugins.haxe.v2.buildtools;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.plugins.haxe.v2.buildsystem.HaxeBuildFile;
import com.intellij.plugins.haxe.v2.buildsystem.HaxeBuildFileType;
import com.intellij.plugins.haxe.v2.buildtools.server.HaxeCompilationServerManager;
import com.intellij.plugins.haxe.v2.buildtools.settings.HaxeBuildToolSettings;
import com.intellij.plugins.haxe.v2.buildtools.settings.HaxeEnvironmentStore;
import com.intellij.util.execution.ParametersListUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * User-typed custom action commands: variable expansion and parsing into a
 * runnable argv.
 */
public final class HaxeCustomCommands {

  /** The variable {@link #expandTarget} resolves to the file's selected target flag. */
  public static final String TARGET_VARIABLE = "${target}";
  /** The execution-time variable {@link #expandServerPort} resolves; resolve and display keep it literal. */
  public static final String SERVER_PORT_VARIABLE = "${serverPort}";
  /** The variable {@link #expandRoots} resolves to the container's root directory. */
  public static final String MODULE_ROOT_VARIABLE = "${moduleRoot}";
  /** The variable {@link #expandRoots} resolves to the project's base directory. */
  public static final String PROJECT_ROOT_VARIABLE = "${projectRoot}";
  /** The variable {@link #expandTools} resolves to the container SDK's haxe executable. */
  public static final String HAXE_VARIABLE = "${haxe}";
  /** The variable {@link #expandTools} resolves to the haxelib executable the build tools use. */
  public static final String HAXELIB_VARIABLE = "${haxelib}";

  private HaxeCustomCommands() {
  }

  /**
   * Expands {@code ${target}} to the file's currently selected target flag
   * (e.g. "windows", "html5"), so one action follows the target switcher the
   * way the default actions do. Types without a selectable target (hxml
   * declares its own, a plain hxp script decides in code) keep the literal,
   * making the unapplied variable visible instead of silently vanishing.
   */
  @NotNull
  public static String expandTarget(@NotNull Project project,
                                    @NotNull VirtualFile file,
                                    @NotNull HaxeBuildFileType type,
                                    @NotNull String command) {
    if (!command.contains(TARGET_VARIABLE)) return command;
    String targetFlag = HaxeBuildSystem.of(type).selectedTargetFlag(project, new HaxeBuildFile(file, type));
    return targetFlag == null ? command : command.replace(TARGET_VARIABLE, targetFlag);
  }

  /**
   * Expands {@code ${serverPort}} to the container's compilation-server port,
   * starting the server when needed. The port is ephemeral (a restart picks a
   * new one), so it can only resolve at EXECUTION time — never during resolve
   * or tree display, which run under read actions and must not launch
   * processes. With the server feature disabled, or the server failing to
   * start, the literal stays visible instead of silently vanishing.
   */
  @NotNull
  public static List<String> expandServerPort(@NotNull Project project,
                                              @NotNull String containerId,
                                              @NotNull List<String> command) {
    boolean wantsPort = command.stream().anyMatch(argument -> argument.contains(SERVER_PORT_VARIABLE));
    if (!wantsPort) return command;
    if (!HaxeBuildToolSettings.getInstance(project).isCompilationServerEnabled()) return command;

    String environmentSdk = HaxeEnvironmentStore.getInstance(project).getSdkName(containerId);
    int port = HaxeCompilationServerManager.getInstance(project).ensureRunning(environmentSdk);
    if (port <= 0) return command;
    return command.stream()
      .map(argument -> argument.replace(SERVER_PORT_VARIABLE, String.valueOf(port)))
      .toList();
  }

  /**
   * Expands {@code ${moduleRoot}} and {@code ${projectRoot}} to the
   * container's root and the project's base directory (the same directory
   * for the project-level container). A root that cannot be resolved keeps
   * the literal, making the unapplied variable visible instead of silently
   * vanishing.
   */
  @NotNull
  public static String expandRoots(@NotNull String text, @Nullable String moduleRoot, @Nullable String projectRoot) {
    String expanded = text;
    if (moduleRoot != null) {
      expanded = expanded.replace(MODULE_ROOT_VARIABLE, moduleRoot);
    }
    if (projectRoot != null) {
      expanded = expanded.replace(PROJECT_ROOT_VARIABLE, projectRoot);
    }
    return expanded;
  }

  /**
   * Expands {@code ${haxe}} and {@code ${haxelib}} to the executables of the
   * container's SDK, so a command follows an SDK switch and runs on every
   * platform. A path containing whitespace is quoted, so the command still
   * splits into the same arguments.
   */
  @NotNull
  public static String expandTools(@NotNull Project project, @Nullable String environmentSdk, @NotNull String command) {
    String expanded = command;
    if (expanded.contains(HAXE_VARIABLE)) {
      String haxe = HaxeToolPathResolver.resolveHaxeExecutable(project, environmentSdk);
      expanded = expanded.replace(HAXE_VARIABLE, quoted(haxe));
    }
    if (expanded.contains(HAXELIB_VARIABLE)) {
      String haxelib = HaxeToolPathResolver.resolveHaxelibExecutable(project, environmentSdk);
      expanded = expanded.replace(HAXELIB_VARIABLE, quoted(haxelib));
    }
    return expanded;
  }

  /**
   * The variable standing for {@code executable} when it is the container
   * SDK's haxe or haxelib, else the executable itself: a copied command then
   * follows the SDK instead of naming one installation's path.
   */
  @NotNull
  public static String toolVariableFor(@NotNull Project project, @Nullable String environmentSdk, @NotNull String executable) {
    if (executable.equals(HaxeToolPathResolver.resolveHaxeExecutable(project, environmentSdk))) return HAXE_VARIABLE;
    if (executable.equals(HaxeToolPathResolver.resolveHaxelibExecutable(project, environmentSdk))) return HAXELIB_VARIABLE;
    return executable;
  }

  /**
   * The inverse of {@link #expandTarget} for a copied command: the first
   * argument after the executable that is the file's selected target flag
   * becomes {@code ${target}}, so the copy follows the target switcher. Extra
   * flags the target adds (such as {@code -64}) stay as they are; a null flag
   * (hxml, hxp scripts) leaves the command unchanged.
   */
  @NotNull
  public static List<String> withTargetVariable(@NotNull List<String> arguments, @Nullable String targetFlag) {
    if (targetFlag == null || arguments.size() < 2) return arguments;
    int index = arguments.subList(1, arguments.size()).indexOf(targetFlag);
    if (index < 0) return arguments;
    List<String> result = new ArrayList<>(arguments);
    result.set(index + 1, TARGET_VARIABLE);
    return result;
  }

  /** A typed command (its {@code ${target}} already expanded) as a runnable argv: tool and root variables expanded, then parsed. */
  @NotNull
  public static List<String> commandLine(@NotNull Project project,
                                         @Nullable String environmentSdk,
                                         @NotNull String command,
                                         @Nullable String moduleRoot,
                                         @Nullable String projectRoot) {
    String withTools = expandTools(project, environmentSdk, command);
    return parse(expandRoots(withTools, moduleRoot, projectRoot));
  }

  /** The value as one command-line argument: wrapped in double quotes when it contains whitespace. */
  @NotNull
  static String quoted(@NotNull String value) {
    return value.chars().anyMatch(Character::isWhitespace) ? "\"" + value + "\"" : value;
  }

  /**
   * A custom row's working directory: blank falls back to the row kind's
   * default, anything else expands the root variables.
   */
  @Nullable
  public static String resolveWorkDirectory(@NotNull String workDirectory, @Nullable String fallback,
                                            @Nullable String moduleRoot, @Nullable String projectRoot) {
    if (StringUtil.isEmptyOrSpaces(workDirectory)) return fallback;
    return expandRoots(workDirectory, moduleRoot, projectRoot);
  }

  /** Parses a user-typed command into its arguments; the program name is resolved through PATH only when launched. */
  @NotNull
  public static List<String> parse(@NotNull String command) {
    return ParametersListUtil.parse(command);
  }
}
