package com.intellij.plugins.haxe.ide.annotator.semantics;

import com.intellij.codeInsight.intention.IntentionAction;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.util.TextRange;
import com.intellij.plugins.haxe.HaxeBundle;
import com.intellij.plugins.haxe.lang.lexer.HaxeTokenTypes;
import com.intellij.plugins.haxe.lang.psi.*;
import com.intellij.plugins.haxe.metadata.HaxeMetadataList;
import com.intellij.plugins.haxe.metadata.psi.HaxeMeta;
import com.intellij.plugins.haxe.metadata.util.HaxeMetadataUtils;
import com.intellij.plugins.haxe.model.HaxeDocumentModel;
import com.intellij.plugins.haxe.model.fixer.HaxeFixer;
import com.intellij.plugins.haxe.v2.compiler.HaxeLanguageLevel;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Quick fixes converting between pre-4.0 syntax and its replacement, in both
 * directions: metadata forms to keywords ({@code @:enum abstract} to
 * {@code enum abstract}, {@code @:final} to {@code final}, {@code @:extern}
 * to {@code extern}) and the renamed std APIs ({@code Std.is} to
 * {@code Std.isOfType} and friends). Shared by the static language-level
 * annotators and the compiler-diagnostics annotator.
 *
 * All fixes are document-text surgery: the edits are computed up front from
 * the current PSI ranges and applied back-to-front, so an earlier edit can
 * never shift a later range. Metadata is always inserted at the DECLARATION
 * start — the compiler only accepts metadata before all modifiers.
 */
public final class HaxeSyntaxMigrationFixes {

  /** The replacement's arrival {@code level}; below it the old spelling is the only one. */
  public record ApiMigration(HaxeLanguageLevel level, String replacement) {
  }

  /// Old API spelling -> its replacement (deprecated once the replacement exists).
  /// Deliberately text-based, like the annotator consuming it: the spellings
  /// are stable and a shadowing local would false-positive either way.
  public static final Map<String, ApiMigration> DEPRECATED_APIS = Map.of(
    "Std.is", new ApiMigration(HaxeLanguageLevel.HAXE_4_1, "Std.isOfType"),
    "Std.instance", new ApiMigration(HaxeLanguageLevel.HAXE_4_0, "Std.downcast"),
    "__js__", new ApiMigration(HaxeLanguageLevel.HAXE_4_0, "js.Syntax.code"),
    "__php__", new ApiMigration(HaxeLanguageLevel.HAXE_4_0, "php.Syntax.code"));

  /// Modern API spelling -> the pre-replacement form usable below its arrival
  /// level. The Syntax.code downgrades prepend `untyped`: the magic
  /// identifiers only exist in untyped context (a redundant repeated untyped
  /// stays valid).
  public static final Map<String, ApiMigration> MODERN_APIS = Map.of(
    "Std.isOfType", new ApiMigration(HaxeLanguageLevel.HAXE_4_1, "Std.is"),
    "Std.downcast", new ApiMigration(HaxeLanguageLevel.HAXE_4_0, "Std.instance"),
    "js.Syntax.code", new ApiMigration(HaxeLanguageLevel.HAXE_4_0, "untyped __js__"),
    "php.Syntax.code", new ApiMigration(HaxeLanguageLevel.HAXE_4_0, "untyped __php__"));

  private record Edit(@NotNull TextRange range, @NotNull String replacement) {
  }

  private HaxeSyntaxMigrationFixes() {
  }

  // ---- forward: metadata form -> keyword form ----

  /**
   * Converts a {@code @:enum}/{@code @:final}/{@code @:extern} metadata to
   * its keyword form; null when the meta is none of those or sits on a
   * construct the keyword cannot express.
   */
  @Nullable
  public static HaxeFixer modernizeMetaFix(@NotNull HaxeMeta meta) {
    PsiElement associated = HaxeMetadataUtils.getAssociatedElement(meta);
    if (associated == null) return null;

    if (meta.isType(HaxeMeta.ENUM)) {
      // `enum` must sit directly before `abstract`, not at the declaration start
      PsiElement abstractToken = abstractKeywordOf(associated);
      if (abstractToken == null) return null;
      return metaToKeywordFix(meta, "enum abstract", insertion(abstractToken.getTextRange().getStartOffset(), "enum "));
    }
    if (meta.isType(HaxeMeta.FINAL)) {
      // on a field the keyword REPLACES `var`; on classes/methods it is a new modifier
      PsiElement varToken = mutabilityVarTokenOf(associated);
      Edit keywordEdit = varToken != null
                         ? new Edit(varToken.getTextRange(), "final")
                         : insertion(associated.getTextRange().getStartOffset(), "final ");
      return metaToKeywordFix(meta, "final", keywordEdit);
    }
    if (meta.isType(HaxeMeta.EXTERN)) {
      return metaToKeywordFix(meta, "extern", insertion(associated.getTextRange().getStartOffset(), "extern "));
    }
    return null;
  }

