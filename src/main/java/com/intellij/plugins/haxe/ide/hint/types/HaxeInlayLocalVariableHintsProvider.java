package com.intellij.plugins.haxe.ide.hint.types;

import com.intellij.codeInsight.hints.declarative.*;
import com.intellij.openapi.editor.Editor;
import com.intellij.plugins.haxe.lang.psi.*;
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


public class HaxeInlayLocalVariableHintsProvider implements InlayHintsProvider {

  @Nullable
  @Override
  public InlayHintsCollector createCollector(@NotNull PsiFile file, @NotNull Editor editor) {
    return new TypeCollector();
  }

  private static class TypeCollector implements SharedBypassCollector {

    @Override
    public void collectFromElement(@NotNull PsiElement element, @NotNull InlayTreeSink sink) {
      if (element instanceof HaxeLocalVarDeclaration varDeclaration) {
        handleLocalVarDeclarationHints(sink, varDeclaration);
      }
    }


    private static void handleLocalVarDeclarationHints(@NotNull InlayTreeSink sink, @NotNull HaxeLocalVarDeclaration varDeclaration) {

      HaxeTypeTag tag = varDeclaration.getTypeTag();
      if (tag == null) {
        ResultHolder type;
        HaxeVarInit init = varDeclaration.getVarInit();
        if (init != null) {
          HaxeGenericResolver resolver = HaxeGenericResolverUtil.generateResolverFromScopeParents(init);
          type = HaxeTypeResolver.getPsiElementType(init, varDeclaration, resolver);
        } else {
          // attempts to resolve type from usage
          type = HaxeTypeResolver.getPsiElementType(varDeclaration, null);
        }

        if (!type.isUnknown() && !type.getType().isInvalid()) {
          int offset = varDeclaration.getComponentName().getTextRange().getEndOffset();
          InlineInlayPosition position = new InlineInlayPosition(offset, false, 0);
          sink.addPresentation(position, null, null, false, appendTypeTextToBuilder(type));
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
