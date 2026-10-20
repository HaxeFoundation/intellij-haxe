package com.intellij.plugins.haxe.ide.annotator.semantics;

import com.intellij.codeInsight.intention.IntentionAction;
import com.intellij.codeInsight.intention.preview.IntentionPreviewInfo;
import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.Annotator;
import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.plugins.haxe.HaxeBundle;
import com.intellij.plugins.haxe.ide.completion.HaxeCompletionUtil;
import com.intellij.plugins.haxe.lang.lexer.HaxeTokenTypes;
import com.intellij.plugins.haxe.lang.psi.*;
import com.intellij.plugins.haxe.model.HaxeClassModel;
import com.intellij.plugins.haxe.model.HaxeClassReferenceModel;
import com.intellij.plugins.haxe.model.HaxeMethodModel;
import com.intellij.plugins.haxe.model.HaxeModifiersModel;
import com.intellij.plugins.haxe.model.fixer.HaxeFixer;
import com.intellij.plugins.haxe.model.fixer.HaxeModifierAddFixer;
import com.intellij.plugins.haxe.model.fixer.HaxeModifierRemoveFixer;
import com.intellij.plugins.haxe.model.fixer.HaxeSimpleFixer;
import com.intellij.plugins.haxe.model.type.HaxeMacroUtil;
import com.intellij.plugins.haxe.util.HaxeElementGenerator;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;

import java.util.List;

import static com.intellij.plugins.haxe.lang.psi.HaxePsiModifier.STATIC;

/**
 * The method-level LANGUAGE errors (mirroring the compiler, never opinions):
 * overload on non-abstract constructors, super-constructor requirements, and
 * static-modifier rules for constructors and {@code __init__}. Everything
 * togglable lives in the method-shaped inspections.
 */
public class HaxeMethodAnnotator implements Annotator {

  @Override
  public void annotate(@NotNull PsiElement element, @NotNull AnnotationHolder holder) {
    if (AnnotatorUtil.shouldSkip(element)) return;

    if (element instanceof HaxeMethod haxeMethod) {
      checkStaticModifiers(haxeMethod, holder);
      checkOverload(haxeMethod, holder);
      checkConstructorSuper(haxeMethod, holder);
    }
  }

  private static void checkStaticModifiers(HaxeMethod methodPsi, AnnotationHolder holder) {
    final HaxeMethodModel currentMethod = methodPsi.getModel();
    final HaxeModifiersModel currentModifiers = currentMethod.getModifiers();
    if (methodPsi instanceof HaxeLocalFunctionDeclaration) return;

    if (currentMethod.isConstructor() && currentModifiers.hasModifier(STATIC)) {
      String errorMessage = HaxeBundle.message("haxe.semantic.constructor.cannot.be.static");
      holder.newAnnotation(HighlightSeverity.ERROR, errorMessage).range(currentMethod.getNameOrBasePsi())
        .withFix(new HaxeModifierRemoveFixer(currentModifiers, STATIC))
        .create();
    }
    else if (currentMethod.isStaticInit() && !currentModifiers.hasModifier(STATIC)) {
      holder.newAnnotation(HighlightSeverity.ERROR, "__init__ must be static").range(currentMethod.getNameOrBasePsi())
        .withFix(new HaxeModifierAddFixer(currentModifiers, STATIC))
        .create();
    }
  }

  private static void checkOverload(HaxeMethod methodPsi, AnnotationHolder holder) {
    if(methodPsi.isConstructor() && methodPsi.isOverload()) {
      HaxeClassModel declaringClass = methodPsi.getModel().getDeclaringClass();
      if(declaringClass != null && !declaringClass.isAbstractType()) { // inline overload allowed for abstracts
        holder.newAnnotation(HighlightSeverity.ERROR, HaxeBundle.message("haxe.semantic.invalid.modifier.overload.constructor"))
                .range(methodPsi.getModiferPsi(HaxeTokenTypes.KOVERLOAD))
                .create();
      }
    }
  }

