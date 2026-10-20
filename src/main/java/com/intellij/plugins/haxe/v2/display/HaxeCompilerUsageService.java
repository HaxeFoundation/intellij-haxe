package com.intellij.plugins.haxe.v2.display;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.io.FileUtilRt;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.openapi.vfs.newvfs.BulkFileListener;
import com.intellij.openapi.vfs.newvfs.events.VFileContentChangeEvent;
import com.intellij.openapi.vfs.newvfs.events.VFileEvent;
import com.intellij.plugins.haxe.HaxeFileType;
import com.intellij.plugins.haxe.display.protocol.DisplayMethods;
import com.intellij.plugins.haxe.display.protocol.FindReferencesKind;
import com.intellij.plugins.haxe.display.protocol.Location;
import com.intellij.plugins.haxe.lang.psi.HaxeComponentName;
import com.intellij.plugins.haxe.lang.psi.HaxeMethod;
import com.intellij.plugins.haxe.lang.psi.HaxeNamedComponent;
import com.intellij.plugins.haxe.v2.compiler.settings.HaxeCompilerSettings;
import com.intellij.plugins.haxe.v2.display.HaxeUsageSearch.UsageState;
import com.intellij.plugins.haxe.v2.display.HaxeUsageVerdictCache.Key;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import lombok.CustomLog;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The compiler's answer to "is this member referenced anywhere?" via
 * {@code display/references}. The compiler sees the post-macro program, so
 * usages that exist only in generated code (a handler a macro wires up)
 * count.
 *
 * Inspections run under the read lock, so queries only read the cache. A
 * miss schedules background hydration and answers UNKNOWN. When the verdict
 * arrives, highlighting restarts and the inspection reads it from the cache.
 * A verdict holds for one revision of its own file and for the saved state
 * of every other file, since the compiler sees other files as saved. A save
 * elsewhere therefore retires the UNUSED verdicts it may contradict (see
 * {@link HaxeUsageVerdictCache}).
 */
@Service(Service.Level.PROJECT)
@CustomLog
public final class HaxeCompilerUsageService {

  private record Request(@NotNull Key key,
                         @NotNull HaxeCompilerDisplayService.DisplayContext context,
                         @NotNull HaxeCompilerDisplayService.FilePayload payload,
                         @NotNull FindReferencesKind kind,
                         long fileStamp) {
  }

  private static final long FAILURE_COOLDOWN_MS = 30_000;

  private final Project project;
  private final HaxeUsageVerdictCache verdicts = new HaxeUsageVerdictCache();
  private final Set<Key> hydrating = ConcurrentHashMap.newKeySet();
  private final Map<Key, Long> failedAt = new ConcurrentHashMap<>();

  public HaxeCompilerUsageService(@NotNull Project project) {
    this.project = project;
    project.getMessageBus().connect().subscribe(VirtualFileManager.VFS_CHANGES, new BulkFileListener() {
      @Override
      public void after(@NotNull List<? extends @NotNull VFileEvent> events) {
        for (VFileEvent event : events) {
          if (event instanceof VFileContentChangeEvent && isHaxeSource(event.getPath())) {
            retireUnusedVerdictsInOtherFiles(event.getPath());
          }
        }
      }
    });
  }

  @NotNull
  public static HaxeCompilerUsageService getInstance(@NotNull Project project) {
    return project.getService(HaxeCompilerUsageService.class);
  }

