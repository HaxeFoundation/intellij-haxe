package com.intellij.plugins.haxe.ide.hint.types;

import com.intellij.codeInsight.hints.declarative.HintFormat;
import com.intellij.codeInsight.hints.declarative.InlayTreeSink;
import com.intellij.codeInsight.hints.declarative.InlineInlayPosition;
import com.intellij.codeInsight.hints.declarative.PresentationTreeBuilder;
import com.intellij.plugins.haxe.lang.psi.*;
import com.intellij.plugins.haxe.util.UsefulPsiTreeUtil;
import com.intellij.psi.PsiElement;
import kotlin.Unit;
import kotlin.jvm.functions.Function1;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Where a type hint goes and how it reads, for every declaration kind the
 * type hints cover. The IDE's providers and the compiler-backed provider
 * place their hints through the same anchors and the same ": Type" shape,
 * so switching the source in the Haxe Compiler settings moves no hint.
 */
public final class HaxeTypeHintAnchors {

  private HaxeTypeHintAnchors() {
  }

  /** The name element of a covered declaration kind; null for any other element. */
  @Nullable
  public static HaxeComponentName componentNameOf(@NotNull PsiElement declaration) {
    return switch (declaration) {
      case HaxeParameter parameter -> parameter.getComponentName();
      case HaxeLocalVarDeclaration local -> local.getComponentName();
      case HaxeSwitchCaseCapture capture -> capture.getComponentName();
      case HaxeFieldDeclaration field -> field.getComponentName();
      case HaxeEnumExtractedValueReference extracted -> extracted.getComponentName();
      case HaxeMethodDeclaration method -> method.getComponentName();
      case HaxeLocalFunctionDeclaration function -> function.getComponentName();
      case HaxeValueIterator iterator -> iterator.getComponentName();
      case HaxeIteratorkey key -> key.getComponentName();
      case HaxeIteratorValue value -> value.getComponentName();
      default -> null;
    };
  }

  /**
   * The offset a hint for the declaration follows: a property's accessors, a
   * function's parameter list, otherwise the name; -1 for an element the hints
   * do not cover or a function missing its closing parenthesis.
   */
  public static int anchorOf(@NotNull PsiElement declaration) {
    return switch (declaration) {
      case HaxeFieldDeclaration field when field.getPropertyDeclaration() != null -> field.getPropertyDeclaration().getTextRange().getEndOffset();
      case HaxeMethodDeclaration method -> afterParameterList(method.getParameterList());
      case HaxeLocalFunctionDeclaration function -> afterParameterList(function.getParameterList());
      case HaxeEnumExtractedValueReference extracted -> extracted.getTextRange().getEndOffset();
      default -> afterName(declaration);
    };
  }

  /** Adds a ": Type" hint at the offset; typeText writes the type's parts, clickable ones with their navigation. */
  public static void addTypeHint(@NotNull InlayTreeSink sink, int offset, @NotNull Function1<PresentationTreeBuilder, Unit> typeText) {
    InlineInlayPosition position = new InlineInlayPosition(offset, true, 0);
    sink.addPresentation(position, null, null, HintFormat.Companion.getDefault(), builder -> {
      builder.text(":", null);
      typeText.invoke(builder);
      return Unit.INSTANCE;
    });
  }

  private static int afterName(@NotNull PsiElement declaration) {
    HaxeComponentName name = componentNameOf(declaration);
    return name != null ? name.getTextRange().getEndOffset() : -1;
  }

  private static int afterParameterList(@Nullable PsiElement parameterList) {
    PsiElement closing = parameterList == null ? null : UsefulPsiTreeUtil.getNextSiblingSkipWhiteSpacesAndComments(parameterList);
    return closing != null ? closing.getTextRange().getEndOffset() : -1;
  }
}
