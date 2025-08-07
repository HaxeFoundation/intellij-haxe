package com.intellij.plugins.haxe.ide.hint.types;

import com.intellij.codeInsight.hints.declarative.*;
import com.intellij.openapi.editor.Editor;
import com.intellij.plugins.haxe.lang.psi.HaxeFunctionLiteral;
import com.intellij.plugins.haxe.lang.psi.HaxeParameter;
import com.intellij.plugins.haxe.model.evaluator.HaxeExpressionEvaluator;
import com.intellij.plugins.haxe.model.evaluator.HaxeExpressionEvaluatorContext;
import com.intellij.plugins.haxe.model.type.ResultHolder;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;


public class HaxeInlayUntypedParameterHintsProvider implements InlayHintsProvider {

  @Nullable
  @Override
  public InlayHintsCollector createCollector(@NotNull PsiFile file, @NotNull Editor editor) {
    return new TypeCollector();
  }

  private static class TypeCollector extends HaxeSharedBypassCollector {

    @Override
    public void collectFromElement(@NotNull PsiElement element, @NotNull InlayTreeSink sink) {
        if (element instanceof HaxeParameter parameter && parameter.getParent().getParent() instanceof HaxeFunctionLiteral) {
          if (parameter.getTypeTag() == null && parameter.getVarInit() == null) {
            handleUntypedParameterHints(parameter, sink);
          }
      }
    }


    private static void handleUntypedParameterHints(@NotNull HaxeParameter parameter,
                                                         @NotNull InlayTreeSink sink) {
      if (parameter.getTypeTag() == null && parameter.getVarInit() == null) {
        ResultHolder result = HaxeExpressionEvaluator.evaluate(parameter, new HaxeExpressionEvaluatorContext(parameter), null).result;

        if (!result.isUnknown() && !result.getType().isInvalid()) {
          int offset = parameter.getComponentName().getTextRange().getEndOffset();
          InlineInlayPosition position = new InlineInlayPosition(offset, true, 0);
          sink.addPresentation(position, null, null, HintFormat.Companion.getDefault(), appendTypeTextToBuilder(result)
          );
        }
      }
    }
  }
}
