package com.intellij.plugins.haxe.v2.buildtools;

import com.intellij.ide.trustedProjects.TrustedProjects;
import com.intellij.ide.trustedProjects.TrustedProjectsListener;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationAction;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.MessageDialogBuilder;
import com.intellij.plugins.haxe.HaxeBundle;
import java.util.concurrent.atomic.AtomicBoolean;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;

/**
 * The trust gate in front of everything that can execute PROJECT-AUTHORED
 * code — hxp scripts, {@code haxelib run lime|openfl|nme} tools from the
 * project's local repository, and any compile (macros run). Plain SDK-tool
 * reads (haxelib list/info/path, haxe --help) stay ungated.
 */
public final class HaxeProjectTrust {

  private HaxeProjectTrust() {
  }

  /** The bare check; callable from any thread. */
  public static boolean isTrusted(@NotNull Project project) {
    return TrustedProjects.isProjectTrusted(project);
  }

  /**
   * The gate for AUTOMATIC evaluation (project open, indexing, resolve,
   * tool-window population): false when untrusted, and the FIRST refusal per
   * project session raises one warning notification with a Trust Project
   * action — every later refusal is silent. Callable from any thread.
   */
  public static boolean checkForBackgroundEvaluation(@NotNull Project project) {
    if (isTrusted(project)) {
      return true;
    }
    SessionState state = project.getService(SessionState.class);
    if (state.warned.compareAndSet(false, true)) {
      ApplicationManager.getApplication().invokeLater(() -> notifyEvaluationDisabled(project));
    }
    return false;
  }

  /**
   * The gate for USER GESTURES (execute command, build, server start, dump
   * navigation): true when trusted, otherwise asks with a modal dialog and
   * grants trust project-wide on yes. The dialog is the whole error surface —
   * a "no" needs no follow-up balloon. EDT only.
   */
  public static boolean confirmForAction(@NotNull Project project, @Nls @NotNull String actionName) {
    if (isTrusted(project)) {
      return true;
    }
    boolean trusted = MessageDialogBuilder
      .yesNo(HaxeBundle.message("haxe.trust.dialog.title"),
             HaxeBundle.message("haxe.trust.dialog.message", actionName))
      .yesText(HaxeBundle.message("haxe.trust.dialog.trust"))
      .noText(HaxeBundle.message("haxe.trust.dialog.cancel"))
      .asWarning()
      .ask(project);
    if (trusted) {
      TrustedProjects.setProjectTrusted(project, true);
    }
    return trusted;
  }

  /** Runs {@code onTrusted} when THIS project becomes trusted; the subscription lives until project close. */
  public static void whenTrusted(@NotNull Project project, @NotNull Runnable onTrusted) {
    SessionState state = project.getService(SessionState.class);
    // the topic is app-level - a project-bus subscription would never see it
    ApplicationManager.getApplication().getMessageBus().connect(state)
      .subscribe(TrustedProjectsListener.TOPIC, new TrustedProjectsListener() {
        @Override
        public void onProjectTrusted(@NotNull Project trusted) {
          if (trusted.equals(project)) {
            onTrusted.run();
          }
        }
      });
  }

  private static void notifyEvaluationDisabled(@NotNull Project project) {
    if (project.isDisposed() || isTrusted(project)) {
      return;
    }
    NotificationAction trustAction =
      new NotificationAction(HaxeBundle.message("haxe.trust.notification.action")) {
        @Override
        public void actionPerformed(@NotNull AnActionEvent e, @NotNull Notification notification) {
          if (confirmForAction(project, HaxeBundle.message("haxe.trust.action.project.evaluation"))) {
            notification.expire();
          }
        }
      };
    String title = HaxeBundle.message("haxe.trust.notification.title");
    String content = HaxeBundle.message("haxe.trust.notification.content");
    HaxeCommandNotifications.notify(project, title, content, NotificationType.WARNING, trustAction);
  }

  /**
   * Once-per-session dedupe for the background-evaluation warning; doubles
   * as the project-lifetime parent for the app-bus trust subscription.
   */
  @Service(Service.Level.PROJECT)
  static final class SessionState implements Disposable {
    final AtomicBoolean warned = new AtomicBoolean();

    @Override
    public void dispose() {
    }
  }
}
