package com.intellij.plugins.haxe.v2.display;

import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.util.TextRange;
import com.intellij.plugins.haxe.HaxeBundle;
import com.intellij.plugins.haxe.display.protocol.Diagnostic;
import com.intellij.plugins.haxe.display.protocol.DiagnosticKind;
import com.intellij.plugins.haxe.display.protocol.Range;
import com.intellij.plugins.haxe.v2.compiler.settings.HaxeCompilerSettings;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;

/**
 * Unused code reported by the compiler's {@code display/diagnostics}: unused
 * local variables and local functions. The compiler reports no unused fields
 * or methods. The quick fix removes the range given in the diagnostic's
 * arguments, which may be wider than the highlighted range. When haxe 5
 * supplies {@code newCode}, the fix replaces the range with it instead.
 * While its toggle is on, this annotator REPLACES the plugin's unused
 * local-variable and local-function inspections, which switch themselves off.
 */
public class HaxeCompilerRemovableCodeAnnotator extends HaxeCompilerDiagnosticsAnnotatorBase {

  @Override
  protected boolean isFeatureEnabled(@NotNull HaxeCompilerSettings settings) {
    return settings.isDiagnosticsRemovableCodeEnabled();
  }

  @Override
  public String getPairedBatchInspectionShortName() {
    return HaxeCompilerDiagnosticsBatchInspections.REMOVABLE_CODE_SHORT_NAME;
  }

  @Override
  protected boolean handles(@NotNull PsiFile file, @NotNull Diagnostic diagnostic) {
    return diagnostic.kind() == DiagnosticKind.REMOVABLE_CODE;
  }

  @Override
  protected void annotate(@NotNull AnnotationHolder holder, @NotNull PsiFile file, @NotNull Document document,
                          @NotNull Diagnostic diagnostic, @NotNull TextRange range) {
    TextRange removal = removalRange(document, diagnostic, range);
    holder.newAnnotation(HaxeDiagnosticsFetcher.severityOf(diagnostic), messageOf(diagnostic))
      .range(range)
      .highlightType(ProblemHighlightType.LIKE_UNUSED_SYMBOL)
      .withFix(fixFor(diagnostic, document, removal))
      .create();
  }

  @NotNull
  private static HaxeReplaceRangeQuickFix fixFor(@NotNull Diagnostic diagnostic, @NotNull Document document,
                                                 @NotNull TextRange removal) {
    String newCode = diagnostic.newCodeArg();
    String label = newCode != null
                   ? HaxeBundle.message("haxe.diagnostics.fix.replace.code")
                   : HaxeBundle.message("haxe.diagnostics.fix.remove.code");
    return new HaxeReplaceRangeQuickFix(label, removal, document.getText(removal), newCode != null ? newCode : "");
  }

  @NotNull
  private static String messageOf(@NotNull Diagnostic diagnostic) {
    String description = diagnostic.descriptionArg();
    return description.isBlank() ? HaxeBundle.message("haxe.diagnostics.generic") : description;
  }

  /** The removal range from the diagnostic's arguments when it still fits the document, else the highlighted range. */
  @NotNull
  private static TextRange removalRange(@NotNull Document document, @NotNull Diagnostic diagnostic,
                                        @NotNull TextRange highlighted) {
    Range fromArgs = diagnostic.removableRangeArg();
    if (fromArgs == null) return highlighted;
    TextRange converted = HaxeDiagnosticsFetcher.toTextRange(document, fromArgs);
    return converted != null ? converted : highlighted;
  }
}