  // ---- backward: keyword form -> metadata form (for pre-4.0 levels) ----

  /** {@code enum abstract} -> {@code @:enum abstract}; token is the KENUM leaf. */
  @Nullable
  public static HaxeFixer enumKeywordDowngradeFix(@NotNull PsiElement enumToken) {
    HaxeAbstractTypeDeclaration declaration = PsiTreeUtil.getParentOfType(enumToken, HaxeAbstractTypeDeclaration.class);
    return declaration == null ? null : keywordToMetaFix(enumToken, "@:enum", declaration);
  }

  /**
   * Downgrades a {@code final} keyword: fields and locals become {@code var}
   * (3.4 has no immutable bindings), classes and methods get {@code @:final}.
   */
  @Nullable
  public static HaxeFixer finalKeywordDowngradeFix(@NotNull PsiElement finalToken) {
    if (finalToken.getParent() instanceof HaxeMutabilityModifier) {
      String fixText = HaxeBundle.message("haxe.quickfix.replace.with", "var");
      TextRange tokenRange = finalToken.getTextRange();
      return HaxeFixer.create(fixText, () -> apply(finalToken, List.of(new Edit(tokenRange, "var"))));
    }
    PsiElement declaration = modifierDeclarationOf(finalToken);
    return declaration == null ? null : keywordToMetaFix(finalToken, "@:final", declaration);
  }

  /** {@code extern} member modifier -> {@code @:extern}; token is the KEXTERN leaf. */
  @Nullable
  public static HaxeFixer externModifierDowngradeFix(@NotNull PsiElement externToken) {
    PsiElement declaration = modifierDeclarationOf(externToken);
    return declaration == null ? null : keywordToMetaFix(externToken, "@:extern", declaration);
  }

  // ---- API references ----

  @NotNull
  public static HaxeFixer replaceReferenceFix(@NotNull HaxeReferenceExpression reference, @NotNull String replacement) {
    String fixText = HaxeBundle.message("haxe.quickfix.replace.with", replacement);
    return HaxeFixer.create(fixText, () -> apply(reference, List.of(new Edit(reference.getTextRange(), replacement))));
  }

  // ---- entry point for compiler diagnostics ----

  /**
   * The modernize fix for whatever deprecated construct the diagnostic range
   * points at — a metadata form, the declaration carrying one, or a renamed
   * std API reference; null when nothing recognizable is there.
   */
  @Nullable
  public static IntentionAction modernizeFixAt(@NotNull PsiFile file, @NotNull TextRange range) {
    PsiElement at = file.findElementAt(range.getStartOffset());
    if (at == null) return null;

    HaxeMeta meta = PsiTreeUtil.getParentOfType(at, HaxeMeta.class, false);
    if (meta != null) return modernizeMetaFix(meta);

    // the compiler may anchor the deprecation on the declaration, not the meta
    HaxeAbstractTypeDeclaration abstractDeclaration =
      PsiTreeUtil.getParentOfType(at, HaxeAbstractTypeDeclaration.class, false);
    if (abstractDeclaration != null) {
      HaxeMetadataList enumMetas = HaxeMetadataUtils.getMetadataList(abstractDeclaration, HaxeMeta.COMPILE_TIME, HaxeMeta.ENUM);
      if (!enumMetas.isEmpty()) return modernizeMetaFix(enumMetas.getFirst());
    }

    HaxeReferenceExpression reference = outermostReferenceOf(at);
    if (reference != null) {
      ApiMigration api = DEPRECATED_APIS.get(reference.getText());
      if (api != null) return replaceReferenceFix(reference, api.replacement());
    }
    return null;
  }

