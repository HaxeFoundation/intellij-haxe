package com.intellij.plugins.haxe.runner.debugger.dap.ide;

import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.search.FilenameIndex;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.xdebugger.XDebuggerUtil;
import com.intellij.xdebugger.XSourcePosition;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import org.jetbrains.annotations.Nullable;

/**
 * Maps a source path reported by the hxcpp-debug-server (the path as the
 * compiler saw it — usually project-relative like {@code src/Main.hx},
 * sometimes absolute) to an IDE source position.
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
    LocalFileSystem localFs = LocalFileSystem.getInstance();
    if (isAbsolute(normalized)) {
      VirtualFile absolute = localFs.findFileByPath(normalized);
      if (absolute != null) {
        return absolute;
      }
    }

    // the fastest and most precise answer: the relative name resolved
    // directly against the build's own classpath roots
    for (String directory : sourceDirectories) {
      VirtualFile underRoot = localFs.findFileByPath(directory + "/" + normalized);
      if (underRoot != null) {
        return underRoot;
      }
    }

    // relative (or stale absolute): find candidates by file name, prefer a
    // full-path suffix match, and among equals one under the build's roots
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

  // Path.of throws InvalidPathException on server-supplied strings that are
  // not paths at all ("?" for native frames); anything unparseable is simply
  // not absolute.
  private static boolean isAbsolute(String normalized) {
    try {
      return Path.of(normalized).isAbsolute();
    } catch (InvalidPathException e) {
      return false;
    }
  }
}
