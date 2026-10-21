package com.intellij.plugins.haxe.v2.display;

import org.jetbrains.annotations.NotNull;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The files whose unsaved editor text a compilation server last received.
 * The server builds its cached module from that text and keeps serving it
 * while the file on disk is unchanged, so the next request for such a file
 * must invalidate it, also when the request itself carries no text: the
 * editor is back to the saved state (Ctrl+Z, a save) and the module the
 * server holds describes text that is gone.
 */
final class HaxeServerUnsavedFiles {
  private final Set<String> files = ConcurrentHashMap.newKeySet();

  /** Whether a request about the file must invalidate it first: it sends unsaved text, or the server holds some. */
  boolean needsInvalidate(int port, @NotNull String filePath, boolean sendsText) {
    return sendsText || files.contains(key(port, filePath));
  }

  /** Records what the server holds after a request about the file: its unsaved text, or the saved file. */
  void requestSent(int port, @NotNull String filePath, boolean sentText) {
    if (sentText) {
      files.add(key(port, filePath));
    }
    else {
      files.remove(key(port, filePath));
    }
  }

  /** A restarted server holds no unsaved text. */
  void clear() {
    files.clear();
  }

  @NotNull
  private static String key(int port, @NotNull String filePath) {
    return port + ":" + filePath;
  }
}
