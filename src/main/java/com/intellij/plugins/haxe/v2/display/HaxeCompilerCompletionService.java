package com.intellij.plugins.haxe.v2.display;

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationAction;
import com.intellij.notification.NotificationGroupManager;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.options.ShowSettingsUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.plugins.haxe.HaxeBundle;
import com.intellij.plugins.haxe.display.protocol.CompletionItem;
import com.intellij.plugins.haxe.display.protocol.CompletionList;
import com.intellij.plugins.haxe.display.protocol.DisplayMethods;
import com.intellij.plugins.haxe.display.transport.DisplayRequestException;
import com.intellij.plugins.haxe.util.HaxeReadActions;
import com.intellij.plugins.haxe.v2.buildtools.settings.ui.HaxeBuildToolsConfigurable;
import com.intellij.plugins.haxe.v2.compiler.settings.HaxeCompilerSettings;
import com.intellij.plugins.haxe.v2.compiler.settings.HaxeCompletionMode;
import lombok.CustomLog;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.TestOnly;

import java.util.concurrent.Future;

/**
 * The compilation server's completion at a caret, one
 * {@code display/completion} request per popup. For an unsaved buffer the
 * file is invalidated on the server first, because a cached module ignores
 * {@code contents} otherwise.
 *
 * Availability is decided up front and cheaply. A file the compiler cannot
 * serve gets no request at all. Once per project, a notification then names
 * the two ways out: enable the server, or switch completion to the IDE.
 *
 * Completion runs under a read action, which must not wait on the network.
 * The request therefore runs on a pooled thread, and the caller waits for at
 * most {@link #ANSWER_TIMEOUT_MS}. Typing cancels the wait. A slow answer,
 * such as from a server that is still starting or from a {@code lime display} run,
 * is abandoned. The request still completes, so the next popup gets a faster
 * answer.
 */
@Service(Service.Level.PROJECT)
@CustomLog
public final class HaxeCompilerCompletionService {

  private static final String NOTIFICATION_GROUP = "haxe.compiler";
  private static final long ANSWER_TIMEOUT_MS = 3_000;

  private final Project project;
  private volatile boolean unavailabilityNotified;

  public static HaxeCompilerCompletionService getInstance(@NotNull Project project) {
    return project.getService(HaxeCompilerCompletionService.class);
  }

  public HaxeCompilerCompletionService(@NotNull Project project) {
    this.project = project;
  }

  /**
   * Whether the compiler can complete in this file at all (see
   * {@link HaxeCompilerFeatureAvailability}). A false answer shows the
   * notification, once per project. Call in a read action; no network.
   */
  public boolean ensureAvailable(@NotNull VirtualFile file) {
    boolean available = HaxeCompilerFeatureAvailability.isAvailable(project, file);
    if (!available) notifyUnavailableOnce();
    return available;
  }

  /**
   * The compiler's items at the offset, or null when the server gives no
   * answer within the timeout. A cancelled completion ends in the platform's
   * cancellation exception. Call in a read action, after
   * {@link #ensureAvailable}.
   */
  @Nullable
  public CompletionList complete(@NotNull VirtualFile file, @NotNull String contents, boolean unsaved, int offset, boolean autoTriggered) {
    HaxeCompilerDisplayService displayService = HaxeCompilerDisplayService.getInstance(project);
    HaxeCompilerDisplayService.DisplayContext context = displayService.contextFor(file);
    if (context == null) return null;
    String path = file.getPath();
    Future<CompletionList> request = ApplicationManager.getApplication()
      .executeOnPooledThread(() -> fetch(displayService, context, path, contents, unsaved, offset, autoTriggered));
    return HaxeCancelableFutures.awaitUnderReadLock(request, ANSWER_TIMEOUT_MS, "display/completion for " + path);
  }

  /**
   * The doc comment of a completion item that arrived without one, fetched
   * with {@code display/completionItem/resolve}. {@code index} is the item's
   * position in the popup's completion list. Null when there is no server to
   * ask or the declaration has no doc. Call on a background thread without
   * holding the read lock.
   */
  @Nullable
  public String resolveDoc(@NotNull VirtualFile file, int index) {
    HaxeCompilerDisplayService displayService = HaxeCompilerDisplayService.getInstance(project);
    HaxeCompilerDisplayService.DisplayContext context = HaxeReadActions.compute(() -> displayService.contextFor(file));
    if (context == null) return null;
    HaxeCompilerDisplayService.Connected connected = displayService.connectFor(context, DisplayMethods.COMPLETION_ITEM_RESOLVE);
    if (connected == null) return null;
    try {
      CompletionItem resolved = connected.client().resolveCompletionItem(connected.args(), index);
      return resolved == null ? null : resolved.doc();
    } catch (DisplayRequestException e) {
      log.info("display/completionItem/resolve failed for " + file.getPath() + " #" + index + ": " + e.getMessage());
      return null;
    }
  }

  @Nullable
  private static CompletionList fetch(HaxeCompilerDisplayService displayService, HaxeCompilerDisplayService.DisplayContext context,
                                      String path, String contents, boolean unsaved, int offset, boolean autoTriggered) {
    HaxeCompilerDisplayService.Connected connected = displayService.connectFor(context, DisplayMethods.COMPLETION);
    if (connected == null) return null;
    try {
      if (unsaved) {
        connected.client().invalidate(connected.args(), path);
      }
      return connected.client().completion(connected.args(), path, offset, unsaved ? contents : null, autoTriggered);
    } catch (DisplayRequestException e) {
      log.info("display/completion failed for " + path + ": " + e.getMessage());
      return null;
    }
  }

  private void notifyUnavailableOnce() {
    if (unavailabilityNotified) return;
    unavailabilityNotified = true;
    String title = HaxeBundle.message("haxe.compiler.completion.unavailable.title");
    String content = HaxeBundle.message("haxe.compiler.completion.unavailable.content");
    Notification notification = NotificationGroupManager.getInstance()
      .getNotificationGroup(NOTIFICATION_GROUP)
      .createNotification(title, content, NotificationType.WARNING);
    notification.addAction(NotificationAction.createSimpleExpiring(
      HaxeBundle.message("haxe.compiler.completion.unavailable.configure"),
      () -> ShowSettingsUtil.getInstance().showSettingsDialog(project, HaxeBuildToolsConfigurable.class)));
    notification.addAction(NotificationAction.createSimpleExpiring(
      HaxeBundle.message("haxe.compiler.completion.unavailable.use.ide"),
      () -> HaxeCompilerSettings.getInstance(project).setCompletionMode(HaxeCompletionMode.IDE_ONLY)));
    notification.notify(project);
  }

  @TestOnly
  public boolean unavailabilityNotifiedForTests() {
    return unavailabilityNotified;
  }
}
