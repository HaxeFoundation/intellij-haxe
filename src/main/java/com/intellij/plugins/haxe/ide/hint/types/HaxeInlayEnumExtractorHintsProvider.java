package com.intellij.plugins.haxe.ide.hint.types;

import com.intellij.codeInsight.hints.declarative.*;
import com.intellij.openapi.editor.Editor;
import com.intellij.plugins.haxe.lang.psi.HaxeEnumExtractedValueReference;
import com.intellij.plugins.haxe.model.evaluator.HaxeExpressionEvaluator;
import com.intellij.plugins.haxe.model.type.*;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class HaxeInlayEnumExtractorHintsProvider implements InlayHintsProvider {

  @Nullable
  @Override
  public InlayHintsCollector createCollector(@NotNull PsiFile file, @NotNull Editor editor) {
    return new TypeCollector();
  }

  private static class TypeCollector extends HaxeSharedBypassCollector {

    @Override
    public void collectFromElement(@NotNull PsiElement element, @NotNull InlayTreeSink sink) {
      if (element instanceof HaxeEnumExtractedValueReference extractedValue) {
        handleEnumArgumentExtractorHints(sink, extractedValue);
      }
    }

    private static void handleEnumArgumentExtractorHints(@NotNull InlayTreeSink sink, HaxeEnumExtractedValueReference extractedValue) {
      InlineInlayPosition position = new InlineInlayPosition(extractedValue.getTextRange().getEndOffset(), true, 0);
      ResultHolder type = HaxeExpressionEvaluator.evaluate(extractedValue, null).result;
      sink.addPresentation(position, null, null, HintFormat.Companion.getDefault(), appendTypeTextToBuilder(type));
    }
  }
}
