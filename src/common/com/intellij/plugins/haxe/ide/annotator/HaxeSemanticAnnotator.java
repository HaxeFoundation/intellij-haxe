/*
 * Copyright 2000-2013 JetBrains s.r.o.
 * Copyright 2014-2015 AS3Boyan
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
package com.intellij.plugins.haxe.ide.annotator;

import com.intellij.codeInsight.intention.IntentionAction;
import com.intellij.lang.ASTNode;
import com.intellij.lang.annotation.Annotation;
import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.Annotator;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.roots.OrderRootType;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.plugins.haxe.lang.lexer.HaxeTokenTypes;
import com.intellij.plugins.haxe.lang.psi.*;
import com.intellij.plugins.haxe.util.*;
import com.intellij.psi.*;
import com.intellij.util.IncorrectOperationException;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

public class HaxeSemanticAnnotator implements Annotator {
  @Override
  public void annotate(PsiElement element, AnnotationHolder holder) {
    analyzeSingle(element, holder);
  }

  static void analyzeSingle(final PsiElement element, AnnotationHolder holder) {
    if (element instanceof HaxePackageStatement) {
      PackageChecker.check((HaxePackageStatement)element, holder);
    }
    else if (element instanceof HaxeMethod) {
      MethodChecker.check((HaxeMethod)element, holder);
    }
  }
}

class MethodChecker {
  static public void check(final HaxeMethod methodPsi, final AnnotationHolder holder) {
    final HaxeMethodModel method = methodPsi.getModel();
    final HaxeClassModel clazz = method.getDeclaringClass();
    final PsiElement overrideAttribute = method.getOverride();
    final HaxeClassModel parentClass = clazz.getExtendingClass();
    final HaxeMethodModel parentMethod = (parentClass != null) ? parentClass.getMethod(method.getName()) : null;
    boolean requiredOverride = false;

    if (method.isConstructor()) {
      requiredOverride = false;
      if (method.getStatic() != null) {
        holder.createErrorAnnotation(method.getNameOrBasePsi(), "Constructor can't be static");
      }
    } else if (method.isStaticInit()) {
      requiredOverride = false;
      if (method.getStatic() == null) {
        holder.createErrorAnnotation(method.getNameOrBasePsi(), "__init__ must be static");
      }
    }
    else if (parentMethod != null) {
      requiredOverride = true;

      if (parentMethod.getInline() != null || parentMethod.getStatic() != null) {
        holder.createErrorAnnotation(method.getNameOrBasePsi(), "Can't override static or inline methods");
      }

      if (method.getVisibility() != parentMethod.getVisibility()) {
        Annotation annotation = holder.createErrorAnnotation(method.getNameOrBasePsi(), "Method doesn't match parent's visibility");
        annotation.registerFix(
          new HaxeSemanticIntentionAction("Change current method visibility") {
            @Override
            public void run() {
              PsiElement currentVisibility = method.getVisibilityPsi();
              if (currentVisibility != null) {
                HaxePsiUtils.replaceElementWithText(currentVisibility, parentMethod.getVisibility().toString());
              }
            }
          }
        );
        annotation.registerFix(
          new HaxeSemanticIntentionAction("Change parent method visibility") {
            @Override
            public void run() {
              PsiElement parentVisibility = parentMethod.getVisibilityPsi();
              if (parentVisibility != null) {
                HaxePsiUtils.replaceElementWithText(parentVisibility, method.getVisibility().toString());
              }
            }
          }
        );
      }
    }

    //System.out.println(aClass);
    if (overrideAttribute != null && !requiredOverride) {
      holder.createErrorAnnotation(method.getNameOrBasePsi(), "Overriding nothing").registerFix(
        new HaxeSemanticIntentionAction("Fix override") {
          @Override
          public void run() {
            HaxePsiUtils.replaceElementWithText(overrideAttribute, "");
          }
        }
      );
    } else if (overrideAttribute == null && requiredOverride) {
      holder.createErrorAnnotation(method.getNameOrBasePsi(), "Must override");
    }
  }
}

class PackageChecker {
  static public void check(final HaxePackageStatement element, final AnnotationHolder holder) {
    final HaxeReferenceExpression expression = ((HaxePackageStatement)element).getReferenceExpression();
    String packageName = (expression != null) ? expression.getText() : "";
    PsiDirectory fileDirectory = element.getContainingFile().getParent();
    List<PsiFileSystemItem> fileRange = PsiFileUtils.getRange(PsiFileUtils.findRoot(fileDirectory), fileDirectory);
    fileRange.remove(0);
    String actualPath = PsiFileUtils.getListPath(fileRange);
    final String actualPackage = actualPath.replace('/', '.');
    final String actualPackage2 = HaxeResolveUtil.getPackageName(element.getContainingFile());
    // @TODO: Should use HaxeResolveUtil

    for (String s : StringUtils.split(packageName, '.')) {
      if (!s.substring(0, 1).toLowerCase().equals(s.substring(0, 1))) {
        //HaxeSemanticError.addError(element, new HaxeSemanticError("Package name '" + s + "' must start with a lower case character"));
        holder.createErrorAnnotation(element, "Package name '" + s + "' must start with a lower case character");
      }
    }

    if (!packageName.equals(actualPackage)) {
      holder.createErrorAnnotation(
        element,
        "Invalid package name! '" + packageName + "' should be '" + actualPackage + "'").registerFix(
        new HaxeSemanticIntentionAction("Fix package") {
          @Override
          public void run() {
            Document document =
              PsiDocumentManager.getInstance(element.getProject()).getDocument(element.getContainingFile());

            if (expression != null) {
              TextRange range = expression.getTextRange();
              document.replaceString(range.getStartOffset(), range.getEndOffset(), actualPackage);
            }
            else {
              int offset =
                element.getNode().findChildByType(HaxeTokenTypes.OSEMI).getTextRange().getStartOffset();
              document.replaceString(offset, offset, actualPackage);
            }
          }
        }
      );
    }
  }
}

class PsiFileUtils {

  static public String getListPath(List<PsiFileSystemItem> range) {
    String out = "";
    for (PsiFileSystemItem item : range) {
      if (out.length() != 0) out += "/";
      out += item.getName();
    }
    return out;
  }

  static public List<PsiFileSystemItem> getRange(PsiFileSystemItem from, PsiFileSystemItem to) {
    LinkedList<PsiFileSystemItem> items = new LinkedList<PsiFileSystemItem>();
    PsiFileSystemItem current = to;
    while (current != null) {
      items.addFirst(current);
      if (current == from) break;
      current = current.getParent();
    }
    return items;
  }

  static public PsiFileSystemItem findRoot(PsiFileSystemItem file) {
    //HaxelibModuleManager
    Project project = file.getProject();
    Module[] modules = ModuleManager.getInstance(project).getModules();
    List<VirtualFile> roots = new ArrayList<VirtualFile>();
    for (Module module : modules) {
      Sdk sdk = ModuleRootManager.getInstance(module).getSdk();
      roots.addAll(Arrays.asList(sdk.getRootProvider().getFiles(OrderRootType.CLASSES)));
    }
    roots.addAll(Arrays.asList(ProjectRootManager.getInstance(project).getContentSourceRoots()));

    PsiFileSystemItem current = file;
    while (current != null) {
      for (VirtualFile root : roots) {
        //System.out.println(root.getCanonicalPath());
        //System.out.println(current.getVirtualFile().getCanonicalPath());
        if (root.getCanonicalPath().equals(current.getVirtualFile().getCanonicalPath())) return current;
      }
      current = current.getParent();
    }
    return null;
  }

  static public String getDirectoryPath(PsiDirectory dir) {
    String out = "";
    while (dir != null) {
      if (out.length() != 0) out = "/" + out;
      out = dir.getName() + out;
      dir = dir.getParent();
    }
    return out;
  }
}

abstract class HaxeSemanticIntentionAction implements IntentionAction {
  private String text;

  public HaxeSemanticIntentionAction(String text) {
    this.text = text;
  }

  @Nls
  @NotNull
  @Override
  public String getText() {
    return this.text;
  }

  @Nls
  @NotNull
  @Override
  public String getFamilyName() {
    return "semantic";
  }

  @Override
  public boolean isAvailable(@NotNull Project project, Editor editor, PsiFile file) {
    return true;
  }

  @Override
  public void invoke(@NotNull Project project, Editor editor, PsiFile file) throws IncorrectOperationException {
    run();
  }

  @Override
  public boolean startInWriteAction() {
    return false;
  }

  abstract public void run();
}
