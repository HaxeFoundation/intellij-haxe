package com.intellij.plugins.haxe.ide.hint.types;

import com.intellij.codeInsight.hints.InlayHintsCollector;
import com.intellij.codeInsight.hints.InlayHintsSink;
import com.intellij.codeInsight.hints.NoSettings;
import com.intellij.codeInsight.hints.presentation.InlayPresentation;
import com.intellij.codeInsight.hints.presentation.ScaleAwarePresentationFactory;
import com.intellij.openapi.editor.Editor;
import com.intellij.plugins.haxe.HaxeHintBundle;
import com.intellij.plugins.haxe.lang.psi.HaxeLocalVarDeclaration;
import com.intellij.plugins.haxe.lang.psi.HaxeVarInit;
import com.intellij.plugins.haxe.model.type.HaxeGenericResolver;
import com.intellij.plugins.haxe.model.type.HaxeGenericResolverUtil;
import com.intellij.plugins.haxe.model.type.HaxeTypeResolver;
import com.intellij.plugins.haxe.model.type.ResultHolder;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;


public class HaxeInlayLocalVariableHintsProvider extends HaxeInlayHintProvider {


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
    return HaxeHintBundle.message("haxe.local.variable.hint.name");
  }

  private static class InlayCollector extends HaxeInlayHintsFactory {
    public InlayCollector(@NotNull Editor editor) {
      super(editor);

    }

    @Override
    public boolean collect(@NotNull PsiElement element, @NotNull Editor editor, @NotNull InlayHintsSink sink) {
      if (element instanceof HaxeLocalVarDeclaration varDeclaration) {
        handleLocalVarDeclarationHints(element, sink, varDeclaration);
        return false;
      }
      return true;
    }


    private void handleLocalVarDeclarationHints(@NotNull PsiElement element,
                                                @NotNull InlayHintsSink sink,
                                                HaxeLocalVarDeclaration varDeclaration) {
      if (varDeclaration.getTypeTag() == null && varDeclaration.getVarInit() != null) {

        HaxeVarInit init = varDeclaration.getVarInit();
        HaxeGenericResolver resolver = HaxeGenericResolverUtil.generateResolverFromScopeParents(init);
        ResultHolder type = HaxeTypeResolver.getPsiElementType(init, element, resolver);



        if (!type.isUnknown() && !type.getType().isInvalid()) {
          int offset = varDeclaration.getComponentName().getTextRange().getEndOffset();
          addInsert(offset, sink, ":" + type.toPresentationString());

        }
      }
    }

  }
}