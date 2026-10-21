package com.intellij.plugins.haxe.profiler;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;

/**
 * Receives a profiled process's telemetry stream without run configurations
 * importing profiler classes: the capture listens on an ephemeral loopback
 * port, the launch hands the endpoint to the injected collector through
 * {@link #ENDPOINT_ENV_VAR}, and the received session is persisted to the
 * given file when the process ends. The only implementation comes from the
 * OPTIONAL profiler descriptor.
 */
public interface HaxeTelemetryCapture {

  /**
   * The env var the injected hxcpp collector reads: {@code host:port} to
   * stream to. The flash lane needs no handover — its runtime reads the
   * receiver's address from {@code ~/.telemetry.cfg}, which the capture
   * installs for the session.
   */
  String ENDPOINT_ENV_VAR = "IJ_HAXE_TELEMETRY";

  @Nullable
  static HaxeTelemetryCapture getInstance() {
    return ApplicationManager.getApplication().getService(HaxeTelemetryCapture.class);
  }

  /**
   * One run's capture, named after the run configuration in the profiler
   * UI; null when the listener cannot open. {@code lane} attributes the
   * session to its profiler entry and phrases the nothing-arrived notice.
   */
  @Nullable
  Handle start(@NotNull Project project, @NotNull String displayName, @NotNull Path sessionFile,
               HaxeProfilableRunConfiguration.@NotNull Lane lane);

  /** Null-safe form of {@link #start}: null without the profiler module too. */
  @Nullable
  static Handle startCapture(@NotNull Project project, @NotNull String displayName, @NotNull Path sessionFile,
                             HaxeProfilableRunConfiguration.@NotNull Lane lane) {
    HaxeTelemetryCapture capture = getInstance();
    return capture == null ? null : capture.start(project, displayName, sessionFile, lane);
  }

  interface Handle {
    int port();

    /**
     * Called after the profiled process terminated: waits briefly for the
     * stream to drain, persists the session and notifies — offering to open
     * it, or explaining that nothing arrived.
     */
    void processExited();
  }
}
