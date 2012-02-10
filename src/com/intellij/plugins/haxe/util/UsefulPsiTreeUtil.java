package com.intellij.plugins.haxe.util;

import com.intellij.psi.PsiComment;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiWhiteSpace;
import org.jetbrains.annotations.Nullable;

/**
 * @author: Fedor.Korotkov
 */
public class UsefulPsiTreeUtil {
  @Nullable
  public static PsiElement getPrevSiblingSkipWhiteSpacesAndComments(@Nullable PsiElement sibling) {
    if (sibling == null) return null;
    PsiElement result = sibling.getPrevSibling();
    while (result != null && isWhitespaceOrComment(result)) {
      result = result.getPrevSibling();
    }
    return result;
  }

  public static boolean isWhitespaceOrComment(PsiElement element) {
    return element instanceof PsiWhiteSpace || element instanceof PsiComment;
  }
}
