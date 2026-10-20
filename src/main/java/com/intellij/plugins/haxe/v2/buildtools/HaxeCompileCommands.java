package com.intellij.plugins.haxe.v2.buildtools;

import com.intellij.plugins.haxe.v2.buildtools.server.HaxeCompilationServerManager;
import com.intellij.plugins.haxe.v2.buildtools.server.HaxeServerAssignment;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.plugins.haxe.HaxeBundle;
import com.intellij.plugins.haxe.v2.buildtools.settings.HaxeBuildToolSettings;
import com.intellij.plugins.haxe.v2.buildtools.settings.HaxeCustomActionsStore;
import com.intellij.plugins.haxe.v2.buildtools.settings.HaxeCustomActionsStore.ActionReference;
import com.intellij.plugins.haxe.v2.buildtools.settings.HaxeActiveBuildFileStore;
import com.intellij.plugins.haxe.v2.buildtools.settings.HaxeEnvironmentStore;
import com.intellij.plugins.haxe.v2.buildtools.settings.HaxeEnvironmentStore.CompileCommand;
import com.intellij.plugins.haxe.v2.buildsystem.HaxeBuildFile;
import com.intellij.plugins.haxe.v2.buildsystem.HaxeBuildFileScanner;
import com.intellij.plugins.haxe.v2.buildsystem.HaxeBuildFileType;
import com.intellij.plugins.haxe.v2.testing.HaxeTestFrameworks;
import com.intellij.util.execution.ParametersListUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Resolves a container's configured build command into a runnable command line,
 * independent of the tool window. Used by the project task runner (IDE Build) and
 * shared connect-injection for the compilation server.
 */
public final class HaxeCompileCommands {

  public record Resolved(@NotNull String containerId,
                         @NotNull List<String> command,
                         @Nullable String workDirectory,
                         @NotNull String presentable,
                         boolean connectEligible) {

    /** A custom action without a command: its run-before chain is the work, nothing runs as the action itself. */
    public boolean chainOnly() {
      return command.isEmpty();
    }
  }

  private HaxeCompileCommands() {
  }

  /**
   * Resolves the container's {@link #effectiveCommand build command}; null
   * when it has none, the file is gone or the type has no build support. Call
   * in a read action.
   */
  @Nullable
  public static Resolved resolve(@NotNull Project project, @NotNull String containerId) {
    CompileCommand compileCommand = effectiveCommand(project, containerId);
    if (compileCommand == null) return null;

    HaxeBuildFile buildFile = HaxeBuildFileScanner.findBuildFile(project, compileCommand.buildFilePath());
    if (buildFile == null) return null;

    String environmentSdk = HaxeEnvironmentStore.getInstance(project).getSdkName(containerId);
    ActionCommand action = compileCommand.actionName() == null
                           ? null
                           : actionCommand(project, environmentSdk, containerId, buildFile, compileCommand.actionName());
    List<String> base = action != null
                        ? action.command()
                        : HaxeBuildSystem.of(buildFile.type()).defaultCommand(project, environmentSdk, buildFile);
    // TODO: the Build command runs one command and never the action's run-before chain, so a chain-only action is unset here
    if (base.isEmpty()) return null;

    String workDirectory = action == null ? null : action.workDirectory();
    return buildResolved(project, containerId, environmentSdk, buildFile.file(), base, compileCommand.arguments(),
                         workDirectory);
  }

  /**
   * The build command the container runs: the one set for it, else its
   * {@link #defaultCommand default}. Null when it skips building or has no
   * build file to default to. Call in a read action.
   */
  @Nullable
  public static CompileCommand effectiveCommand(@NotNull Project project, @NotNull String containerId) {
    HaxeEnvironmentStore store = HaxeEnvironmentStore.getInstance(project);
    if (store.isBuildSkipped(containerId)) return null;
    CompileCommand stored = store.getCompileCommand(containerId);
    if (stored != null) return stored;
    List<HaxeBuildFile> buildFiles = HaxeKnownBuildFiles.forContainer(project, containerId);
    List<String> buildFilePaths = buildFiles.stream()
      .map(buildFile -> buildFile.file().getPath())
      .toList();
    List<String> testsPaths = HaxeTestFrameworks.testsBuildPathsAmong(project, containerId, buildFiles);
    return defaultCommand(project, buildFilePaths, testsPaths);
  }

  /**
   * The build command of a container with none set, among its build files
   * and the tests builds among them: the {@link #defaultBuildFilePath default
   * build file} with its type's default build action and no extra arguments
   * ({@code haxe build.hxml}, {@code lime build <selected target>}). Null
   * when no build file qualifies.
   */
  @Nullable
  public static CompileCommand defaultCommand(@NotNull Project project,
                                              @NotNull List<String> buildFilePaths,
                                              @NotNull List<String> testsPaths) {
    String activePath = HaxeActiveBuildFileStore.getInstance(project).getActiveFilePath();
    String defaultPath = defaultBuildFilePath(buildFilePaths, testsPaths, activePath);
    return defaultPath == null ? null : new CompileCommand(defaultPath, null, "");
  }

