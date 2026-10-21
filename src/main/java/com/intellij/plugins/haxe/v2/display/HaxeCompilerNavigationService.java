package com.intellij.plugins.haxe.v2.display;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.plugins.haxe.display.protocol.DisplayMethods;
import com.intellij.plugins.haxe.display.protocol.FindReferencesKind;
import com.intellij.plugins.haxe.display.protocol.Location;
import com.intellij.plugins.haxe.util.HaxeReadActions;
import com.intellij.plugins.haxe.v2.compiler.settings.HaxeCompilerSettings;
import com.intellij.plugins.haxe.v2.display.HaxeCompilerDisplayService.DisplayContext;
import com.intellij.plugins.haxe.v2.display.HaxeCompilerDisplayService.FilePayload;
import com.intellij.plugins.haxe.v2.display.HaxeCompilerDisplayService.FileRequest;
import lombok.CustomLog;
import org.jetbrains.annotations.NotNull;

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
 * {@code contents} for a module it has cached. An offset travels as the
 * server counts it in the text it reads ({@link HaxeServerOffsets}).
 *
 * Availability is checked up front, as for completion: a file the compiler
 * cannot serve gets no request, and a notification, shown once per project,
 * offers to configure the server or to switch back to the IDE's features.
 */
@Service(Service.Level.PROJECT)
@CustomLog
public final class HaxeCompilerNavigationService {

  private final Project project;

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

  private void notifyUnavailableOnce() {
    HaxeCompilerNotifications.getInstance(project).notifyUnavailableOnce(
      "haxe.compiler.ide.features.unavailable.title",
      "haxe.compiler.ide.features.unavailable.content",
      "haxe.compiler.ide.features.unavailable.configure",
      "haxe.compiler.ide.features.unavailable.use.ide",
      () -> HaxeCompilerSettings.getInstance(project).setCompilerIdeFeaturesEnabled(false));
  }

  /** Every reference of the symbol at the offset, declaration excluded; empty when the server gives none. Background thread. */
  @NotNull
  public List<Location> references(@NotNull VirtualFile file, int offset, @NotNull FindReferencesKind kind) {
    return request(file, offset, DisplayMethods.FIND_REFERENCES,
                   (connected, path, payload) -> connected.client().references(connected.args(), path, payload.serverOffset(), payload.contents(), kind));
  }

  /** The declarations of the symbol at the offset; empty when the server gives none. Background thread. */
  @NotNull
  public List<Location> definition(@NotNull VirtualFile file, int offset) {
    return request(file, offset, DisplayMethods.GOTO_DEFINITION,
                   (connected, path, payload) -> connected.client().definition(connected.args(), path, payload.serverOffset(), payload.contents()));
  }

  /**
   * {@link #definition} for a caller that holds the read lock and so must
   * not block on the network: the request runs on a pooled thread while the
   * caller waits cancelably for at most
   * {@link HaxeCancelableFutures#ANSWER_TIMEOUT_MS}. Empty when the server
   * gives none or the wait runs out.
   */
  @NotNull
  public List<Location> definitionUnderReadLock(@NotNull VirtualFile file, int offset) {
    Future<List<Location>> request = ApplicationManager.getApplication().executeOnPooledThread(() -> definition(file, offset));
    String label = "display/definition for " + file.getPath();
    List<Location> answer = HaxeCancelableFutures.awaitUnderReadLock(request, HaxeCancelableFutures.ANSWER_TIMEOUT_MS, label);
    return answer == null ? List.of() : answer;
  }

  @NotNull
  private List<Location> request(@NotNull VirtualFile file, int offset, @NotNull String method, @NotNull FileRequest<List<Location>> request) {
    HaxeCompilerDisplayService displayService = HaxeCompilerDisplayService.getInstance(project);
    DisplayContext context = HaxeReadActions.compute(() -> displayService.contextFor(file));
    if (context == null) return List.of();
    FilePayload payload = HaxeReadActions.compute(() -> HaxeCompilerDisplayService.payloadOf(file, offset));
    List<Location> locations = displayService.send(context, method, file.getPath(), payload, request);
    return locations == null ? List.of() : locations;
  }

}
