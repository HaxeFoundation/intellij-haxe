package com.intellij.plugins.haxe.ide.annotator.semantics;

import com.intellij.lang.ASTNode;
import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.Annotator;
import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.openapi.project.DumbAware;
import com.intellij.plugins.haxe.HaxeBundle;
import com.intellij.plugins.haxe.ide.annotator.HaxeStandardAnnotation;
import com.intellij.plugins.haxe.lang.lexer.HaxeTokenTypes;
import com.intellij.plugins.haxe.lang.psi.*;
import com.intellij.plugins.haxe.model.fixer.HaxeFixer;
import com.intellij.plugins.haxe.v2.compiler.HaxeLanguageLevel;
import com.intellij.plugins.haxe.v2.compiler.HaxeLanguageLevelUtil;
import com.intellij.psi.PsiElement;
import com.intellij.psi.impl.source.tree.LeafPsiElement;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Flags language constructs the module's language level does not support:
 * newer syntax below its introduction level, and constructs removed at the
 * level in use. The grammar accepts the superset of all levels, so these
 * are post-parse checks.
 */
public class HaxeLanguageFeatureAnnotator implements Annotator, DumbAware {

  @Override
  public void annotate(@NotNull PsiElement element, @NotNull AnnotationHolder holder) {
    if (AnnotatorUtil.shouldSkip(element)) return;

    if (element instanceof LeafPsiElement leaf) {
      annotateLeaf(leaf, holder);
      return;
    }

    switch (element) {
      case HaxeCoalescingAssignOperator operator ->
        requireLevel(holder, operator, HaxeLanguageLevel.HAXE_4_3, "haxe.feature.null.coalescing");
      // before HaxeLocalVarDeclaration: capture vars are a subtype of it
      case HaxeSwitchCaseCaptureVar captureVar ->
        requireLevel(holder, captureVar, HaxeLanguageLevel.HAXE_4_0, "haxe.feature.case.var.patterns");
      case HaxeLocalVarDeclarationList list ->
        requireTokenLevel(holder, list, HaxeTokenTypes.KSTATIC, HaxeLanguageLevel.HAXE_4_3, "haxe.feature.local.static.variables");
      case HaxeLocalVarDeclaration declaration ->
        requireTokenLevel(holder, declaration, HaxeTokenTypes.KSTATIC, HaxeLanguageLevel.HAXE_4_3, "haxe.feature.local.static.variables");
      case HaxeRestParameter rest ->
        requireLevel(holder, rest, HaxeLanguageLevel.HAXE_4_2, "haxe.feature.rest.arguments");
      case HaxeModuleFieldDeclaration field ->
        requireLevel(holder, nameOrSelf(field), HaxeLanguageLevel.HAXE_4_2, "haxe.feature.module.fields");
      case HaxeModuleMethodDeclaration method ->
        requireLevel(holder, nameOrSelf(method), HaxeLanguageLevel.HAXE_4_2, "haxe.feature.module.fields");
      case HaxeKeyValueIterator iterator ->
        requireLevel(holder, iterator, HaxeLanguageLevel.HAXE_4_0, "haxe.feature.key.value.iterators");
      case HaxeAbstractTypeDeclaration declaration -> checkEnumAbstract(declaration, holder);
      case HaxeFunctionLiteral literal -> checkArrowFunction(literal, holder);
      case HaxePropertyAccessor accessor -> checkPropertyAccessor(accessor, holder);
      case HaxeCatchStatement catchStatement -> checkUntypedCatch(catchStatement, holder);
      case HaxeComponentName componentName -> checkReservedIdentifier(componentName, holder);
      case HaxeReferenceExpression reference -> checkApiMigration(reference, holder);
      default -> { }
    }
  }

  /** Token-level features: operators and keywords whose mere presence needs a level. */
  private static void annotateLeaf(@NotNull LeafPsiElement leaf, @NotNull AnnotationHolder holder) {
    IElementType type = leaf.getElementType();
    if (type == HaxeTokenTypes.OQUEST_DOT) {
      requireLevel(holder, leaf, HaxeLanguageLevel.HAXE_4_3, "haxe.feature.safe.navigation");
    }
    else if (type == HaxeTokenTypes.KFINAL) {
      // every final position (fields, locals, classes, methods) is 4.0+;
      // 3.4 only had the @:final metadata, which is META text, not this token
      requireLevel(holder, leaf, HaxeLanguageLevel.HAXE_4_0, "haxe.feature.final.keyword",
                   HaxeSyntaxMigrationFixes.finalKeywordDowngradeFix(leaf));
    }
    else if (type == HaxeTokenTypes.KEXTERN && isMemberModifierContext(leaf)) {
      // the extern CLASS keyword is old; extern as a FIELD/METHOD modifier is
      // 4.0+ (3.4 spelled it @:extern)
      requireLevel(holder, leaf, HaxeLanguageLevel.HAXE_4_0, "haxe.feature.extern.field.modifier",
                   HaxeSyntaxMigrationFixes.externModifierDowngradeFix(leaf));
    }
    else if (type == HaxeTokenTypes.OBIT_AND && isIntersectionTypeContext(leaf)) {
      requireLevel(holder, leaf, HaxeLanguageLevel.HAXE_4_0, "haxe.feature.intersection.types");
    }
    else if (type == HaxeTokenTypes.LITBIN) {
      checkBinaryLiteral(leaf, holder);
    }
  }

