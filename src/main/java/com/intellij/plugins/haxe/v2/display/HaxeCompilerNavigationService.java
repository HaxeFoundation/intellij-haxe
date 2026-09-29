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
import com.intellij.plugins.haxe.display.protocol.DisplayMethods;
import com.intellij.plugins.haxe.display.protocol.FindReferencesKind;
import com.intellij.plugins.haxe.display.protocol.Location;
import com.intellij.plugins.haxe.display.transport.DisplayRequestException;
import com.intellij.plugins.haxe.util.HaxeReadActions;
import com.intellij.plugins.haxe.v2.buildtools.settings.ui.HaxeBuildToolsConfigurable;
import com.intellij.plugins.haxe.v2.compiler.settings.HaxeCompilerSettings;
import com.intellij.plugins.haxe.v2.display.HaxeCompilerDisplayService.Connected;
import com.intellij.plugins.haxe.v2.display.HaxeCompilerDisplayService.DisplayContext;
import lombok.CustomLog;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.TestOnly;

import java.util.List;
import java.util.concurrent.Future;

/**
 * Asks the compilation server for Find Usages ({@code display/references})
 * and go-to-declaration ({@code display/definition}) at a position in a
 * file. Both are single, user-initiated requests that block on the network.
 * Find Usages calls from a progress thread. Go-to-declaration calls under
 * the read lock, so it uses the variant that runs the request on a pooled
 * thread and waits cancelably for a bounded time. A file with unsaved
 * changes is invalidated on the server first, because the server ignores
 * {@code contents} for a module it has cached.
 *
 * Availability is checked up front, as for completion: a file the compiler
 * cannot serve gets no request, and a notification, shown once per project,
 * offers to configure the server or to switch back to the IDE's features.
 */
@Service(Service.Level.PROJECT)
@CustomLog
public final class HaxeCompilerNavigationService {

  private static final String NOTIFICATION_GROUP = "haxe.compiler";
  private static final long ANSWER_TIMEOUT_MS = 3_000;

  @FunctionalInterface
  private interface Request {
    List<Location> send(@NotNull Connected connected, @Nullable String contents) throws DisplayRequestException;
  }

  private final Project project;
  private volatile boolean unavailabilityNotified;

  public static HaxeCompilerNavigationService getInstance(@NotNull Project project) {
    return project.getService(HaxeCompilerNavigationService.class);
  }

  public HaxeCompilerNavigationService(@NotNull Project project) {
    this.project = project;
  }

  /**
   * Whether the compiler can answer for this file (see
   * {@link HaxeCompilerFeatureAvailability}). A false answer shows the
   * notification, once per project. Call in a read action; no network.
   */
  public boolean ensureAvailable(@NotNull VirtualFile file) {
    boolean available = HaxeCompilerFeatureAvailability.isAvailable(project, file);
    if (!available) notifyUnavailableOnce();
    return available;
  }

  /** Every reference of the symbol at the offset, declaration excluded; empty when the server gives none. Background thread. */
  @NotNull
  public List<Location> references(@NotNull VirtualFile file, int offset, @NotNull FindReferencesKind kind) {
    String path = file.getPath();
    return request(file, DisplayMethods.FIND_REFERENCES,
                   (connected, contents) -> connected.client().references(connected.args(), path, offset, contents, kind));
  }

  /** The declarations of the symbol at the offset; empty when the server gives none. Background thread. */
  @NotNull
  public List<Location> definition(@NotNull VirtualFile file, int offset) {
    String path = file.getPath();
    return request(file, DisplayMethods.GOTO_DEFINITION,
                   (connected, contents) -> connected.client().definition(connected.args(), path, offset, contents));
  }

  /**
   * {@link #definition} for a caller that holds the read lock and so must
   * not block on the network: the request runs on a pooled thread while the
   * caller waits cancelably for at most {@link #ANSWER_TIMEOUT_MS}. Empty
   * when the server gives none or the wait runs out.
   */
  @NotNull
  public List<Location> definitionUnderReadLock(@NotNull VirtualFile file, int offset) {
    Future<List<Location>> request = ApplicationManager.getApplication().executeOnPooledThread(() -> definition(file, offset));
    String label = "display/definition for " + file.getPath();
    List<Location> answer = HaxeCancelableFutures.awaitUnderReadLock(request, ANSWER_TIMEOUT_MS, label);
    return answer == null ? List.of() : answer;
  }

  @NotNull
  private List<Location> request(@NotNull VirtualFile file, @NotNull String method, @NotNull Request request) {
    HaxeCompilerDisplayService displayService = HaxeCompilerDisplayService.getInstance(project);
    DisplayContext context = HaxeReadActions.compute(() -> displayService.contextFor(file));
    if (context == null) return List.of();
    String contents = HaxeReadActions.compute(() -> HaxeCompilerDisplayService.unsavedContents(file));
    Connected connected = displayService.connectFor(context, method);
    if (connected == null) return List.of();
    try {
      if (contents != null) {
        connected.client().invalidate(connected.args(), file.getPath());
      }
      return request.send(connected, contents);
    } catch (DisplayRequestException e) {
      log.info(method + " failed for " + file.getPath() + ": " + e.getMessage());
      return List.of();
    }
  }

  private void notifyUnavailableOnce() {
    if (unavailabilityNotified) return;
    unavailabilityNotified = true;
    String title = HaxeBundle.message("haxe.compiler.ide.features.unavailable.title");
    String content = HaxeBundle.message("haxe.compiler.ide.features.unavailable.content");
    Notification notification = NotificationGroupManager.getInstance()
      .getNotificationGroup(NOTIFICATION_GROUP)
      .createNotification(title, content, NotificationType.WARNING);
    notification.addAction(NotificationAction.createSimpleExpiring(
      HaxeBundle.message("haxe.compiler.ide.features.unavailable.configure"),
      () -> ShowSettingsUtil.getInstance().showSettingsDialog(project, HaxeBuildToolsConfigurable.class)));
    notification.addAction(NotificationAction.createSimpleExpiring(
      HaxeBundle.message("haxe.compiler.ide.features.unavailable.use.ide"),
      () -> HaxeCompilerSettings.getInstance(project).setCompilerIdeFeaturesEnabled(false)));
    notification.notify(project);
  }

  @TestOnly
  public boolean unavailabilityNotifiedForTests() {
    return unavailabilityNotified;
  }
}
