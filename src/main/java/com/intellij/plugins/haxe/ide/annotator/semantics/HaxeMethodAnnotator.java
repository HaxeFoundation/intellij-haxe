package com.intellij.plugins.haxe.ide.annotator.semantics;

import com.intellij.codeInsight.intention.IntentionAction;
import com.intellij.codeInsight.intention.preview.IntentionPreviewInfo;
import com.intellij.lang.annotation.*;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.plugins.haxe.HaxeBundle;
import com.intellij.plugins.haxe.ide.completion.HaxeCompletionUtil;
import com.intellij.plugins.haxe.lang.lexer.HaxeTokenTypes;
import com.intellij.plugins.haxe.lang.psi.*;
import com.intellij.plugins.haxe.model.*;
import com.intellij.plugins.haxe.model.evaluator.assign.HaxeFunctionCompatible;
import com.intellij.plugins.haxe.model.evaluator.assign.HaxeOverrideOrImplementEvaluation;
import com.intellij.plugins.haxe.model.fixer.*;
import com.intellij.plugins.haxe.model.type.HaxeMacroUtil;
import com.intellij.plugins.haxe.util.HaxeElementGenerator;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.IncorrectOperationException;
import lombok.CustomLog;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;

import static com.intellij.plugins.haxe.ide.annotator.HaxeSemanticAnnotatorInspections.*;
import static com.intellij.plugins.haxe.ide.annotator.semantics.AnnotatorUtil.hasMacroForCodeGeneration;
import static com.intellij.plugins.haxe.lang.psi.HaxePsiModifier.*;
import static com.intellij.plugins.haxe.lang.psi.HaxePsiModifier.OVERRIDE;

@CustomLog
public class HaxeMethodAnnotator implements Annotator {

  public static final String DEFAULT_ARG_NAME = "_";

