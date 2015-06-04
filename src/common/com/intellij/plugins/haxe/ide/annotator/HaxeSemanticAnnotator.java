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
import com.intellij.plugins.haxe.util.HaxeClassModel;
import com.intellij.plugins.haxe.util.HaxeMethodModel;
import com.intellij.plugins.haxe.util.HaxePsiUtils;
import com.intellij.plugins.haxe.util.HaxeResolveUtil;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.util.PsiUtil;
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
    PsiFile file = element.getContainingFile();
    // Analyze upon file changes
    Long analyzedFileTime = file.getUserData(HaxeSemanticError.KEY_ANALYZED_METHOD_TIME);
    if (analyzedFileTime == null || analyzedFileTime != file.getModificationStamp()) {
      analyze(file);
      file.putUserData(HaxeSemanticError.KEY_ANALYZED_METHOD_TIME, file.getModificationStamp());
    }

    List<HaxeSemanticError> errors = element.getUserData(HaxeSemanticError.KEY_HAXE_ERROR);
    if (errors != null) {
      for (HaxeSemanticError error : errors) {
        Annotation annotation = holder.createErrorAnnotation(element, error.message);
        if (error.quickfix != null) annotation.registerFix(error.quickfix);
      }
    }
  }


  static void analyze(final PsiElement element) {
    HaxeSemanticError.clearErrors(element);

    if (element instanceof PsiJavaToken) {
      if (((PsiJavaToken)element).getTokenType() == HaxeTokenTypes.LITINT) {
        //element.putUserData(KEY_HAXE_ERROR, "litint!");
      }
    }
    else if (element instanceof HaxePackageStatement) {
      PackageChecker.check((HaxePackageStatement)element);
    }
    else if (element instanceof HaxeMethod) {
      HaxeClass clazz = HaxePsiUtils.getAncestor(element, HaxeClass.class);
      final HaxeDeclarationAttribute overrideAttribute = HaxePsiUtils.getChild(element, HaxeDeclarationAttribute.class, "override");
      HaxeClassModel parentClass = clazz.getModel().getExtendingClass();
      boolean requiredOverride = false;

      if (parentClass != null) {
        HaxeMethodModel method = parentClass.getMethod(((HaxeMethod)element).getModel().getName());
        if (method != null) {
          requiredOverride = true;
        }
      }

      //System.out.println(aClass);
      if (overrideAttribute != null && !requiredOverride) {
        HaxeSemanticError.addError(
          element,
          new HaxeSemanticError(
            "Overriding nothing",
            new HaxeSemanticIntentionAction("Fix override") {
              @Override
              public void run() {
                HaxePsiUtils.replaceElementWithText(overrideAttribute, "");
              }
            }
          )
        );
      } else if (overrideAttribute == null && requiredOverride) {
        HaxeSemanticError.addError(
          element,
          new HaxeSemanticError(
            "Must override"
          )
        );
      }
      //PackageChecker.check((HaxePackageStatement)element);
    }

    for (ASTNode node : element.getNode().getChildren(null)) {
      analyze(node.getPsi());
    }
  }
}

class PackageChecker {
  static public void check(final HaxePackageStatement element) {
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
        HaxeSemanticError.addError(element, new HaxeSemanticError("Package name '" + s + "' must start with a lower case character"));
      }
    }

    if (!packageName.equals(actualPackage)) {
      HaxeSemanticError.addError(
        element,
        new HaxeSemanticError(
          "Invalid package name! '" + packageName + "' should be '" + actualPackage + "'",
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
        )
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

class HaxeSemanticError {
  public String message;
  public IntentionAction quickfix;

  public HaxeSemanticError(String message, IntentionAction quickfix) {
    this.message = message;
    this.quickfix = quickfix;
  }

  public HaxeSemanticError(String message) {
    this(message, null);
  }

  static public Key<List<HaxeSemanticError>> KEY_HAXE_ERROR = new Key<List<HaxeSemanticError>>("KEY_HAXE_ERROR");
  static public Key<Long> KEY_ANALYZED_METHOD_TIME = new Key<Long>("HAXE_ANALYZED_METHOD");

  public static void clearErrors(PsiElement element) {
    element.putUserData(KEY_HAXE_ERROR, null);
  }

  public static void addError(PsiElement element, HaxeSemanticError error) {
    List<HaxeSemanticError> data = element.getUserData(KEY_HAXE_ERROR);
    if (data == null) {
      element.putUserData(KEY_HAXE_ERROR, new LinkedList<HaxeSemanticError>());
    }
    element.getUserData(KEY_HAXE_ERROR).add(error);
  }
}