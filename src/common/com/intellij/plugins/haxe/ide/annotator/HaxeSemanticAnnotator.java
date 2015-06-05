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
import com.intellij.lang.annotation.Annotation;
import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.Annotator;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.plugins.haxe.lang.lexer.HaxeTokenTypes;
import com.intellij.plugins.haxe.lang.psi.*;
import com.intellij.plugins.haxe.util.*;
import com.intellij.psi.*;
import com.intellij.util.IncorrectOperationException;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;

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
    final HaxeMethodModel currentMethod = methodPsi.getModel();
    final HaxeClassModel currentClass = currentMethod.getDeclaringClass();
    final HaxeModifiersModel currentModifiers = currentMethod.getModifiers();

    final HaxeClassModel parentClass = currentClass.getExtendingClass();
    final HaxeMethodModel parentMethod = (parentClass != null) ? parentClass.getMethod(currentMethod.getName()) : null;
    final HaxeModifiersModel parentModifiers = (parentMethod != null) ? parentMethod.getModifiers() : null;

    boolean requiredOverride = false;

    if (currentMethod.isConstructor()) {
      requiredOverride = false;
      if (currentModifiers.hasModifier(HaxeModifierType.STATIC)) {
        holder.createErrorAnnotation(currentMethod.getNameOrBasePsi(), "Constructor can't be static").registerFix(
          new HaxeSemanticIntentionAction("Remove static") {
            @Override
            public void run() {
              currentModifiers.removeModifier(HaxeModifierType.STATIC);
            }
          }
        );
      }
    } else if (currentMethod.isStaticInit()) {
      requiredOverride = false;
      if (!currentModifiers.hasModifier(HaxeModifierType.STATIC)) {
        holder.createErrorAnnotation(currentMethod.getNameOrBasePsi(), "__init__ must be static").registerFix(
          new HaxeSemanticIntentionAction("Add static") {
            @Override
            public void run() {
              currentModifiers.addModifier(HaxeModifierType.STATIC);
            }
          }
        );
      }
    }
    else if (parentMethod != null) {
      requiredOverride = true;

      if (parentModifiers.hasAnyModifier(HaxeModifierType.INLINE, HaxeModifierType.STATIC, HaxeModifierType.FINAL)) {
        Annotation annotation = holder.createErrorAnnotation(currentMethod.getNameOrBasePsi(), "Can't override static, inline or final methods");
        for (HaxeModifierType mod : new HaxeModifierType[] { HaxeModifierType.FINAL, HaxeModifierType.INLINE, HaxeModifierType.STATIC }) {
          if (parentModifiers.hasModifier(mod)) {
            annotation.registerFix(new RemoveModifierIntent("Remove parent " + mod.s, parentModifiers, mod));
          }
        }
      }

      if (currentModifiers.getVisibility() != parentModifiers.getVisibility()) {
        Annotation annotation = holder.createErrorAnnotation(currentMethod.getNameOrBasePsi(), "Method doesn't match parent's visibility");
        annotation.registerFix(
          new HaxeSemanticIntentionAction("Change current method visibility") {
            @Override
            public void run() {
              currentModifiers.replaceVisibility(parentModifiers.getVisibility());
            }
          }
        );
        annotation.registerFix(
          new HaxeSemanticIntentionAction("Change parent method visibility") {
            @Override
            public void run() {
              parentModifiers.replaceVisibility(currentModifiers.getVisibility());
            }
          }
        );
      }
    }

    //System.out.println(aClass);
    if (currentModifiers.hasModifier(HaxeModifierType.OVERRIDE) && !requiredOverride) {
      holder.createErrorAnnotation(currentModifiers.getModifierPsi(HaxeModifierType.OVERRIDE), "Overriding nothing").registerFix(
        new RemoveModifierIntent("Remove override", currentModifiers, HaxeModifierType.OVERRIDE)
      );
    } else if (!currentModifiers.hasModifier(HaxeModifierType.OVERRIDE) && requiredOverride) {
      holder.createErrorAnnotation(currentMethod.getNameOrBasePsi(), "Must override").registerFix(
        new HaxeSemanticIntentionAction("Add override") {
          @Override
          public void run() {
            currentModifiers.addModifier(HaxeModifierType.OVERRIDE);
          }
        }
      );
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

class RemoveModifierIntent extends HaxeSemanticIntentionAction {
  private HaxeModifiersModel modifiers;
  private HaxeModifierType modifierToRemove;

  public RemoveModifierIntent(String text, HaxeModifiersModel modifiers, HaxeModifierType modifierToRemove) {
    super(text);
    this.modifiers = modifiers;
    this.modifierToRemove = modifierToRemove;
  }

  @Override
  public void run() {
    modifiers.removeModifier(modifierToRemove);
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
