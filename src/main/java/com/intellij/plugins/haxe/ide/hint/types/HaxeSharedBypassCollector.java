package com.intellij.plugins.haxe.ide.hint.types;

import com.intellij.codeInsight.hints.declarative.*;
import com.intellij.plugins.haxe.model.type.*;
import com.intellij.psi.PsiElement;
import com.intellij.psi.SmartPointerManager;
import com.intellij.psi.SmartPsiElementPointer;
import kotlin.Unit;
import kotlin.jvm.functions.Function1;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;

/** The IDE's type-hint collectors: a resolved type becomes a hint at the declaration's shared anchor, its type names clickable. */
public abstract class HaxeSharedBypassCollector implements SharedBypassCollector {

  /** Adds the type's hint for the declaration; nothing for an unknown or invalid type or an unanchored declaration. */
  protected static void addTypeHint(@NotNull PsiElement declaration, @NotNull InlayTreeSink sink, @NotNull ResultHolder type) {
    int offset = HaxeTypeHintAnchors.anchorOf(declaration);
    if (offset < 0 || type.isUnknown() || type.getType().isInvalid()) return;
    HaxeTypeHintAnchors.addTypeHint(sink, offset, typeTexts(type));
  }

  /** The type's hint text: class names clickable, type parameters and function signatures spelled out. */
  @NotNull
  protected static Function1<PresentationTreeBuilder, Unit> typeTexts(@NotNull ResultHolder type) {
    return builder -> {
      buildTypeTexts(type, builder);
      return Unit.INSTANCE;
    };
  }

  @Nullable
  private static InlayActionData navigationTo(@NotNull ResultHolder type) {
    if (type.isUnknown()) return null;
    PsiElement element = type.getType().getTypePsi();
    if (element == null) return null;
    SmartPsiElementPointer<PsiElement> pointer = SmartPointerManager.getInstance(element.getProject()).createSmartPsiElementPointer(element);
    return new InlayActionData(new PsiPointerInlayActionPayload(pointer), PsiPointerInlayActionNavigationHandler.HANDLER_ID);
  }

  private static void buildTypeTexts(@NotNull ResultHolder type, @NotNull PresentationTreeBuilder builder) {
    switch (type.getType()) {
      case SpecificHaxeClassReference classReference -> buildClassTexts(type, classReference, builder);
      case SpecificEnumValueReference enumValueReference -> buildTypeTexts(enumValueReference.getEnumClass().createHolder(), builder);
      case SpecificFunctionReference functionReference -> buildFunctionTexts(functionReference, builder);
      default -> builder.text(type.toPresentationString(), navigationTo(type));
    }
  }

  private static void buildClassTexts(@NotNull ResultHolder type, @NotNull SpecificHaxeClassReference classReference, @NotNull PresentationTreeBuilder builder) {
    String className = classReference.getHaxeClassReference().getName();
    if (className != null) {
      builder.text(className, navigationTo(type));
    }
    // a Dynamic with unknown or unresolved type parameters shows without them
    if (type.isDynamic() && type.containsUnknownOrUnresolvedTypeParameters()) return;
    ResultHolder[] specifics = classReference.getSpecifics();
    if (specifics.length == 0) return;
    builder.text("<", null);
    buildSeparated(Arrays.asList(specifics), builder);
    builder.text(">", null);
  }

  private static void buildFunctionTexts(@NotNull SpecificFunctionReference functionReference, @NotNull PresentationTreeBuilder builder) {
    List<ResultHolder> argumentTypes = functionReference.getArguments().stream().map(HaxeArgument::getType).toList();
    builder.text("(", null);
    buildSeparated(argumentTypes, builder);
    builder.text(")", null);
    builder.text("->", null);
    buildTypeTexts(functionReference.getReturnType(), builder);
  }

  private static void buildSeparated(@NotNull List<ResultHolder> types, @NotNull PresentationTreeBuilder builder) {
    for (int i = 0; i < types.size(); i++) {
      if (i > 0) builder.text(", ", null);
      buildTypeTexts(types.get(i), builder);
    }
  }
}
