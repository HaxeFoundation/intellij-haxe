package com.intellij.plugins.haxe.v2.buildtools.settings.ui;

import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.options.SearchableConfigurable;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.projectRoots.ProjectJdkTable;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.plugins.haxe.HaxeBundle;
import com.intellij.plugins.haxe.config.sdk.HaxeSdkType;
import com.intellij.plugins.haxe.util.HaxeModuleDetection;
import com.intellij.plugins.haxe.v2.buildtools.server.HaxeCompilationServerManager;
import com.intellij.plugins.haxe.v2.buildtools.libraries.HaxeLibrarySync;
import com.intellij.plugins.haxe.v2.buildtools.projectmodel.HaxeModuleSdkApplier;
import com.intellij.plugins.haxe.v2.buildtools.HaxeBuildConfigListener;
import com.intellij.plugins.haxe.v2.buildtools.HaxeToolPathResolver;
import com.intellij.plugins.haxe.v2.buildtools.settings.HaxeBuildToolSettings;
import com.intellij.plugins.haxe.v2.buildtools.settings.HaxeEnvironmentStore;
import com.intellij.plugins.haxe.v2.testing.run.HaxeTestRunConfigurations;
import com.intellij.util.concurrency.AppExecutorUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Settings page under Build, Execution, Deployment | Build Tools | Haxe.
 */
public final class HaxeBuildToolsConfigurable implements SearchableConfigurable {

  public static final String ID = "settings.haxe.build.tools";

  private final Project project;
  private HaxeBuildToolsSettingsPanel panel;

  public HaxeBuildToolsConfigurable(@NotNull Project project) {
    this.project = project;
  }

  @Override
  public @NotNull String getId() {
    return ID;
  }

  @Override
  public String getDisplayName() {
    return HaxeBundle.message("haxe.build.tools.configurable.name");
  }

  @Override
  public @Nullable JComponent createComponent() {
    if (panel == null) {
      panel = new HaxeBuildToolsSettingsPanel();
      panel.addSdkSelectionListener(this::updateInheritedDefaults);
    }
    reset();
    return panel.getComponent();
  }

  /** The grayed defaults the empty overrides inherit, recomputed for the SDK currently selected in the panel. */
  private void updateInheritedDefaults() {
    HaxeBuildToolsSettingsPanel currentPanel = panel;
    String sdkName = currentPanel.getSelectedSdkName();
    // the defaults probe every PATH directory on disk - computed off the EDT,
    // or a slow/network PATH freezes the settings dialog on each SDK change
    ReadAction.nonBlocking(() -> HaxeToolPathResolver.inheritedRuntimeDefaults(sdkName))
      .expireWhen(() -> panel != currentPanel)
      .finishOnUiThread(ModalityState.defaultModalityState(), defaults -> {
        boolean selectionUnchanged = Objects.equals(sdkName, currentPanel.getSelectedSdkName());
        if (selectionUnchanged) {
          currentPanel.setInheritedDefaults(defaults);
        }
      })
      .submit(AppExecutorUtil.getAppExecutorService());
  }

  @Override
  public boolean isModified() {
    if (panel == null) return false;
    HaxeBuildToolSettings settings = getSettings();
    return !Objects.equals(panel.getSelectedSdkName(), settings.getSdkName())
           || !panel.getHaxelibPath().equals(settings.getHaxelibPath())
           || !panel.getNekoPath().equals(settings.getNekoPath())
           || !panel.getHashlinkPath().equals(settings.getHashlinkPath())
           || !panel.getNodePath().equals(settings.getNodePath())
           || !panel.getFlashPlayerPath().equals(settings.getFlashPlayerPath())
           || !panel.getFlexSdkName().equals(settings.getFlexSdkName())
           || panel.isServerEnabled() != settings.isCompilationServerEnabled()
           || panel.getServerPort() != settings.getCompilationServerPort()
           || !panel.getServerArguments().equals(settings.getCompilationServerArguments())
           || panel.isLiveTestReporting() != settings.isLiveTestReporting();
  }

