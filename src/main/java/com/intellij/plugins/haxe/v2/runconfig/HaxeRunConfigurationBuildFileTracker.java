package com.intellij.plugins.haxe.v2.runconfig;

import com.intellij.execution.BeforeRunTask;
import com.intellij.execution.RunManager;
import com.intellij.execution.RunnerAndConfigurationSettings;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.newvfs.BulkFileListener;
import com.intellij.openapi.vfs.newvfs.events.VFileEvent;
import com.intellij.plugins.haxe.v2.buildtools.settings.HaxeBuildFileMoveTracker.Move;
import com.intellij.plugins.haxe.v2.buildtools.settings.HaxeBuildFileMoveTracker;
import com.intellij.plugins.haxe.v2.buildtools.settings.HaxeStoredPaths;
import com.intellij.plugins.haxe.v2.testing.run.HaxeTestRunConfiguration;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Keeps the build-file path of every Haxe action and test run configuration,
 * and of every Haxe before-run step, with its file when it, or a folder above
 * it, moves or is renamed. The stores have {@link HaxeBuildFileMoveTracker};
 * these paths live in the run manager's configurations instead.
 */
public final class HaxeRunConfigurationBuildFileTracker implements BulkFileListener {

  private final Project project;

  public HaxeRunConfigurationBuildFileTracker(@NotNull Project project) {
    this.project = project;
  }

  @Override
  public void after(@NotNull List<? extends @NotNull VFileEvent> events) {
    if (project.isDisposed()) return;
    List<Move> moves = HaxeBuildFileMoveTracker.movesIn(events);
    if (moves.isEmpty()) return;
    for (RunnerAndConfigurationSettings settings : RunManager.getInstance(project).getAllSettings()) {
      for (Move move : moves) {
        follow(settings.getConfiguration(), move);
      }
    }
  }

  private static void follow(@NotNull RunConfiguration configuration, @NotNull Move move) {
    if (configuration instanceof HaxeActionRunConfiguration action) {
      action.setBuildFilePath(HaxeStoredPaths.moved(action.getBuildFilePath(), move.oldPath(), move.newPath()));
    }
    if (configuration instanceof HaxeTestRunConfiguration test) {
      test.setBuildFilePath(HaxeStoredPaths.moved(test.getBuildFilePath(), move.oldPath(), move.newPath()));
    }
    for (BeforeRunTask<?> task : configuration.getBeforeRunTasks()) {
      if (task instanceof HaxeActionBeforeRunTaskProvider.Task step) {
        step.setBuildFilePath(HaxeStoredPaths.moved(step.getBuildFilePath(), move.oldPath(), move.newPath()));
      }
    }
  }
}
