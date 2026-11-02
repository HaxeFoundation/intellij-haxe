package com.intellij.plugins.haxe.ide.annotator.semantics;

import com.intellij.lang.annotation.AnnotationBuilder;
import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.Annotator;
import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.plugins.haxe.HaxeBundle;
import com.intellij.plugins.haxe.ide.inspections.intentions.HaxeIntroduceConstructorIntention;
import com.intellij.plugins.haxe.lang.psi.*;
import com.intellij.plugins.haxe.lang.psi.impl.HaxeReferenceExpressionImpl;
import com.intellij.plugins.haxe.lang.util.HaxeExpressionUtil;
import com.intellij.plugins.haxe.metadata.psi.HaxeMeta;
import com.intellij.plugins.haxe.model.*;
import com.intellij.plugins.haxe.model.fixer.HaxeFixer;
import com.intellij.plugins.haxe.model.type.HaxeConstraintsTypeUtil;
import com.intellij.plugins.haxe.model.type.HaxeTypeResolver;
import com.intellij.plugins.haxe.model.type.ResultHolder;
import com.intellij.plugins.haxe.util.HaxeResolveUtil;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiPackage;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Set;

import static com.intellij.plugins.haxe.lang.psi.HaxeCodeFragmentUtil.isInCodeFragment;
import static com.intellij.plugins.haxe.metadata.psi.HaxeMeta.*;

public class HaxeAccessAnnotator implements Annotator {
  @Override
  public void annotate(@NotNull PsiElement element, @NotNull AnnotationHolder holder) {
    if (AnnotatorUtil.shouldSkip(element)) return;

    if (element instanceof HaxeReferenceExpression referenceExpression) {
      // we want to ignore references used in package, type or metas
      if (isInCodeFragment(element)) return;
      if (checkIfShouldBeIgnored(referenceExpression)) return;
      checkAccessForReference(referenceExpression, holder);
    }
    else if (element instanceof HaxeNewExpression newExpression) {
      if (isInCodeFragment(element)) return;
      checkAccessForConstructor(newExpression, holder);
    }
   else if (element instanceof HaxeCompiletimeMetaArg compileTimeMeta) {
      if (isInCodeFragment(element)) return;
      HaxeMeta haxeMeta = PsiTreeUtil.getParentOfType(compileTimeMeta, HaxeMeta.class);
      if (haxeMeta != null) {
        if (haxeMeta.isType(ALLOW) || haxeMeta.isType(ACCESS)) {
          checkIfFullyQualified(haxeMeta, holder);
        }
      }
    }
  }

  private void checkAccessForConstructor(HaxeNewExpression newExpression, @NotNull AnnotationHolder holder) {
    PsiElement constructor = newExpression.resolve();

    if(constructor == null) {
      // check if class has constructor (could be our new expression just got the wrong arguments (see overloads in abstracs))
      HaxeMethodModel constructorModel = findDefaultConstructor(newExpression);
      // No constructors in type
      if (constructorModel == null) {
          // not a Constructible abstract
          if (!isConstructableType(newExpression) && !isReificationReferences(newExpression.getType())) {
              HaxeClass haxeClass = findHaxeClass(newExpression);
              String message = getQualifiedName(newExpression) + " does not have a constructor";
              AnnotationBuilder annotationBuilder = holder.newAnnotation(HighlightSeverity.ERROR, message)
                      .range(newExpression);
              if (haxeClass != null) {
                  annotationBuilder.withFix(new HaxeIntroduceConstructorIntention(newExpression, haxeClass));
              }
              annotationBuilder.create();
          }
      }
    }else {
      if (constructor instanceof HaxeConstructorDeclaration declaration) {
        HaxeMethodModel memberModel = declaration.getModel();

        if(!memberModel.isPublic()){
          HaxeClass currentClass = PsiTreeUtil.getStubOrPsiParentOfType(newExpression, HaxeClass.class);
          HaxeClassModel memberClassModel = memberModel.getDeclaringClass();
          HaxeClass memberClass = memberClassModel == null ? null : memberClassModel.haxeClass;

          // if same class then private access allowed
          if (memberClass == currentClass) return;
          // if inherited member then private access allowed
          if (inheritsFrom(currentClass, memberClass)) return;
          if (HaxeAccessRules.isGrantedByMetadata(memberModel, newExpression)) return;

          String message =  "Cannot access private constructor of " + getQualifiedName(newExpression);
          holder.newAnnotation(HighlightSeverity.ERROR, message)
                  .range(newExpression)
                  .create();
        }
      }
    }
  }

