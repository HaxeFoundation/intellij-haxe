package com.intellij.plugins.haxe.ide.hint.types;

import com.intellij.codeInsight.hints.declarative.*;
import com.intellij.openapi.editor.Editor;
import com.intellij.plugins.haxe.lang.psi.HaxeExpression;
import com.intellij.plugins.haxe.lang.psi.HaxeFieldDeclaration;
import com.intellij.plugins.haxe.model.HaxeFieldModel;
import com.intellij.plugins.haxe.model.type.HaxeGenericResolver;
import com.intellij.plugins.haxe.model.type.HaxeGenericResolverUtil;
import com.intellij.plugins.haxe.model.type.HaxeTypeResolver;
import com.intellij.plugins.haxe.model.type.ResultHolder;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class HaxeInlayFieldHintsProvider implements InlayHintsProvider {

  @Nullable
  @Override
  public InlayHintsCollector createCollector(@NotNull PsiFile file, @NotNull Editor editor) {
    return new TypeCollector();
  }

  private static class TypeCollector extends HaxeSharedBypassCollector {

    @Override
    public void collectFromElement(@NotNull PsiElement element, @NotNull InlayTreeSink sink) {
      if (element instanceof HaxeFieldDeclaration fieldDeclaration) {
        handleFieldDeclarationHints(element, sink, fieldDeclaration);
      }
    }


    private static void handleFieldDeclarationHints(@NotNull PsiElement element,
                                                    @NotNull InlayTreeSink sink,
                                                    HaxeFieldDeclaration fieldDeclaration) {
      HaxeFieldModel field = (HaxeFieldModel)fieldDeclaration.getModel();

      if (!field.hasTypeTag()) {

        HaxeExpression expression = field.getInitializerExpression();
        if (expression != null) {

          HaxeGenericResolver resolver = HaxeGenericResolverUtil.generateResolverFromScopeParents(expression);
          ResultHolder type = HaxeTypeResolver.getPsiElementType(element,  resolver);

          if (!type.isUnknown() && !type.getType().isInvalid()) {
            int offset;
            if (fieldDeclaration.getPropertyDeclaration() != null) {
              offset = fieldDeclaration.getPropertyDeclaration().getTextRange().getEndOffset();
            }
            else {
              offset = field.getPsiField().getComponentName().getTextRange().getEndOffset();
            }
            InlineInlayPosition position = new InlineInlayPosition(offset, true, 0);
            sink.addPresentation(position, null, null, HintFormat.Companion.getDefault(), appendTypeTextToBuilder(type)
            );
          }
        }
      }
    }
  }
}
