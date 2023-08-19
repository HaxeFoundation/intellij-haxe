package com.intellij.plugins.haxe.ide.hint.types;

import com.intellij.codeInsight.hints.*;
import com.intellij.codeInsight.hints.presentation.InlayPresentation;
import com.intellij.openapi.editor.Editor;
import com.intellij.plugins.haxe.HaxeHintBundle;
import com.intellij.plugins.haxe.lang.psi.HaxeSwitchCaseCaptureVar;
import com.intellij.plugins.haxe.model.type.HaxeExpressionEvaluator;
import com.intellij.plugins.haxe.model.type.HaxeExpressionEvaluatorContext;
import com.intellij.plugins.haxe.model.type.ResultHolder;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;


public class HaxeInlayCaptureVariableHintsProvider extends HaxeInlayHintProvider {


  @Nullable
  @Override
  public InlayHintsCollector getCollectorFor(@NotNull PsiFile file,
                                             @NotNull Editor editor,
                                             @NotNull NoSettings settings,
                                             @NotNull InlayHintsSink sink) {
    return new TypeCollector(editor);
  }

  @Nls(capitalization = Nls.Capitalization.Sentence)
  @NotNull
  @Override
  public String getName() {
    return HaxeHintBundle.message("haxe.capture.variable.hint.name");
  }

  private static class TypeCollector extends HaxeInlayHintsFactory {
    public TypeCollector(@NotNull Editor editor) {
      super(editor);
    }

    @Override
    public boolean collect(@NotNull PsiElement element, @NotNull Editor editor, @NotNull InlayHintsSink sink) {
      if (element instanceof HaxeSwitchCaseCaptureVar varDeclaration) {
        handleCaptureVarDeclarationHints(element, sink, varDeclaration);
        return false;
      }
      return true;
    }


    private void handleCaptureVarDeclarationHints(@NotNull PsiElement element,
                                                  @NotNull InlayHintsSink sink,
                                                  HaxeSwitchCaseCaptureVar varDeclaration) {
      if (varDeclaration.getTypeTag() == null && varDeclaration.getVarInit() == null) {
        ResultHolder result = HaxeExpressionEvaluator.evaluate(varDeclaration, new HaxeExpressionEvaluatorContext(element), null).result;

        if (!result.isUnknown() && !result.getType().isInvalid()) {
          int offset = varDeclaration.getComponentName().getTextRange().getEndOffset();
          addInsert(offset, sink ,  ":" + result.toPresentationString());
        }
      }
    }
  }
}

