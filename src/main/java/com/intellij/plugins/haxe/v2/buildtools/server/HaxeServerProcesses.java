package com.intellij.plugins.haxe.v2.buildtools.server;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.plugins.haxe.display.client.JsonRpcDisplayClient;
import com.intellij.plugins.haxe.display.haxe3.Haxe3DisplayClient;
import com.intellij.plugins.haxe.display.protocol.server.ServerMemory;
import com.intellij.plugins.haxe.display.transport.DisplayChannel;
import com.intellij.plugins.haxe.display.transport.DisplayRequestException;
import com.intellij.plugins.haxe.v2.compiler.settings.HaxeServerGeneration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * A manager of haxe server processes, as the server console renders them:
 * one tab per entry with its output, a start/restart/stop toolbar and the
 * request statistics. The shared compilation servers ({@code --server-listen},
 * {@code --wait} in Haxe 3) and the dedicated display processes
 * ({@code --server-connect}, {@code --wait stdio} in Haxe 3) each have one;
 * ids are unique across managers.
 */
public interface HaxeServerProcesses {

  /** UI snapshot of one process; {@code id} identifies it across managers. */
  record ServerInfo(@NotNull String id, @NotNull String displayName, int port, boolean running) {
  }

  /** Receives one process's output, plus lifecycle lines such as its termination, for the console. */
  interface ServerOutputListener {
    void onOutput(@NotNull String text, @NotNull Key<?> outputType);
  }

  /** Every known entry, running or stopped, in the order they were first requested. */
  @NotNull
  List<ServerInfo> getServers();

  boolean isRunning(@NotNull String id);

  /** Registers a console listener for one entry and replays its buffered output to it. */
  void addOutputListener(@NotNull String id, @NotNull ServerOutputListener listener);

  void removeOutputListener(@NotNull String id, @NotNull ServerOutputListener listener);

  /** Restarts the process, or starts it when it is not running. Call off the EDT. */
  void restartServer(@NotNull String id);

  void stopServer(@NotNull String id);

  /** Stops the process and forgets the entry, backlog included: a closed console tab. */
  void removeServer(@NotNull String id);

  /**
   * The process's cache statistics ({@code server/memory}); null when the
   * process printed its report to its own output, the console, instead
   * (Haxe 3). Performs a round-trip; call off the EDT.
   */
  @Nullable
  ServerMemory fetchMemory(@NotNull String id) throws DisplayRequestException;

  /** The cache statistics of the server behind the channel; null when it is a Haxe 3 server, which prints them. */
  @Nullable
  static ServerMemory memoryOf(@NotNull Project project, @Nullable String sdkName, @NotNull DisplayChannel channel)
    throws DisplayRequestException {
    if (HaxeServerGenerations.of(project, sdkName) != HaxeServerGeneration.HAXE_3) {
      return new JsonRpcDisplayClient(channel).serverMemory(List.of());
    }
    new Haxe3DisplayClient(channel).requestMemoryReport();
    return null;
  }
}
