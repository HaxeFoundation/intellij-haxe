package com.intellij.plugins.haxe.v2.buildtools.server;

import com.intellij.plugins.haxe.v2.buildtools.HaxeProjectTrust;
import com.intellij.plugins.haxe.v2.buildtools.HaxeCommandNotifications;
import com.intellij.plugins.haxe.v2.buildtools.HaxeToolPathResolver;
import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.process.OSProcessHandler;
import com.intellij.execution.process.ProcessEvent;
import com.intellij.execution.process.ProcessListener;
import com.intellij.execution.process.ProcessOutputType;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.plugins.haxe.HaxeBundle;
import com.intellij.plugins.haxe.v2.buildtools.settings.HaxeBuildToolSettings;
import com.intellij.util.execution.ParametersListUtil;
import com.intellij.util.io.BaseOutputReader;
import com.intellij.util.net.NetUtils;
import lombok.CustomLog;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Runs the project's haxe compilation servers ({@code haxe --wait <port>}).
 * A server's cache belongs to one haxe BINARY, and a project whose modules
 * use different SDKs can need several haxe versions at once. The manager
 * therefore runs one server process per resolved haxe executable and keys
 * the instance by that path. The first compile that connects through an SDK
 * starts its server.
 *
 * A stopped or crashed instance keeps its entry and its output backlog, so
 * its console tab survives a restart. All servers stop with the project.
 * Methods whose names end in {@code Locked} expect the caller to hold this
 * manager's lock.
 */
@Service(Service.Level.PROJECT)
@CustomLog
public final class HaxeCompilationServerManager implements Disposable {

  /** The managed server always runs on the local machine; every client connects to loopback. */
  public static final String SERVER_HOST = "127.0.0.1";

  /** Output chunks kept per instance, replayed into a console that opens later. */
  private static final int BACKLOG_LIMIT = 2000;

  /** Receives one server's process output, plus lifecycle lines such as its termination, for the server console. */
  public interface ServerOutputListener {
    void onOutput(@NotNull String text, @NotNull Key<?> outputType);
  }

  /** UI snapshot of one server instance; {@code id} is the haxe executable path the instance is keyed by. */
  public record ServerInfo(@NotNull String id, @NotNull String displayName, int port, boolean running) {
  }

  private record OutputChunk(@NotNull String text, @NotNull Key<?> outputType) {
  }

  private static final class ServerInstance {
    final String executablePath;
    final String displayName;
    final List<ServerOutputListener> listeners = new CopyOnWriteArrayList<>();
    final Deque<OutputChunk> backlog = new ArrayDeque<>();
    OSProcessHandler handler;
    int port = -1;

    ServerInstance(String executablePath, String displayName) {
      this.executablePath = executablePath;
      this.displayName = displayName;
    }

    boolean isAlive() {
      return handler != null && !handler.isProcessTerminated();
    }
  }

  private final Project project;
  private final Map<String, ServerInstance> servers = new LinkedHashMap<>();

  public HaxeCompilationServerManager(@NotNull Project project) {
    this.project = project;
  }

  @NotNull
  public static HaxeCompilationServerManager getInstance(@NotNull Project project) {
    return project.getService(HaxeCompilationServerManager.class);
  }

  /** The id of the server instance serving the container: its effective SDK's haxe executable. */
  @NotNull
  public static String serverIdFor(@NotNull Project project, @NotNull String containerId) {
    String sdkName = HaxeToolPathResolver.effectiveSdkName(project, containerId);
    return HaxeToolPathResolver.resolveHaxeExecutable(project, sdkName);
  }

  /**
   * Starts the server for the haxe binary the given SDK resolves to, unless
   * it already runs, and returns its port. Returns -1 when the server is
   * disabled or failed to start; callers then compile without --connect.
   */
  public synchronized int ensureRunning(@Nullable String preferredSdkName) {
    HaxeBuildToolSettings settings = HaxeBuildToolSettings.getInstance(project);
    if (!settings.isCompilationServerEnabled()) {
      return -1;
    }
    // Every compile through the server runs project macros, so an untrusted
    // project gets no server. Clients then behave as if it were disabled.
    if (!HaxeProjectTrust.checkForBackgroundEvaluation(project)) {
      return -1;
    }
    String executablePath = HaxeToolPathResolver.resolveHaxeExecutable(project, preferredSdkName);
    ServerInstance instance = servers.get(executablePath);
    if (instance == null) {
      instance = new ServerInstance(executablePath, displayNameFor(preferredSdkName, executablePath));
      servers.put(executablePath, instance);
    }
    if (instance.isAlive()) {
      return instance.port;
    }
    return startLocked(instance);
  }

  /** The running server's port for the given SDK's binary, or -1. */
  public synchronized int runningPortForSdk(@Nullable String preferredSdkName) {
    return runningPort(HaxeToolPathResolver.resolveHaxeExecutable(project, preferredSdkName));
  }

