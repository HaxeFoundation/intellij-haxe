package com.intellij.plugins.haxe.v2.display;

import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.ExternalAnnotator;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.util.TextRange;
import com.intellij.plugins.haxe.display.protocol.Diagnostic;
import com.intellij.plugins.haxe.v2.compiler.settings.HaxeCompilerSettings;
import com.intellij.psi.PsiFile;
import lombok.CustomLog;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Base class of the compiler-diagnostics external annotators. An annotator
 * runs only while both the master compiler-diagnostics switch and its own
 * toggle are on. It collects the request under the read lock, fetches the
 * diagnostics without the lock, and then annotates every diagnostic it
 * handles whose range still fits the document. Subclasses supply their
 * toggle, their paired batch inspection, the diagnostics they handle and the
 * annotation itself.
 */
@CustomLog
abstract class HaxeCompilerDiagnosticsAnnotatorBase
  extends ExternalAnnotator<HaxeDiagnosticsFetcher.Request, List<Diagnostic>> {

  /** Whether this annotator's own toggle is on; it counts only under the master switch. */
  protected abstract boolean isFeatureEnabled(@NotNull HaxeCompilerSettings settings);

  /** Whether this annotator renders the diagnostic in {@code file}. */
  protected abstract boolean handles(@NotNull PsiFile file, @NotNull Diagnostic diagnostic);

  /** Creates the annotation for a handled diagnostic at its document range. */
  protected abstract void annotate(@NotNull AnnotationHolder holder, @NotNull PsiFile file, @NotNull Document document,
                                   @NotNull Diagnostic diagnostic, @NotNull TextRange range);

  @Override
  @Nullable
  public final HaxeDiagnosticsFetcher.Request collectInformation(@NotNull PsiFile file, @NotNull Editor editor, boolean hasErrors) {
    boolean enabled = isEnabled(file);
    log.debug(getClass().getSimpleName() + " pass for " + file.getName() + ": enabled=" + enabled);
    return enabled ? HaxeDiagnosticsFetcher.collect(file, editor) : null;
  }

  /** Batch (Inspect Code) entry, reached through the paired inspection. */
  @Override
  @Nullable
  public final HaxeDiagnosticsFetcher.Request collectInformation(@NotNull PsiFile file) {
    return isEnabled(file) ? HaxeDiagnosticsFetcher.collect(file) : null;
  }

  @Override
  @Nullable
  public final List<Diagnostic> doAnnotate(HaxeDiagnosticsFetcher.Request request) {
    return HaxeDiagnosticsFetcher.fetch(request);
  }

  @Override
  public final void apply(@NotNull PsiFile file, @Nullable List<Diagnostic> diagnostics, @NotNull AnnotationHolder holder) {
    if (diagnostics == null) return;
    Document document = file.getViewProvider().getDocument();
    if (document == null) return;
    int annotated = 0;
    for (Diagnostic diagnostic : diagnostics) {
      if (!handles(file, diagnostic)) continue;
      TextRange range = HaxeDiagnosticsFetcher.toTextRange(document, diagnostic.range());
      if (range == null) continue;
      annotate(holder, file, document, diagnostic, range);
      annotated++;
    }
    log.debug(getClass().getSimpleName() + " annotated " + annotated + " of " + diagnostics.size()
              + " diagnostics in " + file.getName());
  }

  private boolean isEnabled(@NotNull PsiFile file) {
    HaxeCompilerSettings settings = HaxeCompilerSettings.getInstance(file.getProject());
    return settings.isCompilerDiagnosticsEnabled() && isFeatureEnabled(settings);
  }
}
