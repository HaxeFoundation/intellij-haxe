package com.intellij.plugins.haxe.ide.hint.types;

import com.intellij.codeInsight.hints.InlayHintsCollector;
import com.intellij.codeInsight.hints.InlayHintsSink;
import com.intellij.codeInsight.hints.NoSettings;
import com.intellij.codeInsight.hints.SettingsKey;
import com.intellij.codeInsight.hints.presentation.InlayPresentation;
import com.intellij.openapi.editor.Editor;
import com.intellij.plugins.haxe.HaxeHintBundle;
import com.intellij.plugins.haxe.lang.psi.HaxeMethodDeclaration;
import com.intellij.plugins.haxe.model.HaxeMethodModel;
import com.intellij.plugins.haxe.model.type.HaxeGenericResolver;
import com.intellij.plugins.haxe.model.type.ResultHolder;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class HaxeInlayReturnTypeHintsProvider extends HaxeInlayHintProvider {


  @Nullable
  @Override
  public InlayHintsCollector getCollectorFor(@NotNull PsiFile file,
                                             @NotNull Editor editor,
                                             @NotNull NoSettings settings,
                                             @NotNull InlayHintsSink sink) {
    return new InlayCollector(editor);
  }


  @Nls(capitalization = Nls.Capitalization.Sentence)
  @NotNull
  @Override
  public String getName() {
    return HaxeHintBundle.message("haxe.return.type.hint.name");
  }

  private static class InlayCollector extends HaxeInlayHintsFactory {
    public InlayCollector(@NotNull Editor editor) {
      super(editor);
    }

    @Override
    public boolean collect(@NotNull PsiElement element, @NotNull Editor editor, @NotNull InlayHintsSink sink) {
      if (element instanceof HaxeMethodDeclaration fieldDeclaration) {
        handleMethodDeclarationHints(sink, fieldDeclaration);
        return false;
      }
      return true;
    }



    private void handleMethodDeclarationHints(InlayHintsSink sink, HaxeMethodDeclaration declaration) {
      HaxeMethodModel methodModel = declaration.getModel();

      if (methodModel != null && methodModel.getReturnTypeTagPsi() == null && !methodModel.isConstructor()) {
        HaxeGenericResolver resolver = methodModel.getGenericResolver(null);
        resolver = resolver.withTypeParametersAsType(methodModel.getGenericParams());
        ResultHolder returnType = methodModel.getReturnType(resolver);
        int offset = declaration.getParameterList().getNextSibling().getTextRange().getEndOffset();
        if (!returnType.isUnknown() && !returnType.getType().isInvalid()) {
          addInsert(offset, sink ,  ":" + returnType.toPresentationString());
        }
      }
    }
  }
}
