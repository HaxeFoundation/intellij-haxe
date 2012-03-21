package com.intellij.plugins.haxe.util;

import com.intellij.lang.ASTNode;
import com.intellij.openapi.util.Condition;
import com.intellij.plugins.haxe.lang.psi.HaxeExpression;
import com.intellij.plugins.haxe.lang.psi.HaxeImportStatement;
import com.intellij.plugins.haxe.lang.psi.HaxePackageStatement;
import com.intellij.plugins.haxe.lang.psi.HaxePsiCompositeElement;
import com.intellij.psi.PsiComment;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiWhiteSpace;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * @author: Fedor.Korotkov
 */
public class UsefulPsiTreeUtil {
  @NotNull
  public static String findPackageName(PsiFile file) {
    final HaxePackageStatement packageStatement = PsiTreeUtil.getChildOfType(file, HaxePackageStatement.class);
    if (packageStatement == null || packageStatement.getExpression() == null) {
      return "";
    }
    return packageStatement.getExpression().getText();
  }

  @Nullable
  public static PsiElement getFirstChildSkipWhiteSpacesAndComments(@Nullable PsiElement root) {
    if (root == null) return null;
    for (PsiElement child : root.getChildren()) {
      if (!isWhitespaceOrComment(child)) {
        return child;
      }
    }
    return null;
  }

  @Nullable
  public static PsiElement getPrevSiblingSkipWhiteSpacesAndComments(@Nullable PsiElement sibling) {
    return getPrevSibling(sibling, new Condition<PsiElement>() {
      @Override
      public boolean value(PsiElement element) {
        return isWhitespaceOrComment(element);
      }
    });
  }

  @Nullable
  public static PsiElement getPrevSiblingSkipWhiteSpaces(@Nullable PsiElement sibling) {
    return getPrevSibling(sibling, new Condition<PsiElement>() {
      @Override
      public boolean value(PsiElement element) {
        return element instanceof PsiWhiteSpace;
      }
    });
  }

  @Nullable
  public static PsiElement getPrevSibling(@Nullable PsiElement sibling, Condition<PsiElement> condition) {
    if (sibling == null) return null;
    PsiElement result = sibling.getPrevSibling();
    while (result != null && condition.value(result)) {
      result = result.getPrevSibling();
    }
    return result;
  }

  @Nullable
  public static ASTNode getPrevSiblingSkipWhiteSpacesAndComments(@Nullable ASTNode sibling) {
    if (sibling == null) return null;
    ASTNode result = sibling.getTreePrev();
    while (result != null && isWhitespaceOrComment(result.getPsi())) {
      result = result.getTreePrev();
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

  @Nullable
  public static HaxePsiCompositeElement getChildOfType(@Nullable HaxePsiCompositeElement element, @Nullable IElementType elementType) {
    if (element == null) return null;
    for (HaxePsiCompositeElement child : PsiTreeUtil.getChildrenOfTypeAsList(element, HaxePsiCompositeElement.class)) {
      if (child.getTokenType() == elementType) {
        return child;
      }
    }
    return null;
  }
}
