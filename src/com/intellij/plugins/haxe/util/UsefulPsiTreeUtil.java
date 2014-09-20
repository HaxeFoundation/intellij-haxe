/*
 * Copyright 2000-2013 JetBrains s.r.o.
 * Copyright 2014-2014 AS3Boyan
 * Copyright 2014-2014 Elias Ku
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.intellij.plugins.haxe.util;

import com.intellij.lang.ASTNode;
import com.intellij.openapi.util.Condition;
import com.intellij.plugins.haxe.lang.psi.*;
import com.intellij.psi.PsiComment;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiWhiteSpace;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.ArrayUtil;
import com.intellij.util.SmartList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * @author: Fedor.Korotkov
 */
public class UsefulPsiTreeUtil {
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
  public static PsiElement getPrevSiblingSkipWhiteSpacesAndComments(@Nullable PsiElement sibling, boolean strictly) {
    return getPrevSiblingSkipingCondition(sibling, new Condition<PsiElement>() {
      @Override
      public boolean value(PsiElement element) {
        return isWhitespaceOrComment(element);
      }
    }, strictly);
  }

  @Nullable
  public static PsiElement getPrevSiblingSkipWhiteSpaces(@Nullable PsiElement sibling, boolean strictly) {
    return getPrevSiblingSkipingCondition(sibling, new Condition<PsiElement>() {
      @Override
      public boolean value(PsiElement element) {
        return element instanceof PsiWhiteSpace;
      }
    }, strictly);
  }

  @Nullable
  public static PsiElement getPrevSiblingSkipingCondition(@Nullable PsiElement sibling, Condition<PsiElement> condition, boolean strictly) {
    if (sibling == null) return null;
    PsiElement result = strictly ? sibling.getPrevSibling() : sibling;
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
  public static HaxeImportStatementRegular findImportByClassName(@NotNull PsiElement psiElement, String className) {
    final List<HaxeImportStatementRegular> haxeImportStatementList = getAllImportStatements(psiElement);
    for (HaxeImportStatementRegular importStatement : haxeImportStatementList) {
      if (importStatementForClassName(importStatement, className)) {
        return importStatement;
      }
    }
    return null;
  }

  public static boolean importStatementForClassName(HaxeImportStatementRegular importStatement, String className) {
    final HaxeImportStatementRegular regularImport = importStatement;
    if(regularImport != null) {
      final HaxeExpression expression = regularImport.getReferenceExpression();
      final String qName = expression.getText();
      return qName.endsWith("." + className);
    }
    // TODO: other import types (inject util logic to ImportStatement?)
    return false;
  }

  @Nullable
  public static String findHelperOwnerQName(PsiElement context, String className) {
    for (HaxeImportStatementRegular importStatement : getAllImportStatements(context)) {
      final HaxeImportStatementRegular regularImport = importStatement;
      if(regularImport != null) {
        final HaxeExpression expression = regularImport.getReferenceExpression();
        final String qName = expression.getText();
        final PsiElement resolve = HaxeResolveUtil.findClassByQName(qName, context);
        if (resolve != null && HaxeResolveUtil.findComponentDeclaration(resolve.getContainingFile(), className) != null) {
          return qName;
        }
      }
      // TODO: other import types (inject util logic to ImportStatement?)
    }
    return null;
  }

  @NotNull
  public static List<HaxeImportStatementRegular> getAllImportStatements(PsiElement element) {
    final HaxeImportStatementRegular[] haxeImportStatements =
      PsiTreeUtil.getChildrenOfType(element.getContainingFile(), HaxeImportStatementRegular.class);
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

  @Nullable
  public static <T extends PsiElement> T[] getChildrenOfType(@Nullable PsiElement element,
                                                             @NotNull Class<T> aClass,
                                                             @Nullable PsiElement lastParent) {
    if (element == null) return null;

    List<T> result = null;
    for (PsiElement child = element.getFirstChild(); child != null; child = child.getNextSibling()) {
      if (lastParent == child) {
        break;
      }
      if (aClass.isInstance(child)) {
        if (result == null) result = new SmartList<T>();
        //noinspection unchecked
        result.add((T)child);
      }
    }
    return result == null ? null : ArrayUtil.toObjectArray(result, aClass);
  }

  public static boolean importStatementForClass(@NotNull HaxeImportStatementRegular importStatement, @NotNull HaxeClass haxeClass) {
    final HaxeImportStatementRegular regularImport = importStatement;
    if(regularImport != null) {
      HaxeReferenceExpression importReferenceExpression = regularImport.getReferenceExpression();
      PsiElement importTarget = importReferenceExpression.resolve();
      if (importTarget == null) {
        return false;
      }
      // in case of helpers just check containing files
      return importTarget.getContainingFile() == haxeClass.getContainingFile();
    }
    // TODO: other import types (inject util logic to ImportStatement?)
    return false;
  }
}
