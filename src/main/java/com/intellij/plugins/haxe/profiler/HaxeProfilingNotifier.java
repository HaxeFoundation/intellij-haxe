package com.intellij.plugins.haxe.profiler;

import com.intellij.execution.process.ProcessEvent;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.process.ProcessListener;
import com.intellij.ide.actions.RevealFileAction;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationAction;
import com.intellij.notification.NotificationGroup;
import com.intellij.notification.NotificationGroupManager;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.project.Project;
import com.intellij.plugins.haxe.HaxeProfilerBundle;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * The profiling lanes' user-facing outcome. {@link #watch} follows a run
 * whose dump file appears only on an orderly shutdown (a killed process,
 * including the IDE Stop button, produces nothing); the capture receivers
 * report through the shared helpers. An outcome lands in the run's profiler
 * tab when it has one, else in a notification.
 */
public final class HaxeProfilingNotifier {

  private HaxeProfilingNotifier() {
  }

  /**
   * {@code missingMessageKey}: a HaxeProfilerBundle key taking the exit code
   * as {0}; each lane explains its own dump rules. With a {@code session}
   * the outcome lands in its profiler tab instead of a notification.
   */
  public static void watch(@NotNull Project project, @NotNull ProcessHandler handler,
                           @NotNull Path dumpPath, @NotNull String missingMessageKey,
                           @Nullable HaxeProfilerProcessUi.Session session) {
    long startedAt = System.currentTimeMillis();
    handler.addProcessListener(new ProcessListener() {
      @Override
      public void processTerminated(@NotNull ProcessEvent event) {
        notifyOutcome(project, dumpPath, startedAt, missingMessageKey, event.getExitCode(), session);
      }
    });
  }

  private static void notifyOutcome(Project project, Path dumpPath, long startedAt, String missingMessageKey,
                                    int exitCode, @Nullable HaxeProfilerProcessUi.Session session) {
    // a dump left behind by an EARLIER run must not read as this run's result
    if (writtenSince(dumpPath, startedAt)) {
      if (session != null) {
        session.dataReady();
      }
      else {
        String content = HaxeProfilerBundle.message("haxe.profiler.dump.written", dumpPath.toString());
        notifySnapshotReady(project, content, dumpPath);
      }
      return;
    }
    reportNothingCaptured(project, session, HaxeProfilerBundle.message(missingMessageKey, exitCode));
  }

  private static boolean writtenSince(Path dumpPath, long startedAt) {
    try {
      return Files.isRegularFile(dumpPath) && Files.getLastModifiedTime(dumpPath).toMillis() >= startedAt;
    }
    catch (IOException e) {
      return false;
    }
  }

  /** Announces a finished snapshot with an action opening it, or revealing the file where no profiler viewer exists. */
  public static void notifySnapshotReady(@NotNull Project project, @NotNull String content, @NotNull Path snapshot) {
    Notification notification = group().createNotification(content, NotificationType.INFORMATION);
    HaxeProfilerSnapshotOpener opener = HaxeProfilerSnapshotOpener.getInstance();
    if (opener != null) {
      String openText = HaxeProfilerBundle.message("haxe.profiler.dump.open");
      notification.addAction(NotificationAction.createSimpleExpiring(openText, () -> opener.open(project, snapshot)));
    }
    else {
      String revealText = RevealFileAction.getActionName();
      notification.addAction(NotificationAction.createSimple(revealText, () -> RevealFileAction.openFile(snapshot.toFile())));
    }
    notification.notify(project);
  }

  /** Explains an empty capture in the run's profiler tab, or in a warning when the run has none. */
  public static void reportNothingCaptured(@NotNull Project project, @Nullable HaxeProfilerProcessUi.Session session,
                                           @NotNull String content) {
    if (session != null) {
      session.failed(content);
    }
    else {
      group().createNotification(content, NotificationType.WARNING).notify(project);
    }
  }

  /** The profiling notification group. */
  @NotNull
  public static NotificationGroup group() {
    return NotificationGroupManager.getInstance().getNotificationGroup("haxe.profiler");
  }
}
