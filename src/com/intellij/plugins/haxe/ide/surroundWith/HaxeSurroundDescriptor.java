package com.intellij.plugins.haxe.ide.surroundWith;

import com.intellij.lang.surroundWith.SurroundDescriptor;
import com.intellij.lang.surroundWith.Surrounder;
import com.intellij.plugins.haxe.lang.psi.HaxeBlockStatement;
import com.intellij.plugins.haxe.util.UsefulPsiTreeUtil;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * @author: Fedor.Korotkov
 */
public class HaxeSurroundDescriptor implements SurroundDescriptor {
  @NotNull
  @Override
  public PsiElement[] getElementsToSurround(PsiFile file, int startOffset, int endOffset) {
    final List<PsiElement> path = UsefulPsiTreeUtil.getPathToParentOfType(file.findElementAt(startOffset), HaxeBlockStatement.class);
    if (path == null || path.size() < 2) {
      return PsiElement.EMPTY_ARRAY;
    }
    final List<PsiElement> result = new ArrayList<PsiElement>();
    PsiElement child = path.get(path.size() - 2);
    while (child != null && child.getTextOffset() < endOffset) {
      result.add(child);
      child = child.getNextSibling();
    }
    return result.toArray(new PsiElement[result.size()]);
  }

  @NotNull
  @Override
  public Surrounder[] getSurrounders() {
    return new Surrounder[]{
      new HaxeIfSurrounder(),
      new HaxeIfElseSurrounder(),
      new HaxeWhileSurrounder(),
      new HaxeDoWhileSurrounder(),
      new HaxeTryCatchSurrounder()
    };
  }

  @Override
  public boolean isExclusive() {
    return false;
  }
}
