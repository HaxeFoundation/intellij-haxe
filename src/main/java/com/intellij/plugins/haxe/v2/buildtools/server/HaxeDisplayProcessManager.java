package com.intellij.plugins.haxe.v2.buildtools.server;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.process.OSProcessHandler;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.Project;
import com.intellij.plugins.haxe.HaxeBundle;
import com.intellij.plugins.haxe.display.protocol.server.ServerMemory;
import com.intellij.plugins.haxe.display.transport.DisplayRequestException;
import com.intellij.plugins.haxe.display.transport.FramedDisplayChannel;
import com.intellij.plugins.haxe.v2.compiler.settings.HaxeCompilerSettings;
import com.intellij.plugins.haxe.v2.compiler.settings.HaxeDisplayConnectionMode;
import com.intellij.plugins.haxe.v2.compiler.settings.HaxeServerGeneration;
import lombok.CustomLog;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

/// Runs one dedicated haxe display process per compilation server instance
/// ([HaxeServerAssignment]: an SDK, or a container with a server of its own)
/// in `--server-connect`
/// mode: the IDE listens on a loopback port, haxe connects to it and keeps
/// that connection for its lifetime, and requests travel as length-prefixed
/// frames ([FramedDisplayChannel]). Haxe 3 has no `--server-connect`; its
/// process runs as `--wait stdio`, and the same frames travel on its stdin
/// and stderr. Against the shared `--wait` server every
/// isolated request pays a 50 ms sleep in haxe's accept loop and queues
/// behind builds; the dedicated process pays neither. It keeps a module cache
/// of its own, so a context warms up in it separately.
///
/// A process that died, or a channel that broke on a timeout, is replaced by
/// the next request; a start that failed is retried after a cooldown. The
/// process mechanics and the console integration live in
/// [HaxeServerProcessManager]; a start here blocks until the process has
/// connected, so it stays off the EDT.
@Service(Service.Level.PROJECT)
@CustomLog
public final class HaxeDisplayProcessManager extends HaxeServerProcessManager<HaxeDisplayProcessManager.DisplayProcess> {

  /** A live channel and the identity the display service keys its per-server state on (the listen port). */
  public record Channel(@NotNull FramedDisplayChannel channel, int id) {
  }

  /** Distinguishes a display process's id from the shared server's, which is the bare executable path. */
  private static final String ID_SUFFIX = "#display";
  private static final int ACCEPT_TIMEOUT_MS = 15_000;
  /** How often the wait for the connection checks that the process still runs. */
  private static final int ACCEPT_POLL_MS = 200;
  private static final long START_FAILURE_COOLDOWN_MS = 10_000;
  /** A stdio process has no port: its id counts on from above every port, so it never meets a port key. */
  private static final AtomicInteger STDIO_IDS = new AtomicInteger(65_535);

  /** A server process plus the connection it opened to the IDE; alive only while that connection works. */
  static final class DisplayProcess extends HaxeServerProcess {
    FramedDisplayChannel channel;
    long failedAt;

    DisplayProcess(@NotNull HaxeServerAssignment assignment, @NotNull String displayName) {
      super(idFor(assignment), assignment.executablePath(), assignment.sdkName(), displayName);
    }

    @Override
    boolean isAlive() {
      return super.isAlive() && channel != null && !channel.isBroken();
    }

    /** Whether the process runs, connected or not. */
    boolean isRunning() {
      return super.isAlive();
    }
  }

  public HaxeDisplayProcessManager(@NotNull Project project) {
    super(project);
  }

  public static HaxeDisplayProcessManager getInstance(@NotNull Project project) {
    return project.getService(HaxeDisplayProcessManager.class);
  }

  /** The id of the display process of a server instance; request metrics are recorded under it. */
  @NotNull
  public static String idFor(@NotNull HaxeServerAssignment assignment) {
    return assignment.id() + ID_SUFFIX;
  }

  /**
   * The channel to the assigned instance's display process, started when
   * needed. Null when the compilation server is disabled, the project is not
   * trusted, or a start failed within the cooldown. Background threads only:
   * a start waits for the process to connect.
   */
  @Nullable
  public synchronized Channel channelFor(@NotNull HaxeServerAssignment assignment) {
    if (!mayStart()) return null;
    DisplayProcess process = processes.get(idFor(assignment));
    if (process == null) {
      String displayName = HaxeBundle.message("haxe.server.console.display.tab", assignment.displayName());
      process = new DisplayProcess(assignment, displayName);
      processes.put(process.id, process);
    }
    if (process.isAlive()) return new Channel(process.channel, process.port);
    if (System.currentTimeMillis() - process.failedAt < START_FAILURE_COOLDOWN_MS) return null;
    stopProcessLocked(process);
    return startLocked(process) > 0 ? new Channel(process.channel, process.port) : null;
  }

