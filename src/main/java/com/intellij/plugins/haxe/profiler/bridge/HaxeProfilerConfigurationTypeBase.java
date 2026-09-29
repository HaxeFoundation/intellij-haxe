package com.intellij.plugins.haxe.profiler.bridge;

import com.intellij.execution.RunManager;
import com.intellij.execution.RunnerAndConfigurationSettings;
import com.intellij.execution.configurations.RunProfile;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.project.Project;
import com.intellij.plugins.haxe.HaxeProfilerBundle;
import com.intellij.plugins.haxe.profiler.HaxeProfilableRunConfiguration;
import com.intellij.profiler.api.AttachableTargetProcess;
import com.intellij.profiler.api.ProfilerProcess;
import com.intellij.profiler.api.configurations.ProfilerAttacher;
import com.intellij.profiler.api.configurations.ProfilerConfigurationState;
import com.intellij.profiler.api.configurations.ProfilerConfigurationType;
import com.intellij.profiler.api.configurations.ProfilerStarter;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.concurrency.Promise;
import org.jetbrains.concurrency.Promises;

import javax.swing.Icon;

/**
 * The shape every Haxe entry of the IU Run-with-Profiler executor shares:
 * the platform's profiler icon, the Haxe settings group, a state persisted
 * as one element carrying its display name, a starter gated on the run
 * configuration's lane and readiness, and no attach-to-running (every lane's
 * collection starts with the process). Subclasses contribute the id, the
 * display name, their state and settings form.
 */
public abstract class HaxeProfilerConfigurationTypeBase<S extends ProfilerConfigurationState>
  implements ProfilerConfigurationType<S> {

  private static final String NAME_ATTRIBUTE = "name";

  /** The run-configuration lane this profiler entry serves. */
  @NotNull
  protected abstract HaxeProfilableRunConfiguration.Lane lane();

  /** The name of the element the state persists as. */
  @NotNull
  protected abstract String stateElementName();

  /** Reads the type's own settings into {@code state}, a template already carrying the display name. */
  protected void readSettings(@NotNull S state, @NotNull Element element) {
  }

  /** Writes the type's own settings next to the display name. */
  protected void writeSettings(@NotNull S state, @NotNull Element element) {
  }

  @Override
  public final @NotNull Icon getIcon() {
    // the same icon the Run with Profiler button carries
    return AllIcons.Actions.Profile;
  }

  @Override
  public final @NotNull String getLanguageSettingsGroup() {
    return HaxeProfilerBundle.message("haxe.profiler.settings.group");
  }

  @Override
  public final boolean isAvailable() {
    return true;
  }

  @Override
  public final @Nullable String getHelpTopic() {
    return null;
  }

  @Override
  public final @NotNull S readState(@NotNull Element element) {
    S state = getTemplateState();
    String name = element.getAttributeValue(NAME_ATTRIBUTE);
    if (name != null) state.setDisplayName(name);
    readSettings(state, element);
    return state;
  }

  @Override
  public final @NotNull Element writeState(@NotNull S state) {
    Element element = new Element(stateElementName());
    element.setAttribute(NAME_ATTRIBUTE, state.getDisplayName());
    writeSettings(state, element);
    return element;
  }

  @Override
  public final @NotNull ProfilerStarter createStarter(@NotNull S state) {
    HaxeProfilableRunConfiguration.Lane lane = lane();
    return new ProfilerStarter() {
      /** Drives the run widget's profiler BUTTON: enabled only while the SELECTED configuration is profilable. */
      @Override
      public boolean isApplicable(@NotNull Project project) {
        RunnerAndConfigurationSettings selected = RunManager.getInstance(project).getSelectedConfiguration();
        return selected != null && canRun(selected.getConfiguration());
      }

      /**
       * The launch-time gate: the lane keeps this entry off other targets'
       * runs, and the readiness gate is false while indexing and when a
       * target switch left the configuration stale.
       */
      @Override
      public boolean canRun(@NotNull RunProfile profile) {
        return profile instanceof HaxeProfilableRunConfiguration configuration
               && configuration.profilingLane() == lane
               && configuration.isProfilingReady();
      }
    };
  }

  @Override
  public final @NotNull ProfilerAttacher createAttacher(@NotNull S state) {
    return new ProfilerAttacher() {
      @Override
      public @NotNull Promise<ProfilerProcess<AttachableTargetProcess>> attachTo(@NotNull AttachableTargetProcess process,
                                                                                 @NotNull Project project) {
        return Promises.rejectedPromise(HaxeProfilerBundle.message("haxe.profiler.attach.unsupported"));
      }
    };
  }
}