  private static void checkConstructorSuper(HaxeMethod methodPsi, AnnotationHolder holder) {
    final HaxeMethodModel currentMethod = methodPsi.getModel();
    HaxeSuperExpression superExpression = PsiTreeUtil.findChildOfType(methodPsi, HaxeSuperExpression.class);
    if(currentMethod.isConstructor()) {
      HaxeClassModel declaringClass = currentMethod.getDeclaringClass();
      if (declaringClass != null) {
        // extern classes does not need implementation, thats also true when extern classes extend extern classes.
        if(currentMethod.getBodyPsi() == null && currentMethod.getDeclaringClass().isExtern()) {
          return;
        }
        if (declaringClass.isClass()) {
          if(HaxeMacroUtil.isInMacroExpression(superExpression)) return;
          List<HaxeClassReferenceModel> extendingTypes = declaringClass.getExtendingTypes();
          if (extendingTypes.isEmpty()) {
            if (superExpression != null && superExpression.getParent() instanceof HaxeCallExpression callExpression) {

              holder.newAnnotation(HighlightSeverity.ERROR, "Current class does not have a super")
                      .range(callExpression)
                      .withFix(createRemoveSuperFix(callExpression))
                      .create();
            }
          } else {
            if (superExpression == null) {
              // only expect one extends when class (interfaces can have multiple)
              HaxeClassReferenceModel first = extendingTypes.getFirst();
              HaxeClassModel baseClass = first.getHaxeClassModel();
              if (baseClass != null) {
                HaxeMethodModel constructor = baseClass.getConstructor(null);
                // super is not required if there is no  constructor in base class
                if (constructor != null) {
                  holder.newAnnotation(HighlightSeverity.ERROR, "Missing super constructor call")
                          .range(currentMethod.getNamePsi())
                          .withFix(createAddSuperFix(currentMethod))
                          .create();
                }
              }
            }
          }
        }
      }
    }
  }

   static HaxeFixer createRemoveSuperFix(HaxeCallExpression callExpression) {
    return new HaxeFixer(HaxeBundle.message("haxe.inspections.remove.super")) {
      @Override
      public void run() {
        if(callExpression.isValid()) {
          PsiElement possibleSemi = callExpression.getNextSibling();
          if(possibleSemi.textMatches(";"))possibleSemi.delete();
          callExpression.delete();
        }
      }
    };
  }
   static IntentionAction createAddSuperFix(HaxeMethodModel methodModel) {
     return new HaxeSimpleFixer(HaxeBundle.message("haxe.inspections.insert.super")) {
       @Override
       public void invoke(@NotNull Project project, Editor editor, PsiFile file) throws IncorrectOperationException {
         insertSuper(editor, file, methodModel.getBodyPsi());
       }

       @Override
       public @NotNull IntentionPreviewInfo generatePreview(@NotNull Project project, @NotNull Editor editor, @NotNull PsiFile file) {
         PsiElement body = PsiTreeUtil.findSameElementInCopy(methodModel.getBodyPsi(), file);
         insertSuper(editor, file, body);
         return IntentionPreviewInfo.DIFF;
       }
       private void insertSuper(Editor editor, PsiFile file, PsiElement bodyPsi) {
         if (bodyPsi.isValid()) {
           HaxeCallExpression superExpression = (HaxeCallExpression)HaxeElementGenerator.createStatementFromText(bodyPsi.getProject(), "super()");
           PsiElement semi = HaxeElementGenerator.createSemi(bodyPsi.getProject());
           PsiElement firstChild = bodyPsi.getFirstChild();
           superExpression = (HaxeCallExpression) bodyPsi.addAfter(superExpression, firstChild);
           if (firstChild == null) {
             bodyPsi.addAfter(semi, superExpression);
           } else if (firstChild.textMatches("{")) {
             PsiElement newLine = HaxeElementGenerator.createNewLine(bodyPsi.getProject());
             bodyPsi.addAfter(semi, superExpression);
             bodyPsi.addBefore(newLine, superExpression);
           }
           editor.getCaretModel().moveToOffset(superExpression.getTextRange().getEndOffset()-1);
           HaxeCompletionUtil.reformatAndAdjustIndent(file, editor, superExpression.getTextRange());

         }
       }
     };
   }
}
