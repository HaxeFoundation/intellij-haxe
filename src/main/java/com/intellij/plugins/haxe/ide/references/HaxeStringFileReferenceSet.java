package com.intellij.plugins.haxe.ide.references;

import com.intellij.openapi.project.ProjectUtil;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.plugins.haxe.lang.psi.HaxeStringLiteralExpression;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiFileSystemItem;
import com.intellij.psi.impl.source.resolve.reference.impl.providers.FileReferenceSet;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * RELATIVE file-path references inside a string literal, split per segment
 * so the platform provides completion (a segment's variants are its resolved
 * parent's children) and rename refactoring. Resolves against two bases: the
 * containing file's directory and the project root. Soft throughout — an
 * unresolvable or half-typed path is prose, never an error; the link
 * painting checks the LAST segment's resolution separately. Absolute paths
 * stay on the single-reference route (see the contributor).
 */
class HaxeStringFileReferenceSet extends FileReferenceSet {

  HaxeStringFileReferenceSet(@NotNull String path, @NotNull HaxeStringLiteralExpression literal, int startInElement) {
    super(path, literal, startInElement, null, SystemInfo.isFileSystemCaseSensitive, false, null);
  }

  @Override
  protected boolean isSoft() {
    return true;
  }

  @Override
  public @NotNull Collection<PsiFileSystemItem> computeDefaultContexts() {
    List<PsiFileSystemItem> contexts = new ArrayList<>(2);
    PsiFile file = getElement().getContainingFile().getOriginalFile();
    PsiDirectory containingDir = file.getParent();
    if (containingDir != null) contexts.add(containingDir);

    VirtualFile projectDir = ProjectUtil.guessProjectDir(file.getProject());
    PsiDirectory projectRoot = projectDir != null ? file.getManager().findDirectory(projectDir) : null;
    if (projectRoot != null && !projectRoot.equals(containingDir)) contexts.add(projectRoot);
    return contexts;
  }
}