    private boolean isReificationReferences(@NotNull HaxeType type) {
      return PsiTreeUtil.findChildOfType(type, HaxeMacroIdentifier.class) != null;
    }


  private static boolean checkIfShouldBeIgnored(HaxeReferenceExpression referenceExpression) {
    PsiElement expressionParent = referenceExpression.getParent();
    if(expressionParent instanceof  HaxePackageStatement) return true;
    if(expressionParent instanceof  HaxeType) return true;

    HaxeMeta haxeMeta = PsiTreeUtil.getParentOfType(expressionParent, HaxeMeta.class);
    if(haxeMeta instanceof  HaxeCompiletimeMetaArg) return true;
    return false;
  }


  private void checkAccessForReference(@NotNull HaxeReferenceExpression referenceExpression, @NotNull AnnotationHolder holder) {


    HaxeClass memberClass = null;
    HaxeMemberModel memberModel = null;
    String memberName = null;

    PsiElement resolve = referenceExpression.resolve();

    if (resolve instanceof HaxeFieldDeclaration fieldDeclaration) {
      HaxeModel model = fieldDeclaration.getModel();
      if (model instanceof HaxeMemberModel haxeMemberModel) {

        memberModel = haxeMemberModel;
        memberName = haxeMemberModel.getName();
        memberClass = getMemberHaxeClass(haxeMemberModel);
      }
    }
    if (resolve instanceof HaxeMethodDeclaration methodDeclaration) {
      HaxeMethodModel model = methodDeclaration.getModel();
      if (model instanceof HaxeMemberModel haxeMemberModel) {
        memberModel = haxeMemberModel;
        memberName = haxeMemberModel.getName();
        memberClass = getMemberHaxeClass(haxeMemberModel);
      }
    }

    // ignore if we cant find member (probably a reference to a type)
    if (memberModel != null) {
      if(memberModel.isEnumMember()) return;
      checkStaticAccess(holder, referenceExpression, memberModel);
      // properties can have mixed access for read and write  so isPublic wont do here
      if (memberModel instanceof HaxeFieldModel fieldModel && fieldModel.isProperty() || !memberModel.isPublic()) {
        checkPrivateAccess(holder, referenceExpression, memberModel, memberClass, memberName);
      }
    }

  }


  private void checkStaticAccess(@NotNull AnnotationHolder holder, @NotNull HaxeReferenceExpression referenceExpression, @NotNull HaxeMemberModel memberModel) {
    // ignore anything inside metas (ex. @:build @:autoBuild etc)
    if (PsiTreeUtil.getParentOfType(referenceExpression, HaxeMeta.class)!= null) return;
    // ignore non chained references (usually local access in same class)
    if (HaxeResolveUtil.getLeftReference(referenceExpression) == null) return;
    if (isStaticExtensionReferences(referenceExpression)) return;

    boolean isStaticAccess = isStaticAccess(referenceExpression);
    boolean isMemberStatic = memberModel.isStatic();
    boolean isMemberInline = memberModel.isInline();
    boolean isModuleMember = memberModel.isModuleMember();
    boolean isConstructor = (memberModel instanceof HaxeMethodModel model) && model.isConstructor();
    boolean isMethodBind = (memberModel instanceof HaxeMethodModel) && referenceExpression.getLastChild().textMatches("bind");

    if(isMethodBind)  return;
    if(isModuleMember)  return;

    if (isStaticAccess && !isMemberStatic && !isConstructor) {
      // static access to enum abstract members should be allowed
      if(memberModel.isEnumMember()) return;

      String errorMessage = HaxeBundle.message("haxe.semantic.static.access.to.instance.field", memberModel.getName());
      holder.newAnnotation(HighlightSeverity.ERROR, errorMessage)
              .range(referenceExpression.getLastChild())
              .create();

    } else if (!isStaticAccess && isMemberStatic) {
      if (isMemberInline) return;// allow static access when inlining

      String errorMessage = HaxeBundle.message("haxe.semantic.instance.access.to.static.field", memberModel.getName());
      holder.newAnnotation(HighlightSeverity.ERROR, errorMessage)
              .range(referenceExpression.getLastChild())
              .create();

    }
  }

