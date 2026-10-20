package com.intellij.plugins.haxe.v2.buildtools.server;

import com.intellij.execution.ExecutionException;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.plugins.haxe.HaxeBundle;
import com.intellij.plugins.haxe.display.protocol.server.ServerMemory;
import com.intellij.plugins.haxe.display.transport.DisplayChannel;
import com.intellij.plugins.haxe.display.transport.DisplayRequestException;
import com.intellij.plugins.haxe.display.transport.HaxeDisplayTransport;
import com.intellij.plugins.haxe.v2.buildtools.*;
import com.intellij.plugins.haxe.v2.buildtools.settings.HaxeBuildToolSettings;
import com.intellij.util.net.NetUtils;
import lombok.CustomLog;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Runs the project's haxe compilation servers ({@code haxe --server-listen <port>}, {@code --wait} on haxe 3).
 * A server's cache belongs to one haxe BINARY, and a project whose modules
 * use different SDKs can need several haxe versions at once. The manager
 * therefore runs one server process per {@link HaxeServerAssignment}: the
 * resolved haxe executable, plus the container for one that runs a server
 * of its own. The first compile that connects through an assignment starts
 * its server. The process mechanics live in {@link HaxeServerProcessManager}.
 */
@Service(Service.Level.PROJECT)
@CustomLog
public final class HaxeCompilationServerManager extends HaxeServerProcessManager<HaxeServerProcess> {

  /** The managed server always runs on the local machine; every client connects to loopback. */
  public static final String SERVER_HOST = "127.0.0.1";

  public HaxeCompilationServerManager(@NotNull Project project) {
    super(project);
  }

  @NotNull
  public static HaxeCompilationServerManager getInstance(@NotNull Project project) {
    return project.getService(HaxeCompilationServerManager.class);
  }

  /** The id of the server instance serving the container; see {@link HaxeServerAssignment#forContainer}. */
  @NotNull
  public static String serverIdFor(@NotNull Project project, @NotNull String containerId) {
    return HaxeServerAssignment.forContainer(project, containerId).id();
  }

  /**
   * Starts the assigned server instance unless it already runs, and returns
   * its port. Returns -1 when the server is disabled or failed to start;
   * callers then compile without --connect. Process creation must stay off
   * the EDT.
   */
  public synchronized int ensureRunning(@NotNull HaxeServerAssignment assignment) {
    if (!mayStart()) {
      return -1;
    }
    HaxeServerProcess process = processes.get(assignment.id());
    if (process == null) {
      process = new HaxeServerProcess(assignment.id(), assignment.executablePath(), assignment.sdkName(),
                                      assignment.displayName());
      processes.put(assignment.id(), process);
    }
    if (process.isAlive()) {
      return process.port;
    }
    return startLocked(process);
  }

  public synchronized boolean isAnyRunning() {
    return processes.values().stream().anyMatch(HaxeServerProcess::isAlive);
  }

  /** The given instance's port while it runs, or -1. */
  public synchronized int runningPort(@NotNull String id) {
    HaxeServerProcess process = processes.get(id);
    return process != null && process.isAlive() ? process.port : -1;
  }

  @Override
  @Nullable
  public ServerMemory fetchMemory(@NotNull String id) throws DisplayRequestException {
    int port = runningPort(id);
    if (port <= 0) throw new DisplayRequestException("compilation server " + id + " is not running");
    DisplayChannel channel = HaxeDisplayTransport.socketPerRequest(SERVER_HOST, port);
    return HaxeServerProcesses.memoryOf(project, sdkNameOf(id), channel);
  }

  @Nullable
  private synchronized String sdkNameOf(@NotNull String id) {
    HaxeServerProcess process = processes.get(id);
    return process == null ? null : process.sdkName;
  }

  @Override
  protected int startLocked(@NotNull HaxeServerProcess process) {
    HaxeBuildToolSettings settings = HaxeBuildToolSettings.getInstance(project);
    try {
      int chosenPort = choosePortLocked(settings, process);
      String listenFlag = HaxeServerGenerations.of(project, process.sdkName).listenFlag();
      startProcess(process, serverCommandLine(process, listenFlag, String.valueOf(chosenPort)));
      process.port = chosenPort;
      log.info("Started haxe compilation server on port " + chosenPort + " (" + process.executablePath + ")");
      fireStateChanged();
      return chosenPort;
    }
    catch (ExecutionException | IOException e) {
      notifyStartFailed(StringUtil.notNullize(e.getMessage()));
      stopProcessLocked(process);
      return -1;
    }
  }

  /** The configured fixed port when one is set and no other running instance uses it, otherwise a free port. */
  private int choosePortLocked(HaxeBuildToolSettings settings, HaxeServerProcess starting) throws IOException {
    int configured = settings.getCompilationServerPort();
    if (configured > 0) {
      boolean taken = processes.values().stream()
        .anyMatch(other -> other != starting && other.isAlive() && other.port == configured);
      if (!taken) {
        return configured;
      }
    }
    return NetUtils.findAvailableSocketPort();
  }

  /** Drops the stopped server's failures and request statistics, so a fresh server starts clean. */
  @Override
  protected void clearDerivedState(@NotNull String id) {
    HaxeContextFailures.getInstance(project).clearForServer(id);
    HaxeServerMetrics.getInstance(project).clear(id);
  }

  @Override
  @NotNull
  protected String terminatedMessage(int exitCode) {
    return HaxeBundle.message("haxe.compilation.server.terminated", exitCode);
  }

  /** The SDK's name when known, else the configured SDK's, else the executable's file name. */
  @NotNull
  static String displayNameFor(@NotNull Project project, @Nullable String preferredSdkName, @NotNull String executablePath) {
    if (preferredSdkName != null) {
      return preferredSdkName;
    }
    Sdk configured = HaxeToolPathResolver.findConfiguredSdk(project);
    return configured != null ? configured.getName() : Path.of(executablePath).getFileName().toString();
  }

  private void notifyStartFailed(@NotNull String detail) {
    String message = HaxeBundle.message("haxe.compilation.server.start.failed");
    HaxeCommandNotifications.notify(project, message, detail, NotificationType.WARNING);
  }
}
