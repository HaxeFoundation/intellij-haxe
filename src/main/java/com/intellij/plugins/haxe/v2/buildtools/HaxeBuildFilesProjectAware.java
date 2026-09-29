package com.intellij.plugins.haxe.v2.buildtools;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.externalSystem.autoimport.ExternalSystemProjectAware;
import com.intellij.openapi.externalSystem.autoimport.ExternalSystemProjectId;
import com.intellij.openapi.externalSystem.autoimport.ExternalSystemProjectListener;
import com.intellij.openapi.externalSystem.autoimport.ExternalSystemProjectReloadContext;
import com.intellij.openapi.externalSystem.autoimport.ExternalSystemRefreshStatus;
import com.intellij.openapi.externalSystem.model.ProjectSystemId;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.plugins.haxe.util.HaxeReadActions;
import com.intellij.plugins.haxe.v2.buildtools.settings.HaxeBuildFilesStore;
import com.intellij.plugins.haxe.v2.buildsystem.HaxeBuildFile;
import com.intellij.plugins.haxe.v2.buildsystem.HaxeBuildFileScanner;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Shows the platform's floating "reload build configuration" icon when a v2 build
 * file changes (Gradle-style) and runs the v2 sync pipeline on reload. Watches the
 * auto-detected build files of every module plus manually added ones.
 */
public final class HaxeBuildFilesProjectAware implements ExternalSystemProjectAware {

  private static final ProjectSystemId SYSTEM_ID = new ProjectSystemId("HAXE", "Haxe");

  private final Project project;
  private final ExternalSystemProjectId projectId;
  private final List<ExternalSystemProjectListener> listeners = new CopyOnWriteArrayList<>();

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
      for (HaxeBuildFile buildFile : HaxeBuildFileScanner.scanProjectRoot(project)) {
        files.add(buildFile.file().getPath());
      }
      for (Module module : ModuleManager.getInstance(project).getModules()) {
        for (HaxeBuildFile buildFile : HaxeBuildFileScanner.scan(module)) {
          files.add(buildFile.file().getPath());
        }
      }
      files.addAll(HaxeBuildFilesStore.getInstance(project).getAllAddedPaths());
      return files;
    });
  }

  @Override
  public void reloadProject(@NotNull ExternalSystemProjectReloadContext context) {
    listeners.forEach(ExternalSystemProjectListener::onProjectReloadStart);
    HaxeProjectSync.sync(project,
                         () -> listeners.forEach(listener -> listener.onProjectReloadFinish(ExternalSystemRefreshStatus.SUCCESS)));
  }

  @Override
  public void subscribe(@NotNull ExternalSystemProjectListener listener, @NotNull Disposable parentDisposable) {
    listeners.add(listener);
    Disposer.register(parentDisposable, () -> listeners.remove(listener));
  }
}