  private static boolean isStaticExtensionReferences(@NotNull HaxeReferenceExpression referenceExpression) {
    if(referenceExpression.getParent() instanceof HaxeCallExpression callExpression) {
      return callExpression.resolveIsStaticExtension();
    }
    return false;
  }

  private boolean isStaticAccess(HaxeReferenceExpression referenceExpression) {
    final HaxeReference leftReference = HaxeResolveUtil.getLeftReference(referenceExpression);
    if (leftReference instanceof HaxeReferenceExpressionImpl callie) {
      PsiElement callieResolved = callie.resolve();

      if (callieResolved instanceof HaxeImportAlias alias) {
        HaxeIdentifier identifier = alias.getIdentifier();
        return callie.getLastChild().textMatches(identifier);
      }
      if (callieResolved instanceof HaxeClass haxeClass) {
        String name = haxeClass.getName();
        if (name != null) {
          return callie.getLastChild().textMatches(name);
        }
      }
    }
      return false;
  }

  private void checkPrivateAccess(@NotNull AnnotationHolder holder, @NotNull HaxeReferenceExpression referenceExpression, @NotNull HaxeMemberModel memberModel, HaxeClass memberClass, String memberName) {
    // ignore anything inside metas (ex. @:build @:autoBuild etc)
    if (PsiTreeUtil.getParentOfType(referenceExpression, HaxeMeta.class)!= null) return;

    HaxeClass currentClass = PsiTreeUtil.getStubOrPsiParentOfType(referenceExpression, HaxeClass.class);

    // ignoring anonymous types for now
    if (memberClass != null) {
      if (memberClass.isAnonymousType() || memberClass.isObjectLiteralType()) return;
    }
    if (currentClass != null) {
      if (currentClass.isAnonymousType() || currentClass.isObjectLiteralType()) return;
    }

    if (memberClass == currentClass) {
      // if same class then private access allowed
      return;
    }

    if (inheritsFrom(currentClass, memberClass)) {
      // if inherited member then private access allowed
      return;
    }

    if (overridesMemberInCommonClass(memberModel, currentClass)) {
      // if inherited member then private access allowed
      return;
    }
    boolean isPublicProperty = false;
    //  check getter and setters if property is public
    if(memberModel instanceof HaxeFieldModel fieldModel) {
      isPublicProperty = fieldModel.isPublic();
      if(isPublicProperty && fieldModel.isProperty()) {
        if (HaxeExpressionUtil.isInWriteOperation(referenceExpression)) {
          HaxeAccessorType setterType = fieldModel.getSetterType();
          if (setterType.isAllowedFromOutside()) {
            return;
          }
        } else {
          HaxeAccessorType getterType = fieldModel.getGetterType();
          if (getterType.isAllowedFromOutside()) {
            return;
          }
        }
      }
    }

    if (HaxeAccessRules.isGrantedByMetadata(memberModel, referenceExpression)) return;

    // TODO better message for properties that are public but deny read or write
    String message = isPublicProperty ? HaxeBundle.message("haxe.semantic.cannot.access.field", memberName)
                                      : HaxeBundle.message("haxe.semantic.cannot.access.private.field", memberName);
    holder.newAnnotation(HighlightSeverity.ERROR, message)
            .range(referenceExpression.getLastChild())
            .create();
  }

