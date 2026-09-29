package com.intellij.plugins.haxe.v2.display;

import com.intellij.codeInsight.navigation.actions.GotoDeclarationHandler;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.plugins.haxe.display.protocol.Location;
import com.intellij.plugins.haxe.lang.psi.HaxeReference;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Go-to-declaration answered by the compilation server. For a reference
 * under the caret, the targets are the PSI elements at the locations
 * {@code display/definition} returns, so the platform navigates, previews
 * and offers a chooser for several targets just as for static results. The
 * platform calls handlers under a cancelable progress, and the request waits
 * a bounded time. Ctrl-hover asks too, so every hovered reference costs one
 * request.
 *
 * No targets are returned for a declaration under the caret, where the
 * platform keeps its show-usages behaviour, nor for a file the compiler
 * cannot serve, where the notification is shown and the platform resolves
 * the reference itself.
 */
public final class HaxeCompilerGotoDeclarationHandler implements GotoDeclarationHandler {

  @Override
  public PsiElement @Nullable [] getGotoDeclarationTargets(@Nullable PsiElement sourceElement, int offset, Editor editor) {
    if (sourceElement == null) return null;
    HaxeReference reference = PsiTreeUtil.getParentOfType(sourceElement, HaxeReference.class, false);
    if (reference == null) return null;
    VirtualFile file = HaxeCompilerDisplayService.physicalFileOf(sourceElement);
    if (file == null) return null;
    Project project = sourceElement.getProject();
    HaxeCompilerNavigationService service = HaxeCompilerNavigationService.getInstance(project);
    if (!service.ensureAvailable(file)) return null;

    List<PsiElement> declarations = declarationsOf(project, service.definitionUnderReadLock(file, offset));
    return declarations.isEmpty() ? null : declarations.toArray(PsiElement.EMPTY_ARRAY);
  }

  @NotNull
  private static List<PsiElement> declarationsOf(@NotNull Project project, @NotNull List<Location> locations) {
    List<PsiElement> declarations = new ArrayList<>();
    for (Location location : locations) {
      PsiFile psiFile = HaxeCompilerLocations.psiFileOf(project, location);
      PsiElement declaration = psiFile == null ? null : HaxeCompilerLocations.declarationIn(psiFile, location.range());
      if (declaration != null) declarations.add(declaration);
    }
    return declarations;
  }
}
