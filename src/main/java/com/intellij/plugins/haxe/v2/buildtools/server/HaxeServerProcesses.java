package com.intellij.plugins.haxe.v2.buildtools.server;

import com.intellij.openapi.util.Key;
import com.intellij.plugins.haxe.display.protocol.server.ServerMemory;
import com.intellij.plugins.haxe.display.transport.DisplayRequestException;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * A manager of haxe server processes, as the server console renders them:
 * one tab per entry with its output, a start/restart/stop toolbar and the
 * request statistics. The shared compilation servers ({@code --wait}) and the
 * dedicated display processes ({@code --server-connect}) each have one; ids
 * are unique across managers.
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

  /** The process's cache statistics ({@code server/memory}). Performs a round-trip; call off the EDT. */
  @NotNull
  ServerMemory fetchMemory(@NotNull String id) throws DisplayRequestException;
}
