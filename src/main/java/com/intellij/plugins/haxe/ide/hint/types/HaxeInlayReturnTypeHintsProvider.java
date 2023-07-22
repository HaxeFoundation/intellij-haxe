package com.intellij.plugins.haxe.ide.hint.types;

import com.intellij.codeInsight.hints.declarative.*;
import com.intellij.openapi.editor.Editor;
import com.intellij.plugins.haxe.lang.psi.HaxeMethodDeclaration;
import com.intellij.plugins.haxe.model.HaxeMethodModel;
import com.intellij.plugins.haxe.model.type.ResultHolder;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import kotlin.Unit;
import kotlin.jvm.functions.Function1;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class HaxeInlayReturnTypeHintsProvider implements InlayHintsProvider {

  @Nullable
  @Override
  public InlayHintsCollector createCollector(@NotNull PsiFile file, @NotNull Editor editor) {
    return new TypeCollector();
  }

  private static class TypeCollector implements SharedBypassCollector {

    @Override
    public void collectFromElement(@NotNull PsiElement element, @NotNull InlayTreeSink sink) {
      if (element instanceof HaxeMethodDeclaration fieldDeclaration) {
        handleMethodDeclarationHints(sink, fieldDeclaration);
      }
    }

    private void handleMethodDeclarationHints(InlayTreeSink sink, HaxeMethodDeclaration declaration) {
      HaxeMethodModel methodModel = declaration.getModel();

      if (methodModel != null && methodModel.getReturnTypeTagPsi() == null && !methodModel.isConstructor()) {
        ResultHolder returnType = methodModel.getReturnType(null);
        int offset = declaration.getParameterList().getNextSibling().getTextRange().getEndOffset();

        InlineInlayPosition position = new InlineInlayPosition(offset, false, 0);
        sink.addPresentation(position, null, null, false, appendTypeTextToBuilder(returnType));
      }
    }


    @NotNull
    private static Function1<PresentationTreeBuilder, Unit> appendTypeTextToBuilder(ResultHolder type) {
      return builder -> {
        builder.text(":" + type.toPresentationString(), null);
        return null;
      };
    }
  }
}
