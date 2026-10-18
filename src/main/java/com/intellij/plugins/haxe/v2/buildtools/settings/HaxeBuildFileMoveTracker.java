package com.intellij.plugins.haxe.v2.buildtools.settings;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.newvfs.BulkFileListener;
import com.intellij.openapi.vfs.newvfs.events.VFileEvent;
import com.intellij.openapi.vfs.newvfs.events.VFileMoveEvent;
import com.intellij.openapi.vfs.newvfs.events.VFilePropertyChangeEvent;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Keeps every store entry keyed by a file path with its file when it, or a
 * folder above it, moves or is renamed (run configurations keep theirs in a
 * tracker of their own, built on {@link #movesIn}).
 */
public final class HaxeBuildFileMoveTracker implements BulkFileListener {

  /** A file or folder renamed or moved: every stored path at or below {@code oldPath} now lives at or below {@code newPath}. */
  public record Move(@NotNull String oldPath, @NotNull String newPath) {
  }

  private final Project project;

  public HaxeBuildFileMoveTracker(@NotNull Project project) {
    this.project = project;
  }

  @Override
  public void after(@NotNull List<? extends @NotNull VFileEvent> events) {
    if (project.isDisposed()) return;
    boolean changed = false;
    for (Move move : movesIn(events)) {
      changed |= moveStored(move.oldPath(), move.newPath());
    }
    if (changed) {
      HaxeBuildSettingsListener.publisher(project).buildFilesChanged();
    }
  }

  /** The moves among VFS events: a move event, or a property change that is a rename. */
  @NotNull
  public static List<Move> movesIn(@NotNull List<? extends VFileEvent> events) {
    List<Move> moves = new ArrayList<>();
    for (VFileEvent event : events) {
      switch (event) {
        case VFileMoveEvent move -> moves.add(new Move(move.getOldPath(), move.getNewPath()));
        case VFilePropertyChangeEvent change when change.isRename() -> moves.add(new Move(change.getOldPath(), change.getNewPath()));
        default -> { }
      }
    }
    return moves;
  }

  private boolean moveStored(@NotNull String oldPath, @NotNull String newPath) {
    boolean changed = HaxeCustomActionsStore.getInstance(project).moveFile(oldPath, newPath);
    changed |= HaxeTargetSelectionStore.getInstance(project).moveFile(oldPath, newPath);
    changed |= HaxeSectionSelectionStore.getInstance(project).moveFile(oldPath, newPath);
    changed |= HaxeWorkDirectoryStore.getInstance(project).moveFile(oldPath, newPath);
    changed |= HaxeBuildFilesStore.getInstance(project).moveFile(oldPath, newPath);
    changed |= HaxeActiveBuildFileStore.getInstance(project).moveFile(oldPath, newPath);
    changed |= HaxeEnvironmentStore.getInstance(project).moveFile(oldPath, newPath);
    changed |= HaxeIdeOverridesStore.getInstance(project).moveFile(oldPath, newPath);
    changed |= HaxeTestsBuildFileStore.getInstance(project).moveFile(oldPath, newPath);
    changed |= HaxeResolveContextStore.getInstance(project).moveFile(oldPath, newPath);
    return changed;
  }
}
