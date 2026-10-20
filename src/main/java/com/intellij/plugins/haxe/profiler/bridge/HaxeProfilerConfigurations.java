package com.intellij.plugins.haxe.profiler.bridge;

import com.intellij.execution.Executor;
import com.intellij.execution.executors.RunExecutorSettings;
import com.intellij.plugins.haxe.profiler.HaxeProfilableRunConfiguration;
import com.intellij.profiler.DefaultProfilerExecutorGroup;
import com.intellij.profiler.api.configurations.ProfilerConfigurationState;
import com.intellij.profiler.api.configurations.ProfilerConfigurationType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Set;

/**
 * Lookup of registered profiler configuration states. Each named profiler
 * configuration is one child executor of the Run-with-Profiler group, so
 * behavior follows the LAUNCHING executor's state; the by-type form is the
 * fallback for contexts without one (display, no-executor defaults).
 */
public final class HaxeProfilerConfigurations {

  private HaxeProfilerConfigurations() {
  }

  /** The state the launching profiler executor carries, or null when the executor is not a profiler entry. */
  @Nullable
  public static ProfilerConfigurationState stateFor(@NotNull Executor executor) {
    return stateForExecutor(executor.getId());
  }

  /** The id-keyed form of {@link #stateFor(Executor)} for callers holding only the executor id. */
  @Nullable
  public static ProfilerConfigurationState stateForExecutor(@NotNull String executorId) {
    DefaultProfilerExecutorGroup group = DefaultProfilerExecutorGroup.Companion.getInstance();
    RunExecutorSettings settings = group == null ? null : group.getRegisteredSettings(executorId);
    return settings instanceof DefaultProfilerExecutorGroup.ProfilerExecutorSettings profilerSettings
           ? profilerSettings.getState()
           : null;
  }

  /** The ids of the registered Haxe profiler configuration types serving the lane. */
  @NotNull
  public static Set<String> typeIdsFor(HaxeProfilableRunConfiguration.@NotNull Lane lane) {
    Set<String> ids = new HashSet<>();
    for (ProfilerConfigurationType<?> type : ProfilerConfigurationType.Companion.getEP_NAME().getExtensionList()) {
      if (type instanceof HaxeProfilerConfigurationTypeBase<?> haxeType && haxeType.lane() == lane) {
        ids.add(type.getId());
      }
    }
    return ids;
  }

  /** The type's FIRST registered state, or its template when none is registered yet. */
  @NotNull
  public static ProfilerConfigurationState stateFor(@NotNull String configurationTypeId) {
    DefaultProfilerExecutorGroup group = DefaultProfilerExecutorGroup.Companion.getInstance();
    if (group != null) {
      for (Executor child : group.childExecutors()) {
        RunExecutorSettings settings = group.getRegisteredSettings(child.getId());
        if (settings instanceof DefaultProfilerExecutorGroup.ProfilerExecutorSettings profilerSettings
            && profilerSettings.getState().getConfigurationTypeId().equals(configurationTypeId)) {
          return profilerSettings.getState();
        }
      }
    }
    return templateFor(configurationTypeId);
  }

  /** The registered configuration type's template state. */
  private static ProfilerConfigurationState templateFor(String configurationTypeId) {
    for (ProfilerConfigurationType<?> type : ProfilerConfigurationType.Companion.getEP_NAME().getExtensionList()) {
      if (type.getId().equals(configurationTypeId)) return type.getTemplateState();
    }
    throw new IllegalArgumentException("unknown profiler configuration type " + configurationTypeId);
  }
}
