package com.intellij.plugins.haxe.runner.debugger.hashlink;

import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.plugins.haxe.lang.psi.HaxeClass;
import com.intellij.plugins.haxe.runner.debugger.dap.ide.DapSourceResolver;
import com.intellij.plugins.haxe.runner.debugger.dap.ide.DapSourceScopes;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.StackFrame;
import com.intellij.plugins.haxe.util.HaxeQnameResolveUtil;
import com.intellij.xdebugger.XDebuggerUtil;
import com.intellij.xdebugger.XSourcePosition;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Maps a source path reported by the HashLink debug adapter (taken from the
 * .hl bytecode's debug tables — usually project-relative like {@code src/Main.hx},
 * sometimes absolute) to an IDE source position. Between the two lookups of
 * {@link DapSourceResolver} (under the build's roots, then by file name),
 * the frame's name is tried: the adapter names a frame after the runtime
 * name of its class ({@code pkg.Cls.method}, {@code pkg.Cls.new} for a
 * constructor), so the class index finds the file even when the
 * debug-table path resolves nowhere.
 */
final class HashLinkSourceResolver {
  private HashLinkSourceResolver() {
  }

  /**
   * Resolves to a position, or null when the file cannot be found. 1-based
   * line. {@code sourceDirectories} — the build's classpath roots — break
   * name ties in their favor: the debug tables' relative names are ambiguous
   * whenever a sibling project in the same IDE project has a same-named file.
   */
  static @Nullable XSourcePosition resolve(Project project,
                                           @Nullable String path,
                                           StackFrame frame,
                                           List<String> sourceDirectories) {
    if (path == null || path.isBlank()) {
      return null;
    }
    int line = frame.getLine();
    String frameName = frame.getName();
    String normalized = FileUtil.toSystemIndependentName(path);
    return ReadAction.nonBlocking(() -> {
      VirtualFile file = findFile(project, normalized, frameName, sourceDirectories);
      return file != null ? XDebuggerUtil.getInstance().createPosition(file, Math.max(0, line - 1)) : null;
    }).executeSynchronously();
  }

  private static @Nullable VirtualFile findFile(Project project,
                                                String normalized,
                                                @Nullable String frameName,
                                                List<String> sourceDirectories) {
    VirtualFile underRoots = DapSourceResolver.findUnderRoots(normalized, sourceDirectories);
    if (underRoots != null) return underRoots;
    VirtualFile byClass = frameName == null ? null : classFile(project, frameName, sourceDirectories);
    if (byClass != null) return byClass;
    return DapSourceResolver.findByName(project, normalized, sourceDirectories);
  }

  /**
   * The file of the class the frame name denotes, when the project has one
   * under the build's roots (anywhere, when the build names no roots); null
   * for a name that qualifies no class ({@code fn@12}, {@code ?}).
   */
  private static @Nullable VirtualFile classFile(Project project, String frameName, List<String> sourceDirectories) {
    for (HaxeClass haxeClass : HaxeQnameResolveUtil.findClassesByRuntimeName(frameName, project)) {
      VirtualFile file = haxeClass.getContainingFile().getVirtualFile();
      if (file != null && DapSourceScopes.acceptsWhenScoped(file.getPath(), sourceDirectories)) return file;
    }
    return null;
  }
}
