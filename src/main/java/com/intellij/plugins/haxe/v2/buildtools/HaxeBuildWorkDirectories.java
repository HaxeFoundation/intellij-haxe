package com.intellij.plugins.haxe.v2.buildtools;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.util.io.OSAgnosticPathUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.plugins.haxe.v2.buildsystem.HaxeBuildFile;
import com.intellij.plugins.haxe.v2.buildsystem.HaxeBuildFileScanner;
import com.intellij.plugins.haxe.v2.buildsystem.HaxeBuildFileType;
import com.intellij.plugins.haxe.util.HaxeReadActions;
import com.intellij.plugins.haxe.v2.buildtools.settings.HaxeWorkDirectoryStore;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.util.List;

/**
 * The directory a build file's commands run in and its relative paths resolve
 * against. The per-file override wins. Without one an hxml is sniffed: its own
 * folder when the declared classpaths resolve there, else the CONTENT ROOT
 * when they resolve there — vshaxe resolves hxml paths against the workspace
 * root, and generated configs follow that convention (OpenFL's
 * scripts/completion.hxml declares root-relative classpaths) — else the folder
 * again. The xml-family tools (lime, nme) anchor at the project file's own
 * folder themselves. Safe from any thread — the sniff takes its own read
 * action.
 */
public final class HaxeBuildWorkDirectories {

  private HaxeBuildWorkDirectories() {
  }

  /** The work directory path, falling back to the project base for a rootless file. */
  @Nullable
  public static String workDirectory(@NotNull Project project, @NotNull VirtualFile buildFile) {
    VirtualFile anchor = anchor(project, buildFile);
    return anchor != null ? anchor.getPath() : project.getBasePath();
  }

  /** The work directory as a virtual file (for resolving relative paths against it), or null for a rootless file. */
  @Nullable
  public static VirtualFile anchor(@NotNull Project project, @NotNull VirtualFile buildFile) {
    String override = HaxeWorkDirectoryStore.getInstance(project).getWorkDirectory(buildFile.getPath());
    if (override != null) {
      VirtualFile directory = buildFile.getFileSystem().findFileByPath(override);
      if (directory != null && directory.isDirectory()) {
        return directory;
      }
    }
    return defaultAnchor(project, buildFile);
  }

  /** The sniffed default, ignoring any override (what clearing the override falls back to). */
  @Nullable
  public static VirtualFile defaultAnchor(@NotNull Project project, @NotNull VirtualFile buildFile) {
    return HaxeReadActions.compute(() -> sniffAnchor(project, buildFile));
  }

  @Nullable
  private static VirtualFile sniffAnchor(Project project, VirtualFile buildFile) {
    VirtualFile parent = buildFile.getParent();
    if (parent == null || HaxeBuildFileScanner.detectType(project, buildFile) != HaxeBuildFileType.HXML) {
      return parent;
    }
    VirtualFile contentRoot = ProjectFileIndex.getInstance(project).getContentRootForFile(buildFile);
    if (contentRoot == null || contentRoot.equals(parent)) {
      return parent;
    }
    List<String> classpaths = HaxeBuildSections
      .inspectSelected(project, new HaxeBuildFile(buildFile, HaxeBuildFileType.HXML))
      .classpaths();
    // the file's own folder stays authoritative when its paths resolve there;
    // only paths that resolve solely at the content root mark a root-relative file
    if (anyClasspathUnder(parent, classpaths)) {
      return parent;
    }
    if (anyClasspathUnder(contentRoot, classpaths)) {
      return contentRoot;
    }
    return parent;
  }

  private static boolean anyClasspathUnder(@NotNull VirtualFile directory, @NotNull List<String> classpaths) {
    for (String classpath : classpaths) {
      // an absolute entry resolves the same everywhere - it says nothing
      // about which directory the file's relative paths anchor to
      if (OSAgnosticPathUtil.isAbsolute(HaxeBuildClasspaths.normalizeEntry(classpath))) continue;
      if (HaxeBuildClasspaths.resolveClasspathEntry(directory, classpath) != null) {
        return true;
      }
    }
    return false;
  }

  /**
   * The build-file argument to embed in a command running in {@code workDirectory}:
   * the path relative to it (the bare name when the file sits in it), absolute when
   * no relative form exists (different drive).
   */
  @NotNull
  public static String fileArgument(@NotNull VirtualFile buildFile, @Nullable String workDirectory) {
    if (workDirectory == null) return buildFile.getName();
    try {
      Path relative = Path.of(workDirectory).relativize(Path.of(buildFile.getPath()));
      return relative.toString().replace('\\', '/');
    }
    catch (IllegalArgumentException e) {
      return buildFile.getPath();
    }
  }

  /** The argument for the file's own work directory - the form build commands embed. */
  @NotNull
  public static String fileArgument(@NotNull Project project, @NotNull VirtualFile buildFile) {
    return fileArgument(buildFile, workDirectory(project, buildFile));
  }
}
