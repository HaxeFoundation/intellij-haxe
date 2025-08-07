package com.intellij.plugins.haxe.ide.hint.types;

import com.intellij.codeInsight.hints.declarative.*;
import com.intellij.openapi.editor.Editor;
import com.intellij.plugins.haxe.lang.psi.HaxeLocalFunctionDeclaration;
import com.intellij.plugins.haxe.lang.psi.HaxeMethodDeclaration;
import com.intellij.plugins.haxe.model.HaxeMethodModel;
import com.intellij.plugins.haxe.model.type.*;
import com.intellij.plugins.haxe.util.UsefulPsiTreeUtil;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class HaxeInlayReturnTypeHintsProvider implements InlayHintsProvider {

  @Nullable
  @Override
  public InlayHintsCollector createCollector(@NotNull PsiFile file, @NotNull Editor editor) {
    return new TypeCollector();
  }

  private static class TypeCollector extends HaxeSharedBypassCollector {

    @Override
    public void collectFromElement(@NotNull PsiElement element, @NotNull InlayTreeSink sink) {
      if (element instanceof HaxeMethodDeclaration fieldDeclaration) {
        handleMethodDeclarationHints(sink, fieldDeclaration);
      }else if (element instanceof HaxeLocalFunctionDeclaration functionDeclaration) {
        handleFunctionDeclarationHints(sink, functionDeclaration);
      }
    }

    private void handleFunctionDeclarationHints(InlayTreeSink sink, HaxeLocalFunctionDeclaration declaration) {

      if (declaration.getTypeTag() == null) {
        SpecificFunctionReference functionReference = HaxeTypeResolver.getPsiElementType(declaration, null).getFunctionType();
        if (functionReference != null) {
          ResultHolder returnType = functionReference.getReturnType();
          PsiElement paramListEnd = UsefulPsiTreeUtil.getNextSiblingSkipWhiteSpacesAndComments(declaration.getParameterList());
          if (paramListEnd == null) return;
          int offset = paramListEnd.getTextRange().getEndOffset();
          if (!returnType.isUnknown() && !returnType.getType().isInvalid()) {
            InlineInlayPosition position = new InlineInlayPosition(offset, true, 0);
            sink.addPresentation(position, null, null, HintFormat.Companion.getDefault(), appendTypeTextToBuilder(returnType));
          }
        }
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
          sink.addPresentation(position, null, null, HintFormat.Companion.getDefault(), appendTypeTextToBuilder(returnType));
        }
      }
    }





  }
}
