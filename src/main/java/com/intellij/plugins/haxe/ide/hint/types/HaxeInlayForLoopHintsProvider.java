package com.intellij.plugins.haxe.ide.hint.types;

import com.intellij.codeInsight.hints.declarative.*;
import com.intellij.openapi.editor.Editor;
import com.intellij.plugins.haxe.lang.psi.*;
import com.intellij.plugins.haxe.model.evaluator.HaxeExpressionEvaluator;
import com.intellij.plugins.haxe.model.type.*;
import com.intellij.psi.PsiElement;
import com.intellij.plugins.haxe.v2.display.HaxeCompilerInlayService;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class HaxeInlayForLoopHintsProvider implements InlayHintsProvider {

  @Nullable
  @Override
  public InlayHintsCollector createCollector(@NotNull PsiFile file, @NotNull Editor editor) {
    if (HaxeCompilerInlayService.isActive(file.getProject())) return null;
    return new TypeCollector();
  }

  private static class TypeCollector extends HaxeSharedBypassCollector {

    @Override
    public void collectFromElement(@NotNull PsiElement element, @NotNull InlayTreeSink sink) {
      if (element instanceof HaxeForStatement forStatement) {
        handleForEachHints(element, sink, forStatement);
      }
    }


    private static void handleForEachHints(@NotNull PsiElement element, @NotNull InlayTreeSink sink, HaxeForStatement forStatement) {
      HaxeIterable iterable = forStatement.getIterable();
      HaxeKeyValueIterator keyValueIterator = forStatement.getKeyValueIterator();
      HaxeGenericResolver resolver = HaxeGenericResolverUtil.generateResolverFromScopeParents(forStatement);
      if (iterable != null && keyValueIterator == null) {
        HaxeValueIterator valueIterator = forStatement.getValueIterator();
        if (valueIterator != null) {
          ResultHolder type = HaxeTypeResolver.getPsiElementType(valueIterator, element, resolver);
          addTypeHint(valueIterator, sink, type);
        }
      }
      if (keyValueIterator != null) {
        HaxeIteratorkey iteratorKey = keyValueIterator.getIteratorkey();
        HaxeIteratorValue iteratorValue = keyValueIterator.getIteratorValue();

        ResultHolder keyType = HaxeExpressionEvaluator.findIteratorType(iteratorKey);
        ResultHolder valueType = HaxeExpressionEvaluator.findIteratorType(iteratorValue);

        addTypeHint(iteratorKey, sink, keyType);
        addTypeHint(iteratorValue, sink, valueType);
      }
    }
  }
}
