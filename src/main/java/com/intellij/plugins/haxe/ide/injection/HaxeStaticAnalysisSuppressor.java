package com.intellij.plugins.haxe.ide.injection;

import com.intellij.codeInspection.InspectionSuppressor;
import com.intellij.codeInspection.SuppressQuickFix;
import com.intellij.plugins.haxe.ide.annotator.semantics.AnnotatorUtil;
import com.intellij.plugins.haxe.v2.display.HaxeCompilerDiagnosticsBatchInspections;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

/**
 * Suppresses the plugin's static-analysis inspections where they must stay
 * silent: in code exempt from analysis (doc-comment code fences, inactive
 * conditional branches), and everywhere while the compiler is the only
 * analysis ("compiler diagnostics only"). The compiler-backed batch
 * inspections report the compiler's own findings and stay active in that
 * mode. This covers the inspections that bypass the HaxeInspection base
 * class, and its shouldSkip check, with their own visitors.
 */
public class HaxeStaticAnalysisSuppressor implements InspectionSuppressor {

  private static final Set<String> COMPILER_BACKED_TOOLS = Set.of(
    HaxeCompilerDiagnosticsBatchInspections.ERRORS_SHORT_NAME,
    HaxeCompilerDiagnosticsBatchInspections.UNUSED_IMPORT_SHORT_NAME,
    HaxeCompilerDiagnosticsBatchInspections.REMOVABLE_CODE_SHORT_NAME);

  @Override
  public boolean isSuppressedFor(@NotNull PsiElement element, @NotNull String toolId) {
    if (AnnotatorUtil.isInAnalysisExemptCode(element)) return true;
    return !COMPILER_BACKED_TOOLS.contains(toolId) && AnnotatorUtil.isStaticAnalysisSuppressed(element);
  }

  @Override
  public SuppressQuickFix @NotNull [] getSuppressActions(@Nullable PsiElement element, @NotNull String toolId) {
    return SuppressQuickFix.EMPTY_ARRAY;
  }
}