  private boolean overridesMemberInCommonClass(HaxeMemberModel memberModel, HaxeClass currentClass) {
    if (!(memberModel instanceof HaxeMethodModel memberMethod)) return false;
    // a cyclic class hierarchy can lead back to a method already visited
    Set<HaxeMethod> visited = new HashSet<>();
    visited.add(memberMethod.getMethod());
    HaxeMethodModel method = memberMethod;
    while (method.isOverride()) {
      method = method.getAncestorMethod(null);
      if (method == null || !visited.add(method.getMethod())) break;
      HaxeClassModel declaringClass = method.getDeclaringClass();
      if (declaringClass != null && inheritsFrom(currentClass, declaringClass.haxeClass)){
        return true;
      }
    }
    return false;
  }

  private void checkIfFullyQualified(HaxeMeta meta, @NotNull AnnotationHolder holder) {
    HaxeReferenceExpression metaReference = HaxeAccessRules.accessMetaReference(meta);
    if (metaReference != null) {
      PsiElement target = metaReference.resolve();
      String metaReferenceText = metaReference.getQualifiedName();
      String qualifiedName = null;
      if (target instanceof PsiPackage aPackage) {
        qualifiedName = aPackage.getQualifiedName();
      } else if (target instanceof HaxeClass aClass) {
        qualifiedName = aClass.getQualifiedName();
      } else if (target instanceof HaxeMethod method) {
        FullyQualifiedInfo qualifiedInfo = method.getModel().getQualifiedInfo();
        if(qualifiedInfo != null) {
          qualifiedName = qualifiedInfo.toShortendImportReferenceString();
        }
      }

      if(qualifiedName != null) {
        if (!qualifiedName.equals(metaReferenceText)) {
          String finalQualifiedName = qualifiedName;
          holder.newAnnotation(HighlightSeverity.WEAK_WARNING, "Reference is not fully qualified")
                  .withFix(new HaxeFixer("Replace with fully qualified path") {
                    @Override
                    public void run() {
                      HaxeDocumentModel.fromElement(metaReference).replaceElementText(metaReference, finalQualifiedName);
                    }
                  })
                  .create();
        }
      }
    }
  }

  private static boolean inheritsFrom(@Nullable HaxeClass currentClass, @Nullable HaxeClass memberClass) {
    if(currentClass == null  || memberClass == null) return false;
    return currentClass.getModel().inheritsFrom(memberClass);
  }

  @Nullable
  private static HaxeClass getMemberHaxeClass(HaxeMemberModel model) {
      HaxeClassModel declaringClass = model.getDeclaringClass();
      if(declaringClass != null) return declaringClass.haxeClass;
    return null;
  }

    private static HaxeMethodModel findDefaultConstructor(HaxeNewExpression newExpression) {
        ResultHolder typeFromType = HaxeTypeResolver.getTypeFromType(newExpression.getType());
        if (typeFromType.getClassType() == null) return null;
        HaxeClassModel haxeClassModel = typeFromType.getClassType().getHaxeClassModel();
        if (haxeClassModel == null) return null;
        return haxeClassModel.getConstructor(null);
    }
  private static boolean isConstructableType(HaxeNewExpression newExpression) {
      ResultHolder typeFromType = HaxeTypeResolver.getTypeFromType(newExpression.getType());
      if (typeFromType.getClassType() == null) return false;
      HaxeClass constructableType = HaxeConstraintsTypeUtil.getConstructableType(newExpression);
      if (constructableType == null) return false;
      HaxeClassModel model = constructableType.getModel();
      return typeFromType.canAssign(model.getInstanceReference().createHolder());
  }

  private static HaxeClass findHaxeClass(HaxeNewExpression newExpression) {
    ResultHolder typeFromType = HaxeTypeResolver.getTypeFromType(newExpression.getType());
    if(typeFromType.getClassType() == null) return null;
    HaxeClassModel haxeClassModel = typeFromType.getClassType().getHaxeClassModel();
    if(haxeClassModel == null) return null;
    return  haxeClassModel.haxeClass;
  }

  private static String getQualifiedName(HaxeNewExpression newExpression) {
    HaxeType type = newExpression.getType();
    return type.getReferenceExpression().getQualifiedName();
  }

}
