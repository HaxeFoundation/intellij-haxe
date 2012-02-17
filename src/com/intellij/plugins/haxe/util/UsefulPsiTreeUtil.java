package com.intellij.plugins.haxe.util;

import com.intellij.plugins.haxe.lang.psi.HaxeExpression;
import com.intellij.plugins.haxe.lang.psi.HaxeImportStatement;
import com.intellij.psi.PsiComment;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiWhiteSpace;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

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

  @Nullable
  public static HaxeImportStatement findImportByClass(@NotNull PsiElement psiElement, String className) {
    final List<HaxeImportStatement> haxeImportStatementList = getAllImportStatements(psiElement);
    for (HaxeImportStatement importStatement : haxeImportStatementList) {
      final HaxeExpression expression = importStatement.getExpression();
      if (expression == null) {
        continue;
      }
      final String qName = expression.getText();
      if (qName.endsWith("." + className)) {
        return importStatement;
      }
    }
    return null;
  }

  private static List<HaxeImportStatement> getAllImportStatements(PsiElement element) {
    final HaxeImportStatement[] haxeImportStatements =
      PsiTreeUtil.getChildrenOfType(element.getContainingFile(), HaxeImportStatement.class);
    if (haxeImportStatements != null) {
      return Arrays.asList(haxeImportStatements);
    }
    return Collections.emptyList();
  }

  @NotNull
  public static <T extends PsiElement> List<T> getSubnodesOfType(@Nullable PsiElement element, @NotNull Class<T> aClass) {
    final List<T> result = new ArrayList<T>();
    final Queue<PsiElement> queue = new LinkedList<PsiElement>();
    queue.add(element);
    while (!queue.isEmpty()) {
      final PsiElement currentElement = queue.poll();
      result.addAll(PsiTreeUtil.getChildrenOfTypeAsList(currentElement, aClass));
      Collections.addAll(queue, currentElement.getChildren());
    }
    return result;
  }

  @Nullable
  public static List<PsiElement> getPathToParentOfType(@Nullable PsiElement element,
                                                       @NotNull Class<? extends PsiElement> aClass) {
    if (element == null) return null;
    final List<PsiElement> result = new ArrayList<PsiElement>();
    while (element != null) {
      result.add(element);
      if (aClass.isInstance(element)) {
        return result;
      }
      if (element instanceof PsiFile) return null;
      element = element.getParent();
    }

    return null;
  }
}
