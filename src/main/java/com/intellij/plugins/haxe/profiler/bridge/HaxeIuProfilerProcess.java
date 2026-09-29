package com.intellij.plugins.haxe.profiler.bridge;

import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import com.intellij.plugins.haxe.HaxeProfilerBundle;
import com.intellij.plugins.haxe.profiler.bridge.hashlink.HaxeHlDumpParserProvider;
import com.intellij.plugins.haxe.profiler.bridge.hxcpp.HaxeHxcppDumpParserProvider;
import com.intellij.profiler.FileBasedProfilerProcess;
import com.intellij.profiler.api.Attached;
import com.intellij.profiler.api.CopyFileDumpWriter;
import com.intellij.profiler.api.ProfilerDumpFileParsingResult;
import com.intellij.profiler.api.ProfilerDumpParserProvider;
import com.intellij.profiler.api.ProfilerDumpWriter;
import com.intellij.profiler.api.ProfilerError;
import com.intellij.profiler.api.ProfilerState;
import com.intellij.profiler.api.ProfilerTargetProcess;
import com.intellij.profiler.api.configurations.ProfilerConfigurationState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.List;

/**
 * One profiled Haxe run in the Profiler tool window: the tab shows the
 * platform's live-profiling placeholder (duration timer, stop-the-target
 * hint) while the run is on, then parses the dump file in the background
 * and swaps to the results — the lifecycle a Java profiling session goes
 * through. State is driven by the owning capture: attached at creation,
 * {@link #captureFinished()} when the dump is complete,
 * {@link #captureFailed} when nothing usable arrived.
 */
final class HaxeIuProfilerProcess extends FileBasedProfilerProcess<HaxeIuProfilerProcess.Target> {

  /** Parsers for every dump kind a session tab can host, matched by file extension. */
  private static final List<ProfilerDumpParserProvider> PARSER_PROVIDERS = List.of(
    new HaxeHxtSessionParserProvider(),
    new HaxeHlDumpParserProvider(),
    new HaxeHxcppDumpParserProvider());

  private final String configurationTypeId;
  private final long attachedTimestamp;

  static final class Target implements ProfilerTargetProcess {
    private final String name;

    Target(String name) {
      this.name = name;
    }

    @Override
    public @NotNull String getFullName() {
      return name;
    }
  }

  HaxeIuProfilerProcess(@NotNull Project project, @NotNull String displayName,
                        @NotNull File dumpFile, @NotNull String configurationTypeId) {
    super(project, new Target(displayName), dumpFile);
    this.configurationTypeId = configurationTypeId;
    this.attachedTimestamp = System.currentTimeMillis();
  }

  /** Flips the tab to the live-profiling placeholder; must precede the finish/fail calls. */
  void markAttached() {
    changeStateAndNotifyAsync(Attached.INSTANCE);
  }

  /** The dump file is complete — reads it on a pooled thread and swaps the tab to the parsed data. */
  void captureFinished() {
    onTargetProcessTerminated();
  }

  void captureFailed(@NotNull String reason) {
    changeStateAndNotifyAsync(new ProfilerError(reason));
  }

  @Override
  public @NotNull ProfilerConfigurationState getProfilerConfiguration() {
    return HaxeProfilerConfigurations.stateFor(configurationTypeId);
  }

  @Override
  public long getAttachedTimestamp() {
    return attachedTimestamp;
  }

  @Override
  public @Nullable String getHelpId() {
    return null;
  }

  @Override
  public boolean canBeStopped() {
    // every lane writes its data at process exit - there is nothing to stop early
    return false;
  }

  @Override
  protected @NotNull ProfilerState readPreparedDump(@NotNull File dump, @NotNull ProgressIndicator indicator) {
    ProfilerDumpParserProvider provider = providerFor(dump);
    if (provider == null) {
      return new ProfilerError(HaxeProfilerBundle.message("haxe.profiler.parse.failed", dump.getName()));
    }
    ProfilerDumpFileParsingResult result = provider.createParser(getProject()).parse(dump, indicator);
    String targetName = getTargetProcess().getFullName();
    String extension = provider.getRequiredFileExtension();
    ProfilerDumpWriter writer = new CopyFileDumpWriter(dump, targetName, attachedTimestamp, extension);
    return asProfilerState(result, writer);
  }

  @Nullable
  private static ProfilerDumpParserProvider providerFor(File dump) {
    String name = dump.getName();
    for (ProfilerDumpParserProvider provider : PARSER_PROVIDERS) {
      if (name.endsWith("." + provider.getRequiredFileExtension())) return provider;
    }
    return null;
  }
}
