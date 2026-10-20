package com.intellij.plugins.haxe.v2.display;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.plugins.haxe.display.protocol.CompletionItem;
import com.intellij.plugins.haxe.display.protocol.CompletionList;
import com.intellij.plugins.haxe.display.protocol.DisplayMethods;
import com.intellij.plugins.haxe.display.transport.DisplayRequestException;
import com.intellij.plugins.haxe.util.HaxeReadActions;
import com.intellij.plugins.haxe.v2.compiler.settings.HaxeCompilerSettings;
import com.intellij.plugins.haxe.v2.compiler.settings.HaxeCompletionMode;
import lombok.CustomLog;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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
 * most {@link HaxeCancelableFutures#ANSWER_TIMEOUT_MS}. Typing cancels the
 * wait. A slow answer, such as from a server that is still starting or from a
 * {@code lime display} run, is abandoned. The request still completes, so the
 * next popup gets a faster answer.
 */
@Service(Service.Level.PROJECT)
@CustomLog
public final class HaxeCompilerCompletionService {

  private final Project project;

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

  private void notifyUnavailableOnce() {
    HaxeCompilerNotifications.getInstance(project).notifyUnavailableOnce(
      "haxe.compiler.completion.unavailable.title",
      "haxe.compiler.completion.unavailable.content",
      "haxe.compiler.completion.unavailable.configure",
      "haxe.compiler.completion.unavailable.use.ide",
      () -> HaxeCompilerSettings.getInstance(project).setCompletionMode(HaxeCompletionMode.IDE_ONLY));
  }

  /**
   * The compiler's items at the offset, or null when the server gives no
   * answer within the timeout. A cancelled completion ends in the platform's
   * cancellation exception. Call in a read action, after
   * {@link #ensureAvailable}.
   */
  @Nullable
  public CompletionList complete(@NotNull VirtualFile file, int offset, boolean autoTriggered) {
    HaxeCompilerDisplayService displayService = HaxeCompilerDisplayService.getInstance(project);
    HaxeCompilerDisplayService.DisplayContext context = displayService.contextFor(file);
    if (context == null) return null;
    String path = file.getPath();
    HaxeCompilerDisplayService.FilePayload payload = HaxeCompilerDisplayService.payloadOf(file, offset);
    Future<CompletionList> request = ApplicationManager.getApplication().executeOnPooledThread(
      () -> displayService.send(context, DisplayMethods.COMPLETION, path, payload, (connected, filePath, sent) ->
        connected.client().completion(connected.args(), filePath, sent.serverOffset(), sent.contents(), autoTriggered)));
    return HaxeCancelableFutures.awaitUnderReadLock(request, HaxeCancelableFutures.ANSWER_TIMEOUT_MS, "display/completion for " + path);
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

}
