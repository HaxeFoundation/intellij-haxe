package com.intellij.plugins.haxe.v2.buildtools.server;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.process.OSProcessHandler;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.project.Project;
import com.intellij.plugins.haxe.v2.buildtools.HaxeProjectTrust;
import com.intellij.plugins.haxe.v2.buildtools.HaxeToolEnvironment;
import com.intellij.plugins.haxe.v2.buildtools.HaxeToolPathResolver;
import com.intellij.plugins.haxe.v2.buildtools.settings.HaxeBuildToolSettings;
import com.intellij.util.execution.ParametersListUtil;
import lombok.CustomLog;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The mechanics every manager of haxe server processes shares: the entries
 * and their console listeners, stop, restart and removal, the handling of a
 * process that died on its own, and the state topic the compiler caches and
 * the server console follow. The start gate ({@link #mayStart}) and the
 * server command line ({@link #serverCommandLine}) live here too. A subclass
 * contributes how a process is launched ({@link #startLocked}), what a
 * stopped one leaves behind ({@link #clearDerivedState}) and the console's
 * termination line.
 *
 * A stopped or crashed entry keeps its output backlog, so its console tab
 * survives a restart. All processes stop with the project. Methods whose
 * names end in {@code Locked} expect the caller to hold this manager's lock.
 */
@CustomLog
abstract class HaxeServerProcessManager<P extends HaxeServerProcess> implements HaxeServerProcesses, Disposable {

  protected final Project project;
  protected final Map<String, P> processes = new LinkedHashMap<>();

  HaxeServerProcessManager(@NotNull Project project) {
    this.project = project;
  }

  /**
   * Whether a server may start at all: the compilation server switched on and
   * the project trusted, since every compile through a server runs project
   * macros. Clients of a refused start behave as if the server were disabled.
   */
  protected boolean mayStart() {
    return HaxeBuildToolSettings.getInstance(project).isCompilationServerEnabled()
           && HaxeProjectTrust.checkForBackgroundEvaluation(project);
  }

  /**
   * The server's command line: its haxe, the configured server arguments,
   * then the mode arguments, with the SDK's folders in front of PATH. It runs
   * in the project directory when that exists; every request carries its own
   * {@code --cwd}, so a missing directory (a test fixture, a freshly moved
   * project) must not fail the start.
   */
  @NotNull
  protected final GeneralCommandLine serverCommandLine(@NotNull P process, @NotNull String... modeArguments) {
    List<String> command = new ArrayList<>();
    command.add(process.executablePath);
    command.addAll(ParametersListUtil.parse(HaxeBuildToolSettings.getInstance(project).getCompilationServerArguments()));
    command.addAll(List.of(modeArguments));
    GeneralCommandLine commandLine = new GeneralCommandLine(command);
    String basePath = project.getBasePath();
    if (basePath != null && new File(basePath).isDirectory()) {
      commandLine.withWorkDirectory(basePath);
    }
    return HaxeToolEnvironment.withSdkOnPath(commandLine, HaxeToolPathResolver.findSdk(project, process.sdkName));
  }

  @Override
  @NotNull
  public synchronized List<ServerInfo> getServers() {
    List<ServerInfo> result = new ArrayList<>();
    for (P process : processes.values()) {
      boolean running = process.isAlive();
      result.add(new ServerInfo(process.id, process.displayName, running ? process.port : -1, running));
    }
    return result;
  }

  @Override
  public synchronized boolean isRunning(@NotNull String id) {
    P process = processes.get(id);
    return process != null && process.isAlive();
  }

  @Override
  public synchronized void addOutputListener(@NotNull String id, @NotNull ServerOutputListener listener) {
    P process = processes.get(id);
    if (process != null) {
      process.output.addListener(listener);
    }
  }

  @Override
  public synchronized void removeOutputListener(@NotNull String id, @NotNull ServerOutputListener listener) {
    P process = processes.get(id);
    if (process != null) {
      process.output.removeListener(listener);
    }
  }

  @Override
  public synchronized void stopServer(@NotNull String id) {
    P process = processes.get(id);
    if (process != null && stopProcessLocked(process)) {
      clearDerivedState(id);
      fireStateChanged();
    }
  }

  /**
   * Stops the process and FORGETS it, backlog included. Closing a console tab
   * calls this, because the user no longer wants that process. The entry
   * comes back when something next asks for it.
   */
  @Override
  public synchronized void removeServer(@NotNull String id) {
    P process = processes.remove(id);
    if (process != null) {
      stopProcessLocked(process);
      clearDerivedState(id);
      // fires even for a dead entry, because the console shows one tab per entry
      fireStateChanged();
    }
  }

  @Override
  public synchronized void restartServer(@NotNull String id) {
    P process = processes.get(id);
    if (process == null) return;
    // The console actions ask for trust on the EDT first; this check covers
    // any other caller. It runs BEFORE the teardown, so a refusal leaves the
    // running process and every state listener untouched.
    if (!HaxeProjectTrust.checkForBackgroundEvaluation(project)) return;
    stopProcessLocked(process);
    clearDerivedState(id);
    startLocked(process);
  }

  /** Stops every process, as a settings change requires. The entries and their backlogs remain. */
  public synchronized void stopAll() {
    boolean anyStopped = false;
    for (P process : processes.values()) {
      anyStopped |= stopProcessLocked(process);
    }
    if (anyStopped) {
      fireStateChanged();
    }
  }

  /** Stops the process and releases what the subclass attached to it; whether it was running. */
  protected final boolean stopProcessLocked(@NotNull P process) {
    boolean wasAlive = process.isAlive();
    process.stop();
    afterStopLocked(process);
    return wasAlive;
  }

  /** Launches the command for the process and routes its termination back here. */
  protected final void startProcess(@NotNull P process, @NotNull GeneralCommandLine commandLine) throws ExecutionException {
    process.start(commandLine, this::terminatedMessage, (handler, exitCode) -> onTerminated(process, handler, exitCode));
  }

  private synchronized void onTerminated(@NotNull P process, @NotNull OSProcessHandler handler, int exitCode) {
    // a deliberate stop clears the handler first, so a matching handler means the process died on its own
    if (process.handler() != handler) return;
    log.info("haxe server process terminated with exit code " + exitCode + " (" + process.id + ")");
    stopProcessLocked(process);
    fireStateChanged();
  }

  /**
   * Publishes the state change on the EDT, outside the manager's lock, so listeners may query the manager
   * or refresh UI. The explicit non-modal state matters: listeners drop PSI caches, and a runnable that a
   * pooled thread submits without a modality state runs write-unsafe.
   */
  protected final void fireStateChanged() {
    ApplicationManager.getApplication().invokeLater(() -> {
      if (!project.isDisposed()) {
        project.getMessageBus().syncPublisher(HaxeCompilationServerListener.TOPIC).serverStateChanged();
      }
    }, ModalityState.nonModal());
  }

  /** Starts the process; the port it then serves on or connected through, or -1 when the start failed. */
  protected abstract int startLocked(@NotNull P process);

  /** Drops what a stopped process left behind (failures, request statistics), so a fresh one starts clean. */
  protected abstract void clearDerivedState(@NotNull String id);

  /** Releases what the subclass attached to the process, such as a connection; the process is stopped. */
  protected void afterStopLocked(@NotNull P process) {
  }

  /** The console line for a process that ended with the exit code. */
  @NotNull
  protected abstract String terminatedMessage(int exitCode);

  @Override
  public void dispose() {
    stopAll();
  }
}
