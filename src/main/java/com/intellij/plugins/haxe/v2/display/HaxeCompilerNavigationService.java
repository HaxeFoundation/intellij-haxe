package com.intellij.plugins.haxe.v2.display;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.SimpleModificationTracker;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.plugins.haxe.display.protocol.DisplayMethods;
import com.intellij.plugins.haxe.display.protocol.FindReferencesKind;
import com.intellij.plugins.haxe.display.protocol.Location;
import com.intellij.plugins.haxe.util.HaxeReadActions;
import com.intellij.plugins.haxe.v2.compiler.settings.HaxeCompilerSettings;
import com.intellij.plugins.haxe.v2.display.HaxeCompilerDisplayService.DisplayContext;
import com.intellij.plugins.haxe.v2.display.HaxeCompilerDisplayService.FilePayload;
import com.intellij.plugins.haxe.v2.display.HaxeCompilerDisplayService.FileRequest;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.util.CachedValue;
import com.intellij.psi.util.CachedValueProvider;
import com.intellij.psi.util.CachedValuesManager;
import com.intellij.psi.util.PsiModificationTracker;
import lombok.CustomLog;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
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
 * Go-to-declaration answers are kept per file until the next PSI change, so
 * the click that follows a Ctrl-hover, or a repeated hover, sends no new
 * request.
 *
 * Availability is checked up front, as for completion: a file the compiler
 * cannot serve gets no request, and a notification, shown once per project,
 * offers to configure the server or to switch back to the IDE's features.
 */
@Service(Service.Level.PROJECT)
@CustomLog
public final class HaxeCompilerNavigationService {

  /** Where in a file a go-to-declaration request was made: its display context and offset. */
  record DefinitionKey(@NotNull String contextKey, int offset) {
  }

  private static final Key<CachedValue<Map<DefinitionKey, List<Location>>>> DEFINITIONS = Key.create("haxe.compiler.definitions");

  private final Project project;
  // bumped by clearCaches: retires every file's kept answers at once
  private final SimpleModificationTracker purges = new SimpleModificationTracker();

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

  /**
   * Every reference of the symbol at the offset, declaration excluded; empty
   * when the server gives none or no server answered. Background thread.
   */
  @NotNull
  public List<Location> references(@NotNull VirtualFile file, int offset, @NotNull FindReferencesKind kind) {
    FileRequest<List<Location>> findReferences = (connected, path, payload) ->
      connected.client().references(connected.args(), path, payload.serverOffset(), payload.contents(), kind);
    List<Location> locations = request(file, offset, DisplayMethods.FIND_REFERENCES, findReferences);
    return locations == null ? List.of() : locations;
  }

  /** The declarations of the symbol at the offset, empty when the server gives none; null when no server answered. Background thread. */
  @Nullable
  public List<Location> definition(@NotNull VirtualFile file, int offset) {
    FileRequest<List<Location>> findDefinition = (connected, path, payload) ->
      connected.client().definition(connected.args(), path, payload.serverOffset(), payload.contents());
    return request(file, offset, DisplayMethods.GOTO_DEFINITION, findDefinition);
  }

  /**
   * {@link #definition} for a caller that holds the read lock and so must
   * not block on the network. An answer kept from an earlier request at the
   * same position and state serves directly; otherwise the request runs on
   * a pooled thread while the caller waits cancelably for at most
   * {@link HaxeCancelableFutures#ANSWER_TIMEOUT_MS}. Empty when the server
   * places nothing; null when no answer came: the file has no display
   * context, no server runs, the compiler rejected the request or the wait
   * ran out.
   */
  @Nullable
  public List<Location> definitionUnderReadLock(@NotNull VirtualFile file, int offset) {
    DisplayContext context = HaxeCompilerDisplayService.getInstance(project).contextFor(file);
    if (context == null) return null;
    PsiFile psiFile = PsiManager.getInstance(project).findFile(file);
    DefinitionKey key = new DefinitionKey(HaxeCompilerDisplayService.contextKey(context), offset);
    List<Location> kept = psiFile != null ? keptDefinition(psiFile, key) : null;
    if (kept != null) return kept;

    Future<List<Location>> request = ApplicationManager.getApplication().executeOnPooledThread(() -> definition(file, offset));
    String label = "display/definition for " + file.getPath();
    List<Location> answer = HaxeCancelableFutures.awaitUnderReadLock(request, HaxeCancelableFutures.ANSWER_TIMEOUT_MS, label);
    // a wait that ran out, or a server that did not answer, says nothing worth keeping
    if (answer == null) return null;
    if (psiFile != null) keepDefinition(psiFile, key, answer);
    return answer;
  }

  /** The answer kept for the position, or null when none was given since the last PSI change. Call in a read action. */
  @Nullable
  List<Location> keptDefinition(@NotNull PsiFile file, @NotNull DefinitionKey key) {
    return definitionsOf(file).get(key);
  }

  void keepDefinition(@NotNull PsiFile file, @NotNull DefinitionKey key, @NotNull List<Location> answer) {
    definitionsOf(file).put(key, List.copyOf(answer));
  }

  /**
   * The answers given for the file since the project's last PSI change: any edit may move the targets' ranges, in
   * this file or another. Go to Declaration commits the documents first, so an edit is always a PSI change by then.
   */
  @NotNull
  private Map<DefinitionKey, List<Location>> definitionsOf(@NotNull PsiFile file) {
    return CachedValuesManager.getManager(project).getCachedValue(file, DEFINITIONS, () -> CachedValueProvider.Result.create(
      new ConcurrentHashMap<>(), PsiModificationTracker.MODIFICATION_COUNT, purges), false);
  }

  public void clearCaches() {
    purges.incModificationCount();
  }

  /** Null when the file has no display context, no server runs, or the request failed (logged). */
  @Nullable
  private List<Location> request(@NotNull VirtualFile file, int offset, @NotNull String method, @NotNull FileRequest<List<Location>> request) {
    HaxeCompilerDisplayService displayService = HaxeCompilerDisplayService.getInstance(project);
    DisplayContext context = HaxeReadActions.compute(() -> displayService.contextFor(file));
    if (context == null) return null;
    FilePayload payload = HaxeReadActions.compute(() -> HaxeCompilerDisplayService.payloadOf(file, offset));
    return displayService.send(context, method, file.getPath(), payload, request);
  }

}
