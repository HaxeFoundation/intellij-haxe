package com.intellij.plugins.haxe.execution.console;

import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.plugins.haxe.util.HaxeFileUtil;
import com.intellij.plugins.haxe.util.HaxeReadActions;
import com.intellij.psi.search.FilenameIndex;
import com.intellij.psi.search.GlobalSearchScope;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Comparator;

/**
 * Finds the source file a stack frame names. A path that exists as written,
 * or under the project root, wins; otherwise the indexed file sharing the
 * longest trailing path with it — a trace pasted from another machine names
 * {@code /home/username/work/app/src/shapes/Circle.hx}, which is this
 * project's {@code src/shapes/Circle.hx}. Project content beats libraries on a tie.
 */
final class HaxeStackFrameFiles {

  private HaxeStackFrameFiles() {
  }

  @Nullable
  static VirtualFile find(@NotNull Project project, @NotNull GlobalSearchScope scope, @NotNull String path) {
    String normalized = path.replace('\\', '/');
    VirtualFile direct = HaxeFileUtil.locateFile(normalized, project.getBasePath());
    if (direct != null) return direct;
    if (DumbService.isDumb(project)) return null;
    return HaxeReadActions.compute(() -> bySuffix(project, scope, normalized));
  }

  @Nullable
  private static VirtualFile bySuffix(@NotNull Project project, @NotNull GlobalSearchScope scope, @NotNull String path) {
    String name = path.substring(path.lastIndexOf('/') + 1);
    Collection<VirtualFile> candidates = FilenameIndex.getVirtualFilesByName(name, scope);
    ProjectFileIndex fileIndex = ProjectFileIndex.getInstance(project);
    Comparator<VirtualFile> longestSuffixThenContent = Comparator
      .comparingInt((VirtualFile file) -> trailingSegmentsInCommon(file.getPath(), path))
      .thenComparing(fileIndex::isInContent);
    return candidates.stream().max(longestSuffixThenContent).orElse(null);
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
