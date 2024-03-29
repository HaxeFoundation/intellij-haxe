package com.intellij.plugins.haxe.ide.hint.types;

import com.intellij.codeInsight.hints.declarative.*;
import com.intellij.openapi.editor.Editor;
import com.intellij.plugins.haxe.lang.psi.HaxeMethodDeclaration;
import com.intellij.plugins.haxe.model.HaxeMethodModel;
import com.intellij.plugins.haxe.model.type.HaxeGenericResolver;
import com.intellij.plugins.haxe.model.type.ResultHolder;
import com.intellij.plugins.haxe.util.UsefulPsiTreeUtil;
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
        HaxeGenericResolver resolver = methodModel.getGenericResolver(null);
        resolver = resolver.withTypeParametersAsType(methodModel.getGenericParams());
        ResultHolder returnType = methodModel.getReturnType(resolver);
        PsiElement paramListEnd = UsefulPsiTreeUtil.getNextSiblingSkipWhiteSpacesAndComments(declaration.getParameterList());
        if (paramListEnd == null) return;
        int offset = paramListEnd.getTextRange().getEndOffset();
        if (!returnType.isUnknown() && !returnType.getType().isInvalid()) {
          InlineInlayPosition position = new InlineInlayPosition(offset, false, 0);
          sink.addPresentation(position, null, null, false, appendTypeTextToBuilder(returnType));
        }
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
