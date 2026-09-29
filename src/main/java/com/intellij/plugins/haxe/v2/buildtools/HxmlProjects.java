package com.intellij.plugins.haxe.v2.buildtools;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/// Facts about plain hxml builds: the direct `haxe <file>` compile and
/// its single default action. The lime counterpart is [LimeProjects].
public final class HxmlProjects {

  /** The hxml default action's name - doubles as its identifier in stored configurations. */
  public static final String BUILD_ACTION = "Build";

  private HxmlProjects() {
  }

  /** Whether a stored action name means the hxml build ("compile" is the action's pre-rename identifier). */
  public static boolean isBuildAction(@NotNull String actionName) {
    return actionName.equals(BUILD_ACTION) || actionName.equals("compile");
  }

  /// The direct compile: `haxe <file>`, run in the file's work directory
  /// (see [HaxeBuildWorkDirectories]) with the file referenced relative to it.
  @NotNull
  public static List<String> buildCommand(@NotNull Project project,
                                          @Nullable String environmentSdk,
                                          @NotNull VirtualFile file) {
    String fileArgument = HaxeBuildWorkDirectories.fileArgument(project, file);
    return List.of(HaxeToolPathResolver.resolveHaxeExecutable(project, environmentSdk), fileArgument);
  }

  /// Whether the command invokes the haxe compiler directly (and so takes `--connect` right after the executable).
  public static boolean isDirectHaxeCommand(@NotNull Project project,
                                            @Nullable String environmentSdk,
                                            @NotNull List<String> command) {
    return command.get(0).equals(HaxeToolPathResolver.resolveHaxeExecutable(project, environmentSdk));
  }

  // TODO: --next chains: a "Build (section)" action beside the whole-file Build
  //  (which compiles every section) - this scoping method is the building block
  /// A multi-section hxml compile scoped to the SELECTED `--next` section: the
  /// file argument is replaced by that section's lines as compiler arguments.
  /// haxe applies trailing CLI arguments to the LAST section of a chained
  /// file, so flags appended after the file (test reporting, debug additions)
  /// would miss every other section; compiling just the one section makes the
  /// trailing flags its own — and skips the unrelated sibling builds.
  /// Single-section files, and commands not carrying the file argument, come
  /// back unchanged. Call in a read action.
  @NotNull
  public static List<String> scopeToSelectedSection(@NotNull Project project,
                                                    @NotNull VirtualFile file,
                                                    @NotNull List<String> command) {
    int fileArgumentIndex = command.indexOf(HaxeBuildWorkDirectories.fileArgument(project, file));
    if (fileArgumentIndex < 0) return command;
    List<String> sectionArguments = HaxeBuildSections.selectedSectionArguments(project, file);
    if (sectionArguments == null) return command;

    List<String> scoped = new ArrayList<>(command.subList(0, fileArgumentIndex));
    scoped.addAll(sectionArguments);
    scoped.addAll(command.subList(fileArgumentIndex + 1, command.size()));
    return scoped;
  }
}