  public synchronized boolean isAnyRunning() {
    return servers.values().stream().anyMatch(ServerInstance::isAlive);
  }

  /** Whether the instance with the given id (the haxe executable path) currently runs. */
  public synchronized boolean isRunning(@NotNull String id) {
    return runningPort(id) > 0;
  }

  /** The given instance's port while it runs, or -1. */
  public synchronized int runningPort(@NotNull String id) {
    ServerInstance instance = servers.get(id);
    return instance != null && instance.isAlive() ? instance.port : -1;
  }

  /** Every known instance, running or stopped, in the order they were first requested. */
  @NotNull
  public synchronized List<ServerInfo> getServers() {
    List<ServerInfo> result = new ArrayList<>();
    for (ServerInstance instance : servers.values()) {
      boolean running = instance.isAlive();
      int port = running ? instance.port : -1;
      result.add(new ServerInfo(instance.executablePath, instance.displayName, port, running));
    }
    return result;
  }

  /** Stops every instance, as a settings change requires. The entries and their backlogs remain. */
  public synchronized void stop() {
    boolean anyStopped = false;
    for (ServerInstance instance : servers.values()) {
      anyStopped |= stopInstanceLocked(instance);
    }
    if (anyStopped) {
      fireStateChanged();
    }
  }

  public synchronized void stopServer(@NotNull String id) {
    ServerInstance instance = servers.get(id);
    if (instance != null && stopInstanceLocked(instance)) {
      clearServerDerivedState(id);
      fireStateChanged();
    }
  }

  /**
   * Stops the instance and FORGETS it, backlog included. Closing a console tab
   * calls this, because the user no longer wants that SDK's server. The entry
   * comes back when a compile against that SDK next asks for a server.
   */
  public synchronized void removeServer(@NotNull String id) {
    ServerInstance instance = servers.remove(id);
    if (instance != null) {
      stopInstanceLocked(instance);
      clearServerDerivedState(id);
      // fires even for a dead instance, because the console shows one tab per entry
      fireStateChanged();
    }
  }

  /** Restarts the instance, or starts it when it is not running. Call off the EDT. */
  public synchronized void restartServer(@NotNull String id) {
    ServerInstance instance = servers.get(id);
    if (instance == null) {
      return;
    }
    // This path skips the trust check in ensureRunning. The console actions
    // ask for trust on the EDT first; this check covers any other caller. It
    // runs BEFORE the teardown, so a refusal leaves the running server and
    // every state listener untouched.
    if (!HaxeProjectTrust.checkForBackgroundEvaluation(project)) {
      return;
    }
    stopInstanceLocked(instance);
    clearServerDerivedState(id);
    startLocked(instance);
  }

  /** Drops the stopped server's failures and request statistics, so a fresh server starts clean. */
  private void clearServerDerivedState(@NotNull String id) {
    HaxeContextFailures.getInstance(project).clearForServer(id);
    HaxeServerMetrics.getInstance(project).clear(id);
  }

  /** Registers a console listener for one instance and replays its buffered output to it. */
  public synchronized void addOutputListener(@NotNull String id, @NotNull ServerOutputListener listener) {
    ServerInstance instance = servers.get(id);
    if (instance == null) {
      return;
    }
    instance.listeners.add(listener);
    instance.backlog.forEach(chunk -> listener.onOutput(chunk.text(), chunk.outputType()));
  }

  public synchronized void removeOutputListener(@NotNull String id, @NotNull ServerOutputListener listener) {
    ServerInstance instance = servers.get(id);
    if (instance != null) {
      instance.listeners.remove(listener);
    }
  }

  private int startLocked(ServerInstance instance) {
    HaxeBuildToolSettings settings = HaxeBuildToolSettings.getInstance(project);
    try {
      int chosenPort = choosePortLocked(settings, instance);
      GeneralCommandLine commandLine = serverCommandLine(settings, instance, chosenPort);
      OSProcessHandler handler = newServerHandler(commandLine);
      handler.addProcessListener(new ServerOutputForwarder(instance, handler));
      handler.startNotify();
      instance.handler = handler;
      instance.port = chosenPort;
      log.info("Started haxe compilation server on port " + chosenPort + " (" + instance.executablePath + ")");
      fireStateChanged();
      return chosenPort;
    }
    catch (ExecutionException | IOException e) {
      notifyStartFailed(StringUtil.notNullize(e.getMessage()));
      stopInstanceLocked(instance);
      return -1;
    }
  }