  // ---- mechanics ----

  @NotNull
  private static HaxeFixer metaToKeywordFix(@NotNull HaxeMeta meta, @NotNull String keywordForm, @NotNull Edit keywordEdit) {
    String fixText = HaxeBundle.message("haxe.quickfix.replace.with", keywordForm);
    return HaxeFixer.create(fixText, () -> {
      TextRange metaRange = withTrailingSpace(meta.getContainer());
      apply(meta, List.of(new Edit(metaRange, ""), keywordEdit));
    });
  }

  @NotNull
  private static HaxeFixer keywordToMetaFix(@NotNull PsiElement keywordToken, @NotNull String metaText,
                                            @NotNull PsiElement declaration) {
    String fixText = HaxeBundle.message("haxe.quickfix.replace.with", metaText);
    return HaxeFixer.create(fixText, () -> {
      Edit removeKeyword = new Edit(withTrailingSpace(keywordToken), "");
      Edit insertMeta = insertion(declaration.getTextRange().getStartOffset(), metaText + " ");
      apply(keywordToken, List.of(removeKeyword, insertMeta));
    });
  }

  /** The declaration a member/class modifier token belongs to — where inserted metadata must go. */
  @Nullable
  private static PsiElement modifierDeclarationOf(@NotNull PsiElement modifierToken) {
    return PsiTreeUtil.getParentOfType(modifierToken,
                                       HaxeMethodDeclaration.class, HaxeFieldDeclaration.class,
                                       HaxeClassDeclaration.class, HaxeAbstractTypeDeclaration.class,
                                       HaxeModuleMethodDeclaration.class, HaxeModuleFieldDeclaration.class,
                                       HaxeExternClassDeclaration.class);
  }

  /** The {@code abstract} keyword token of an abstract declaration, or null. */
  @Nullable
  private static PsiElement abstractKeywordOf(@NotNull PsiElement associated) {
    HaxeAbstractClassType classType = PsiTreeUtil.findChildOfType(associated, HaxeAbstractClassType.class);
    if (classType == null) return null;
    var token = classType.getNode().findChildByType(HaxeTokenTypes.KABSTRACT);
    return token == null ? null : token.getPsi();
  }

  /** The {@code var} token of the associated field's mutability modifier, or null when not a var field. */
  @Nullable
  private static PsiElement mutabilityVarTokenOf(@NotNull PsiElement associated) {
    HaxeMutabilityModifier mutability = PsiTreeUtil.findChildOfType(associated, HaxeMutabilityModifier.class);
    if (mutability == null) return null;
    var token = mutability.getNode().findChildByType(HaxeTokenTypes.KVAR);
    return token == null ? null : token.getPsi();
  }

  /** The widest reference chain containing {@code element} ({@code Std.is}, not {@code Std}). */
  @Nullable
  private static HaxeReferenceExpression outermostReferenceOf(@NotNull PsiElement element) {
    HaxeReferenceExpression reference = PsiTreeUtil.getParentOfType(element, HaxeReferenceExpression.class, false);
    while (reference != null && reference.getParent() instanceof HaxeReferenceExpression parent) {
      reference = parent;
    }
    return reference;
  }

  @NotNull
  private static Edit insertion(int offset, @NotNull String text) {
    return new Edit(TextRange.from(offset, 0), text);
  }

  @NotNull
  private static TextRange withTrailingSpace(@NotNull PsiElement element) {
    TextRange range = element.getTextRange();
    CharSequence text = element.getContainingFile().getViewProvider().getContents();
    int end = range.getEndOffset();
    while (end < text.length() && (text.charAt(end) == ' ' || text.charAt(end) == '\t')) {
      end++;
    }
    return new TextRange(range.getStartOffset(), end);
  }

  private static void apply(@NotNull PsiElement context, @NotNull List<Edit> edits) {
    Document document = HaxeDocumentModel.fromElement(context).getDocument();
    List<Edit> backToFront = new ArrayList<>(edits);
    backToFront.sort(Comparator.comparingInt((Edit edit) -> edit.range().getStartOffset()).reversed());
    for (Edit edit : backToFront) {
      document.replaceString(edit.range().getStartOffset(), edit.range().getEndOffset(), edit.replacement());
    }
  }
}
