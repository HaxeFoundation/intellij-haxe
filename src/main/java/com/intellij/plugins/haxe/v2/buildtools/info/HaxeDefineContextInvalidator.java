package com.intellij.plugins.haxe.v2.buildtools.info;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.impl.PushedFilePropertiesUpdater;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.plugins.haxe.HaxeFileType;
import com.intellij.plugins.haxe.lang.psi.stubs.HaxeStubableFileService;
import com.intellij.util.FileContentUtilCore;
import com.intellij.util.indexing.FileBasedIndex;
import lombok.CustomLog;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Re-indexes and reparses the haxe files whose parse depends on the define
 * context — those containing conditional compilation — after the context
 * changed. Files without it parse the same under every define set and are
 * left alone. Nothing is stored: every change invalidates every conditional
 * file, project and library alike, so two projects sharing libraries never
 * see each other's state.
 */
@CustomLog
final class HaxeDefineContextInvalidator {

  private HaxeDefineContextInvalidator() {
  }

  /**
   * Called from a pooled thread. The platform's per-file invalidation
   * re-indexes in the background and reloads only loaded PSI; open editors
   * additionally get the platform's forced reparse, the one event that
   * rebuilds their highlighting lexer.
   */
  static void invalidateConditionalFiles(@NotNull Project project) {
    List<VirtualFile> conditionalFiles = collectConditionalFiles(project);
    PushedFilePropertiesUpdater updater = PushedFilePropertiesUpdater.getInstance(project);
    Disposable expiry = HaxeDefineContextService.getInstance(project);
    for (VirtualFile file : conditionalFiles) {
      ReadAction.nonBlocking(() -> invalidate(updater, file))
        .expireWith(expiry)
        .executeSynchronously();
    }
    Runnable reparseOpenEditors = () -> reparseOpenEditors(project, conditionalFiles);
    ApplicationManager.getApplication().invokeLater(reparseOpenEditors, ModalityState.nonModal(), project.getDisposed());
    log.debug("define context: " + conditionalFiles.size() + " conditional haxe files invalidated");
  }

  /** Walks the indexable files without a lock — this is where an uncached stubable flag reads its file. */
  @NotNull
  private static List<VirtualFile> collectConditionalFiles(@NotNull Project project) {
    List<VirtualFile> conditionalFiles = new ArrayList<>();
    FileBasedIndex.getInstance().iterateIndexableFiles(file -> {
      if (isHaxeFile(file) && !HaxeStubableFileService.isStubable(file)) conditionalFiles.add(file);
      return true;
    }, project, null);
    return conditionalFiles;
  }

  /**
   * Idempotent, so a cancelled and retried read action is safe. Returns
   * Void so the lambda binds the Callable overload of nonBlocking (the
   * Runnable one is deprecated).
   */
  @Nullable
  private static Void invalidate(@NotNull PushedFilePropertiesUpdater updater, @NotNull VirtualFile file) {
    if (file.isValid()) {
      updater.filePropertiesChanged(file, HaxeDefineContextInvalidator::isHaxeFile);
    }
    return null;
  }

  private static void reparseOpenEditors(@NotNull Project project, @NotNull List<VirtualFile> conditionalFiles) {
    Set<VirtualFile> conditional = new HashSet<>(conditionalFiles);
    VirtualFile[] openFiles = FileEditorManager.getInstance(project).getOpenFiles();
    List<VirtualFile> openConditionalFiles = Arrays.stream(openFiles).filter(conditional::contains).toList();
    if (!openConditionalFiles.isEmpty()) {
      FileContentUtilCore.reparseFiles(openConditionalFiles);
    }
  }

  private static boolean isHaxeFile(@NotNull VirtualFile file) {
    return file.getFileType() == HaxeFileType.INSTANCE;
  }
}
