package com.intellij.plugins.haxe.v2.buildtools;

import com.intellij.plugins.haxe.util.HaxeExternalPaths;
import com.intellij.plugins.haxe.v2.buildtools.libraries.HaxeLibrarySync;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.OrderEnumerator;
import com.intellij.openapi.roots.OrderRootType;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.io.OSAgnosticPathUtil;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.plugins.haxe.v2.buildsystem.HaxeBuildFile;
import com.intellij.plugins.haxe.v2.buildsystem.HaxeBuildFileScanner;
import com.intellij.plugins.haxe.v2.buildsystem.HaxeBuildFileType;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The directories a build's sources come from. First its declared
 * classpaths, resolved against the file's work directory
 * ({@link HaxeBuildWorkDirectories}): the {@code -cp} entries of an hxml's
 * selected section, or for the lime family the raw
 * {@code <source>}/{@code <classpath>} entries of the project xml (no tool
 * runs, so haxelib-provided paths are not seen). An hxml build also gets its
 * work directory itself, because haxe treats the directory it runs in as an
 * implicit classpath; the file's own folder plays no part unless it IS that
 * directory. A lime-family build also gets its generated source folders
 * ({@link HaxeGeneratedSourceFolders}) once a build has created them.
 * <p>
 * Name-based lookups such as test-result navigation and debugger source
 * mapping scope themselves to these directories; otherwise they would pick
 * a same-named file from a SIBLING project in the same IDE project.
 */
public final class HaxeBuildClasspaths {

  private HaxeBuildClasspaths() {
  }

  /** An hxml classpath entry trimmed and normalized to VFS (forward-slash) separators. */
  @NotNull
  static String normalizeEntry(@NotNull String entry) {
    return FileUtil.toSystemIndependentName(entry.trim());
  }

  /**
   * The entry resolved to an existing DIRECTORY - absolute entries directly,
   * relative ones against the anchor - or null, also for an entry naming no
   * usable path.
   */
  @Nullable
  static VirtualFile resolveClasspathEntry(@NotNull VirtualFile anchor, @NotNull String entry) {
    String normalized = normalizeEntry(entry);
    if (HaxeExternalPaths.pathOf(normalized) == null) return null;
    VirtualFile resolved = OSAgnosticPathUtil.isAbsolute(normalized)
                           ? LocalFileSystem.getInstance().findFileByPath(normalized)
                           : anchor.findFileByRelativePath(normalized);
    return resolved != null && resolved.isDirectory() ? resolved : null;
  }

  /**
   * The build's source directories as absolute paths with VFS separators;
   * empty when the build file cannot be inspected. Call in a read action.
   */
  @NotNull
  public static List<String> sourceDirectories(@NotNull Project project, @NotNull String buildFilePath) {
    HaxeBuildFile buildFile = HaxeBuildFileScanner.findBuildFile(project, buildFilePath);
    if (buildFile == null) return List.of();
    List<String> classpaths = HaxeBuildSections.inspectSelected(project, buildFile).classpaths();
    return sourceDirectories(project, buildFile, classpaths);
  }

  /**
   * {@link #sourceDirectories(Project, String)} for a caller that already
   * inspected the build file, sparing a second parse of its selected
   * section. Call in a read action.
   */
  @NotNull
  public static List<String> sourceDirectories(@NotNull Project project,
                                               @NotNull HaxeBuildFile buildFile,
                                               @NotNull List<String> classpaths) {
    VirtualFile anchor = HaxeBuildWorkDirectories.anchor(project, buildFile.file());
    if (anchor == null) return List.of();

    List<String> directories = new ArrayList<>();
    if (buildFile.type() == HaxeBuildFileType.HXML) {
      directories.add(anchor.getPath());
    }
    for (VirtualFile directory : resolvedDirectories(anchor, classpaths)) {
      directories.add(directory.getPath());
    }
    // the tool compiles its generated folders as class paths of their own, so their files are the build's like any other
    for (Path generated : HaxeGeneratedSourceFolders.of(project, buildFile)) {
      if (Files.isDirectory(generated)) directories.add(FileUtil.toSystemIndependentName(generated.toString()));
    }

    // -lib sources live outside the -cp entries; the sync'd managed module
    // libraries carry their resolved roots - include them so a file inside
    // library code stays in scope. The module's union of libraries
    // over-includes, which errs on the safe (fail-open) side.
    Module module = ModuleUtilCore.findModuleForFile(buildFile.file(), project);
    if (module != null) {
      collectManagedLibraryRoots(module, directories);
    }
    return directories;
  }

  /**
   * The build's own classpath entries (its selected section) as existing
   * directories: without the implicit work directory and the library roots
   * {@link #sourceDirectories} adds. Call in a read action.
   */
  @NotNull
  public static List<VirtualFile> declaredDirectories(@NotNull Project project, @NotNull HaxeBuildFile buildFile) {
    VirtualFile anchor = HaxeBuildWorkDirectories.anchor(project, buildFile.file());
    if (anchor == null) return List.of();
    List<String> classpaths = HaxeBuildSections.inspectSelected(project, buildFile).classpaths();
    return resolvedDirectories(anchor, classpaths);
  }

  @NotNull
  static List<VirtualFile> resolvedDirectories(@NotNull VirtualFile anchor, @NotNull List<String> classpaths) {
    List<VirtualFile> directories = new ArrayList<>();
    for (String classpath : classpaths) {
      VirtualFile resolved = resolveClasspathEntry(anchor, classpath);
      if (resolved != null) {
        directories.add(resolved);
      }
    }
    return directories;
  }

  private static void collectManagedLibraryRoots(@NotNull Module module, @NotNull List<String> directories) {
    OrderEnumerator.orderEntries(module).forEachLibrary(library -> {
      String name = library.getName();
      if (name == null || !name.startsWith(HaxeLibrarySync.MANAGED_PREFIX)) {
        return true;
      }
      for (VirtualFile root : library.getFiles(OrderRootType.SOURCES)) {
        if (root.isDirectory()) {
          directories.add(root.getPath());
        }
      }
      return true;
    });
  }
}