  @Override
  public void annotate(@NotNull PsiElement element, @NotNull AnnotationHolder holder) {
    if(!element.isValid()) return;

    if (element instanceof HaxeMethod haxeMethod) {
        check(haxeMethod, holder);
      }
  }
  static public void check(final HaxeMethod methodPsi, final AnnotationHolder holder) {
    final HaxeMethodModel currentMethod = methodPsi.getModel();
    checkTypeTagInInterfacesAndExternClass(currentMethod, holder);
    checkMethodArguments(currentMethod, holder);
    checkOverride(methodPsi, holder);
    checkOverload(methodPsi, holder);
    checkConstructorSuper(methodPsi, holder);
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

  private static void checkTypeTagInInterfacesAndExternClass(final HaxeMethodModel currentMethod, final AnnotationHolder holder) {
    if (!MISSING_TYPE_TAG_ON_EXTERN_AND_INTERFACE.isEnabled(currentMethod.getBasePsi())) return;

    HaxeClassModel currentClass = currentMethod.getDeclaringClass();
    if (currentClass != null) { //make sure it's not a module method
      if (currentClass.isExtern() || currentClass.isInterface()) {
        if (currentMethod.getReturnTypeTagPsi() == null && !currentMethod.isConstructor()) {
          holder.newAnnotation(HighlightSeverity.ERROR, HaxeBundle.message("haxe.semantic.type.required"))
            .range(currentMethod.getNameOrBasePsi())
            .create();
        }
        for (final HaxeParameterModel param : currentMethod.getParameters()) {
          if (param.getTypeTagPsi() == null) {
            holder.newAnnotation(HighlightSeverity.ERROR, HaxeBundle.message("haxe.semantic.type.required"))
              .range(param.getBasePsi())
              .create();
          }
        }
      }
    }
  }

  private static void checkMethodArguments(final HaxeMethodModel currentMethod, final AnnotationHolder holder) {
    PsiElement methodPsi = currentMethod.getBasePsi();
    boolean checkParameterInitializers = PARAMETER_INITIALIZER_TYPES.isEnabled(methodPsi);
    boolean checkRepeatedParameterName = REPEATED_PARAMETER_NAME_CHECK.isEnabled(methodPsi);

    if (!checkParameterInitializers
        && !checkRepeatedParameterName) {
      return;
    }

    HashMap<String, PsiElement> argumentNames = new HashMap<String, PsiElement>();
    for (final HaxeParameterModel param : currentMethod.getParameters()) {
      String paramName = param.getName();


      if (checkParameterInitializers) {
        HaxeVarInit varInitPsi = param.getVarInitPsi();
        HaxeTypeTag typeTagPsi = param.getTypeTagPsi();
        if (varInitPsi != null) {
          checkConstExpression(varInitPsi, holder);
          if (typeTagPsi != null) {
            HaxeSemanticsUtil.TypeTagChecker.check(param.getBasePsi(), typeTagPsi, varInitPsi, true, holder);
          }
        }
      }

      if (checkRepeatedParameterName) {
        if (argumentNames.containsKey(paramName) && !paramName.equals(DEFAULT_ARG_NAME)) {
          String warningMessage = HaxeBundle.message("haxe.semantic.repeated.argument.name", paramName);
          holder.newAnnotation(HighlightSeverity.WARNING, warningMessage).range(param.getNamePsi()).create();
          holder.newAnnotation(HighlightSeverity.WARNING, warningMessage).range(argumentNames.get(paramName)).create();
        }
        else {
          argumentNames.put(paramName, param.getNamePsi());
        }
      }
    }
  }

  private static void checkConstExpression(HaxeVarInit varInitPsi, AnnotationHolder holder) {
    HaxeExpression expression = varInitPsi.getExpression();
    checkConstExpression(holder, expression);

  }

  private static void checkConstExpression(AnnotationHolder holder, PsiElement expression) {
    if (expression instanceof HaxeConstantExpression) return;
    if (expression instanceof HaxeArrayLiteral
        || expression instanceof HaxeMapLiteral
        || expression instanceof HaxeObjectLiteral) {
      annotateNotConstant(expression, holder);

    } else if (expression instanceof HaxeCallExpression ) {
      annotateNotConstant(expression, holder);

    } else if (expression instanceof HaxeParenthesizedExpression parenthesizedExpression) {
      Collection<HaxeExpression> children = PsiTreeUtil.findChildrenOfAnyType(parenthesizedExpression,
              HaxeParenthesizedExpression.class,
              HaxeReferenceExpression.class,
              HaxeArrayLiteral.class,
              HaxeMapLiteral.class,
              HaxeObjectLiteral.class);

      for (HaxeExpression haxeExpression : children) {
        checkConstExpression(holder, haxeExpression);
      }


    } else if (expression instanceof HaxeReferenceExpression referenceExpression) {
      PsiElement resolve = referenceExpression.resolve();
      if (resolve instanceof HaxeEnumValueDeclaration) return;
      if (resolve instanceof HaxePsiField field ) {
        if( field.isInline())return;
        PsiClass containingClass = field.getContainingClass();
        if(containingClass != null && containingClass.isEnum()){
          // make sure its not a property if in abstract enum
          if(field instanceof HaxeFieldDeclaration declaration
             && declaration.getPropertyDeclaration() == null) return;
        }
      }
      annotateNotConstant(expression, holder);
    }
  }

  private static void annotateNotConstant(PsiElement element, AnnotationHolder holder) {
    holder.newAnnotation(HighlightSeverity.ERROR, "Default argument value should be constant")
            .range(element)
            .create();
  }

  private static final String[] OVERRIDE_FORBIDDEN_MODIFIERS = {FINAL, INLINE, STATIC};

  private static void checkOverride(final HaxeMethod methodPsi, final AnnotationHolder holder) {
    final HaxeMethodModel currentMethod = methodPsi.getModel();
    final HaxeClassModel currentClass = currentMethod.getDeclaringClass();
    final HaxeModifiersModel currentModifiers = currentMethod.getModifiers();

    final HaxeMethodModel parentMethod = currentClass != null ? currentClass.getAncestorMethod(currentMethod.getName(), null) : null;
    final HaxeClassModel parentClass = parentMethod != null ? parentMethod.getDeclaringClass() : null;
    final HaxeModifiersModel parentModifiers = (parentMethod != null) ? parentMethod.getModifiers() : null;

    // ignore local functions
    if (methodPsi instanceof HaxeLocalFunctionDeclaration) return;

    if (!METHOD_OVERRIDE_CHECK.isEnabled(methodPsi)) { // TODO: This check is not granular enough.
      // If the rest of the checks are disabled, we don't want to inhibit the signature check.
      if (null != parentMethod) {
        checkMethodsSignatureCompatibility(currentMethod, parentMethod, holder, true);
      }
      return;
    }

    boolean requiredOverride = false;

    if (currentMethod.isConstructor()) {
      if (currentModifiers.hasModifier(STATIC)) {
        String errorMessage = HaxeBundle.message("haxe.semantic.constructor.cannot.be.static");
        holder.newAnnotation(HighlightSeverity.ERROR, errorMessage).range(currentMethod.getNameOrBasePsi())
        .withFix(new HaxeModifierRemoveFixer(currentModifiers, STATIC))
        .create();
      }
    }
    else if (currentMethod.isStaticInit()) {
      if (!currentModifiers.hasModifier(STATIC)) {
        holder.newAnnotation(HighlightSeverity.ERROR, "__init__ must be static").range(currentMethod.getNameOrBasePsi())
        .withFix(new HaxeModifierAddFixer(currentModifiers, STATIC))
        .create();
      }
    }
    else if (parentMethod != null) {
      if (parentMethod.isStatic()) {
        holder.newAnnotation(HighlightSeverity.WEAK_WARNING, "Method '" + currentMethod.getName()
                                                        + "' shadows a static method of a superclass")
          .range(currentMethod.getNameOrBasePsi())
          .create();
      }
      else {
        if (!currentClass.isInterface()
            && !currentClass.isAnonymous()
            && !parentMethod.isAbstract()
            && !parentClass.isInterface()) {
          requiredOverride = true;
        }

        if (parentModifiers.hasAnyModifier(OVERRIDE_FORBIDDEN_MODIFIERS) && !parentClass.isInterface()) {
          AnnotationBuilder builder = holder.newAnnotation(HighlightSeverity.ERROR, "Can't override static, inline or final methods")
           .range(currentMethod.getNameOrBasePsi());

          for (String modifier : OVERRIDE_FORBIDDEN_MODIFIERS) {
            if (parentModifiers.hasModifier(modifier)) {
              builder.withFix(
                new HaxeModifierRemoveFixer(parentModifiers, modifier, "Remove " + modifier + " from " + parentMethod.getFullName())
              );
            }
          }
          builder.create();
        }
        // ignore if empty (override inherits from parent)
        if(!currentModifiers.getVisibility().equals(EMPTY)) {
          if (HaxePsiModifier.hasLowerVisibilityThan(currentModifiers.getVisibility(), parentModifiers.getVisibility())) {
            HaxeModifierReplaceVisibilityFixer changeCurrentVisibilityFix = new HaxeModifierReplaceVisibilityFixer(currentModifiers, parentModifiers.getVisibility(), "Change current method visibility to '"+parentModifiers.getVisibility()+"'");
            HaxeModifierReplaceVisibilityFixer changeParentVisibilityFix = new HaxeModifierReplaceVisibilityFixer(parentModifiers, currentModifiers.getVisibility(), "Change parent method visibility '"+currentModifiers.getVisibility()+"'");
            holder.newAnnotation(HighlightSeverity.ERROR, "Field " +
                                                            currentMethod.getName() +
                                                            " has less visibility (public/private) than superclass one.")
                    .range(currentMethod.getNameOrBasePsi())
                    .withFix(changeCurrentVisibilityFix)
                    .withFix(changeParentVisibilityFix)
                    .create();
          }
        }else {
          if (HaxePsiModifier.hasLowerVisibilityThan(currentModifiers.getVisibility(), parentModifiers.getVisibility())) {
            HaxeModifierReplaceVisibilityFixer changeCurrentVisibilityFix = new HaxeModifierReplaceVisibilityFixer(currentModifiers, parentModifiers.getVisibility(), "Add current method visibility to '"+parentModifiers.getVisibility()+"'");
            HaxeModifierReplaceVisibilityFixer changeParentVisibilityFix = new HaxeModifierReplaceVisibilityFixer(parentModifiers, currentModifiers.getVisibility(), "Add parent method visibility '"+currentModifiers.getVisibility()+"'");
            holder.newAnnotation(HighlightSeverity.WEAK_WARNING, "Field " +
                                                                 currentMethod.getName() +
                                                                 " has no visibility modifier but overrides parent with '" +
                                                                 parentModifiers.getVisibility() + "'")
                    .range(currentMethod.getNameOrBasePsi())
                    .withFix(changeCurrentVisibilityFix)
                    .withFix(changeParentVisibilityFix)
                    .create();
          }
        }
      }
    }

    //System.out.println(aClass);
    if (currentModifiers.hasModifier(OVERRIDE) && !requiredOverride) {
      if (!hasMacroForCodeGeneration(currentMethod.getDeclaringClass())) {
        holder.newAnnotation(HighlightSeverity.ERROR, "Overriding nothing").range(currentModifiers.getModifierPsi(OVERRIDE))
          .withFix(new HaxeModifierRemoveFixer(currentModifiers, OVERRIDE))
          .create();
      }
    }
    else if (requiredOverride) {
      if (!currentModifiers.hasModifier(OVERRIDE)) {
        if (hasMacroForCodeGeneration(currentMethod.getDeclaringClass())) {
          holder.newAnnotation(HighlightSeverity.WEAK_WARNING, "Positionally missing override")
            .range(currentMethod.getNameOrBasePsi())
            .withFix(new HaxeModifierAddFixer(currentModifiers, OVERRIDE))
            .create();
        } else {
          holder.newAnnotation(HighlightSeverity.ERROR, "Must override").range(currentMethod.getNameOrBasePsi())
            .withFix(new HaxeModifierAddFixer(currentModifiers, OVERRIDE))
            .create();
        }
      }

      else {
        // It is rightly overriden. Now check the signature.
//        checkMethodsSignatureCompatibility(currentMethod, parentMethod, holder);
        checkMethodsSignatureCompatibility(currentMethod, parentMethod, holder, true);
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

  static boolean checkMethodsSignatureCompatibility(
    @NotNull final HaxeMethodModel currentMethod,
    @NotNull final HaxeMethodModel parentMethod ) {
    return checkMethodsSignatureCompatibility(currentMethod,parentMethod, null, false);
  }

  static boolean checkMethodsSignatureCompatibility(
    @NotNull final HaxeMethodModel currentMethod,
    @NotNull final HaxeMethodModel parentMethod,
    final AnnotationHolder holder,
    boolean shouldAnnotate) {

    if (!METHOD_SIGNATURE_COMPATIBILITY.isEnabled(currentMethod.getBasePsi())) return true;

    if (parentMethod.isInInterface() && !currentMethod.isInInterface()) {
      HaxeOverrideOrImplementEvaluation implementCheck = HaxeFunctionCompatible.checkThisImplementsThat(currentMethod,parentMethod, shouldAnnotate);
      implementCheck.annotate(holder);
      return implementCheck.result;

    } else {
      HaxeOverrideOrImplementEvaluation overrideCheck = HaxeFunctionCompatible.checkThisOverrideThat( currentMethod,parentMethod, shouldAnnotate);
      overrideCheck.annotate(holder);
      return overrideCheck.result;
    }
  }
}