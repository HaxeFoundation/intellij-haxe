package com.intellij.plugins.haxe.ide.hint.types;

import com.intellij.codeInsight.hints.declarative.*;
import com.intellij.openapi.editor.Editor;
import com.intellij.plugins.haxe.lang.psi.*;
import com.intellij.plugins.haxe.model.evaluator.HaxeExpressionEvaluator;
import com.intellij.plugins.haxe.model.type.*;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class HaxeInlayForLoopHintsProvider implements InlayHintsProvider {

  @Nullable
  @Override
  public InlayHintsCollector createCollector(@NotNull PsiFile file, @NotNull Editor editor) {
    return new TypeCollector();
  }

  private static class TypeCollector extends HaxeSharedBypassCollector {

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
        HaxeValueIterator valueIterator = forStatement.getValueIterator();
        if (valueIterator != null) {
          ResultHolder type = HaxeTypeResolver.getPsiElementType(valueIterator, element, resolver);
          createInlayHint(valueIterator.getComponentName(), sink, type);
        }
      }
      if (keyValueIterator != null) {
        HaxeIteratorkey iteratorKey = keyValueIterator.getIteratorkey();
        HaxeIteratorValue iteratorValue = keyValueIterator.getIteratorValue();

        ResultHolder keyType = HaxeExpressionEvaluator.findIteratorType(iteratorKey);
        ResultHolder valueType = HaxeExpressionEvaluator.findIteratorType(iteratorValue);

        if (!keyType.isUnknown()) createInlayHint(iteratorKey.getComponentName(), sink, keyType);
        if (!valueType.isUnknown()) createInlayHint(iteratorValue.getComponentName(), sink, valueType);
      }

    }



    private static void createInlayHint(@NotNull HaxeComponentName componentName,@NotNull InlayTreeSink sink, ResultHolder type ) {
      if (!type.isUnknown() && !type.getType().isInvalid()) {
        int offset = componentName.getTextRange().getEndOffset();
        InlineInlayPosition position = new InlineInlayPosition(offset, true, 0);
        sink.addPresentation(position, null, null, HintFormat.Companion.getDefault(), appendTypeTextToBuilder(type)
        );
      }
    }
  }
}