  /**
   * The build file a container builds without a build command: the project's
   * active build file when it is one of the container's, else the container's
   * only build file that is not a tests build. Null when neither picks one.
   */
  @Nullable
  static String defaultBuildFilePath(@NotNull List<String> buildFilePaths,
                                     @NotNull List<String> testsPaths,
                                     @Nullable String activePath) {
    if (activePath != null && buildFilePaths.contains(activePath)) return activePath;
    List<String> builds = buildFilePaths.stream()
      .filter(path -> !testsPaths.contains(path))
      .toList();
    return builds.size() == 1 ? builds.getFirst() : null;
  }

  /**
   * Resolves a specific action of a build file into a runnable command line (the
   * run configuration path - the container is derived from the file's module).
   * Null for an unknown action; a custom action without a command resolves
   * {@link Resolved#chainOnly() chain-only}. Call in a read action.
   */
  @Nullable
  public static Resolved resolveAction(@NotNull Project project,
                                       @NotNull String buildFilePath,
                                       @NotNull String actionName,
                                       @NotNull String extraArguments) {
    HaxeBuildFile buildFile = HaxeBuildFileScanner.findBuildFile(project, buildFilePath);
    if (buildFile == null) return null;

    String containerId = HaxeContainers.containerIdFor(project, buildFile.file());
    String environmentSdk = HaxeEnvironmentStore.getInstance(project).getSdkName(containerId);
    ActionCommand action = actionCommand(project, environmentSdk, containerId, buildFile, actionName);
    if (action == null) return null;
    if (action.command().isEmpty()) return chainOnly(containerId, action.workDirectory());
    return buildResolved(project, containerId, environmentSdk, buildFile.file(), action.command(), extraArguments,
                         action.workDirectory());
  }

  /** The resolution of a custom action without a command: nothing to run, connect or extend with arguments. */
  @NotNull
  private static Resolved chainOnly(@NotNull String containerId, @Nullable String workDirectory) {
    return new Resolved(containerId, List.of(), workDirectory, "", false);
  }

  /** An action of a chain, by the label it reads under, with its resolved command. */
  public record NamedAction(@NotNull String name, @NotNull Resolved resolved) {
  }

  /** The actions an action runs before itself, resolved in order, or the problem stopping them. */
  public record RunBefore(@NotNull List<NamedAction> actions, @Nullable String problem) {
  }

  /**
   * Resolves the actions {@code actionName} runs before itself, in order. The
   * problem names a reference that loops back, or one naming no action of the
   * file (a built-in can disappear, as Clean does when the selected section
   * stops building C++). Call in a read action.
   */
  @NotNull
  public static RunBefore resolveRunBefore(@NotNull Project project, @NotNull String buildFilePath, @NotNull String actionName) {
    List<ActionReference> chain = HaxeCustomActionsStore.getInstance(project).runBeforeChain(buildFilePath, actionName);
    if (chain == null) {
      return new RunBefore(List.of(), HaxeBundle.message("haxe.action.chain.cycle", actionName));
    }
    List<NamedAction> actions = new ArrayList<>();
    for (ActionReference reference : chain) {
      String label = HaxeActionReferences.label(project, buildFilePath, reference);
      // each referenced action runs as its own file runs it: its SDK, work directory and variables
      Resolved resolved = resolveAction(project, Objects.requireNonNull(reference.buildFile()), reference.action(), "");
      if (resolved == null) {
        return new RunBefore(List.of(), HaxeBundle.message("haxe.action.chain.missing", actionName, label));
      }
      // a chain-only member's own actions are already in the flattened chain; it runs nothing itself
      if (!resolved.chainOnly()) actions.add(new NamedAction(label, resolved));
    }
    return new RunBefore(List.copyOf(actions), null);
  }

  /** Action names offered for a build file: its built-in actions plus its custom actions. */
  @NotNull
  public static List<String> availableActionNames(@NotNull Project project, @NotNull VirtualFile file) {
    HaxeBuildFileType type = HaxeBuildFileScanner.detectType(project, file);
    List<String> names = new ArrayList<>();
    if (type != null) {
      names.addAll(HaxeBuildSystem.of(type).actionNames(project, new HaxeBuildFile(file, type)));
    }
    for (HaxeCustomActionsStore.CustomAction custom : HaxeCustomActionsStore.getInstance(project).getActions(file.getPath())) {
      if (!names.contains(custom.name())) {
        names.add(custom.name());
      }
    }
    return names;
  }

