package com.intellij.plugins.haxe.profiler;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;

/**
 * Opens a profiler snapshot file in a viewer. The only implementation is the
 * IU profiler bridge, registered from the OPTIONAL profiler descriptor — on
 * IDEs without the profiler module {@link #getInstance()} is null and callers
 * fall back to revealing the file.
 */
public interface HaxeProfilerSnapshotOpener {

  @Nullable
  static HaxeProfilerSnapshotOpener getInstance() {
    return ApplicationManager.getApplication().getService(HaxeProfilerSnapshotOpener.class);
  }

  /** Parses and shows the snapshot in the Profiler tool window. */
  void open(@NotNull Project project, @NotNull Path snapshot);
}
