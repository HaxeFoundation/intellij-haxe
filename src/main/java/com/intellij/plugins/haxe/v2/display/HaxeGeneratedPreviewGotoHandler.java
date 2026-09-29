package com.intellij.plugins.haxe.v2.display;

import com.intellij.codeInsight.navigation.actions.GotoDeclarationHandler;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.plugins.haxe.lang.psi.HaxeReferenceExpression;
import com.intellij.plugins.haxe.v2.compiler.settings.HaxeCompilerSettings;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import lombok.CustomLog;
import org.jetbrains.annotations.Nullable;


/**
 * Redirects go-to-declaration on members that exist ONLY in the compiler's
 * post-macro program (blueprint-resolved, no source declaration) to the
 * generated-code preview: a dump build produces the module's typed-AST text
 * and the caret lands on the member. The dump build runs under a cancelable
 * modal progress. Canceling abandons the navigation, not the build: a build
 * that finishes anyway lands in the cache for the next attempt.
 */
@CustomLog
public class HaxeGeneratedPreviewGotoHandler implements GotoDeclarationHandler {

  @Override
  public PsiElement @Nullable [] getGotoDeclarationTargets(@Nullable PsiElement sourceElement, int offset, Editor editor) {
    if (sourceElement == null) return null;
    Project project = sourceElement.getProject();
    if (!HaxeCompilerSettings.getInstance(project).getCompletionMode().usesCompiler()) return null;

    HaxeReferenceExpression reference = PsiTreeUtil.getParentOfType(sourceElement, HaxeReferenceExpression.class, false);
    if (reference == null) return null;

    PsiElement resolved = reference.resolve();
    if (resolved == null) return null;
    PsiFile resolvedFile = resolved.getContainingFile();
    if (resolvedFile == null) return null;
    String dotPath = resolvedFile.getUserData(HaxeCompilerResolveService.BLUEPRINT_DOT_PATH);
    if (dotPath == null) return null;

    VirtualFile contextFile = HaxeCompilerDisplayService.physicalFileOf(sourceElement);
    if (contextFile == null) return null;
    HaxeCompilerDisplayService.DisplayContext context = HaxeCompilerDisplayService.getInstance(project).contextFor(contextFile);
    if (context == null) return null;

    String memberName = reference.getReferenceName();
    return new PsiElement[]{HaxeGeneratedPreviewTarget.createElement(resolved, context, dotPath, memberName)};
  }
}
