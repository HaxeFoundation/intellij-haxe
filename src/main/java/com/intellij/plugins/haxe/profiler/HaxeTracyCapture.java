package com.intellij.plugins.haxe.profiler;

import com.intellij.execution.Executor;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;

/**
 * Captures a Tracy-instrumented run without run configurations importing
 * profiler classes. Direction is INVERTED versus the telemetry capture: the
 * client inside the app LISTENS on the port the launch assigns through
 * {@link #PORT_ENV_VAR}, and the receiver connects out to it. The received
 * session is persisted to the given file when the stream ends. The only
 * implementation comes from the OPTIONAL profiler descriptor.
 */
public interface HaxeTracyCapture {

  /** Tracy's own env var: the port the client's listener binds. */
  String PORT_ENV_VAR = "TRACY_PORT";
  /**
   * Tracy's own env var (value "1"): at program exit the client keeps
   * listening until a server has connected and drained the session. Without
   * it a short-lived program exits before the receiver's connect lands and
   * the whole capture is silently lost. No hang risk: the receiver connects
   * while the process lives, and killing the run kills the lingering
   * process with it.
   */
  String NO_EXIT_ENV_VAR = "TRACY_NO_EXIT";

  @Nullable
  static HaxeTracyCapture getInstance() {
    return ApplicationManager.getApplication().getService(HaxeTracyCapture.class);
  }

  /**
   * One run's capture, named after the run configuration in the profiler
   * UI; null when no port could be allocated. {@code executor} is the
   * launching Run-with-Profiler entry — each named profiler configuration
   * is one, and the capture takes ITS settings.
   */
  @Nullable
  Handle start(@NotNull Project project, @NotNull String displayName, @NotNull Path sessionFile,
               @NotNull Executor executor);

  /** Null-safe form of {@link #start}: null without the profiler module too. */
  @Nullable
  static Handle startCapture(@NotNull Project project, @NotNull String displayName, @NotNull Path sessionFile,
                             @NotNull Executor executor) {
    HaxeTracyCapture capture = getInstance();
    return capture == null ? null : capture.start(project, displayName, sessionFile, executor);
  }

  interface Handle {
    int port();

    /**
     * Called after the profiled process terminated: acknowledges the
     * client's shutdown wait (or abandons the connect attempts), letting
     * the capture finish, persist and notify.
     */
    void processExited();
  }
}
