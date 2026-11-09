package com.intellij.plugins.haxe.v2.buildtools.libraries;

import com.intellij.notification.NotificationType;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.plugins.haxe.HaxeBundle;
import com.intellij.plugins.haxe.haxelib.HaxelibRepositoryLayout;
import com.intellij.plugins.haxe.v2.buildtools.HaxeCommandNotifications;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;

/**
 * The background haxelib mutations (install, set, remove) with the follow-up
 * every caller owes the user: a failure is reported under the action's title;
 * a success refreshes the project's libraries ({@link HaxeLibrarySync}) and
 * then runs the caller's own step on the EDT - a toast, a panel reload.
 */
public final class HaxelibMutations {

  /** A haxelib mutation on the task's thread: null on success, else the failure detail (see {@link HaxelibInstaller}). */
  @FunctionalInterface
  public interface Mutation {
    @Nullable
    String run(@NotNull ProgressIndicator indicator);
  }

  private HaxelibMutations() {
  }

  /** Runs the mutation under the progress title; a failure is notified under {@code failureTitle}, a success synced and followed by {@code onSynced}. */
  public static void queue(@NotNull Project project,
                           @NotNull String progressTitle,
                           @NotNull String failureTitle,
                           @NotNull Mutation mutation,
                           @Nullable Runnable onSynced) {
    new Task.Backgroundable(project, progressTitle, true) {
      @Override
      public void run(@NotNull ProgressIndicator indicator) {
        String failure = mutation.run(indicator);
        if (failure == null) {
          refreshLocalRepository(project);
          HaxeLibrarySync.sync(project, onSynced);
        }
        else {
          HaxeCommandNotifications.notify(project, failureTitle, failure, NotificationType.ERROR);
        }
      }
    }.queue();
  }

  /**
   * Reloads the local haxelib repository ({@code .haxelib} from disk  as the IDE does not pick up on changes automatically.
   */
  static void refreshLocalRepository(@NotNull Project project) {
    String basePath = project.getBasePath();
    if (basePath == null) return;
    Path repositoryPath = Path.of(basePath, HaxelibRepositoryLayout.LOCAL_REPOSITORY_DIR_NAME);
    VirtualFile repository = LocalFileSystem.getInstance().refreshAndFindFileByNioFile(repositoryPath);
    if (repository != null) {
      VfsUtil.markDirtyAndRefresh(false, true, true, repository);
    }
  }

  /**
   * Installs the library in the background and announces the success once the
   * libraries are synced. {@code version} is the pinned version (null for the
   * latest), {@code resolvedVersion} the one haxelib currently selects - see
   * {@link HaxelibInstaller#install}.
   */
  public static void install(@NotNull Project project, @NotNull String name, @Nullable String version, @Nullable String resolvedVersion) {
    String progressTitle = HaxeBundle.message("haxe.toolwindow.install.library.progress", name);
    String failureTitle = HaxeBundle.message("haxe.toolwindow.install.library.failed", name);
    Mutation install = indicator -> HaxelibInstaller.install(project, name, version, resolvedVersion);
    queue(project, progressTitle, failureTitle, install, () -> notifyInstalled(project, name));
  }

  private static void notifyInstalled(@NotNull Project project, @NotNull String name) {
    String installed = HaxeBundle.message("haxe.toolwindow.install.library.success", name);
    HaxeCommandNotifications.notify(project, installed, NotificationType.INFORMATION);
  }
}
