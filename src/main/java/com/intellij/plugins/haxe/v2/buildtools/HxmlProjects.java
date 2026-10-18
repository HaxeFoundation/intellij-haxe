package com.intellij.plugins.haxe.v2.buildtools;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.plugins.haxe.config.HaxeTarget;
import com.intellij.plugins.haxe.util.HaxeReadActions;
import com.intellij.plugins.haxe.v2.buildsystem.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.UnaryOperator;

/// Facts about plain hxml builds: the direct `haxe <file>` compile, its
/// default action and, for a C++ build, hxcpp's clean. The lime counterpart is
/// [LimeProjects].
public final class HxmlProjects {

  /** The hxml default action's name - doubles as its identifier in stored configurations. */
  public static final String BUILD_ACTION = "Build";
  /** The action cleaning a C++ build's object files; offered only while the selected section targets C++. */
  public static final String CLEAN_ACTION = "Clean";

  private HxmlProjects() {
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

  /// The `-cpp` folder of the selected section as the file spells it; null
  /// when that section does not build C++. Haxe leaves hxcpp's `Build.xml`
  /// there, and hxcpp keeps its object files below it.
  @Nullable
  public static String cppOutput(@NotNull Project project, @NotNull HaxeBuildFile buildFile) {
    HaxeBuildFileInfo info = HaxeReadActions.compute(() -> HaxeBuildSections.inspectSelected(project, buildFile));
    return info.target() == HaxeTarget.CPP ? info.targetOutput() : null;
  }

  /// hxcpp's own clean: the `clean` target of its build tool deletes the
  /// object files, so the next compile rebuilds every one. It runs in the
  /// `-cpp` folder, where the generated `Build.xml` is.
  @NotNull
  public static List<String> cleanCommand(@NotNull Project project, @Nullable String environmentSdk) {
    String haxelib = HaxeToolPathResolver.resolveHaxelibExecutable(project, environmentSdk);
    return List.of(haxelib, "run", "hxcpp", "Build.xml", "clean");
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
    return sectionArguments == null ? command : splice(command, fileArgumentIndex, sectionArguments);
  }

  /**
   * The command with its file argument replaced by the SELECTED section's
   * lines, parsed as compiler arguments and passed through {@code transform}.
   * Unlike {@link #scopeToSelectedSection} it splices whatever the section
   * count: a single-section file inlines its whole content. Null when the
   * command carries no file argument or the section cannot be read. Call in a
   * read action.
   */
  @Nullable
  public static List<String> spliceSelectedSection(@NotNull Project project,
                                                   @NotNull VirtualFile file,
                                                   @NotNull List<String> command,
                                                   @NotNull UnaryOperator<List<String>> transform) {
    int fileArgumentIndex = command.indexOf(HaxeBuildWorkDirectories.fileArgument(project, file));
    if (fileArgumentIndex < 0) return null;
    String section = HaxeBuildSections.selectedSectionContent(project, new HaxeBuildFile(file, HaxeBuildFileType.HXML));
    if (section == null) return null;
    List<String> sectionArguments = transform.apply(HxmlArguments.parseLines(section.lines().toList()));
    return splice(command, fileArgumentIndex, sectionArguments);
  }

  /** The command with the file argument at {@code fileArgumentIndex} replaced by the section's arguments. */
  @NotNull
  private static List<String> splice(@NotNull List<String> command, int fileArgumentIndex, @NotNull List<String> sectionArguments) {
    List<String> spliced = new ArrayList<>(command.subList(0, fileArgumentIndex));
    spliced.addAll(sectionArguments);
    spliced.addAll(command.subList(fileArgumentIndex + 1, command.size()));
    return spliced;
  }
}
