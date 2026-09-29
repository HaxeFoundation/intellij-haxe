package com.intellij.plugins.haxe.v2.display;

import com.intellij.codeInsight.intention.IntentionAction;
import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.lang.annotation.AnnotationBuilder;
import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.util.TextRange;
import com.intellij.plugins.haxe.HaxeBundle;
import com.intellij.plugins.haxe.display.protocol.Diagnostic;
import com.intellij.plugins.haxe.display.protocol.DiagnosticKind;
import com.intellij.plugins.haxe.display.protocol.InitializeResult;
import com.intellij.plugins.haxe.display.protocol.MissingFields;
import com.intellij.plugins.haxe.ide.annotator.semantics.HaxeSyntaxMigrationFixes;
import com.intellij.plugins.haxe.v2.compiler.HaxeLanguageLevel;
import com.intellij.plugins.haxe.v2.compiler.HaxeLanguageLevelUtil;
import com.intellij.plugins.haxe.v2.compiler.settings.HaxeCompilerSettings;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Error highlighting from the compiler's {@code display/diagnostics}, one
 * request per highlighting pass, carrying the editor buffer when it differs
 * from disk. Covers the problem kinds: compiler and parser errors,
 * deprecation warnings, unresolved identifiers and missing fields. The
 * compiler's suggestions become quick fixes (imports, spelling corrections
 * and the missing members). Unused imports and removable code have their own
 * annotators and toggles. Opt-in on the Haxe Compiler settings page; requires
 * the compilation server and haxe 4.3 or newer, which added the JSON-RPC
 * diagnostics method.
 */
public class HaxeCompilerDiagnosticsAnnotator extends HaxeCompilerDiagnosticsAnnotatorBase {

  @Override
  protected boolean isFeatureEnabled(@NotNull HaxeCompilerSettings settings) {
    return settings.isDiagnosticsErrorsEnabled();
  }

  @Override
  public String getPairedBatchInspectionShortName() {
    return HaxeCompilerDiagnosticsBatchInspections.ERRORS_SHORT_NAME;
  }

  /** The problem kinds, except warnings the module's language level hides. */
  @Override
  protected boolean handles(@NotNull PsiFile file, @NotNull Diagnostic diagnostic) {
    if (!isProblemKind(diagnostic.kind())) return false;
    HaxeLanguageLevel level = HaxeLanguageLevelUtil.getLanguageLevel(file);
    return HaxeDiagnosticMessageFilter.shouldShow(level, connectedHaxeVersion(file), diagnostic);
  }

  /**
   * The problem kinds. Unused imports and removable code have their own
   * annotators. Inactive #if regions are already shown by the define
   * context, so a weak warning per block would only add noise.
   */
  private static boolean isProblemKind(@NotNull DiagnosticKind kind) {
    return switch (kind) {
      case COMPILER_ERROR, PARSER_ERROR, DEPRECATION_WARNING, UNRESOLVED_IDENTIFIER, MISSING_FIELDS, UNKNOWN -> true;
      case UNUSED_IMPORT, REMOVABLE_CODE, INACTIVE_BLOCK -> false;
    };
  }

  @Override
  protected void annotate(@NotNull AnnotationHolder holder, @NotNull PsiFile file, @NotNull Document document,
                          @NotNull Diagnostic diagnostic, @NotNull TextRange range) {
    AnnotationBuilder builder =
      holder.newAnnotation(HaxeDiagnosticsFetcher.severityOf(diagnostic), messageOf(diagnostic)).range(range);
    switch (diagnostic.kind()) {
      case UNRESOLVED_IDENTIFIER -> builder = builder.highlightType(ProblemHighlightType.LIKE_UNKNOWN_SYMBOL);
      case DEPRECATION_WARNING -> builder = builder.highlightType(ProblemHighlightType.LIKE_DEPRECATED);
      default -> {
      }
    }
    IntentionAction modernize = modernizeFixFor(file, diagnostic, range);
    if (modernize != null) {
      builder = builder.withFix(modernize);
    }
    for (IntentionAction fix : compilerFixesFor(document, diagnostic, range)) {
      builder = builder.withFix(fix);
    }
    builder.create();
  }

