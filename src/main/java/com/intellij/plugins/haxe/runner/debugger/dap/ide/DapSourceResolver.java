package com.intellij.plugins.haxe.runner.debugger.dap.ide;

import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.plugins.haxe.util.HaxeExternalPaths;
import com.intellij.psi.search.FilenameIndex;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.xdebugger.XDebuggerUtil;
import com.intellij.xdebugger.XSourcePosition;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import org.jetbrains.annotations.Nullable;

/**
 * Maps a source path reported by a debug adapter (the path as the compiler
 * saw it — usually project-relative like {@code src/Main.hx}, sometimes
 * absolute) to an IDE source position, in two steps: {@link #findUnderRoots},
 * then {@link #findByName}. A backend with a lookup of its own in between
 * (HashLink's frame names) calls the two itself.
 */
public final class DapSourceResolver {
  private DapSourceResolver() {
  }

  /** Resolves to a position, or null when the file cannot be found. 1-based line. */
  public static @Nullable XSourcePosition resolve(Project project, @Nullable String path, int line) {
    return resolve(project, path, line, List.of());
  }

  /**
   * Resolves to a position, or null when the file cannot be found. 1-based
   * line. {@code sourceDirectories} — the build's classpath roots — resolve a
   * relative path directly and break name ties in their favor: the reported
   * relative names are ambiguous whenever a sibling project in the same IDE
   * project has a same-named file.
   */
  public static @Nullable XSourcePosition resolve(Project project,
                                                  @Nullable String path,
                                                  int line,
                                                  List<String> sourceDirectories) {
    if (path == null || path.isBlank()) {
      return null;
    }
    String normalized = FileUtil.toSystemIndependentName(path);
    // synchronous read on the DAP request/pump thread (never the EDT);
    // nonBlocking + executeSynchronously retries around write actions
    return ReadAction.nonBlocking(() -> {
      VirtualFile file = findFile(project, normalized, sourceDirectories);
      return file != null ? XDebuggerUtil.getInstance().createPosition(file, Math.max(0, line - 1)) : null;
    }).executeSynchronously();
  }

  private static @Nullable VirtualFile findFile(Project project, String normalized, List<String> sourceDirectories) {
    VirtualFile underRoots = findUnderRoots(normalized, sourceDirectories);
    return underRoots != null ? underRoots : findByName(project, normalized, sourceDirectories);
  }

  /**
   * The path as written when absolute, else the relative name resolved
   * directly against the build's own classpath roots: the fastest and most
   * precise answer. Null when neither exists.
   */
  public static @Nullable VirtualFile findUnderRoots(String normalized, List<String> sourceDirectories) {
    LocalFileSystem localFs = LocalFileSystem.getInstance();
    if (isAbsolute(normalized)) {
      VirtualFile absolute = localFs.findFileByPath(normalized);
      if (absolute != null) {
        return absolute;
      }
    }
    for (String directory : sourceDirectories) {
      VirtualFile underRoot = localFs.findFileByPath(directory + "/" + normalized);
      if (underRoot != null) {
        return underRoot;
      }
    }
    return null;
  }

  /**
   * A relative (or stale absolute) path by its file name: among the files
   * sharing the name, a full-path suffix match, else any of them; either way
   * one under the build's roots first. Null when no file carries the name.
   * Call in a read action.
   */
  public static @Nullable VirtualFile findByName(Project project, String normalized, List<String> sourceDirectories) {
    String fileName = normalized.substring(normalized.lastIndexOf('/') + 1);
    if (fileName.isBlank()) {
      return null;
    }
    Collection<VirtualFile> candidates =
      FilenameIndex.getVirtualFilesByName(fileName, GlobalSearchScope.allScope(project));
    VirtualFile suffixMatch = null;
    VirtualFile byName = null;
    boolean byNameUnderRoots = false;
    for (VirtualFile candidate : candidates) {
      String candidatePath = candidate.getPath();
      boolean matchesSuffix = candidatePath.endsWith("/" + normalized) || candidatePath.equals(normalized);
      boolean underRoots = DapSourceScopes.underAny(candidatePath, sourceDirectories);
      if (matchesSuffix && underRoots) {
        return candidate;
      }
      if (matchesSuffix && suffixMatch == null) {
        suffixMatch = candidate;
      }
      boolean betterByName = byName == null || (underRoots && !byNameUnderRoots);
      if (betterByName) {
        byName = candidate;
        byNameUnderRoots = underRoots;
      }
    }
    return suffixMatch != null ? suffixMatch : byName;
  }

  // server-supplied strings are not always paths ("?" for native frames); one
  // naming no usable path is not absolute either
  private static boolean isAbsolute(String normalized) {
    Path path = HaxeExternalPaths.pathOf(normalized);
    return path != null && path.isAbsolute();
  }
}
