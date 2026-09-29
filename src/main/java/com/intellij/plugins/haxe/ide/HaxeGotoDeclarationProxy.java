package com.intellij.plugins.haxe.ide;

import com.intellij.codeInsight.navigation.actions.GotoDeclarationHandler;
import com.intellij.openapi.editor.Editor;
import com.intellij.plugins.haxe.v2.compiler.settings.HaxeCompilerSettings;
import com.intellij.plugins.haxe.v2.display.HaxeCompilerGotoDeclarationHandler;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.Nullable;

/**
 * The registered go-to-declaration handler: it delegates to the
 * compiler-backed handler while "Use compiler for IDE features" is on, and
 * to the static handler otherwise. When the chosen handler answers nothing,
 * the platform resolves the reference under the caret itself; no handler
 * can switch that off.
 */
public final class HaxeGotoDeclarationProxy implements GotoDeclarationHandler {

  private final GotoDeclarationHandler staticHandler = new HaxeGotoDeclarationHandler();
  private final GotoDeclarationHandler compilerHandler = new HaxeCompilerGotoDeclarationHandler();

  @Override
  public PsiElement @Nullable [] getGotoDeclarationTargets(@Nullable PsiElement sourceElement, int offset, Editor editor) {
    if (sourceElement == null) return null;
    boolean useCompiler = HaxeCompilerSettings.getInstance(sourceElement.getProject()).isCompilerIdeFeaturesEnabled();
    GotoDeclarationHandler handler = useCompiler ? compilerHandler : staticHandler;
    return handler.getGotoDeclarationTargets(sourceElement, offset, editor);
  }
}
