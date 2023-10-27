package com.intellij.plugins.haxe.ide.hint.types;

import com.intellij.codeInsight.hints.declarative.*;
import com.intellij.openapi.editor.Editor;
import com.intellij.plugins.haxe.lang.psi.*;
import com.intellij.plugins.haxe.model.type.*;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import kotlin.Unit;
import kotlin.jvm.functions.Function1;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class HaxeInlayForLoopHintsProvider implements InlayHintsProvider {

  @Nullable
  @Override
  public InlayHintsCollector createCollector(@NotNull PsiFile file, @NotNull Editor editor) {
    return new TypeCollector();
  }

  private static class TypeCollector implements SharedBypassCollector {

    @Override
    public void collectFromElement(@NotNull PsiElement element, @NotNull InlayTreeSink sink) {
      if (element instanceof HaxeForStatement forStatement) {
        handleForEachHints(element, sink, forStatement);
      }
    }


    private static void handleForEachHints(@NotNull PsiElement element,
                                                    @NotNull InlayTreeSink sink,
                                           HaxeForStatement forStatement) {



      HaxeIterable iterable = forStatement.getIterable();
      HaxeKeyValueIterator keyValueIterator = forStatement.getKeyValueIterator();
      HaxeGenericResolver resolver = HaxeGenericResolverUtil.generateResolverFromScopeParents(forStatement);
      if (iterable != null && keyValueIterator == null) {
        HaxeComponentName componentName = forStatement.getComponentName();
        ResultHolder type = extractIteratorElementType(HaxeTypeResolver.getPsiElementType(iterable, element, resolver));
        createInlayHint(componentName, sink, type);
      }
      if (keyValueIterator != null) {
        HaxeIteratorkey iteratorKey = keyValueIterator.getIteratorkey();
        HaxeIteratorValue iteratorValue = keyValueIterator.getIteratorValue();

        ResultHolder keyValueIteratorType = HaxeTypeResolver.getPsiElementType(iterable, element, resolver);
        ResultHolder keyType  = extractKeyValueType(keyValueIteratorType, true);
        ResultHolder valueType  = extractKeyValueType(keyValueIteratorType, false);

        createInlayHint(iteratorKey.getComponentName(), sink, keyType);
        createInlayHint(iteratorValue.getComponentName(), sink, valueType);
      }

    }

    private static ResultHolder extractKeyValueType(ResultHolder type, boolean key) {
      if (!type.isUnknown() && type.getClassType() != null) {
        @NotNull ResultHolder[] specifics = type.getClassType().getSpecifics();
        if (specifics.length == 2) {
          return specifics[key ? 0 : 1];
        }
      }
      PsiElement element = type.getOrigin();
      if(element == null) element = type.getElementContext(); // TODO mlo check why type origin is null (probably string missing iterator or smoething?)
      return SpecificHaxeClassReference.getUnknown(element).createHolder();
    }

    private static ResultHolder extractIteratorElementType(ResultHolder type) {
      if (!type.isUnknown() && type.getClassType() != null) {
        @NotNull ResultHolder[] specifics = type.getClassType().getSpecifics();
        if (specifics.length == 1) {
          return specifics[0];
        }
      }
      return type;
    }

    private static void createInlayHint(@NotNull HaxeComponentName componentName,@NotNull InlayTreeSink sink, ResultHolder type ) {
      if (!type.isUnknown() && !type.getType().isInvalid()) {
        int offset = componentName.getTextRange().getEndOffset();
        InlineInlayPosition position = new InlineInlayPosition(offset, false, 0);
        sink.addPresentation(position, null, null, false, appendTypeTextToBuilder(type)
        );
      }
    }

    @NotNull
    private static Function1<PresentationTreeBuilder, Unit> appendTypeTextToBuilder(ResultHolder type) {
      return builder -> {
        builder.text( ":"+type.toPresentationString(), null);
        return null;
      };
    }
  }
}
