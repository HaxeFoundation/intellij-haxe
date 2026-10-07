package com.intellij.plugins.haxe.execution.console;

import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.plugins.haxe.util.HaxeFileUtil;
import com.intellij.plugins.haxe.util.HaxeReadActions;
import com.intellij.psi.search.FilenameIndex;
import com.intellij.psi.search.GlobalSearchScope;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;

/**
 * Finds the source file a stack frame names. The compiler prints positions
 * relative to its working directory or a classpath root, so a launch's own
 * directories (see {@link HaxeLaunchSearchScope}) are tried first; then a
 * path that exists as written, or under the project root; otherwise the
 * indexed file sharing the longest trailing path with it — a trace pasted
 * from another machine names {@code /home/username/work/app/src/shapes/Circle.hx},
 * which is this project's {@code src/shapes/Circle.hx}. A suffix match under
 * the launch's directories beats a project-root hit, which may belong to a
 * sibling build; then project content beats libraries.
 */
final class HaxeStackFrameFiles {

  private HaxeStackFrameFiles() {
  }

  @Nullable
  static VirtualFile find(@NotNull Project project, @NotNull GlobalSearchScope scope, @NotNull String path) {
    String normalized = path.replace('\\', '/');
    List<VirtualFile> preferred = HaxeLaunchSearchScope.preferredDirectoriesOf(scope);
    VirtualFile underLaunch = underAny(preferred, normalized);
    if (underLaunch != null) return underLaunch;
    VirtualFile direct = HaxeFileUtil.locateFile(normalized, project.getBasePath());
    if (DumbService.isDumb(project) || direct != null && preferred.isEmpty()) return direct;
    // a project-root hit for a relative path may be a sibling build's file;
    // the same suffix under the launch's own directories wins over it
    VirtualFile indexed = HaxeReadActions.compute(() -> bySuffix(project, scope, preferred, normalized));
    if (indexed != null && isUnderAny(indexed, preferred)) return indexed;
    return direct != null ? direct : indexed;
  }

  @Nullable
  private static VirtualFile underAny(@NotNull List<VirtualFile> directories, @NotNull String relativePath) {
    for (VirtualFile directory : directories) {
      VirtualFile file = directory.findFileByRelativePath(relativePath);
      if (file != null && !file.isDirectory()) return file;
    }
    return null;
  }

  @Nullable
  private static VirtualFile bySuffix(@NotNull Project project,
                                      @NotNull GlobalSearchScope scope,
                                      @NotNull List<VirtualFile> preferred,
                                      @NotNull String path) {
    String name = path.substring(path.lastIndexOf('/') + 1);
    Collection<VirtualFile> candidates = FilenameIndex.getVirtualFilesByName(name, scope);
    ProjectFileIndex fileIndex = ProjectFileIndex.getInstance(project);
    Comparator<VirtualFile> preferredThenLongestSuffixThenContent = Comparator
      .comparing((VirtualFile file) -> isUnderAny(file, preferred))
      .thenComparingInt(file -> trailingSegmentsInCommon(file.getPath(), path))
      .thenComparing(fileIndex::isInContent);
    return candidates.stream().max(preferredThenLongestSuffixThenContent).orElse(null);
  }

  private static boolean isUnderAny(@NotNull VirtualFile file, @NotNull List<VirtualFile> directories) {
    return directories.stream().anyMatch(directory -> VfsUtilCore.isAncestor(directory, file, false));
  }

  private static int trailingSegmentsInCommon(@NotNull String filePath, @NotNull String tracePath) {
    String[] fileSegments = filePath.split("/");
    String[] traceSegments = tracePath.split("/");
    int limit = Math.min(fileSegments.length, traceSegments.length);
    int common = 0;
    while (common < limit && sameSegmentFromEnd(fileSegments, traceSegments, common)) {
      common++;
    }
    return common;
  }

  private static boolean sameSegmentFromEnd(String[] fileSegments, String[] traceSegments, int fromEnd) {
    return fileSegments[fileSegments.length - 1 - fromEnd].equals(traceSegments[traceSegments.length - 1 - fromEnd]);
  }
}
