package com.intellij.plugins.haxe.profiler.bridge;

import com.intellij.openapi.application.ApplicationManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registry of captures still being WRITTEN, keyed by their session file.
 * A receiver registers when it starts spooling and marks the entry done
 * when the stream ends; the data layer asks {@link #find} to render a
 * just-parsed file as a LIVE view (self-refreshing chart, final rebuild on
 * completion) instead of a finished capture. Entries leave the map on
 * completion, so a later open of the same file renders normally.
 */
public final class HaxeLiveCaptures {

  private static final Map<Path, Entry> LIVE = new ConcurrentHashMap<>();

  private HaxeLiveCaptures() {
  }

  /** One capture's live phase; {@link #finished} is idempotent. */
  public static final class Entry {
    private final Path file;
    private final List<Runnable> completionListeners = new ArrayList<>();
    private volatile boolean live = true;

    private Entry(Path file) {
      this.file = file;
    }

    public boolean isLive() {
      return live;
    }

    /** Runs on the EDT when the capture completes; immediately when it already has. */
    public void onCompletion(@NotNull Runnable listener) {
      synchronized (completionListeners) {
        if (live) {
          completionListeners.add(listener);
          return;
        }
      }
      ApplicationManager.getApplication().invokeLater(listener);
    }

    /** The stream ended (drained, torn or failed): the file is final now. */
    public void finished() {
      List<Runnable> listeners;
      synchronized (completionListeners) {
        if (!live) return;
        live = false;
        listeners = List.copyOf(completionListeners);
        completionListeners.clear();
      }
      LIVE.remove(file, this);
      for (Runnable listener : listeners) {
        ApplicationManager.getApplication().invokeLater(listener);
      }
    }
  }

  /** Registers a capture whose session file starts growing now. */
  @NotNull
  public static Entry register(@NotNull Path sessionFile) {
    Entry entry = new Entry(normalize(sessionFile));
    LIVE.put(entry.file, entry);
    return entry;
  }

  /** The live entry writing this file, or null for a finished (or foreign) capture. */
  @Nullable
  public static Entry find(@NotNull Path sessionFile) {
    return LIVE.get(normalize(sessionFile));
  }

  private static Path normalize(Path file) {
    return file.toAbsolutePath().normalize();
  }
}