  @NotNull
  private GeneralCommandLine serverCommandLine(HaxeBuildToolSettings settings, ServerInstance instance, int port) {
    List<String> command = new ArrayList<>();
    command.add(instance.executablePath);
    command.addAll(ParametersListUtil.parse(settings.getCompilationServerArguments()));
    command.add("--wait");
    command.add(String.valueOf(port));

    GeneralCommandLine commandLine = new GeneralCommandLine(command);
    // Every request carries its own --cwd, so the server's working directory
    // does not matter. A missing project directory, as in a test fixture or a
    // freshly moved project, must not fail the start.
    String basePath = project.getBasePath();
    if (basePath != null && new File(basePath).isDirectory()) {
      commandLine.withWorkDirectory(basePath);
    }
    return commandLine;
  }

  /** A handler for the server process. It prints the command line as its first output. */
  @NotNull
  private static OSProcessHandler newServerHandler(@NotNull GeneralCommandLine commandLine) throws ExecutionException {
    return new OSProcessHandler(commandLine) {
      // The server is a long-running, mostly idle daemon, and the default reader busy-polls and wastes CPU.
      @Override
      protected @NotNull BaseOutputReader.Options readerOptions() {
        return BaseOutputReader.Options.forMostlySilentProcess();
      }
    };
  }

  /** Forwards one server process's output to its console listeners and reports when the process ends. */
  private final class ServerOutputForwarder implements ProcessListener {
    private final ServerInstance instance;
    private final OSProcessHandler handler;

    ServerOutputForwarder(ServerInstance instance, OSProcessHandler handler) {
      this.instance = instance;
      this.handler = handler;
    }

    @Override
    public void onTextAvailable(@NotNull ProcessEvent event, @NotNull Key outputType) {
      broadcast(instance, event.getText(), outputType);
    }

    @Override
    public void processTerminated(@NotNull ProcessEvent event) {
      String terminated = HaxeBundle.message("haxe.compilation.server.terminated", event.getExitCode()) + "\n";
      broadcast(instance, terminated, ProcessOutputType.SYSTEM);
      onServerTerminated(instance, handler, event.getExitCode());
    }
  }

  /** The configured fixed port when one is set and no other running instance uses it, otherwise a free port. */
  private int choosePortLocked(HaxeBuildToolSettings settings, ServerInstance starting) throws IOException {
    int configured = settings.getCompilationServerPort();
    if (configured > 0) {
      boolean taken = servers.values().stream()
        .anyMatch(other -> other != starting && other.isAlive() && other.port == configured);
      if (!taken) {
        return configured;
      }
    }
    return NetUtils.findAvailableSocketPort();
  }

  /** Stops the instance's process and returns whether it was running. */
  private boolean stopInstanceLocked(ServerInstance instance) {
    // clears the handler before destroying, so the termination listener recognizes a deliberate stop
    OSProcessHandler handler = instance.handler;
    boolean wasAlive = instance.isAlive();
    instance.handler = null;
    instance.port = -1;
    if (handler != null && !handler.isProcessTerminated()) {
      handler.destroyProcess();
    }
    return wasAlive;
  }

  private String displayNameFor(@Nullable String preferredSdkName, String executablePath) {
    if (preferredSdkName != null) {
      return preferredSdkName;
    }
    Sdk configured = HaxeToolPathResolver.findConfiguredSdk(project);
    return configured != null ? configured.getName() : Path.of(executablePath).getFileName().toString();
  }

  /** Adds the output to the instance's backlog and passes it to the current listeners. */
  private synchronized void broadcast(ServerInstance instance, @NotNull String text, @NotNull Key<?> outputType) {
    instance.backlog.addLast(new OutputChunk(text, outputType));
    while (instance.backlog.size() > BACKLOG_LIMIT) {
      instance.backlog.removeFirst();
    }
    for (ServerOutputListener listener : instance.listeners) {
      listener.onOutput(text, outputType);
    }
  }

  private synchronized void onServerTerminated(ServerInstance instance, OSProcessHandler handler, int exitCode) {
    // A deliberate stop clears the handler first, so a matching handler means the server died on its own.
    if (instance.handler == handler) {
      log.info("haxe compilation server terminated with exit code " + exitCode + " (" + instance.executablePath + ")");
      instance.handler = null;
      instance.port = -1;
      fireStateChanged();
    }
  }

  /**
   * Publishes the state change on the EDT, outside the manager's lock, so listeners may query the manager
   * or refresh UI. The explicit non-modal state matters: listeners drop PSI caches, and a runnable that a
   * pooled thread submits without a modality state runs write-unsafe.
   */
  private void fireStateChanged() {
    ApplicationManager.getApplication().invokeLater(() -> {
      if (!project.isDisposed()) {
        project.getMessageBus().syncPublisher(HaxeCompilationServerListener.TOPIC).serverStateChanged();
      }
    }, ModalityState.nonModal());
  }

  private void notifyStartFailed(@NotNull String detail) {
    String message = HaxeBundle.message("haxe.compilation.server.start.failed");
    HaxeCommandNotifications.notify(project, message, detail, NotificationType.WARNING);
  }

  @Override
  public void dispose() {
    stop();
  }
}
