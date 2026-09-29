package com.intellij.plugins.haxe.v2.testing.run;

import com.intellij.openapi.project.Project;
import com.intellij.plugins.haxe.v2.buildtools.HaxeBuildConfigListener;
import com.intellij.plugins.haxe.v2.buildtools.settings.HaxeBuildSettingsListener;
import org.jetbrains.annotations.NotNull;

/**
 * Recomputes test configurations' persisted before-run compile steps when the
 * build configuration is resynced (build files reparsed) or a build setting
 * changes (the tool window's target selection): the tests build's content
 * and its selected target feed those arguments (the suite-name define,
 * single-stage vs artifact), so an hxml edit or a target switch must reach
 * existing configurations without waiting for them to be re-edited.
 */
public final class HaxeTestCompileStepResync implements HaxeBuildConfigListener, HaxeBuildSettingsListener {

  private final Project project;

  public HaxeTestCompileStepResync(@NotNull Project project) {
    this.project = project;
  }

  @Override
  public void buildConfigurationChanged() {
    resync();
  }

  @Override
  public void buildSettingsChanged() {
    resync();
  }

  private void resync() {
    // resyncCompileSteps schedules its own background compute + EDT apply
    if (!project.isDisposed()) {
      HaxeTestRunConfigurations.resyncCompileSteps(project);
    }
  }
}
