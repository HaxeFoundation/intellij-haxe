package com.intellij.plugins.haxe.ide.projectStructure.autoimport;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.externalSystem.autoimport.*;
import com.intellij.openapi.externalSystem.model.ProjectSystemId;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.plugins.haxe.config.HaxeConfiguration;
import com.intellij.plugins.haxe.haxelib.HaxelibProjectUpdater;
import com.intellij.plugins.haxe.ide.module.HaxeModuleSettings;
import com.intellij.plugins.haxe.ide.module.HaxeModuleType;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Collectors;

public class HaxelibAutoImport implements ExternalSystemProjectAware, Disposable {

  private static final  ProjectSystemId mySystemId = new ProjectSystemId("Haxelib");
  public static final ExternalSystemProjectId mySystemProjectId = new ExternalSystemProjectId(mySystemId, "Haxelib");
  private  Project project;
  private List<ExternalSystemProjectListener> myListeners = new ArrayList<>();

  public HaxelibAutoImport(Project project) {
    this.project = project;
  }

  @NotNull
  @Override
  public ExternalSystemProjectId getProjectId() {
    return mySystemProjectId;
  }

  @NotNull
  @Override
  public Set<String> getSettingsFiles() {
    Collection<Module> modules = ModuleUtil.getModulesOfType(project, HaxeModuleType.getInstance());
    return modules.stream()
      .map(HaxeModuleSettings::getInstance)
      .map(this::getBuildConfigFile)
      .filter(Objects::nonNull)
      .collect(Collectors.toSet());

  }

  private String getBuildConfigFile(HaxeModuleSettings settings) {
    HaxeConfiguration configuration = settings.getBuildConfiguration();
    return switch (configuration) {
      case OPENFL -> settings.getOpenFLPath();
      case HXML -> settings.getHxmlPath();
      case NMML -> settings.getNmmlPath();
      default -> null;
    };
  }

  @Override
  public boolean isIgnoredSettingsFileEvent(@NotNull String path, @NotNull ExternalSystemSettingsFilesModificationContext context) {
    Set<String> files = getSettingsFiles();
    if(files.contains(path)){
      return false;
    }
    return true;
  }

  @Override
  public void reloadProject(@NotNull ExternalSystemProjectReloadContext context) {
    if(context.isExplicitReload() || settingsFilesChanged(context.getSettingsFilesContext())) {

      myListeners.forEach(ExternalSystemProjectListener::onProjectReloadStart);

      HaxelibProjectUpdater instance = HaxelibProjectUpdater.INSTANCE;
      HaxelibProjectUpdater.ProjectTracker tracker = instance.findProjectTracker(project);
      clearHaxelibCaches();

      if (tracker != null) {
        tracker.getCache().clear();
        instance.synchronizeClasspaths(tracker);
      }

      myListeners.forEach(l -> l.onProjectReloadFinish(ExternalSystemRefreshStatus.SUCCESS));
    }
  }

  private boolean settingsFilesChanged(ExternalSystemSettingsFilesReloadContext modifications) {
    return !(modifications.getCreated().isEmpty() && modifications.getDeleted().isEmpty() && modifications.getUpdated().isEmpty());
  }

  private void clearHaxelibCaches() {
    Collection<Module> modules = ModuleUtil.getModulesOfType(project, HaxeModuleType.getInstance());
    modules.forEach(module ->  HaxelibProjectUpdater.getLibraryCache(module).reload());
  }
  @Override
  public void subscribe(@NotNull ExternalSystemProjectListener listener, @NotNull Disposable disposable) {
    Disposer.register(disposable, this);
    myListeners.add(listener);
  }

  @Override
  public void dispose() {
    myListeners.clear();
    project = null;
  }
}
