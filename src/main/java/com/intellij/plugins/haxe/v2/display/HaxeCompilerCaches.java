package com.intellij.plugins.haxe.v2.display;

import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.project.Project;
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
   * Clears every compiler-derived cache (compiled contexts, type catalog,
   * metadata registry, blueprints, usage verdicts, dumps, preview files and
   * diagnostics), then drops the PSI caches and restarts the daemon. The
   * order matters: the services empty first, the PSI drop removes results
   * derived from them, and the restart recomputes from scratch. Call on the
   * EDT.
   */
  public static void clearAndRehighlight(@NotNull Project project, @NotNull @NonNls String reason) {
    HaxeCompilerDisplayService.getInstance(project).resetCompiledContexts();

    HaxeCompilerTypeCatalogService.getInstance(project).clearCaches();
    HaxeCompilerMetadataService.getInstance(project).clearCache();
    HaxeCompilerResolveService.getInstance(project).clearCaches();
    HaxeCompilerUsageService.getInstance(project).clearCaches();
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
}
