/*
 * Copyright 2000-2013 JetBrains s.r.o.
 * Copyright 2014-2014 AS3Boyan
 * Copyright 2014-2014 Elias Ku
 * Copyright 2017-2018 Ilya Malanin
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
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Condition;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.plugins.haxe.HaxeFileType;
import com.intellij.plugins.haxe.lang.psi.HaxeClass;
import com.intellij.plugins.haxe.lang.psi.HaxeParenthesizedExpression;
import com.intellij.plugins.haxe.lang.psi.HaxePsiCompositeElement;
import com.intellij.psi.*;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.ArrayUtil;
import com.intellij.util.SmartList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;


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
    return getPrevSiblingSkippingCondition(sibling, new Condition<PsiElement>() {
      @Override
      public boolean value(PsiElement element) {
        return isWhitespaceOrComment(element);
      }
    }, strictly);
  }

  @Nullable
  public static PsiElement getPrevSiblingSkipWhiteSpaces(@Nullable PsiElement sibling, boolean strictly) {
    return getPrevSiblingSkippingCondition(sibling, new Condition<PsiElement>() {
      @Override
      public boolean value(PsiElement element) {
        return element instanceof PsiWhiteSpace;
      }
    }, strictly);
  }

  @Nullable
  public static PsiElement getPrevSiblingSkippingCondition(@Nullable PsiElement sibling, Condition<PsiElement> condition, boolean strictly) {
    if (sibling == null) return null;
    PsiElement result = strictly ? sibling.getPrevSibling() : sibling;
    while (result != null && condition.value(result)) {
      result = result.getPrevSibling();
    }
    return result;
  }

  @Nullable
  public static PsiElement getNextSiblingSkippingCondition(@Nullable PsiElement sibling, Condition<PsiElement> condition, boolean strictly) {
    if (sibling == null) return null;
    PsiElement result = strictly ? sibling.getNextSibling() : sibling;
    while (result != null && condition.value(result)) {
      result = result.getNextSibling();
    }
    return result;
  }

  @Nullable
  public static ASTNode getPrevSiblingSkipWhiteSpacesAndComments(@Nullable ASTNode sibling) {
    if (sibling == null) return null;
    ASTNode result = sibling.getTreePrev();
    while (result != null && isWhitespaceOrComment(result)) {
      result = result.getTreePrev();
    }
    return result;
  }

  @Nullable
  public static ASTNode getNextSiblingSkipWhiteSpacesAndComments(@Nullable ASTNode sibling) {
    if (sibling == null) return null;
    ASTNode result = sibling.getTreeNext();
    while (result != null && isWhitespaceOrComment(result.getPsi())) {
      result = result.getTreeNext();
    }
    return result;
  }

  public static boolean isWhitespaceOrComment(ASTNode node) {
    return isWhitespaceOrComment(node.getPsi());
  }

  public static boolean isWhitespaceOrComment(PsiElement element) {
    return element instanceof PsiWhiteSpace || element instanceof PsiComment;
  }

  private static void populateClassesList(List<HaxeClass> classList, Project project, VirtualFile file) {
    VirtualFile[] files = file.getChildren();
    for (VirtualFile virtualFile : files) {
      if (virtualFile.getFileType().equals(HaxeFileType.HAXE_FILE_TYPE)) {
        PsiFile psiFile = PsiManager.getInstance(project).findFile(virtualFile);

        String nameWithoutExtension = virtualFile.getNameWithoutExtension();

        List<HaxeClass> haxeClassList = HaxeResolveUtil.findComponentDeclarations(psiFile);
        for (HaxeClass haxeClass : haxeClassList) {
          if (Objects.equals(haxeClass.getName(), nameWithoutExtension)) {
            classList.add(haxeClass);
          }
        }
      }
    }
  }

  public static List<HaxeClass> getClassesInDirectory(Project project, VirtualFile file) {
    List<HaxeClass> classList = new ArrayList<HaxeClass>();
    populateClassesList(classList, project, file);
    return classList;
  }

  @Nullable
  public static PsiElement getParentOfType(@Nullable PsiElement element, @NotNull Class<? extends PsiElement> aClass) {
    if (element == null || element instanceof PsiFile)
      return null;

    // Don't start with the passed element.
    element = element.getParent();
    while (element != null && !(element instanceof PsiFile)) {
      if (aClass.isInstance(element)) {
        return element;
      }
      element = element.getParent();
    }
    return null;
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

  static public <T extends PsiElement> T getAncestor(PsiElement element, Class<T> clazz) {
    if (element == null) return null;
    if (clazz.isAssignableFrom(element.getClass())) return (T)element;
    return getAncestor(element.getParent(), clazz);
  }

  static public <T extends PsiElement> T getChild(PsiElement element, Class<T> clazz) {
    if (element == null) return null;
    for (PsiElement psiElement : element.getChildren()) {
      if (clazz.isAssignableFrom(psiElement.getClass())) return (T)psiElement;
    }
    return null;
  }

  static public <T extends PsiElement> T getLastChild(PsiElement element, Class<T> clazz, @Nullable T stub) {
    if (element == null) return stub;
    for (PsiElement psiElement = element.getLastChild(); psiElement.getPrevSibling() != null; psiElement = psiElement.getPrevSibling()) {
      if (clazz.isAssignableFrom(psiElement.getClass())) return (T)psiElement;
    }
    return stub;
  }

  @Nullable
  static public <T extends PsiElement> T getLastChild(PsiElement element, Class<T> clazz) {
    return getLastChild(element, clazz, null);
  }

  static public PsiElement getToken(PsiElement element, String token) {
    for (ASTNode node : element.getNode().getChildren(null)) {
      if (node.getText().equals(token))  return node.getPsi();
    }
    return null;
  }

  static public <T extends PsiElement> T getChildWithText(PsiElement element, Class<T> clazz, String text) {
    if (element == null) return null;
    for (PsiElement psiElement : element.getChildren()) {
      if (clazz.isAssignableFrom(psiElement.getClass()) && psiElement.getText().equals(text)) return (T)psiElement;
    }
    return null;
  }

  static public <T extends PsiElement> List<T> getChildren(PsiElement element, Class<T> clazz) {
    if (element == null) return null;
    ArrayList<T> ts = new ArrayList<T>();
    for (PsiElement psiElement : element.getChildren()) {
      if (clazz.isAssignableFrom(psiElement.getClass())) ts.add((T)psiElement);
    }
    return ts;
  }

  static public void replaceElementWithText(PsiElement element, String text) {
    Document document = PsiDocumentManager.getInstance(element.getProject()).getDocument(element.getContainingFile());
    TextRange range = element.getTextRange();
    document.replaceString(range.getStartOffset(), range.getEndOffset(), text);
  }

  public static PsiElement getNextSiblingNoSpaces(PsiElement element) {
    if (element == null) return null;
    PsiElement sibling = element.getNextSibling();
    while (sibling != null && sibling instanceof PsiWhiteSpace) {
      sibling = sibling.getNextSibling();
    }
    return sibling;
  }

  @Nullable
  public static PsiElement getNextSiblingSkipWhiteSpacesAndComments(@Nullable PsiElement element) {
    if (element == null) return null;
    PsiElement sibling = element.getNextSibling();
    while (sibling != null && isWhitespaceOrComment(sibling)) {
      sibling = sibling.getNextSibling();
    }
    return sibling;
  }

  @Nullable
  public static PsiElement skipParenthesizedExprUp(@Nullable PsiElement parent) {
    while (parent instanceof HaxeParenthesizedExpression) {
      parent = parent.getParent();
    }
    return parent;
  }
}
