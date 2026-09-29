package com.intellij.plugins.haxe.v2.display;

import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.util.TextRange;
import com.intellij.plugins.haxe.HaxeBundle;
import com.intellij.plugins.haxe.display.protocol.Diagnostic;
import com.intellij.plugins.haxe.display.protocol.DiagnosticKind;
import com.intellij.plugins.haxe.v2.compiler.settings.HaxeCompilerSettings;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;

/**
 * Unused imports reported by the compiler's {@code display/diagnostics},
 * with a quick fix removing the reported range. While its toggle is on, this
 * annotator REPLACES the plugin's own unused-import inspection, which
 * switches itself off. The compiler's post-macro view alone then decides
 * whether an import is used.
 */
public class HaxeCompilerUnusedImportAnnotator extends HaxeCompilerDiagnosticsAnnotatorBase {

  @Override
  protected boolean isFeatureEnabled(@NotNull HaxeCompilerSettings settings) {
    return settings.isDiagnosticsUnusedImportsEnabled();
  }

  @Override
  public String getPairedBatchInspectionShortName() {
    return HaxeCompilerDiagnosticsBatchInspections.UNUSED_IMPORT_SHORT_NAME;
  }

  @Override
  protected boolean handles(@NotNull PsiFile file, @NotNull Diagnostic diagnostic) {
    return diagnostic.kind() == DiagnosticKind.UNUSED_IMPORT;
  }

  @Override
  protected void annotate(@NotNull AnnotationHolder holder, @NotNull PsiFile file, @NotNull Document document,
                          @NotNull Diagnostic diagnostic, @NotNull TextRange range) {
    String fixName = HaxeBundle.message("haxe.diagnostics.fix.remove.import");
    HaxeReplaceRangeQuickFix fix = new HaxeReplaceRangeQuickFix(fixName, range, document.getText(range), "");
    holder.newAnnotation(HaxeDiagnosticsFetcher.severityOf(diagnostic), HaxeBundle.message("haxe.diagnostics.unused.import"))
      .range(range)
      .highlightType(ProblemHighlightType.LIKE_UNUSED_SYMBOL)
      .withFix(fix)
      .create();
  }
}
