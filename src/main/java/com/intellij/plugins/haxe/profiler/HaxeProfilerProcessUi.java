package com.intellij.plugins.haxe.profiler;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;

/**
 * The Profiler tool window's view of a live profiled run, without run
 * configurations importing profiler classes. Attaching opens a process tab
 * showing the platform's live-profiling placeholder (duration timer) and
 * its green "Profiler attached" balloon on the tool window button; the
 * session then swaps the SAME tab to the parsed results when the dump
 * completes — the lifecycle a Java profiling session gets. The only
 * implementation comes from the OPTIONAL profiler descriptor.
 */
public interface HaxeProfilerProcessUi {

  /** One profiled run's tab in the Profiler tool window. */
  interface Session {
    /** The dump file is complete: parses it in the background and swaps the tab to the results. */
    void dataReady();

    /** Nothing usable was captured; the tab shows the reason. */
    void failed(@NotNull String reason);
  }

  /** Opens the process tab for a run whose dump will appear at {@code dumpFile}. */
  @Nullable
  Session attached(@NotNull Project project, @NotNull String displayName, @NotNull Path dumpFile);

  /** Null-safe form of {@link #attached}: null without the profiler module too. */
  @Nullable
  static Session notifyAttached(@NotNull Project project, @NotNull String displayName, @NotNull Path dumpFile) {
    HaxeProfilerProcessUi ui = ApplicationManager.getApplication().getService(HaxeProfilerProcessUi.class);
    return ui == null ? null : ui.attached(project, displayName, dumpFile);
  }
}
