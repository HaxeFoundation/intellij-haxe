package com.intellij.plugins.haxe.ide.hint.types;

import com.intellij.codeInsight.hints.declarative.*;
import com.intellij.openapi.editor.Editor;
import com.intellij.plugins.haxe.lang.psi.HaxeReference;
import com.intellij.plugins.haxe.lang.psi.HaxeResolver;
import com.intellij.plugins.haxe.lang.psi.HaxeSwitchCaseCaptureVar;
import com.intellij.plugins.haxe.lang.psi.HaxeSwitchCaseExpr;
import com.intellij.plugins.haxe.model.evaluator.HaxeExpressionEvaluator;
import com.intellij.plugins.haxe.model.evaluator.HaxeExpressionEvaluatorContext;
import com.intellij.plugins.haxe.model.type.*;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;


public class HaxeInlayCaptureVariableHintsProvider implements InlayHintsProvider {

  @Nullable
  @Override
  public InlayHintsCollector createCollector(@NotNull PsiFile file, @NotNull Editor editor) {
    return new TypeCollector();
  }

  private static class TypeCollector extends HaxeSharedBypassCollector {

    @Override
    public void collectFromElement(@NotNull PsiElement element, @NotNull InlayTreeSink sink) {
      if (element instanceof HaxeSwitchCaseCaptureVar varDeclaration) {
        handleCaptureVarDeclarationHints(element, sink, varDeclaration);
      }
      //TODO mlo:  useful for debugging, not sure if its useful for end user
//      if(element instanceof HaxeSwitchCaseExpr switchCaseExpr) {
//        handleCaptureVarDeclarationHints(element, sink, switchCaseExpr);
//      }
    }


    private static void handleCaptureVarDeclarationHints(@NotNull PsiElement element,
                                                         @NotNull InlayTreeSink sink,
                                                         HaxeSwitchCaseCaptureVar varDeclaration) {
      if (varDeclaration.getTypeTag() == null && varDeclaration.getVarInit() == null) {
        ResultHolder result = HaxeExpressionEvaluator.evaluate(varDeclaration, new HaxeExpressionEvaluatorContext(element), null).result;

        if (!result.isUnknown() && !result.getType().isInvalid()) {
          int offset = varDeclaration.getComponentName().getTextRange().getEndOffset();
          InlineInlayPosition position = new InlineInlayPosition(offset, true, 0);
          sink.addPresentation(position, null, null, false, appendTypeTextToBuilder(result)
          );
        }
      }
    }

    private static void handleCaptureVarDeclarationHints(@NotNull PsiElement element,
                                                         @NotNull InlayTreeSink sink,
                                                         HaxeSwitchCaseExpr switchCaseExpr) {
      if(switchCaseExpr.getFirstChild() instanceof HaxeReference reference && HaxeResolver.isCaptureVariable(switchCaseExpr)) {
          ResultHolder result = HaxeExpressionEvaluator.evaluate(reference, new HaxeExpressionEvaluatorContext(element), null).result;

          if (!result.isUnknown() && !result.getType().isInvalid()) {
            int offset = switchCaseExpr.getTextRange().getEndOffset();
            InlineInlayPosition position = new InlineInlayPosition(offset, true, 0);
            sink.addPresentation(position, null, null, false, appendTypeTextToBuilder(result)
            );
          }
      }
    }
  }
}
