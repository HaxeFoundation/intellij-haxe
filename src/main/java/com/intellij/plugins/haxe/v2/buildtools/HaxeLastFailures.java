package com.intellij.plugins.haxe.v2.buildtools;

import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The last failure per key, kept for the tool window: a change of a key's
 * failure publishes {@link HaxeBuildEventListener#failureRecorded}, so the
 * tree repaints; recording null clears the key.
 */
public abstract class HaxeLastFailures {
  private final Project project;
  private final Map<String, String> failures = new ConcurrentHashMap<>();

  protected HaxeLastFailures(@NotNull Project project) {
    this.project = project;
  }

  /** The key's last failure, or null while it succeeds. */
  @Nullable
  public String lastFailure(@NotNull String key) {
    return failures.get(key);
  }

  /** Current failures per key (a copy). */
  @NotNull
  public Map<String, String> snapshot() {
    return Map.copyOf(failures);
  }

  /** Records an outcome; a null failure means success. When the key's failure changes, the tool window repaints. */
  public void record(@NotNull String key, @Nullable String failure) {
    boolean changed = failure != null
                      ? !failure.equals(failures.put(key, failure))
                      : failures.remove(key) != null;
    if (changed && !project.isDisposed()) {
      HaxeBuildEventListener.publisher(project).failureRecorded(key);
    }
  }

  /** The keys with a failure (a copy). */
  @NotNull
  protected List<String> failingKeys() {
    return List.copyOf(failures.keySet());
  }
}