  /**
   * Cache-only usage verdict for a declaration. UNKNOWN when the feature is
   * off, the file has no display context, or the answer is still being
   * fetched. Call in a read action.
   */
  @NotNull
  public UsageState usageState(@NotNull HaxeNamedComponent declaration) {
    if (!HaxeCompilerSettings.getInstance(project).isCompilerDiagnosticsEnabled()) return UsageState.UNKNOWN;
    if (DumbService.isDumb(project)) return UsageState.UNKNOWN;
    HaxeComponentName componentName = declaration.getComponentName();
    if (componentName == null) return UsageState.UNKNOWN;
    VirtualFile virtualFile = HaxeCompilerDisplayService.physicalFileOf(declaration);
    if (virtualFile == null) return UsageState.UNKNOWN;
    HaxeCompilerDisplayService.DisplayContext context = HaxeCompilerDisplayService.getInstance(project).contextFor(virtualFile);
    if (context == null) return UsageState.UNKNOWN;

    long fileStamp = virtualFile.getModificationStamp();
    Key key = new Key(HaxeCompilerDisplayService.contextKey(context),
                      virtualFile.getPath(),
                      componentName.getText(),
                      componentName.getTextRange().getStartOffset());
    UsageState cached = verdicts.get(key, fileStamp);
    if (cached != null) return cached;

    // no request while the text does not parse; cached verdicts are still
    // served above, only new server work waits for valid syntax
    if (!HaxeCompilerDisplayService.isSyntaxClean(project, virtualFile)) return UsageState.UNKNOWN;

    HaxeCompilerDisplayService.FilePayload payload = HaxeCompilerDisplayService.payloadOf(virtualFile, key.offset());
    scheduleHydration(new Request(key, context, payload, referenceKindOf(declaration), fileStamp));
    return UsageState.UNKNOWN;
  }

  /** How the compiler searches a declaration's references: a method's search includes its base and overriding declarations, since an override can be reached through a base-typed call; anything else is searched directly. */
  @NotNull
  public static FindReferencesKind referenceKindOf(@Nullable HaxeNamedComponent declaration) {
    return declaration instanceof HaxeMethod ? FindReferencesKind.WITH_BASE_AND_DESCENDANTS : FindReferencesKind.DIRECT;
  }

  public void clearCaches() {
    verdicts.clear();
    failedAt.clear();
  }

  private static boolean isHaxeSource(@NotNull String path) {
    return FileUtilRt.extensionEquals(path, HaxeFileType.DEFAULT_EXTENSION);
  }

  private void retireUnusedVerdictsInOtherFiles(@NotNull String savedPath) {
    if (verdicts.dropUnusedInOtherFiles(savedPath)) {
      HaxeCompilerCaches.restartHighlightingLater(project, "haxe: a saved file may reference members held unused");
    }
  }

  private void scheduleHydration(@NotNull Request request) {
    Long failed = failedAt.get(request.key());
    if (failed != null && System.currentTimeMillis() - failed < FAILURE_COOLDOWN_MS) return;
    if (!hydrating.add(request.key())) return;

    ApplicationManager.getApplication().executeOnPooledThread(() -> {
      try {
        UsageState state = fetchVerdict(request);
        if (state != UsageState.UNKNOWN) {
          verdicts.put(request.key(), request.fileStamp(), state);
          failedAt.remove(request.key());
          HaxeCompilerCaches.restartHighlightingLater(project, "haxe: compiler usage data updated");
        } else {
          failedAt.put(request.key(), System.currentTimeMillis());
        }
      } catch (Throwable t) {
        log.warn("usage lookup failed for " + request.key().memberName() + ": " + t.getMessage());
        failedAt.put(request.key(), System.currentTimeMillis());
      } finally {
        hydrating.remove(request.key());
      }
    });
  }

  @NotNull
  private UsageState fetchVerdict(@NotNull Request request) {
    HaxeCompilerDisplayService displayService = HaxeCompilerDisplayService.getInstance(project);
    List<Location> references = displayService.send(
      request.context(), DisplayMethods.FIND_REFERENCES, request.key().filePath(), request.payload(),
      (connected, path, payload) -> connected.client().references(connected.args(), path, payload.serverOffset(), payload.contents(), request.kind()));
    if (references == null) return UsageState.UNKNOWN;
    return references.isEmpty() ? UsageState.UNUSED : UsageState.USED;
  }
}
