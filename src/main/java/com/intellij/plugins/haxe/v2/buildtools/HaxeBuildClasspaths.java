package com.intellij.plugins.haxe.v2.buildtools;

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
import java.util.ArrayList;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The directories a build's sources come from: the build file's own directory
 * plus its declared classpaths, resolved against the file's work directory
 * ({@link HaxeBuildWorkDirectories}) — hxml {@code -cp} entries of the selected
 * section; for the lime family the raw {@code <source>}/{@code <classpath>}
 * entries of the project xml (no tool run, so haxelib-provided paths are not
 * seen). Name-based lookups scope themselves to these — test-result
 * navigation and debugger source mapping would otherwise pick a same-named
 * file from a SIBLING project in the same IDE project.
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
   * relative ones against the anchor - or null.
   */
  @Nullable
  static VirtualFile resolveClasspathEntry(@NotNull VirtualFile anchor, @NotNull String entry) {
    String normalized = normalizeEntry(entry);
    VirtualFile resolved = OSAgnosticPathUtil.isAbsolute(normalized)
                           ? LocalFileSystem.getInstance().findFileByPath(normalized)
                           : anchor.findFileByRelativePath(normalized);
    return resolved != null && resolved.isDirectory() ? resolved : null;
  }

  /** Absolute directory paths (VFS separators), or empty when the build file cannot be inspected. Call in a read action. */
  @NotNull
  public static List<String> sourceDirectories(@NotNull Project project, @NotNull String buildFilePath) {
    HaxeBuildFile buildFile = HaxeBuildFileScanner.findBuildFile(project, buildFilePath);
    if (buildFile == null) return List.of();
    List<String> classpaths = HaxeBuildSections.inspectSelected(project, buildFile).classpaths();
    return sourceDirectories(project, buildFile.file(), classpaths);
  }

  /** Same, for a caller that already inspected the build file (no second parse of its selected section). Call in a read action. */
  @NotNull
  public static List<String> sourceDirectories(@NotNull Project project,
                                               @NotNull VirtualFile buildFile,
                                               @NotNull List<String> classpaths) {
    VirtualFile parent = buildFile.getParent();
    VirtualFile anchor = HaxeBuildWorkDirectories.anchor(project, buildFile);
    if (parent == null || anchor == null) return List.of();

    List<String> directories = new ArrayList<>();
    directories.add(parent.getPath());
    for (String classpath : classpaths) {
      VirtualFile resolved = resolveClasspathEntry(anchor, classpath);
      if (resolved != null) {
        directories.add(resolved.getPath());
      }
    }

    // -lib sources live outside the -cp entries; the sync'd managed module
    // libraries carry their resolved roots - include them so a file inside
    // library code stays in scope. The module's union of libraries
    // over-includes, which errs on the safe (fail-open) side.
    Module module = ModuleUtilCore.findModuleForFile(buildFile, project);
    if (module != null) {
      collectManagedLibraryRoots(module, directories);
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