  /** A WARNING, not an error: the plugin's lexer has always accepted 0b literals, but the compiler only gained them in Haxe 5. */
  private static void checkBinaryLiteral(@NotNull LeafPsiElement literal, @NotNull AnnotationHolder holder) {
    if (HaxeLanguageLevelUtil.isAtLeast(literal, HaxeLanguageLevel.HAXE_5_0)) return;
    String feature = HaxeBundle.message("haxe.feature.binary.literals");
    HaxeStandardAnnotation.requiresLanguageLevel(holder, literal, HaxeLanguageLevel.HAXE_5_0, feature, HighlightSeverity.WARNING)
      .create();
  }

  /** The {@code enum} of {@code enum abstract} is 4.0+ (the keywords live under the ABSTRACT_CLASS_TYPE child). */
  private static void checkEnumAbstract(@NotNull HaxeAbstractTypeDeclaration declaration, @NotNull AnnotationHolder holder) {
    HaxeAbstractClassType classType = PsiTreeUtil.getChildOfType(declaration, HaxeAbstractClassType.class);
    ASTNode enumToken = classType == null ? null : classType.getNode().findChildByType(HaxeTokenTypes.KENUM);
    if (enumToken == null) return;
    PsiElement keyword = enumToken.getPsi();
    requireLevel(holder, keyword, HaxeLanguageLevel.HAXE_4_0, "haxe.feature.enum.abstract",
                 HaxeSyntaxMigrationFixes.enumKeywordDowngradeFix(keyword));
  }

  /** The `catch (e)` shorthand (implicit haxe.Exception) is 4.1+. */
  private static void checkUntypedCatch(@NotNull HaxeCatchStatement catchStatement, @NotNull AnnotationHolder holder) {
    HaxeParameter parameter = catchStatement.getParameter();
    if (parameter != null && parameter.getTypeTag() == null) {
      requireLevel(holder, parameter, HaxeLanguageLevel.HAXE_4_1, "haxe.feature.untyped.catch");
    }
  }

  /** `operator` and `overload` became reserved keywords in 4.0. */
  private static void checkReservedIdentifier(@NotNull HaxeComponentName componentName, @NotNull AnnotationHolder holder) {
    String name = componentName.getText();
    if (!"operator".equals(name) && !"overload".equals(name)) return;
    if (!HaxeLanguageLevelUtil.isAtLeast(componentName, HaxeLanguageLevel.HAXE_4_0)) return;
    HaxeStandardAnnotation.removedAtLanguageLevel(holder, componentName, HaxeLanguageLevel.HAXE_4_0,
                                                  HaxeBundle.message("haxe.feature.reserved.identifiers"))
      .create();
  }

  /**
   * Renamed std APIs, both directions of the tables in
   * {@link HaxeSyntaxMigrationFixes} — deliberately text-based: the spellings
   * are stable, and a local named Std shadowing the toplevel class would
   * false-positive either way, which is not worth a resolve on every
   * reference. Old spelling at the replacement's level: deprecated, fix
   * modernizes. New spelling below its arrival level: unavailable, fix
   * downgrades.
   */
  private static void checkApiMigration(@NotNull HaxeReferenceExpression reference, @NotNull AnnotationHolder holder) {
    String text = reference.getText();

    HaxeSyntaxMigrationFixes.ApiMigration deprecated = HaxeSyntaxMigrationFixes.DEPRECATED_APIS.get(text);
    if (deprecated != null && HaxeLanguageLevelUtil.isAtLeast(reference, deprecated.level())) {
      String message = HaxeBundle.message("haxe.semantic.deprecated.since.language.level",
                                          text, deprecated.level().getPresentableText(), deprecated.replacement());
      holder.newAnnotation(HighlightSeverity.WARNING, message)
        .range(reference)
        .withFix(HaxeSyntaxMigrationFixes.replaceReferenceFix(reference, deprecated.replacement()))
        .create();
      return;
    }

    HaxeSyntaxMigrationFixes.ApiMigration modern = HaxeSyntaxMigrationFixes.MODERN_APIS.get(text);
    if (modern != null && !HaxeLanguageLevelUtil.isAtLeast(reference, modern.level())) {
      HaxeLanguageLevel current = HaxeLanguageLevelUtil.getLanguageLevel(reference);
      String message = HaxeBundle.message("haxe.semantic.api.requires.language.level",
                                          text, modern.level().getPresentableText(), current.getPresentableText());
      String setLevelText = HaxeBundle.message("haxe.quickfix.set.language.level", modern.level().getPresentableText());
      holder.newAnnotation(HighlightSeverity.WARNING, message)
        .range(reference)
        .withFix(HaxeSyntaxMigrationFixes.replaceReferenceFix(reference, modern.replacement()))
        .withFix(HaxeFixer.create(setLevelText, () -> HaxeLanguageLevelUtil.setLanguageLevel(reference, modern.level())))
        .create();
    }
  }