  @Override
  public void apply() {
    if (panel == null) return;
    HaxeBuildToolSettings settings = getSettings();
    boolean sdkChanged = !Objects.equals(panel.getSelectedSdkName(), settings.getSdkName());

    boolean serverConfigChanged = sdkChanged
      || panel.isServerEnabled() != settings.isCompilationServerEnabled()
      || panel.getServerPort() != settings.getCompilationServerPort()
      || !panel.getServerArguments().equals(settings.getCompilationServerArguments());

    settings.setSdkName(panel.getSelectedSdkName());
    if (sdkChanged) {
      // the default SDK is only real once it reaches the module entities -
      // resolution reads the project model, not this settings page
      applyDefaultSdkToModules();
      // library classpaths come from the SDK's haxelib
      HaxeLibrarySync.sync(project, null);
    }
    settings.setHaxelibPath(panel.getHaxelibPath());
    settings.setNekoPath(panel.getNekoPath());
    settings.setHashlinkPath(panel.getHashlinkPath());
    settings.setNodePath(panel.getNodePath());
    settings.setFlashPlayerPath(panel.getFlashPlayerPath());
    settings.setFlexSdkName(panel.getFlexSdkName());

    settings.setCompilationServerEnabled(panel.isServerEnabled());
    settings.setCompilationServerPort(panel.getServerPort());
    settings.setCompilationServerArguments(panel.getServerArguments());

    boolean liveTestReportingChanged = panel.isLiveTestReporting() != settings.isLiveTestReporting();
    settings.setLiveTestReporting(panel.isLiveTestReporting());
    if (liveTestReportingChanged) {
      // artifact-target test configurations persist their compile arguments in
      // the before-run step - resync, or the toggle only applies after a
      // configuration edit
      HaxeTestRunConfigurations.resyncCompileSteps(project);
    }

    if (serverConfigChanged) {
      // these settings invalidate the running server; the next connected compile
      // restarts it. Unrelated edits (haxelib/neko/hashlink paths) keep the warm caches.
      HaxeCompilationServerManager.getInstance(project).stop();
    }
    // the tool window's rows and the define context derive from these settings
    project.getMessageBus().syncPublisher(HaxeBuildConfigListener.TOPIC).buildConfigurationChanged();
  }

  /** Haxe modules without an Environment override follow the default SDK. */
  private void applyDefaultSdkToModules() {
    HaxeModuleSdkApplier applier = HaxeModuleSdkApplier.getInstance(project);
    HaxeEnvironmentStore environment = HaxeEnvironmentStore.getInstance(project);
    ReadAction.nonBlocking(() -> modulesFollowingDefaultSdk(environment))
      .inSmartMode(project)
      .finishOnUiThread(ModalityState.defaultModalityState(), names -> {
        for (String moduleName : names) {
          applier.applyAsync(moduleName, HaxeToolPathResolver.effectiveSdkName(project, moduleName));
        }
      })
      .submit(AppExecutorUtil.getAppExecutorService());
  }

  @NotNull
  private List<String> modulesFollowingDefaultSdk(@NotNull HaxeEnvironmentStore environment) {
    List<String> names = new ArrayList<>();
    for (Module module : ModuleManager.getInstance(project).getModules()) {
      if (HaxeModuleDetection.isHaxeModule(module) && environment.getSdkName(module.getName()) == null) {
        names.add(module.getName());
      }
    }
    return names;
  }

  @Override
  public void reset() {
    if (panel == null) return;
    HaxeBuildToolSettings settings = getSettings();
    panel.reset(getHaxeSdkNames(), settings.getSdkName(), settings.getHaxelibPath());
    panel.resetRuntimeOverrides(settings.getNekoPath(),
                                settings.getHashlinkPath(),
                                settings.getNodePath(),
                                settings.getFlashPlayerPath(),
                                settings.getFlexSdkName());
    panel.resetServerFields(settings.isCompilationServerEnabled(),
                            settings.getCompilationServerPort(),
                            settings.getCompilationServerArguments());
    panel.resetLiveTestReporting(settings.isLiveTestReporting());
    updateInheritedDefaults();
  }

  @Override
  public void disposeUIResources() {
    panel = null;
  }

  @NotNull
  private HaxeBuildToolSettings getSettings() {
    return HaxeBuildToolSettings.getInstance(project);
  }

  @NotNull
  private static Set<String> getHaxeSdkNames() {
    Set<String> names = new LinkedHashSet<>();
    for (Sdk sdk : ProjectJdkTable.getInstance().getSdksOfType(HaxeSdkType.getInstance())) {
      names.add(sdk.getName());
    }
    return names;
  }
}
