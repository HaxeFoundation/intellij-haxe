package com.intellij.plugins.haxe.v2.display;

import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.project.Project;
import com.intellij.plugins.haxe.model.HaxeResolveCaches;
import com.intellij.psi.PsiManager;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

/**
 * The single entry point for clearing everything derived from the
 * compilation server, plus the highlighting restart the services post after
 * hydration. Callers never clear individual services, because a partial
 * clear keeps PSI-level results derived from the skipped caches alive.
 */
public final class HaxeCompilerCaches {

  private HaxeCompilerCaches() {
  }

  /**
   * Clears every compiler-derived cache (compiled contexts, server
   * capabilities, lime display arguments, type catalog, metadata registry,
   * blueprints, usage verdicts, hover types, compiler inlay members, dumps,
   * preview files and diagnostics), then
   * drops the PSI caches and restarts the daemon. The order matters: the
   * services empty first, the PSI drop removes results derived from them, and
   * the restart recomputes from scratch. Call on the EDT.
   */
  public static void clearAndRehighlight(@NotNull Project project, @NotNull @NonNls String reason) {
    HaxeCompilerDisplayService.getInstance(project).clearCaches();

    HaxeCompilerTypeCatalogService.getInstance(project).clearCaches();
    HaxeCompilerMetadataService.getInstance(project).clearCache();
    HaxeCompilerResolveService.getInstance(project).clearCaches();
    HaxeCompilerUsageService.getInstance(project).clearCaches();
    HaxeCompilerHoverTypes.getInstance(project).clearCaches();
    HaxeCompilerTypeLookupService.getInstance(project).clearCaches();
    HaxeCompilerInlayService.getInstance(project).clearCaches();
    HaxeGeneratedDumpService.getInstance(project).clearCaches();

    HaxeGeneratedCodePreview.clearCaches();
    HaxeDiagnosticsFetcher.clearCache(project);

    PsiManager.getInstance(project).dropPsiCaches();
    DaemonCodeAnalyzer.getInstance(project).restart(reason);
  }

  /**
   * Restarts highlighting from any thread. The restart is posted to the EDT
   * with an explicit non-modal state, because a post from a pooled thread
   * without one runs write-unsafe. It is skipped once the project is disposed.
   */
  public static void restartHighlightingLater(@NotNull Project project, @NotNull @NonNls String reason) {
    Runnable restart = () -> DaemonCodeAnalyzer.getInstance(project).restart(reason);
    ApplicationManager.getApplication().invokeLater(restart, ModalityState.nonModal(), project.getDisposed());
  }

  /**
   * {@link #restartHighlightingLater} for a compiler answer that changes what
   * cached evaluations and resolves returned. The evaluator's own result
   * caches go first, since they are keyed by element and not by PSI state;
   * dropping the resolve caches alone is not enough either: type-evaluation
   * CachedValues and the inlay passes follow the PSI modification count,
   * which only the PSI cache drop moves.
   */
  public static void dropPsiCachesAndRestartLater(@NotNull Project project, @NotNull @NonNls String reason) {
    Runnable restart = () -> {
      HaxeResolveCaches.clear(project);
      PsiManager.getInstance(project).dropPsiCaches();
      DaemonCodeAnalyzer.getInstance(project).restart(reason);
    };
    ApplicationManager.getApplication().invokeLater(restart, ModalityState.nonModal(), project.getDisposed());
  }
}