  /** An {@code &} between types (constraint or structure intersection), as opposed to the bitwise operator. */
  private static boolean isIntersectionTypeContext(@NotNull PsiElement amp) {
    PsiElement parent = amp.getParent();
    return parent instanceof HaxeConstraintTypeList
           || parent instanceof HaxeAnonymousType
           || parent instanceof HaxeTypeOrAnonymous
           || parent instanceof HaxeTypeListPart;
  }

  /** Arrow form only - a keyword `function` literal is fine at every level. */
  private static void checkArrowFunction(@NotNull HaxeFunctionLiteral literal, @NotNull AnnotationHolder holder) {
    ASTNode node = literal.getNode();
    if (node.findChildByType(HaxeTokenTypes.KFUNCTION) != null) return;
    ASTNode arrow = node.findChildByType(HaxeTokenTypes.OARROW);
    if (arrow == null) return;
    requireLevel(holder, arrow.getPsi(), HaxeLanguageLevel.HAXE_4_0, "haxe.feature.arrow.functions");
  }

  /**
   * Accessors are keywords (get/set/null/default/never/dynamic) at 4.0+; an
   * accessor parsed as a reference expression is the removed pre-4.0
   * accessor-METHOD-name form. A `private` accessor modifier is 5.0+.
   */
  private static void checkPropertyAccessor(@NotNull HaxePropertyAccessor accessor, @NotNull AnnotationHolder holder) {
    requireTokenLevel(holder, accessor, HaxeTokenTypes.KPRIVATE, HaxeLanguageLevel.HAXE_5_0, "haxe.feature.private.accessors");

    HaxeReferenceExpression customName = accessor.getReferenceExpression();
    if (customName != null && HaxeLanguageLevelUtil.isAtLeast(accessor, HaxeLanguageLevel.HAXE_4_0)) {
      HaxeStandardAnnotation.removedAtLanguageLevel(holder, customName, HaxeLanguageLevel.HAXE_4_0,
                                                    HaxeBundle.message("haxe.feature.custom.accessor.names"))
        .create();
    }
  }

  private static void requireLevel(@NotNull AnnotationHolder holder,
                                   @NotNull PsiElement element,
                                   @NotNull HaxeLanguageLevel required,
                                   @NotNull String featureKey) {
    requireLevel(holder, element, required, featureKey, null);
  }

  /** {@code downgradeFix} rewrites the construct into its pre-{@code required} form. */
  private static void requireLevel(@NotNull AnnotationHolder holder,
                                   @NotNull PsiElement element,
                                   @NotNull HaxeLanguageLevel required,
                                   @NotNull String featureKey,
                                   @Nullable HaxeFixer downgradeFix) {
    if (HaxeLanguageLevelUtil.isAtLeast(element, required)) return;
    var annotation = HaxeStandardAnnotation.requiresLanguageLevel(holder, element, required, HaxeBundle.message(featureKey));
    if (downgradeFix != null) {
      annotation = annotation.withFix(downgradeFix);
    }
    annotation.create();
  }

  /** An {@code extern} token used as a field/method modifier, as opposed to the extern CLASS keyword. */
  private static boolean isMemberModifierContext(@NotNull PsiElement externToken) {
    PsiElement parent = externToken.getParent();
    return parent instanceof HaxeMethodModifier || parent instanceof HaxeFieldModifier;
  }

  /** Gates on a DIRECT child token, annotating just that token when present. */
  private static void requireTokenLevel(@NotNull AnnotationHolder holder,
                                        @NotNull PsiElement element,
                                        @NotNull IElementType tokenType,
                                        @NotNull HaxeLanguageLevel required,
                                        @NotNull String featureKey) {
    ASTNode token = element.getNode().findChildByType(tokenType);
    if (token == null) return;
    requireLevel(holder, token.getPsi(), required, featureKey);
  }

  @NotNull
  private static PsiElement nameOrSelf(@NotNull PsiElement declaration) {
    HaxeComponentName name = PsiTreeUtil.getChildOfType(declaration, HaxeComponentName.class);
    return name != null ? name : declaration;
  }
}
