package com.intellij.plugins.haxe.profiler.bridge;

import com.intellij.openapi.project.Project;
import com.intellij.plugins.haxe.profiler.HaxeProfilerSnapshotOpener;
import com.intellij.profiler.actions.ImportProfilerResultAction;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;

/** Hands the snapshot to the IU profiler's import path — provider lookup by extension and signature included. */
public class HaxeIuSnapshotOpener implements HaxeProfilerSnapshotOpener {

  @Override
  public void open(@NotNull Project project, @NotNull Path snapshot) {
    ImportProfilerResultAction.Companion.importProfilerDumpAsAssociated(project, snapshot.toFile());
  }
}
