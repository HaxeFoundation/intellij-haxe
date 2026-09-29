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
import com.intellij.plugins.haxe.display.transport.DisplayRequestException;
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
                         @Nullable String contents,
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

    // overriding methods can be reached through a base-typed call
    FindReferencesKind kind = declaration instanceof HaxeMethod
                              ? FindReferencesKind.WITH_BASE_AND_DESCENDANTS
                              : FindReferencesKind.DIRECT;
    String contents = HaxeCompilerDisplayService.unsavedContents(virtualFile);
    scheduleHydration(new Request(key, context, contents, kind, fileStamp));
    return UsageState.UNKNOWN;
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
    HaxeCompilerDisplayService.Connected connected =
      HaxeCompilerDisplayService.getInstance(project).connectFor(request.context(), DisplayMethods.FIND_REFERENCES);
    if (connected == null) return UsageState.UNKNOWN;
    try {
      String filePath = request.key().filePath();
      if (request.contents() != null) {
        connected.client().invalidate(connected.args(), filePath);
      }
      List<Location> references = connected.client()
        .references(connected.args(), filePath, request.key().offset(), request.contents(), request.kind());
      return references.isEmpty() ? UsageState.UNUSED : UsageState.USED;
    } catch (DisplayRequestException e) {
      log.info("display/references failed for " + request.key().memberName() + ": " + e.getMessage());
      return UsageState.UNKNOWN;
    }
  }
}
