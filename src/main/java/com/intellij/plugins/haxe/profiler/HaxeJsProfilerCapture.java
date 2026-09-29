package com.intellij.plugins.haxe.profiler;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;

/**
 * Captures a browser run's V8 CPU profile without run configurations
 * importing profiler classes: the launch starts a Chromium child with a
 * DevTools port, and this capture attaches over CDP and drives the
 * sampling profiler in periodic stop/start SEGMENTS streamed into
 * {@code sessionFile} — the live view follows the run, and a browser
 * closed by hand keeps every segment already collected. The only
 * implementation comes from the OPTIONAL profiler descriptor.
 */
public interface HaxeJsProfilerCapture {

  @Nullable
  static HaxeJsProfilerCapture getInstance() {
    return ApplicationManager.getApplication().getService(HaxeJsProfilerCapture.class);
  }

  /**
   * One run's capture: connects to {@code debugPort} (retrying while the
   * browser starts), profiles the first page target and shows the profiler
   * session tab. {@code contentRoot} and {@code baseUrl} locate the served
   * scripts' source maps, so sampled positions map back to the .hx sources;
   * either may be null (an external URL run has no local scripts). Null
   * without the profiler module.
   */
  @Nullable
  Handle start(@NotNull Project project, @NotNull String displayName, @NotNull Path sessionFile,
               int debugPort, int samplingIntervalUs, @Nullable Path contentRoot, @Nullable String baseUrl);

  /** Null-safe form of {@link #start}: null without the profiler module too. */
  @Nullable
  static Handle startCapture(@NotNull Project project, @NotNull String displayName, @NotNull Path sessionFile,
                             int debugPort, int samplingIntervalUs, @Nullable Path contentRoot, @Nullable String baseUrl) {
    HaxeJsProfilerCapture capture = getInstance();
    return capture == null ? null
                           : capture.start(project, displayName, sessionFile, debugPort, samplingIntervalUs, contentRoot, baseUrl);
  }

  interface Handle {
    /**
     * Collects the final segment and opens the session; must run while the
     * browser still lives (the Stop action calls it before the kill).
     * Bounded — a wedged browser cannot hang the stop.
     */
    void finishCapture();

    /** The browser died without a stop: finalizes the session with the segments already streamed. */
    void connectionLost();
  }
}
