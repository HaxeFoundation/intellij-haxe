package com.intellij.plugins.haxe.ide.hint.types;

import com.intellij.codeInsight.hints.InlayHintsCollector;
import com.intellij.codeInsight.hints.InlayHintsSink;
import com.intellij.codeInsight.hints.NoSettings;
import com.intellij.openapi.editor.Editor;
import com.intellij.plugins.haxe.HaxeHintBundle;
import com.intellij.plugins.haxe.lang.psi.HaxeExpression;
import com.intellij.plugins.haxe.lang.psi.HaxeFieldDeclaration;
import com.intellij.plugins.haxe.model.HaxeFieldModel;
import com.intellij.plugins.haxe.model.type.HaxeGenericResolver;
import com.intellij.plugins.haxe.model.type.HaxeGenericResolverUtil;
import com.intellij.plugins.haxe.model.type.HaxeTypeResolver;
import com.intellij.plugins.haxe.model.type.ResultHolder;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class HaxeInlayFieldHintsProvider extends HaxeInlayHintProvider {

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
    return HaxeHintBundle.message("haxe.field.hint.name");
  }

  private static class InlayCollector extends HaxeInlayHintsFactory {
    public InlayCollector(@NotNull Editor editor) {
      super(editor);
    }

    @Override
    public boolean collect(@NotNull PsiElement element, @NotNull Editor editor, @NotNull InlayHintsSink sink) {
      if (element instanceof HaxeFieldDeclaration fieldDeclaration) {
        handleFieldDeclarationHints(element, sink, fieldDeclaration);
      }
      return true;
    }


    private void handleFieldDeclarationHints(@NotNull PsiElement element,
                                             @NotNull InlayHintsSink sink,
                                             HaxeFieldDeclaration fieldDeclaration) {
      HaxeFieldModel field = new HaxeFieldModel(fieldDeclaration);
      if (!field.hasTypeTag() && field.getInitializerExpression() != null) {
        HaxeExpression expression = field.getInitializerExpression();

        HaxeGenericResolver resolver = HaxeGenericResolverUtil.generateResolverFromScopeParents(expression);
        ResultHolder type = HaxeTypeResolver.getPsiElementType(expression, element, resolver);

        if (!type.isUnknown() && !type.getType().isInvalid()) {
          int offset = -1;
          if (fieldDeclaration.getPropertyDeclaration() != null) {
            offset = fieldDeclaration.getPropertyDeclaration().getTextRange().getEndOffset();
          }
          else {
            offset = field.getPsiField().getComponentName().getTextRange().getEndOffset();
          }

          addInsert(offset, sink, ":" + type.toPresentationString());
        }
      }
    }
  }
}
