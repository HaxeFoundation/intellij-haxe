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
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.PackageIndex;
import com.intellij.openapi.util.Condition;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.plugins.haxe.HaxeFileType;
import com.intellij.plugins.haxe.lang.psi.*;
import com.intellij.psi.*;
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

  @Nullable
  public static HaxeImportStatementWithInSupport findImportWithInByClassName(@NotNull PsiElement psiElement, String className) {
    final List<HaxeImportStatementWithInSupport> haxeImportStatementList = getAllInImportStatements(psiElement);
    for (HaxeImportStatementWithInSupport importStatement : haxeImportStatementList) {
      if (importInStatementForClassName(importStatement, className)) {
        return importStatement;
      }
    }
    return null;
  }

  @NotNull
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

  @NotNull
  public static boolean importInStatementForClassName(HaxeImportStatementWithInSupport importStatementWithInSupport, String classname) {
    HaxeIdentifier identifier = importStatementWithInSupport.getIdentifier();
    if (identifier != null) {
      String qName = identifier.getText();
      return qName.contentEquals(classname);
    }
    return false;
  }

  @NotNull
  public static String getPackageStatementForImportStatementWithWildcard(HaxeImportStatementWithWildcard importStatementWithWildcard) {
    String text = importStatementWithWildcard.getReferenceExpression().getText();
    String packageStatement = text.substring(0, text.length() - 2);
    return packageStatement;
  }

  @NotNull
  public static List<HaxeClass> getClassesForImportStatementWithWildcard(HaxeImportStatementWithWildcard importStatementWithWildcard) {
    List<HaxeClass> classList = new ArrayList<HaxeClass>();

    String packageStatement = getPackageStatementForImportStatementWithWildcard(importStatementWithWildcard);
    Project project = importStatementWithWildcard.getProject();
    VirtualFile[] virtualDirectoriesForPackage = getVirtualDirectoriesForPackage(packageStatement, project);
    for (VirtualFile file : virtualDirectoriesForPackage) {
      VirtualFile[] files = file.getChildren();
      for (VirtualFile virtualFile : files) {
        if (virtualFile.getFileType().equals(HaxeFileType.HAXE_FILE_TYPE)) {
          PsiFile psiFile = PsiManager.getInstance(project).findFile(virtualFile);

          String nameWithoutExtension = virtualFile.getNameWithoutExtension();

          List<HaxeClass> haxeClassList = HaxeResolveUtil.findComponentDeclarations(psiFile);
          for (HaxeClass haxeClass : haxeClassList) {
            if (haxeClass.getName().equals(nameWithoutExtension)) {
              classList.add(haxeClass);
            }
          }
        }
      }
    }
    return classList;
  }
  
  @NotNull
  public static boolean importStatementWithWildcardForClassName(HaxeImportStatementWithWildcard importStatementWithWildcard, String classname) {
    if (!Character.isUpperCase(classname.charAt(0))) {
      return false;
    }

    String packageStatement = getPackageStatementForImportStatementWithWildcard(importStatementWithWildcard);
    Project project = importStatementWithWildcard.getProject();
    VirtualFile[] virtualDirectoriesForPackage = getVirtualDirectoriesForPackage(packageStatement, project);
    for (VirtualFile file : virtualDirectoriesForPackage) {
      VirtualFile[] files = file.getChildren();
      for (VirtualFile virtualFile : files) {
        if (virtualFile.getFileType().equals(HaxeFileType.HAXE_FILE_TYPE)) {
          PsiFile psiFile = PsiManager.getInstance(project).findFile(virtualFile);

          String nameWithoutExtension = virtualFile.getNameWithoutExtension();

          if (!nameWithoutExtension.equals(classname)) {
            continue;
          }

          List<HaxeClass> haxeClassList = HaxeResolveUtil.findComponentDeclarations(psiFile);
          for (HaxeClass haxeClass : haxeClassList) {
            if (haxeClass.getName().equals(classname)) {
              return true;
            }
          }
        }
      }
    }

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
  public static List<HaxeImportStatementWithInSupport> getAllInImportStatements(PsiElement element) {
    final HaxeImportStatementWithInSupport[] haxeImportStatements =
      PsiTreeUtil.getChildrenOfType(element.getContainingFile(), HaxeImportStatementWithInSupport.class);
    if (haxeImportStatements != null) {
      return Arrays.asList(haxeImportStatements);
    }
    return Collections.emptyList();
  }

  @NotNull
  public static List<HaxeImportStatementWithWildcard> getAllImportStatementsWithWildcard(PsiElement element) {
    final HaxeImportStatementWithWildcard[] haxeImportStatements =
      PsiTreeUtil.getChildrenOfType(element.getContainingFile(), HaxeImportStatementWithWildcard.class);
    if (haxeImportStatements != null) {
      return Arrays.asList(haxeImportStatements);
    }
    return Collections.emptyList();
  }

  @NotNull
  public static VirtualFile[] getVirtualDirectoriesForPackage(String packageStatement, Project project) {
    VirtualFile[] directoriesByPackageName = PackageIndex.getInstance(project).getDirectoriesByPackageName(packageStatement, true);
    return directoriesByPackageName;
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
