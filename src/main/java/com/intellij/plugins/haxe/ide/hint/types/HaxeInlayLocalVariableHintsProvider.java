package com.intellij.plugins.haxe.ide.hint.types;

import com.intellij.codeInsight.hints.declarative.*;
import com.intellij.openapi.editor.Editor;
import com.intellij.plugins.haxe.lang.psi.*;
import com.intellij.plugins.haxe.model.type.*;
import com.intellij.psi.PsiElement;
import com.intellij.plugins.haxe.v2.display.HaxeCompilerInlayService;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;


public class HaxeInlayLocalVariableHintsProvider implements InlayHintsProvider {

  @Nullable
  @Override
  public InlayHintsCollector createCollector(@NotNull PsiFile file, @NotNull Editor editor) {
    if (HaxeCompilerInlayService.isActive(file.getProject())) return null;
    return new TypeCollector();
  }

  private static class TypeCollector extends HaxeSharedBypassCollector {

    @Override
    public void collectFromElement(@NotNull PsiElement element, @NotNull InlayTreeSink sink) {
      if (element instanceof HaxeLocalVarDeclaration varDeclaration) {
        handleLocalVarDeclarationHints(sink, varDeclaration);
      }
    }


    private static void handleLocalVarDeclarationHints(@NotNull InlayTreeSink sink, @NotNull HaxeLocalVarDeclaration varDeclaration) {
      if (varDeclaration instanceof  HaxeSwitchCaseCaptureVar) return; // handled by HaxeInlayCaptureVariableHintsProvider
      HaxeTypeTag tag = varDeclaration.getTypeTag();
      if (tag == null) {
        // attempts to resolve type from init or usage
        ResultHolder type = HaxeTypeResolver.getPsiElementType(varDeclaration, null);

        addTypeHint(varDeclaration, sink, type);
      }
    }
  }
}
