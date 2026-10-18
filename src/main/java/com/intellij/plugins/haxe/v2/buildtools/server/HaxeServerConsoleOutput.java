package com.intellij.plugins.haxe.v2.buildtools.server;

import com.intellij.openapi.util.Key;
import com.intellij.plugins.haxe.v2.buildtools.server.HaxeServerProcesses.ServerOutputListener;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * One process's console output: the listeners of its open console tabs and a
 * bounded backlog that is replayed into a tab opened later.
 */
final class HaxeServerConsoleOutput {

  /** Output chunks kept per process, replayed into a console that opens later. */
  private static final int BACKLOG_LIMIT = 2000;

  private record OutputChunk(@NotNull String text, @NotNull Key<?> outputType) {
  }

  private final List<ServerOutputListener> listeners = new CopyOnWriteArrayList<>();
  private final Deque<OutputChunk> backlog = new ArrayDeque<>();

  synchronized void addListener(@NotNull ServerOutputListener listener) {
    listeners.add(listener);
    backlog.forEach(chunk -> listener.onOutput(chunk.text(), chunk.outputType()));
  }

  void removeListener(@NotNull ServerOutputListener listener) {
    listeners.remove(listener);
  }

  /** Adds the output to the backlog and passes it to the current listeners. */
  synchronized void broadcast(@NotNull String text, @NotNull Key<?> outputType) {
    backlog.addLast(new OutputChunk(text, outputType));
    while (backlog.size() > BACKLOG_LIMIT) {
      backlog.removeFirst();
    }
    for (ServerOutputListener listener : listeners) {
      listener.onOutput(text, outputType);
    }
  }
}
