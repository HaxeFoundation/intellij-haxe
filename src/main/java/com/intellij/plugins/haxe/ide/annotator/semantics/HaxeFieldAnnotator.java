package com.intellij.plugins.haxe.ide.annotator.semantics;

import com.intellij.lang.annotation.AnnotationBuilder;
import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.Annotator;
import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.plugins.haxe.HaxeBundle;
import com.intellij.plugins.haxe.ide.quickfix.CreateGetterSetterQuickfix;
import com.intellij.plugins.haxe.ide.quickfix.HaxeSwitchMutabilityModifier;
import com.intellij.plugins.haxe.lang.psi.*;
import com.intellij.plugins.haxe.model.*;
import com.intellij.plugins.haxe.model.fixer.HaxeFixer;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;

import static com.intellij.plugins.haxe.ide.annotator.HaxeSemanticAnnotatorInspections.*;
import static com.intellij.plugins.haxe.ide.annotator.semantics.AnnotatorUtil.hasMacroForCodeGeneration;
import static com.intellij.plugins.haxe.lang.psi.HaxePsiModifier.IS_VAR;

public class HaxeFieldAnnotator implements Annotator {
  @Override
  public void annotate(@NotNull PsiElement element, @NotNull AnnotationHolder holder) {
    if (element instanceof HaxeFieldDeclaration field) {
      check(field, holder);
    }
  }

