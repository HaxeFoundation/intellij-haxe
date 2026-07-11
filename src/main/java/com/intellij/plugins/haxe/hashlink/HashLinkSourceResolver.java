package com.intellij.plugins.haxe.hashlink;

import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.search.FilenameIndex;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.xdebugger.XDebuggerUtil;
import com.intellij.xdebugger.XSourcePosition;
import java.nio.file.Path;
import java.util.Collection;
import org.jetbrains.annotations.Nullable;

/**
 * Maps a source path reported by the debug adapter (taken from the .hl
 * bytecode's debug tables — usually project-relative like {@code src/Main.hx},
 * sometimes absolute) to an IDE source position.
 */
final class HashLinkSourceResolver {
  private HashLinkSourceResolver() {
  }

  /** Resolves to a position, or null when the file cannot be found. 1-based line. */
  static @Nullable XSourcePosition resolve(Project project, @Nullable String path, int line) {
    if (path == null || path.isBlank()) {
      return null;
    }
    String normalized = FileUtil.toSystemIndependentName(path);
    return ReadAction.compute(() -> {
      VirtualFile file = findFile(project, normalized);
      return file != null ? XDebuggerUtil.getInstance().createPosition(file, Math.max(0, line - 1)) : null;
    });
  }

  private static @Nullable VirtualFile findFile(Project project, String normalized) {
    if (Path.of(normalized).isAbsolute()) {
      VirtualFile absolute = LocalFileSystem.getInstance().findFileByPath(normalized);
      if (absolute != null) {
        return absolute;
      }
    }
    // relative (or stale absolute): find candidates by file name, prefer the one
    // whose full path ends with the reported path
    String fileName = normalized.substring(normalized.lastIndexOf('/') + 1);
    Collection<VirtualFile> candidates =
      FilenameIndex.getVirtualFilesByName(fileName, GlobalSearchScope.allScope(project));
    VirtualFile byName = null;
    for (VirtualFile candidate : candidates) {
      if (candidate.getPath().endsWith("/" + normalized) || candidate.getPath().equals(normalized)) {
        return candidate;
      }
      if (byName == null) {
        byName = candidate;
      }
    }
    return byName;
  }
}