  /** Makes the connection the project's choice for SDKs of the generation; leaving the dedicated processes stops them. */
  public void chooseConnection(@NotNull HaxeServerGeneration generation, @NotNull HaxeDisplayConnectionMode mode) {
    HaxeCompilerSettings settings = HaxeCompilerSettings.getInstance(project);
    if (settings.getDisplayConnectionMode(generation) == mode) return;
    settings.setDisplayConnectionMode(generation, mode);
    if (mode == HaxeDisplayConnectionMode.SHARED_SERVER) stopAll();
  }

  /** The {@link Channel#id()} of the assigned instance's display process while it runs, or -1; starts nothing. */
  public synchronized int runningIdFor(@NotNull HaxeServerAssignment assignment) {
    DisplayProcess process = processes.get(idFor(assignment));
    return process != null && process.isAlive() ? process.port : -1;
  }

  @Override
  protected int startLocked(@NotNull DisplayProcess process) {
    try {
      if (HaxeServerGenerations.of(project, process.sdkName) == HaxeServerGeneration.HAXE_3) {
        connectOverStdio(process);
      }
      else {
        connectOverSocket(process);
      }
      process.failedAt = 0;
      fireStateChanged();
      return process.port;
    } catch (ExecutionException | IOException e) {
      log.info("haxe display process failed to start (" + process.executablePath + "): " + e.getMessage());
      process.failedAt = System.currentTimeMillis();
      stopProcessLocked(process);
      return -1;
    }
  }

  /** Haxe 4 on connects back to a loopback port the IDE listens on, and the frames travel on that socket. */
  private void connectOverSocket(@NotNull DisplayProcess process) throws ExecutionException, IOException {
    try (ServerSocket listener = new ServerSocket(0, 1, InetAddress.getLoopbackAddress())) {
      listener.setSoTimeout(ACCEPT_POLL_MS);
      int port = listener.getLocalPort();
      String address = HaxeCompilationServerManager.SERVER_HOST + ":" + port;
      startProcess(process, serverCommandLine(process, "--server-connect", address));
      Socket connection = acceptWhileRunning(listener, process);
      process.channel = new FramedDisplayChannel(connection);
      process.port = port;
      log.info("Started haxe display process on loopback port " + port + " (" + process.executablePath + ")");
    }
  }

  /** Haxe 3 reads the frames on its stdin and answers on its stderr; closing the channel ends the process. */
  private void connectOverStdio(@NotNull DisplayProcess process) throws ExecutionException {
    startStdioProcess(process, serverCommandLine(process, HaxeServerGeneration.HAXE_3.listenFlag(), "stdio"));
    OSProcessHandler handler = process.handler();
    if (handler == null) throw new ExecutionException("the process exited at once");
    InputStream stderr = handler.getProcess().getErrorStream();
    String description = "haxe display process over stdio (" + process.executablePath + ")";
    process.channel = FramedDisplayChannel.overStdio(stderr, handler.getProcessInput(), handler::destroyProcess, description);
    process.port = STDIO_IDS.incrementAndGet();
    log.info("Started " + description);
  }

  /**
   * The connection the started process opens. Gives up as soon as the
   * process exits, as a haxe without {@code --server-connect} does at once,
   * else after the accept timeout.
   */
  @NotNull
  private static Socket acceptWhileRunning(@NotNull ServerSocket listener, @NotNull DisplayProcess process) throws IOException {
    long deadline = System.currentTimeMillis() + ACCEPT_TIMEOUT_MS;
    while (true) {
      try {
        return listener.accept();
      } catch (SocketTimeoutException e) {
        if (!process.isRunning()) throw new IOException("the process exited before connecting", e);
        if (System.currentTimeMillis() >= deadline) throw e;
      }
    }
  }

  @Override
  protected void afterStopLocked(@NotNull DisplayProcess process) {
    if (process.channel != null) {
      process.channel.close();
      process.channel = null;
    }
  }

  @Override
  protected void clearDerivedState(@NotNull String id) {
    HaxeServerMetrics.getInstance(project).clear(id);
  }

  @Override
  @NotNull
  protected String terminatedMessage(int exitCode) {
    return HaxeBundle.message("haxe.display.process.terminated", exitCode);
  }

  @Override
  @Nullable
  public ServerMemory fetchMemory(@NotNull String id) throws DisplayRequestException {
    FramedDisplayChannel channel;
    String sdkName;
    synchronized (this) {
      DisplayProcess process = processes.get(id);
      if (process == null || !process.isAlive()) throw new DisplayRequestException("display process " + id + " is not running");
      channel = process.channel;
      sdkName = process.sdkName;
    }
    return HaxeServerProcesses.memoryOf(project, sdkName, channel);
  }
}
