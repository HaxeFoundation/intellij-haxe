package com.intellij.plugins.haxe.profiler;

import com.intellij.execution.Executor;
import com.intellij.openapi.application.ApplicationManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.util.List;

/**
 * Bridges run configurations to the IU "Run with Profiler" executor without
 * them importing profiler classes. The only implementation comes from the
 * OPTIONAL profiler descriptor; on IDEs without the profiler module
 * {@link #getInstance()} is null and every launch is a plain one.
 */
public interface HaxeProfilerExecutorSupport {

  @Nullable
  static HaxeProfilerExecutorSupport getInstance() {
    return ApplicationManager.getApplication().getService(HaxeProfilerExecutorSupport.class);
  }

  /**
   * The sampling rate a profiler launch under this executor should use, or
   * null when the executor is not a HashLink profiler one (plain Run, Debug,
   * or another profiler configuration's executor).
   */
  @Nullable
  Integer hashlinkSamplesPerSecondFor(@NotNull Executor executor);

  /**
   * The compile additions an hxcpp profiling build must carry (profiler
   * defines plus the injected start/stop bootstrap writing {@code dumpPath}),
   * or null when the executor is not the hxcpp profiler one.
   * {@code limeFamily} picks the tool spelling — lime's {@code --haxeflag=}
   * wrappers against plain haxe flags.
   */
  @Nullable
  List<String> hxcppProfilingAdditionsFor(@NotNull Executor executor, boolean limeFamily, @NotNull Path dumpPath);

  /**
   * The compile additions an hxcpp TRACY launch must add to its build (the
   * tracy defines — no bootstrap: the runtime instruments every function
   * itself), or null when the executor is not the hxcpp tracy one.
   */
  @Nullable
  List<String> hxcppTracyAdditionsFor(@NotNull Executor executor, boolean limeFamily);

  /**
   * True when an hxcpp TRACY launch under this executor should start the
   * program ELEVATED so the client's system tracing can stream the
   * scheduler's context switches (the Process CPU curve). False for other
   * executors, when the setting is off, and on OSes where tracy has no
   * system-tracing backend (macOS).
   */
  boolean hxcppTracyElevatedFor(@NotNull Executor executor);

  /**
   * The compile additions a FLASH/AIR profiling build must carry (the
   * injected flash.sampler collector and its boot macro), or null when the
   * executor is not the flash profiler one.
   */
  @Nullable
  List<String> flashProfilingAdditionsFor(@NotNull Executor executor, boolean limeFamily);

  /**
   * Compile additions for a HashLink-profiling launch (the {@code hl_profile}
   * define frameworks key their profiler instrumentation on); null when the
   * launch is not a HashLink-profiling one.
   */
  @Nullable
  List<String> hlProfilingAdditionsFor(@NotNull Executor executor, boolean limeFamily);

  /**
   * Compile additions for a JS-profiling launch (source-map emission, so
   * sampled positions map back to the .hx sources); null when the launch
   * is not a JS-profiling one.
   */
  @Nullable
  List<String> jsProfilingAdditionsFor(@NotNull Executor executor, boolean limeFamily);

  /**
   * V8's sampling interval (microseconds) a browser launch under this
   * executor should profile with, or null when the executor is not the
   * JavaScript profiler one.
   */
  @Nullable
  Integer jsSamplingIntervalUsFor(@NotNull Executor executor);

  /**
   * Every profiler child executor that can launch the lane (its
   * registered profiler entries, in the executor group's order) — what a
   * tool-window "Profile" action offers and executes with. Empty when the
   * lane has no entries.
   */
  @NotNull
  List<ProfilerEntry> profilerExecutorsFor(HaxeProfilableRunConfiguration.@NotNull Lane lane);

  /** One registered profiler entry: its executor and the profile's user-visible name ("hxcpp Tracy"). */
  record ProfilerEntry(@NotNull Executor executor, @NotNull String displayName) {
  }

  /** Null-safe lookup: null unless the profiler module is present AND the executor is a HashLink profiler one. */
  @Nullable
  static Integer hashlinkSamplesFor(@NotNull Executor executor) {
    HaxeProfilerExecutorSupport support = getInstance();
    return support == null ? null : support.hashlinkSamplesPerSecondFor(executor);
  }

  /** Null-safe form of {@link #hxcppProfilingAdditionsFor}. */
  @Nullable
  static List<String> hxcppProfilingAdditions(@NotNull Executor executor, boolean limeFamily, @NotNull Path dumpPath) {
    HaxeProfilerExecutorSupport support = getInstance();
    return support == null ? null : support.hxcppProfilingAdditionsFor(executor, limeFamily, dumpPath);
  }

  /** Null-safe form of {@link #hxcppTracyAdditionsFor}. */
  @Nullable
  static List<String> hxcppTracyAdditions(@NotNull Executor executor, boolean limeFamily) {
    HaxeProfilerExecutorSupport support = getInstance();
    return support == null ? null : support.hxcppTracyAdditionsFor(executor, limeFamily);
  }

  /** Null-safe form of {@link #hxcppTracyElevatedFor}. */
  static boolean hxcppTracyElevated(@NotNull Executor executor) {
    HaxeProfilerExecutorSupport support = getInstance();
    return support != null && support.hxcppTracyElevatedFor(executor);
  }

  /** Null-safe form of {@link #flashProfilingAdditionsFor}. */
  @Nullable
  static List<String> flashProfilingAdditions(@NotNull Executor executor, boolean limeFamily) {
    HaxeProfilerExecutorSupport support = getInstance();
    return support == null ? null : support.flashProfilingAdditionsFor(executor, limeFamily);
  }

  /** Null-safe form of {@link #hlProfilingAdditionsFor}. */
  @Nullable
  static List<String> hlProfilingAdditions(@NotNull Executor executor, boolean limeFamily) {
    HaxeProfilerExecutorSupport support = getInstance();
    return support == null ? null : support.hlProfilingAdditionsFor(executor, limeFamily);
  }

  /** Null-safe form of {@link #jsSamplingIntervalUsFor}. */
  @Nullable
  static Integer jsSamplingIntervalUs(@NotNull Executor executor) {
    HaxeProfilerExecutorSupport support = getInstance();
    return support == null ? null : support.jsSamplingIntervalUsFor(executor);
  }

  /** Null-safe form of {@link #jsProfilingAdditionsFor}. */
  @Nullable
  static List<String> jsProfilingAdditions(@NotNull Executor executor, boolean limeFamily) {
    HaxeProfilerExecutorSupport support = getInstance();
    return support == null ? null : support.jsProfilingAdditionsFor(executor, limeFamily);
  }

  /** Null-safe form of {@link #profilerExecutorsFor}: empty without the profiler module too. */
  @NotNull
  static List<ProfilerEntry> profilerExecutors(HaxeProfilableRunConfiguration.@NotNull Lane lane) {
    HaxeProfilerExecutorSupport support = getInstance();
    return support == null ? List.of() : support.profilerExecutorsFor(lane);
  }
}
