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
import kotlin.Unit;
import kotlin.jvm.functions.Function1;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class HaxeInlayFieldHintsProvider implements InlayHintsProvider {

  @Nullable
  @Override
  public InlayHintsCollector createCollector(@NotNull PsiFile file, @NotNull Editor editor) {
    return new TypeCollector();
  }

  private static class TypeCollector implements SharedBypassCollector {

    @Override
    public void collectFromElement(@NotNull PsiElement element, @NotNull InlayTreeSink sink) {
      if (element instanceof HaxeFieldDeclaration fieldDeclaration) {
        handleFieldDeclarationHints(element, sink, fieldDeclaration);
      }
    }


    private static void handleFieldDeclarationHints(@NotNull PsiElement element,
                                                    @NotNull InlayTreeSink sink,
                                                    HaxeFieldDeclaration fieldDeclaration) {
      HaxeFieldModel field = new HaxeFieldModel(fieldDeclaration);
      if (!field.hasTypeTag() && field.hasInitializer()) {
        HaxeExpression expression = field.getInitializerExpression();

        HaxeGenericResolver resolver = HaxeGenericResolverUtil.generateResolverFromScopeParents(expression);
        ResultHolder type = HaxeTypeResolver.getPsiElementType(expression, element, resolver);


        int offset = field.getPsiField().getComponentName().getTextRange().getEndOffset();
        InlineInlayPosition position = new InlineInlayPosition(offset, false, 0);
        sink.addPresentation(position, null, null, false, appendTypeTextToBuilder(type)
        );
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
