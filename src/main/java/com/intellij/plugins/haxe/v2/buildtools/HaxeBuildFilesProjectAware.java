package com.intellij.plugins.haxe.v2.buildtools;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.externalSystem.autoimport.ExternalSystemProjectAware;
import com.intellij.openapi.externalSystem.autoimport.ExternalSystemProjectId;
import com.intellij.openapi.externalSystem.autoimport.ExternalSystemProjectListener;
import com.intellij.openapi.externalSystem.autoimport.ExternalSystemProjectReloadContext;
import com.intellij.openapi.externalSystem.autoimport.ExternalSystemRefreshStatus;
import com.intellij.openapi.externalSystem.autoimport.ExternalSystemSettingsFilesModificationContext;
import com.intellij.openapi.externalSystem.model.ProjectSystemId;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.plugins.haxe.util.HaxeReadActions;
import com.intellij.plugins.haxe.v2.buildtools.settings.HaxeBuildFilesStore;
import com.intellij.plugins.haxe.v2.buildtools.settings.HaxeBuildSettingsListener;
import com.intellij.plugins.haxe.v2.buildsystem.HaxeBuildFile;
import com.intellij.util.messages.MessageBusConnection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

/**
 * Shows the platform's floating "reload build configuration" icon when a v2 build
 * file changes (Gradle-style) and runs the v2 sync pipeline on reload. Watches the
 * tool window's build files (detected plus manually added) and the configuration
 * files they import (see {@link HaxeBuildFileIncludes}).
 */
public final class HaxeBuildFilesProjectAware implements ExternalSystemProjectAware {

  private static final ProjectSystemId SYSTEM_ID = new ProjectSystemId("HAXE", "Haxe");

  private final Project project;
  private final ExternalSystemProjectId projectId;
  private final List<ExternalSystemProjectListener> listeners = new CopyOnWriteArrayList<>();
  // The platform's settings-files tracker persists every path it was ever
  // given, so a file that is no longer imported stays watched until it is
  // deleted. Only events for paths in the last collected set count.
  private volatile @Nullable Set<String> lastCollected;

  public HaxeBuildFilesProjectAware(@NotNull Project project) {
    this.project = project;
    this.projectId = new ExternalSystemProjectId(SYSTEM_ID, project.getName());
  }

  @Override
  public @NotNull ExternalSystemProjectId getProjectId() {
    return projectId;
  }

  @Override
  public @NotNull Set<String> getSettingsFiles() {
    return HaxeReadActions.compute(() -> {
      Set<String> files = new HashSet<>();
      for (HaxeBuildFile buildFile : HaxeKnownBuildFiles.all(project)) {
        files.add(buildFile.file().getPath());
        files.addAll(HaxeBuildFileIncludes.configurationIncludes(project, buildFile));
      }
      files.addAll(HaxeBuildFilesStore.getInstance(project).getAllAddedPaths());
      lastCollected = files.stream().map(FileUtil::toSystemIndependentName).collect(Collectors.toUnmodifiableSet());
      return files;
    });
  }

  /**
   * The platform's own rule (events while a reload starts, files created as it
   * finishes) plus: an event for a path the build files no longer import is
   * absorbed instead of marking the project modified.
   */
  @Override
  public boolean isIgnoredSettingsFileEvent(@NotNull String path, @NotNull ExternalSystemSettingsFilesModificationContext context) {
    return ExternalSystemProjectAware.super.isIgnoredSettingsFileEvent(path, context) || !isWatched(path);
  }

  /** Whether the last collected settings files contain the path; everything counts until the first collection. */
  boolean isWatched(@NotNull String path) {
    Set<String> collected = lastCollected;
    return collected == null || collected.contains(FileUtil.toSystemIndependentName(path));
  }

  @Override
  public void reloadProject(@NotNull ExternalSystemProjectReloadContext context) {
    listeners.forEach(ExternalSystemProjectListener::onProjectReloadStart);
    HaxeProjectSync.sync(project,
                         () -> listeners.forEach(listener -> listener.onProjectReloadFinish(ExternalSystemRefreshStatus.SUCCESS)));
  }

  /**
   * The tracker re-reads {@link #getSettingsFiles()} on
   * {@link ExternalSystemProjectListener#onSettingsFilesListChange()}, sent
   * whenever the watched set may have changed: after a sync (the edit that
   * triggered it may have changed the imports) and when a build file is
   * registered or removed.
   */
  @Override
  public void subscribe(@NotNull ExternalSystemProjectListener listener, @NotNull Disposable parentDisposable) {
    listeners.add(listener);
    Disposer.register(parentDisposable, () -> listeners.remove(listener));
    MessageBusConnection connection = project.getMessageBus().connect(parentDisposable);
    connection.subscribe(HaxeBuildConfigListener.TOPIC, (HaxeBuildConfigListener)listener::onSettingsFilesListChange);
    connection.subscribe(HaxeBuildSettingsListener.TOPIC, (HaxeBuildSettingsListener)listener::onSettingsFilesListChange);
  }
}
