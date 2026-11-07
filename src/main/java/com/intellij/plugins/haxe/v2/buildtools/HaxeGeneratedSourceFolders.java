package com.intellij.plugins.haxe.v2.buildtools;

import com.intellij.openapi.project.Project;
import com.intellij.plugins.haxe.v2.buildsystem.HaxeBuildFile;
import com.intellij.plugins.haxe.v2.buildsystem.HaxeBuildFileInspector;
import com.intellij.plugins.haxe.v2.buildsystem.HaxeBuildFileType;
import com.intellij.plugins.haxe.v2.buildtools.settings.HaxeFrameworkTargetSettings.Framework;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.util.List;

/**
 * The folders a build tool writes generated sources to for a build file's
 * selected target: the generated {@code ApplicationMain}, asset classes and
 * the like. The tool compiles each folder as a class path of its own. Lime
 * and OpenFL use {@code <app path>/<target dir>/haxe} and its
 * {@code _generated} subfolder ({@link LimeProjects#generatedSourceDirectories}),
 * NME uses {@code <app path>/<platform dir>/haxe}
 * ({@link NmeProjects#generatedSourceDirectory}).
 * <p>
 * A build creates the folders and a clean deletes them. The ones that exist
 * count among the build's source directories
 * ({@link HaxeBuildClasspaths#sourceDirectories}), so breakpoints and the
 * resolve context reach their files.
 */
public final class HaxeGeneratedSourceFolders {

  private HaxeGeneratedSourceFolders() {
  }

  /** The framework whose generated-sources setting covers the build file; null for a kind no tool generates sources for. */
  @Nullable
  public static Framework frameworkOf(@NotNull HaxeBuildFileType type) {
    return switch (type) {
      case OPENFL -> Framework.OPENFL;
      case LIME -> Framework.LIME;
      case NMML -> Framework.NME;
      // TODO: an hxp project exports like lime, but its app path comes from code, not from xml
      default -> null;
    };
  }

  /**
   * The build file's generated source folders for its selected target, as
   * absolute paths, whether or not a build has created them yet. Call in a
   * read action.
   */
  @NotNull
  public static List<Path> of(@NotNull Project project, @NotNull HaxeBuildFile buildFile) {
    String content = HaxeBuildFileInspector.loadText(buildFile.file());
    if (content == null) return List.of();
    Path projectDirectory = Path.of(buildFile.file().getParent().getPath());
    return switch (buildFile.type()) {
      case OPENFL, LIME -> limeFolders(project, buildFile, projectDirectory, content);
      case NMML -> nmeFolders(project, buildFile, projectDirectory, content);
      default -> List.of();
    };
  }

  @NotNull
  private static List<Path> limeFolders(@NotNull Project project,
                                        @NotNull HaxeBuildFile buildFile,
                                        @NotNull Path projectDirectory,
                                        @NotNull String content) {
    String targetFlag = LimeProjects.selectedTargetFlag(project, buildFile.type(), buildFile.file());
    return LimeProjects.generatedSourceDirectories(projectDirectory, content, targetFlag);
  }

  @NotNull
  private static List<Path> nmeFolders(@NotNull Project project,
                                       @NotNull HaxeBuildFile buildFile,
                                       @NotNull Path projectDirectory,
                                       @NotNull String content) {
    String targetFlag = NmeProjects.selectedTargetFlag(project, buildFile.file());
    String relative = NmeProjects.generatedSourceDirectory(content, targetFlag);
    if (relative == null) return List.of();
    return List.of(projectDirectory.resolve(relative).normalize());
  }
}
