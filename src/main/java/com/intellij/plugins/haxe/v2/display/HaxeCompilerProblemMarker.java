package com.intellij.plugins.haxe.v2.display;

import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.plugins.haxe.display.protocol.Diagnostic;
import com.intellij.plugins.haxe.display.protocol.DiagnosticSeverity;
import com.intellij.plugins.haxe.display.protocol.FileDiagnostics;
import com.intellij.plugins.haxe.util.HaxeReadActions;
import com.intellij.plugins.haxe.v2.display.HaxeCompilerProblemMarks.Update;
import com.intellij.problems.WolfTheProblemSolver;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Marks the files in which the compiler reported errors, through
 * {@link WolfTheProblemSolver}: their names turn red in the Project view and
 * the editor tabs. The edited file is judged by its own diagnostics. Every
 * other project file is judged by the whole-project sweep, which lists only
 * the files the build reaches. {@link HaxeCompilerProblemMarks} keeps the
 * two kinds of evidence apart. Library files are never marked, since they
 * are not the user's to fix.
 *
 * TODO: a broken file the build never reaches gets no mark until it is
 *  opened; listing the module's sources in the sweep ({@code fileContents})
 *  would type them all.
 */
@Service(Service.Level.PROJECT)
public final class HaxeCompilerProblemMarker {

  /** The external-source identity of these marks, so clearing them never clears another source's. */
  private static final Object SOURCE = new Object();

  private final Project project;
  private final HaxeCompilerProblemMarks marks = new HaxeCompilerProblemMarks();

  public HaxeCompilerProblemMarker(@NotNull Project project) {
    this.project = project;
  }

  @NotNull
  public static HaxeCompilerProblemMarker getInstance(@NotNull Project project) {
    return project.getService(HaxeCompilerProblemMarker.class);
  }

  /**
   * Applies one diagnostics pass: the edited file's own diagnostics and, when
   * the sweep answered, the whole-project sweep (null when it did not). Call
   * on a background thread.
   */
  public void updateFromDiagnostics(@NotNull String editedFilePath,
                                    @NotNull List<FileDiagnostics> editedFileResults,
                                    @Nullable List<FileDiagnostics> sweepResults) {
    String editedPath = FileUtil.toSystemIndependentName(editedFilePath);
    boolean editedBroken = brokenPaths(editedFileResults).contains(editedPath);
    Set<String> sweepBroken = sweepResults == null ? null : projectFilesAmong(brokenPaths(sweepResults));
    Update update = marks.apply(editedPath, editedBroken, sweepBroken);

    WolfTheProblemSolver problemSolver = WolfTheProblemSolver.getInstance(project);
    for (String path : update.clear()) {
      VirtualFile file = LocalFileSystem.getInstance().findFileByPath(path);
      if (file != null) problemSolver.clearProblemsFromExternalSource(file, SOURCE);
    }
    for (String path : update.report()) {
      VirtualFile file = LocalFileSystem.getInstance().findFileByPath(path);
      if (file != null) problemSolver.reportProblemsFromExternalSource(file, SOURCE);
    }
  }

  /** The files with error-severity entries, as system-independent paths. */
  @NotNull
  private static Set<String> brokenPaths(@NotNull List<FileDiagnostics> results) {
    Set<String> broken = new HashSet<>();
    for (FileDiagnostics entry : results) {
      if (hasErrorSeverity(entry)) broken.add(FileUtil.toSystemIndependentName(entry.file()));
    }
    return broken;
  }

  @NotNull
  private Set<String> projectFilesAmong(@NotNull Set<String> paths) {
    Set<String> inProject = new HashSet<>();
    for (String path : paths) {
      VirtualFile file = LocalFileSystem.getInstance().findFileByPath(path);
      if (file == null || !file.isValid()) continue;
      boolean inContent = HaxeReadActions.compute(() -> ProjectFileIndex.getInstance(project).isInContent(file));
      if (inContent) inProject.add(file.getPath());
    }
    return inProject;
  }

  private static boolean hasErrorSeverity(@NotNull FileDiagnostics entry) {
    for (Diagnostic diagnostic : entry.diagnostics()) {
      if (diagnostic.severity() == DiagnosticSeverity.ERROR) return true;
    }
    return false;
  }
}
