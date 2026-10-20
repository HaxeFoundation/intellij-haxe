package com.intellij.plugins.haxe.v2.buildtools;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.newvfs.BulkFileListener;
import com.intellij.openapi.vfs.newvfs.events.VFileContentChangeEvent;
import com.intellij.openapi.vfs.newvfs.events.VFileCopyEvent;
import com.intellij.openapi.vfs.newvfs.events.VFileCreateEvent;
import com.intellij.openapi.vfs.newvfs.events.VFileDeleteEvent;
import com.intellij.openapi.vfs.newvfs.events.VFileEvent;
import com.intellij.openapi.vfs.newvfs.events.VFileMoveEvent;
import com.intellij.openapi.vfs.newvfs.events.VFilePropertyChangeEvent;
import com.intellij.plugins.haxe.v2.buildsystem.HaxeBuildFileScanner;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Turns file-system changes to build files into {@link HaxeBuildEventListener}
 * events: saved, created, copied, moved, renamed or deleted. Any project
 * file with a build-file extension counts, so a configuration file that a
 * build file includes does too. The IDE's own settings files are xml as
 * well and are left out, as are files outside the project's content.
 */
public final class HaxeBuildFileChangeListener implements BulkFileListener {
  /** Where the IDE keeps its settings (workspace.xml, the plugin's stores), saved on the IDE's own schedule. */
  private static final String IDE_SETTINGS_FOLDER = "/.idea/";

  private final Project project;

  public HaxeBuildFileChangeListener(@NotNull Project project) {
    this.project = project;
  }

  @Override
  public void after(@NotNull List<? extends @NotNull VFileEvent> events) {
    if (project.isDisposed()) return;
    HaxeBuildEventListener publisher = HaxeBuildEventListener.publisher(project);
    for (VFileEvent event : events) {
      announce(event, publisher);
    }
  }

  /** Publishes the build-file event a VFS event amounts to, if any. */
  private void announce(@NotNull VFileEvent event, @NotNull HaxeBuildEventListener publisher) {
    switch (event) {
      case VFileContentChangeEvent change when concernsBuildFile(change) ->
        publisher.buildFileContentChanged(change.getPath());
      case VFileDeleteEvent deletion when concernsBuildFile(deletion) -> publisher.buildFileDeleted(deletion.getPath());
      case VFileCreateEvent creation when concernsBuildFile(creation) -> publisher.buildFileAdded(creation.getPath());
      case VFileCopyEvent copy when concernsBuildFile(copy) -> publisher.buildFileAdded(copy.getPath());
      case VFileMoveEvent move ->
        announceRelocation(move.getOldPath(), move.getNewPath(), move.getFile(), publisher);
      case VFilePropertyChangeEvent rename when VirtualFile.PROP_NAME.equals(rename.getPropertyName()) ->
        announceRelocation(rename.getOldPath(), rename.getNewPath(), rename.getFile(), publisher);
      default -> { }
    }
  }

  /** A move or rename leaves the old path and arrives at the new one; either side may be a build file. */
  private void announceRelocation(@NotNull String oldPath,
                                  @NotNull String newPath,
                                  @NotNull VirtualFile file,
                                  @NotNull HaxeBuildEventListener publisher) {
    if (isWatchedPath(oldPath)) publisher.buildFileDeleted(oldPath);
    if (isWatchedPath(newPath) && isInContent(file)) publisher.buildFileAdded(newPath);
  }

  /**
   * Whether a save, creation, copy or deletion concerns a project build file.
   * A move or rename is judged per path instead.
   */
  boolean concernsBuildFile(@NotNull VFileEvent event) {
    if (!isWatchedPath(event.getPath())) return false;
    return switch (event) {
      // a deleted file is in no content root any more; its path decided above
      case VFileDeleteEvent ignored -> true;
      case VFileContentChangeEvent change -> isInContent(change.getFile());
      case VFileCreateEvent creation -> isInContent(creation.getFile());
      case VFileCopyEvent copy -> isInContent(copy.findCreatedFile());
      default -> false;
    };
  }

  private boolean isInContent(@Nullable VirtualFile file) {
    return file != null && ProjectFileIndex.getInstance(project).isInContent(file);
  }

  private static boolean isWatchedPath(@NotNull String path) {
    return HaxeBuildFileScanner.hasBuildFileExtension(path) && !path.contains(IDE_SETTINGS_FOLDER);
  }
}
