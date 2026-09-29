package com.intellij.plugins.haxe.runner.debugger.hxcpp.intellij;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.Executor;
import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.execution.configurations.RunProfileState;
import com.intellij.execution.process.ElevationService;
import com.intellij.execution.process.ProcessEvent;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.process.ProcessListener;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.plugins.haxe.HaxeProfilerBundle;
import com.intellij.plugins.haxe.profiler.HaxeProfilableRunConfiguration;
import com.intellij.plugins.haxe.profiler.HaxeProfilerExecutorSupport;
import com.intellij.plugins.haxe.profiler.HaxeProfilerProcessUi;
import com.intellij.plugins.haxe.profiler.HaxeProfilingNotifier;
import com.intellij.plugins.haxe.profiler.HaxeTelemetryCapture;
import com.intellij.plugins.haxe.profiler.HaxeTracyCapture;
import com.intellij.plugins.haxe.runner.debugger.dap.ide.DapCommandLineRunningState;
import com.intellij.plugins.haxe.runner.debugger.dap.ide.DapExecutableRunConfigurationBase;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;

/**
 * An HXCPP (IntelliJ debug server) run/debug configuration: exactly the
 * shared executable configuration. For debugging, the executable must have
 * been compiled with {@code -debug} and {@code -lib
 * intellij-hxcpp-debug-server}. No host/port settings: the IDE listens on an
 * ephemeral loopback port per session and hands it to the debuggee through
 * env vars, so nothing is baked into the build and concurrent sessions never
 * collide. A build with the library runs normally outside the debugger (the
 * server makes one quick connect attempt and stays out of the way).
 */
public class HxcppIntellijRunConfiguration extends DapExecutableRunConfigurationBase implements HaxeProfilableRunConfiguration {

  /** The report the injected profiling bootstrap writes beside the executable. */
  public static final String PROFILER_DUMP_FILE_NAME = "hxcppprofile.hxcppprof";
  /** The telemetry session the IDE receiver persists beside the executable. */
  public static final String TELEMETRY_SESSION_FILE_NAME = "hxcppprofile.hxtsession";

  public HxcppIntellijRunConfiguration(String name, Project project, ConfigurationFactory factory) {
    super(name, project, factory);
  }

  @Override
  public @NotNull SettingsEditor<? extends RunConfiguration> getConfigurationEditor() {
    return new HxcppIntellijRunConfigurationEditor(getProject());
  }

  @Override
  public @NotNull Lane profilingLane() {
    return Lane.HXCPP;
  }

  @Override
  public boolean isProfilingReady() {
    return !DumbService.isDumb(getProject()) && expectedDumpPath() != null;
  }

  /**
   * Where a profiling run's report appears: the before-launch compile bakes
   * this ABSOLUTE path into the injected bootstrap, so the executable's
   * working directory cannot displace it. Null while no executable is
   * configured yet.
   */
  @Nullable
  public Path expectedDumpPath() {
    String executable = getExecutablePath();
    if (executable.isBlank()) return null;
    Path parent = Path.of(executable).getParent();
    return parent == null ? null : parent.resolve(PROFILER_DUMP_FILE_NAME);
  }

  @Override
  public RunProfileState getState(@NotNull Executor executor, @NotNull ExecutionEnvironment env) throws ExecutionException {
    requireModule();
    Path dumpPath = expectedDumpPath();
    // non-null exactly when the IU "Run with Profiler" executor launched us
    // with the hxcpp profiler configuration selected (the additions themselves
    // go into the before-launch compile, not this command line)
    boolean profiling = dumpPath != null
                        && HaxeProfilerExecutorSupport.hxcppProfilingAdditions(executor, false, dumpPath) != null;
    // the telemetry capture (samples with a time axis) supersedes the text
    // report when the receiver opens; the report stays the fallback outcome
    HaxeTelemetryCapture.Handle capture = profiling
                                          ? HaxeTelemetryCapture.startCapture(getProject(), getName(), dumpPath.resolveSibling(TELEMETRY_SESSION_FILE_NAME), Lane.HXCPP)
                                          : null;
    // the tracy entry: exact zones, no bootstrap - the receiver connects to
    // the client listening on the port we assign
    boolean tracy = dumpPath != null && HaxeProfilerExecutorSupport.hxcppTracyAdditions(executor, false) != null;
    HaxeTracyCapture.Handle tracyCapture = tracy
                                           ? HaxeTracyCapture.startCapture(getProject(), getName(), dumpPath.resolveSibling(TELEMETRY_SESSION_FILE_NAME), executor)
                                           : null;
    // the Process CPU curve needs the scheduler's context switches, which
    // tracy's system tracing only streams from a privileged process
    boolean elevated = tracy && HaxeProfilerExecutorSupport.hxcppTracyElevated(executor);
    return new DapCommandLineRunningState(env, getProject(), () -> profiledCommandLine(capture, tracyCapture)) {
      @Override
      protected @NotNull ProcessHandler createProcessHandler(@NotNull GeneralCommandLine commandLine) throws ExecutionException {
        return elevated ? elevatedProcessHandler(commandLine) : super.createProcessHandler(commandLine);
      }

      @Override
      protected @NotNull ProcessHandler startProcess() throws ExecutionException {
        ProcessHandler handler = super.startProcess();
        if (capture != null || tracyCapture != null) {
          handler.addProcessListener(new ProcessListener() {
            @Override
            public void processTerminated(@NotNull ProcessEvent event) {
              if (capture != null) capture.processExited();
              if (tracyCapture != null) tracyCapture.processExited();
            }
          });
        }
        else if (profiling) {
          // the telemetry receiver could not open - the text report written
          // at exit is still a full profiling result
          HaxeProfilerProcessUi.Session session = HaxeProfilerProcessUi.notifyAttached(getProject(), getName(), dumpPath);
          HaxeProfilingNotifier.watch(getProject(), handler, dumpPath, "haxe.profiler.hxcpp.dump.missing", session);
        }
        return handler;
      }
    };
  }

  /**
   * Starts the command through the platform's elevation service (UAC on
   * Windows, pkexec/sudo on Linux; the prompt appears at launch and the
   * agent proxies stdio, so console and kill work as in a plain run).
   * Declining the prompt fails the launch the same way any start failure
   * does.
   */
  @NotNull
  private static ProcessHandler elevatedProcessHandler(@NotNull GeneralCommandLine commandLine) throws ExecutionException {
    ElevationService elevation = ApplicationManager.getApplication().getService(ElevationService.class);
    if (elevation == null || !elevation.isAvailable()) {
      throw new ExecutionException(HaxeProfilerBundle.message("haxe.profiler.tracy.elevation.unavailable"));
    }
    return elevation.createProcessHandler(commandLine);
  }

  /** The run command line, handing each active capture its endpoint through the lane's env var. */
  private GeneralCommandLine profiledCommandLine(@Nullable HaxeTelemetryCapture.Handle telemetry,
                                                 @Nullable HaxeTracyCapture.Handle tracy) throws ExecutionException {
    GeneralCommandLine commandLine = createCommandLine();
    if (telemetry != null) {
      commandLine.withEnvironment(HaxeTelemetryCapture.ENDPOINT_ENV_VAR, "127.0.0.1:" + telemetry.port());
    }
    if (tracy != null) {
      commandLine.withEnvironment(HaxeTracyCapture.PORT_ENV_VAR, String.valueOf(tracy.port()));
      commandLine.withEnvironment(HaxeTracyCapture.NO_EXIT_ENV_VAR, "1");
    }
    return commandLine;
  }
}
