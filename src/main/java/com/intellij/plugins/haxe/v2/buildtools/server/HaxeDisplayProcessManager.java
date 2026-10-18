package com.intellij.plugins.haxe.v2.buildtools.server;

import com.intellij.execution.ExecutionException;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.Project;
import com.intellij.plugins.haxe.HaxeBundle;
import com.intellij.plugins.haxe.display.client.HaxeDisplayClient;
import com.intellij.plugins.haxe.display.protocol.server.ServerMemory;
import com.intellij.plugins.haxe.display.transport.DisplayRequestException;
import com.intellij.plugins.haxe.display.transport.FramedDisplayChannel;
import com.intellij.plugins.haxe.v2.buildtools.HaxeToolPathResolver;
import lombok.CustomLog;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;

/// Runs one dedicated haxe display process per SDK in `--server-connect`
/// mode: the IDE listens on a loopback port, haxe connects to it and keeps
/// that connection for its lifetime, and requests travel as length-prefixed
/// frames ([FramedDisplayChannel]). Against the shared `--wait` server every
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
  private static final long START_FAILURE_COOLDOWN_MS = 10_000;

  /** A server process plus the connection it opened to the IDE; alive only while that connection works. */
  static final class DisplayProcess extends HaxeServerProcess {
    FramedDisplayChannel channel;
    long failedAt;

    DisplayProcess(@NotNull String executablePath, @Nullable String sdkName, @NotNull String displayName) {
      super(idFor(executablePath), executablePath, sdkName, displayName);
    }

    @Override
    boolean isAlive() {
      return super.isAlive() && channel != null && !channel.isBroken();
    }
  }

  public HaxeDisplayProcessManager(@NotNull Project project) {
    super(project);
  }

  public static HaxeDisplayProcessManager getInstance(@NotNull Project project) {
    return project.getService(HaxeDisplayProcessManager.class);
  }

  /** The id of the display process for a haxe executable; request metrics are recorded under it. */
  @NotNull
  public static String idFor(@NotNull String executablePath) {
    return executablePath + ID_SUFFIX;
  }

  /**
   * The channel to the SDK's display process, started when needed. Null when
   * the compilation server is disabled, the project is not trusted, or a
   * start failed within the cooldown. Background threads only: a start waits
   * for the process to connect.
   */
  @Nullable
  public synchronized Channel channelFor(@Nullable String sdkName) {
    if (!mayStart()) return null;
    String executablePath = HaxeToolPathResolver.resolveHaxeExecutable(project, sdkName);
    DisplayProcess process = processes.get(idFor(executablePath));
    if (process == null) {
      String name = HaxeCompilationServerManager.displayNameFor(project, sdkName, executablePath);
      process = new DisplayProcess(executablePath, sdkName, HaxeBundle.message("haxe.server.console.display.tab", name));
      processes.put(process.id, process);
    }
    if (process.isAlive()) return new Channel(process.channel, process.port);
    if (System.currentTimeMillis() - process.failedAt < START_FAILURE_COOLDOWN_MS) return null;
    stopProcessLocked(process);
    return startLocked(process) > 0 ? new Channel(process.channel, process.port) : null;
  }

  /** The {@link Channel#id()} (listen port) of the SDK's display process while it runs, or -1; starts nothing. */
  public synchronized int runningIdFor(@Nullable String sdkName) {
    String executablePath = HaxeToolPathResolver.resolveHaxeExecutable(project, sdkName);
    DisplayProcess process = processes.get(idFor(executablePath));
    return process != null && process.isAlive() ? process.port : -1;
  }

  @Override
  protected int startLocked(@NotNull DisplayProcess process) {
    try (ServerSocket listener = new ServerSocket(0, 1, InetAddress.getLoopbackAddress())) {
      listener.setSoTimeout(ACCEPT_TIMEOUT_MS);
      int port = listener.getLocalPort();
      startProcess(process, serverCommandLine(process, "--server-connect", HaxeCompilationServerManager.SERVER_HOST + ":" + port));
      Socket connection = listener.accept();
      process.channel = new FramedDisplayChannel(connection);
      process.port = port;
      process.failedAt = 0;
      log.info("Started haxe display process on loopback port " + port + " (" + process.executablePath + ")");
      fireStateChanged();
      return port;
    } catch (ExecutionException | IOException e) {
      log.info("haxe display process failed to start (" + process.executablePath + "): " + e.getMessage());
      process.failedAt = System.currentTimeMillis();
      stopProcessLocked(process);
      return -1;
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
  @NotNull
  public ServerMemory fetchMemory(@NotNull String id) throws DisplayRequestException {
    FramedDisplayChannel channel;
    synchronized (this) {
      DisplayProcess process = processes.get(id);
      if (process == null || !process.isAlive()) throw new DisplayRequestException("display process " + id + " is not running");
      channel = process.channel;
    }
    return new HaxeDisplayClient(channel).serverMemory(List.of());
  }
}