  /**
   * The compiler's own suggestions as fixes: an import or a spelling
   * correction per unresolved-identifier suggestion, and one fix adding the
   * listed members per missing-fields entry.
   */
  @NotNull
  static List<IntentionAction> compilerFixesFor(@NotNull Document document, @NotNull Diagnostic diagnostic,
                                               @NotNull TextRange range) {
    return switch (diagnostic.kind()) {
      case UNRESOLVED_IDENTIFIER -> suggestionFixes(document, diagnostic, range);
      case MISSING_FIELDS -> missingFieldFixes(diagnostic);
      default -> List.of();
    };
  }

  @NotNull
  private static List<IntentionAction> suggestionFixes(@NotNull Document document, @NotNull Diagnostic diagnostic,
                                                       @NotNull TextRange range) {
    List<IntentionAction> fixes = new ArrayList<>();
    String currentText = document.getText(range);
    for (Diagnostic.IdentifierSuggestion suggestion : diagnostic.suggestionArgs()) {
      if (suggestion.isImportCandidate()) {
        fixes.add(new HaxeCompilerImportQuickFix(suggestion.name()));
      }
      else {
        String label = HaxeBundle.message("haxe.diagnostics.fix.change.to", suggestion.name());
        fixes.add(new HaxeReplaceRangeQuickFix(label, range, currentText, suggestion.name()));
      }
    }
    return fixes;
  }

  @NotNull
  private static List<IntentionAction> missingFieldFixes(@NotNull Diagnostic diagnostic) {
    MissingFields missing = diagnostic.missingFieldsArg();
    if (missing == null) return List.of();
    List<IntentionAction> fixes = new ArrayList<>();
    for (MissingFields.Entry entry : missing.entries()) {
      fixes.add(new HaxeImplementMissingFieldsQuickFix(missing.typeName(), entry));
    }
    return fixes;
  }

  /**
   * A syntax-migration fix when the diagnostic is a deprecation pointing at a
   * construct with a known modern spelling (@:enum abstract, @:final,
   * @:extern, the renamed std APIs). {@link HaxeDiagnosticMessageFilter}
   * decides what counts as a deprecation, for the filter and this fix alike.
   */
  @Nullable
  private static IntentionAction modernizeFixFor(@NotNull PsiFile file, @NotNull Diagnostic diagnostic,
                                                 @NotNull TextRange range) {
    if (!HaxeDiagnosticMessageFilter.isDeprecationWarning(diagnostic, connectedHaxeVersion(file))) return null;
    return HaxeSyntaxMigrationFixes.modernizeFixAt(file, range);
  }

  @Nullable
  private static InitializeResult.SemVer connectedHaxeVersion(@NotNull PsiFile file) {
    return HaxeCompilerDisplayService.getInstance(file.getProject()).connectedHaxeVersion();
  }

  @NotNull
  private static String messageOf(@NotNull Diagnostic diagnostic) {
    return switch (diagnostic.kind()) {
      case UNRESOLVED_IDENTIFIER -> HaxeBundle.message("haxe.diagnostics.unresolved.identifier");
      case COMPILER_ERROR, PARSER_ERROR, DEPRECATION_WARNING -> orGeneric(diagnostic.messageArg());
      case MISSING_FIELDS -> HaxeBundle.message("haxe.diagnostics.missing.fields");
      default -> HaxeBundle.message("haxe.diagnostics.generic");
    };
  }

  @NotNull
  private static String orGeneric(@NotNull String message) {
    return message.isBlank() ? HaxeBundle.message("haxe.diagnostics.generic") : message;
  }
}