  @NotNull
  private static Resolved buildResolved(@NotNull Project project,
                                        @NotNull String containerId,
                                        @Nullable String environmentSdk,
                                        @NotNull VirtualFile file,
                                        @NotNull List<String> base,
                                        @NotNull String extraArguments,
                                        @Nullable String workDirectoryOverride) {
    List<String> command = new ArrayList<>(base);
    command.addAll(ParametersListUtil.parse(extraArguments));
    String workDirectory = workDirectoryOverride != null ? workDirectoryOverride
                                                         : HaxeBuildWorkDirectories.workDirectory(project, file);
    boolean eligible = connectEligible(project, environmentSdk, command);
    return new Resolved(containerId, command, workDirectory, String.join(" ", command), eligible);
  }

  /**
   * True when the command can compile through the server: a direct haxe
   * compile or a lime/openfl/nme build. A swf build qualifies too, although a
   * Haxe server since 4.1 may write a broken swf on a recompile of the same
   * build; restarting the server clears it.
   */
  public static boolean connectEligible(@NotNull Project project,
                                        @Nullable String environmentSdk,
                                        @NotNull List<String> command) {
    return HxmlProjects.isDirectHaxeCommand(project, environmentSdk, command)
           || LimeProjects.isToolCommand(command)
           || NmeProjects.isToolCommand(command);
  }

  /// The shared execution-time server step: expands a custom action's
  /// `${serverPort}` variable, then adds `--connect <port>` when the
  /// compilation server is enabled and the container participates - right
  /// after the executable for a direct haxe compile, appended for a
  /// lime/openfl/nme build (the tools forward it to their haxe calls);
  /// otherwise returns the command unchanged.
  @NotNull
  public static List<String> connectIfEnabled(@NotNull Project project,
                                              @NotNull String containerId,
                                              boolean connectEligible,
                                              @NotNull List<String> rawCommand) {
    List<String> command = HaxeCustomCommands.expandServerPort(project, containerId, rawCommand);
    boolean useServer = connectEligible
                        && HaxeBuildToolSettings.getInstance(project).isCompilationServerEnabled()
                        && HaxeEnvironmentStore.getInstance(project).isUsingCompilationServer(containerId);
    if (!useServer) return command;

    String environmentSdk = HaxeEnvironmentStore.getInstance(project).getSdkName(containerId);
    HaxeServerAssignment assignment = HaxeServerAssignment.forContainer(project, containerId);
    int port = HaxeCompilationServerManager.getInstance(project).ensureRunning(assignment);
    if (port <= 0) return command;

    List<String> connected = new ArrayList<>(command);
    List<String> connectArguments = List.of("--connect", String.valueOf(port));
    if (HxmlProjects.isDirectHaxeCommand(project, environmentSdk, command)) {
      connected.addAll(1, connectArguments);
    }
    else if (NmeProjects.isToolCommand(command)) {
      // The nme tool pushes a standalone "--connect" into its haxeflags twice
      // (all versions), so haxe reads the duplicate as the port and fails with
      // "Invalid port". A single "--connect <port>" token dodges the double
      // push and becomes one line of the generated build.hxml, which haxe
      // parses as flag + value.
      connected.add("--connect " + port);
    }
    else {
      connected.addAll(connectArguments);
    }
    return connected;
  }

  /** A resolved action's command, plus a CUSTOM action's work directory (null = the build file's directory). */
  private record ActionCommand(@NotNull List<String> command, @Nullable String workDirectory) {
  }

  /** A named default action's command, or a custom action's; null when unknown. */
  @Nullable
  private static ActionCommand actionCommand(@NotNull Project project,
                                             @Nullable String environmentSdk,
                                             @NotNull String containerId,
                                             @NotNull HaxeBuildFile buildFile,
                                             @NotNull String actionName) {
    HaxeBuildSystem buildSystem = HaxeBuildSystem.of(buildFile.type());
    List<String> defaultAction = buildSystem.actionCommand(project, environmentSdk, buildFile, actionName);
    if (defaultAction != null) {
      return new ActionCommand(defaultAction, buildSystem.actionWorkDirectory(project, buildFile, actionName));
    }

    VirtualFile file = buildFile.file();
    HaxeCustomActionsStore.CustomAction custom = HaxeCustomActionsStore.getInstance(project).findAction(file.getPath(), actionName);
    if (custom == null) return null;

    String moduleRoot = HaxeContainers.containerRootPath(project, containerId);
    String projectRoot = HaxeContainers.projectRootPath(project);
    String expanded = HaxeCustomCommands.expandTarget(project, file, buildFile.type(), custom.command());
    List<String> command = HaxeCustomCommands.commandLine(project, environmentSdk, expanded, moduleRoot, projectRoot);
    String workDirectory = HaxeCustomCommands.resolveWorkDirectory(custom.workDirectory(), null, moduleRoot, projectRoot);
    return new ActionCommand(command, workDirectory);
  }
}