  public static void check(final HaxeFieldDeclaration var, final AnnotationHolder holder) {
    HaxeFieldModel field = (HaxeFieldModel)var.getModel();
    if (field.isProperty()) {
      checkProperty(field, holder);
    }
    else {
      if (FINAL_FIELD_IS_INITIALIZED.isEnabled(var)) {
        if (field.isFinal() && !field.isExtern()) {
          if (field.getDeclaringClass() == null || !field.getDeclaringClass().isExtern()) {
            if (!field.hasInitializer()) {
              if (!isParentInterface(var) && !isParentAnonymousStructure(var) && !isParentAbstractEnum(var)) {
                if (field.isStatic()) {
                  holder.newAnnotation(HighlightSeverity.ERROR, HaxeBundle.message("haxe.semantic.final.static.var.init", field.getName()))
                    .range(var)
                    .create();
                }
                else if (!isFieldInitializedInTheConstructor(field) && !field.getDeclaringClass().isStructInit()) {
                  holder.newAnnotation(HighlightSeverity.ERROR, HaxeBundle.message("haxe.semantic.final.var.init", field.getName()))
                    .range(var)
                    .create();
                }
              }
            }
            else {
              if (isParentInterface(var)) {
                holder.newAnnotation(HighlightSeverity.ERROR,
                                     HaxeBundle.message("haxe.semantic.final.static.var.init.interface", field.getName()))
                  .range(var)
                  .create();
              }
            }
          }
        }
      }
    }

    if (field.hasInitializer() && field.hasTypeTag()) {
      HaxeSemanticsUtil.TypeTagChecker.check(field.getBasePsi(), field.getTypeTagPsi(), field.getInitializerPsi(), false, holder);
    } else if (!field.hasInitializer() && !field.hasTypeTag()) {
      HaxeClassModel declaringClass = field.getDeclaringClass();
      //NOTE: abstract enums can have finals without init or typeHint
      if(declaringClass != null  && !declaringClass.isEnum()) {
        holder.newAnnotation(HighlightSeverity.ERROR, HaxeBundle.message("haxe.semantic.var.init.or.type.hint", field.getName()))
                .range(var)
                .create();
      }
    }


    // Checking for variable redefinition.
    if (FIELD_REDEFINITION.isEnabled(var)) {
      HashSet<HaxeClassModel> classSet = new HashSet<>();
      HaxeClassModel fieldDeclaringClass = field.getDeclaringClass();
      // TODO  add support for module as parent ?
      if (fieldDeclaringClass == null) return;
      if (fieldDeclaringClass.isInterface() || fieldDeclaringClass.isAnonymous()) {
        return;
      }
      classSet.add(fieldDeclaringClass);
      while (fieldDeclaringClass != null ) {
        fieldDeclaringClass = fieldDeclaringClass.getParentClass();
        if (classSet.contains(fieldDeclaringClass)) {
          break;
        }
        else {
          if (fieldDeclaringClass != null) {
            if (!fieldDeclaringClass.isInterface() && !fieldDeclaringClass.isAnonymous()) {
              classSet.add(fieldDeclaringClass);
            }
          }
        }
        if (fieldDeclaringClass != null) {
          for (HaxeFieldModel parentField : fieldDeclaringClass.getFields()) {
            if (parentField.getName().equals(field.getName())) {
              String message;
              if (parentField.isStatic()) {
                message = HaxeBundle.message("haxe.semantic.static.field.override", field.getName());
                holder.newAnnotation(HighlightSeverity.WEAK_WARNING, message)
                  .range(field.getNameOrBasePsi())
                  .create();
              }
              else {
                if (hasMacroForCodeGeneration(field.getDeclaringClass())) {
                  message = HaxeBundle.message("haxe.semantic.variable.redefinition.possibly", field.getName(), fieldDeclaringClass.getName());
                  message += HaxeBundle.message("haxe.semantic.macro.generated");
                  holder.newAnnotation(HighlightSeverity.WEAK_WARNING, message)
                    .range(field.getBasePsi())
                    .create();
                }
                else {
                  message = HaxeBundle.message("haxe.semantic.variable.redefinition", field.getName(), fieldDeclaringClass.getName());
                  holder.newAnnotation(HighlightSeverity.ERROR, message)
                    .range(field.getBasePsi())
                    .create();
                }
              }
              break;
            }
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

  private static void checkProperty(final HaxeFieldModel field, final AnnotationHolder holder) {
    final HaxeDocumentModel document = field.getDocument();

    PsiElement fieldBasePsi = field.getBasePsi();
    if (PROPERTY_ACCESSOR_VALID.isEnabled(fieldBasePsi)) {
// TODO: Bug here.  (set,get) are being marked as errors.
      if (field.getGetterPsi() != null && !field.getGetterType().isValidGetter()) {
        holder.newAnnotation(HighlightSeverity.ERROR, "Invalid getter accessor")
          .range(field.getGetterPsi())
          .create();
      }

      if (field.getSetterPsi() != null && !field.getSetterType().isValidSetter()) {
        holder.newAnnotation(HighlightSeverity.ERROR, "Invalid setter accessor")
          .range(field.getSetterPsi())
          .create();
      }
    }

    if (field.isFinal()) {
      if (PROPERTY_CANNOT_BE_FINAL.isEnabled(fieldBasePsi)) {
        holder.newAnnotation(HighlightSeverity.ERROR, HaxeBundle.message("haxe.semantic.property.cant.be.final"))
          .range(fieldBasePsi)
          .withFix(new HaxeSwitchMutabilityModifier((HaxeFieldDeclaration)fieldBasePsi))
          .create();
      }
    }
    else {
      if (PROPERTY_IS_NOT_REAL_VARIABLE.isEnabled(fieldBasePsi)) {
        final HaxeVarInit initializerPsi = field.getInitializerPsi();
        if (field.isProperty() && !field.isRealVar() && null != initializerPsi) {

          AnnotationBuilder builder =
            holder.newAnnotation(HighlightSeverity.ERROR, "This field cannot be initialized because it is not a real variable")
              .range(initializerPsi)
              .withFix(removeInitFix(document, initializerPsi))
              .withFix(addIsVarFix(field));

          if (field.getSetterPsi() != null) {
            builder.withFix(makeSetterNullFix(field, document));
          }

          builder.create();
        }
      }
    }
    checkPropertyAccessorMethods(field, holder);
  }

  @NotNull
  private static HaxeFixer makeSetterNullFix(HaxeFieldModel field, HaxeDocumentModel document) {
    return new HaxeFixer("Make setter null") {
      @Override
      public void run() {
        document.replaceElementText(field.getSetterPsi(), "null");
      }
    };
  }

  @NotNull
  private static HaxeFixer addIsVarFix(HaxeFieldModel field) {
    return new HaxeFixer("Add @:isVar") {
      @Override
      public void run() {
        field.getModifiers().addModifier(IS_VAR);
      }
    };
  }

  @NotNull
  private static HaxeFixer removeInitFix(HaxeDocumentModel document, HaxeVarInit initializerPsi) {
    return new HaxeFixer("Remove init") {
      @Override
      public void run() {
        document.replaceElementText(initializerPsi, "", StripSpaces.BEFORE);
      }
    };
  }

  private static void checkPropertyAccessorMethods(final HaxeFieldModel field, final AnnotationHolder holder) {
    if (!PROPERTY_ACCESSOR_EXISTENCE.isEnabled(field.getBasePsi())) {
      return;
    }

    HaxeClassModel declaringClass = field.getDeclaringClass();
    if(declaringClass != null) {
      if (declaringClass.isInterface() || declaringClass.isAnonymous() || declaringClass.isExtern()) return;
    }


    HaxeCommonMembersModel membersModel = declaringClass != null ? declaringClass : field.getDeclaringModule();

    if (field.getGetterType() == HaxeAccessorType.GET) {
      HaxeMethodModel getterMethod = field.getGetterMethod();
      if (getterMethod == null && field.getGetterPsi() != null) {
        holder.newAnnotation(HighlightSeverity.ERROR, "Can't find getter method")
          .range(field.getGetterPsi())
          .withFix(new CreateGetterSetterQuickfix(membersModel, field, true))
          .create();
      }
    }

    if (field.getSetterType() == HaxeAccessorType.SET) {
      HaxeMethodModel setterMethod = field.getSetterMethod();
      if (setterMethod == null && field.getSetterPsi() != null) {
        holder.newAnnotation(HighlightSeverity.ERROR, "Can't find setter method")
          .range(field.getSetterPsi())
          .withFix(new CreateGetterSetterQuickfix(membersModel, field, false))
          .create();
      }
    }
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
      HaxeExpression expression = (o.getExpressionList()).get(0);
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
