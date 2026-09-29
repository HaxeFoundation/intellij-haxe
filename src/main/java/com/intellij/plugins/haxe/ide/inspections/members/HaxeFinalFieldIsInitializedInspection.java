package com.intellij.plugins.haxe.ide.inspections.members;

import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.plugins.haxe.lang.psi.HaxeFieldDeclaration;
import com.intellij.psi.PsiElementVisitor;
import org.jetbrains.annotations.NotNull;
import com.intellij.plugins.haxe.ide.annotator.HaxeProblemReporter;
import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.plugins.haxe.HaxeBundle;
import com.intellij.plugins.haxe.lang.psi.*;
import com.intellij.plugins.haxe.model.*;
import com.intellij.psi.PsiElement;
import com.intellij.plugins.haxe.ide.inspections.HaxeInspection;

/** final fields must be initialized in place or in every constructor. */
public class HaxeFinalFieldIsInitializedInspection extends HaxeInspection {

  @Override
  public @NotNull PsiElementVisitor buildVisitor(@NotNull ProblemsHolder holder, boolean isOnTheFly) {
    return checkVisitor(holder, HaxeFieldDeclaration.class, HaxeFinalFieldIsInitializedInspection::checkFinalFieldInitialized);
  }
  public static void checkFinalFieldInitialized(final HaxeFieldDeclaration var, final HaxeProblemReporter reporter) {
    HaxeFieldModel field = (HaxeFieldModel)var.getModel();
    if (field.isProperty()) return;
    if (field.isFinal() && !field.isExtern()) {
      if (field.getDeclaringClass() == null || !field.getDeclaringClass().isExtern()) {
        if (!field.hasInitializer()) {
          if (!isParentInterface(var) && !isParentAnonymousStructure(var) && !isParentAbstractEnum(var)) {
            if (field.isStatic()) {
              reporter.problem(HighlightSeverity.ERROR, HaxeBundle.message("haxe.semantic.final.static.var.init", field.getName()))
                .range(var)
                .create();
            }
            else if (!isFieldInitializedInTheConstructor(field) && !field.getDeclaringClass().isStructInit()) {
              reporter.problem(HighlightSeverity.ERROR, HaxeBundle.message("haxe.semantic.final.var.init", field.getName()))
                .range(var)
                .create();
            }
          }
        }
        else {
          if (isParentInterface(var)) {
            reporter.problem(HighlightSeverity.ERROR,
                             HaxeBundle.message("haxe.semantic.final.static.var.init.interface", field.getName()))
              .range(var)
              .create();
          }
        }
      }
    }
  }

  private static boolean isParentInterface(HaxeFieldDeclaration var) {
    return var.getParent() instanceof HaxeInterfaceBody;
  }

  private static boolean isParentAnonymousStructure(HaxeFieldDeclaration var) {
    return var.getParent() instanceof HaxeAnonymousTypeBody;
  }

  private static boolean isParentAbstractEnum(HaxeFieldDeclaration var) {
    if(var.getParent().getParent() instanceof HaxeAbstractTypeDeclaration declaration) {
     return declaration.isEnum();
    }
    return  false;
  }

  private static boolean isFieldInitializedInTheConstructor(HaxeFieldModel field) {
    HaxeClassModel declaringClass = field.getDeclaringClass();
    if (declaringClass == null) return false;
    HaxeMethodModel constructor = declaringClass.getConstructor(null);
    if (constructor == null) return false;
    PsiElement body = constructor.getBodyPsi();
    if (body == null) return false;

    final InitVariableVisitor visitor = new InitVariableVisitor(field.getName());
    body.accept(visitor);
    return visitor.result;
  }

  static class InitVariableVisitor extends HaxeVisitor {
    public boolean result = false;

    private final String fieldName;

    InitVariableVisitor(String fieldName) {
      this.fieldName = fieldName;
    }

    @Override
    public void visitElement(PsiElement element) {
      super.visitElement(element);
      if (result) return;
      if (element instanceof HaxeIdentifier || element instanceof HaxePsiToken || element instanceof HaxeStringLiteralExpression) return;
      element.acceptChildren(this);
    }

    @Override
    public void visitAssignExpression(@NotNull HaxeAssignExpression o) {
      HaxeExpression expression = o.getExpressionList().getFirst();
      if (expression instanceof HaxeReferenceExpression reference) {
        final HaxeIdentifier identifier = reference.getIdentifier();

        if (identifier.textMatches(fieldName)) {
          PsiElement firstChild = reference.getFirstChild();
          if (firstChild instanceof HaxeThisExpression || firstChild == identifier) {
            this.result = true;
            return;
          }
        }
      }

      super.visitAssignExpression(o);
    }
  }

}
