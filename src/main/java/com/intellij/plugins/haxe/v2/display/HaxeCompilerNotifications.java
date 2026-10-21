package com.intellij.plugins.haxe.v2.display;

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationAction;
import com.intellij.notification.NotificationGroup;
import com.intellij.notification.NotificationGroupManager;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.options.ShowSettingsUtil;
import com.intellij.openapi.project.Project;
import com.intellij.plugins.haxe.HaxeBundle;
import com.intellij.plugins.haxe.v2.buildtools.settings.ui.HaxeBuildToolsConfigurable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.TestOnly;

/**
 * Balloons of the compiler services - the one home of the {@code haxe.compiler}
 * notification group registered in plugin.xml. The "compiler unavailable"
 * notice shows once per project: a cause that stops completion stops
 * navigation too, so the two features share one notice, shown by whichever
 * runs into the cause first.
 */
@Service(Service.Level.PROJECT)
public final class HaxeCompilerNotifications {

  private static final String GROUP_ID = "haxe.compiler";

  private final Project project;
  private volatile boolean unavailabilityNotified;

  public HaxeCompilerNotifications(@NotNull Project project) {
    this.project = project;
  }

  @NotNull
  public static HaxeCompilerNotifications getInstance(@NotNull Project project) {
    return project.getService(HaxeCompilerNotifications.class);
  }

  /** A warning balloon. */
  public void warn(@NotNull String title, @NotNull String content) {
    group().createNotification(title, content, NotificationType.WARNING).notify(project);
  }

  /**
   * The once-per-project "compiler unavailable" notice: the feature's title
   * and content, a Configure action opening the Build Tools page, and the
   * feature's own way back to the IDE ({@code useIdeKey} labels it,
   * {@code useIde} does it).
   */
  public void notifyUnavailableOnce(@NotNull String titleKey,
                                    @NotNull String contentKey,
                                    @NotNull String configureKey,
                                    @NotNull String useIdeKey,
                                    @NotNull Runnable useIde) {
    if (unavailabilityNotified) return;
    unavailabilityNotified = true;
    Notification notification =
      group().createNotification(HaxeBundle.message(titleKey), HaxeBundle.message(contentKey), NotificationType.WARNING);
    notification.addAction(NotificationAction.createSimpleExpiring(
      HaxeBundle.message(configureKey),
      () -> ShowSettingsUtil.getInstance().showSettingsDialog(project, HaxeBuildToolsConfigurable.class)));
    notification.addAction(NotificationAction.createSimpleExpiring(HaxeBundle.message(useIdeKey), useIde));
    notification.notify(project);
  }

  @TestOnly
  public boolean unavailabilityNotifiedForTests() {
    return unavailabilityNotified;
  }

  /** Arms the once-per-project notice again; the shared light project otherwise keeps it fired between tests. */
  @TestOnly
  public void resetUnavailabilityNotificationForTests() {
    unavailabilityNotified = false;
  }

  @NotNull
  private static NotificationGroup group() {
    return NotificationGroupManager.getInstance().getNotificationGroup(GROUP_ID);
  }
}
