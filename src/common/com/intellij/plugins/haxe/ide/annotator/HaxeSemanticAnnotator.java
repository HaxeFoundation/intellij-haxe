/*
 * Copyright 2000-2013 JetBrains s.r.o.
 * Copyright 2014-2015 AS3Boyan
 * Copyright 2014-2014 Elias Ku
 * Copyright 2017-2018 Ilya Malanin
 * Copyright 2018 Eric Bishton
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

import com.intellij.lang.annotation.Annotation;
import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.plugins.haxe.HaxeBundle;
import com.intellij.plugins.haxe.HaxeComponentType;
import com.intellij.plugins.haxe.ide.quickfix.*;
import com.intellij.plugins.haxe.lang.psi.*;
import com.intellij.plugins.haxe.lang.psi.impl.AbstractHaxeTypeDefImpl;
import com.intellij.plugins.haxe.model.*;
import com.intellij.plugins.haxe.model.type.*;
import com.intellij.plugins.haxe.util.PsiFileUtils;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFileSystemItem;
import com.intellij.psi.PsiIdentifier;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.ui.UIUtil;
import com.intellij.xml.util.XmlStringUtil;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

import static com.intellij.psi.PsiModifier.ModifierConstant;

public class HaxeSemanticAnnotator extends HaxeNonRecursiveAnnotator {
  private static final Logger LOG = Logger.getInstance(HaxeSemanticAnnotator.class);
  private static final @ModifierConstant String[] NOT_ALLOWED_OVERRIDE_MODIFIERS =
    {HaxePsiModifier.INLINE, HaxePsiModifier.STATIC, HaxePsiModifier.FINAL, HaxePsiModifier.FINAL_META};

  static {
    LOG.setLevel(Level.DEBUG);
  }

  @Override
  public void visitPackageStatement(@NotNull HaxePackageStatement o) {
    validatePackageNameCase(o);

    final PsiDirectory containingDirectory = o.getContainingFile().getContainingDirectory();
    if (containingDirectory != null) {
      validateActualPathOfPackage(o, containingDirectory);
    }
  }

  private void validatePackageNameCase(HaxePackageStatement o) {
    HaxeReferenceExpression referenceExpression = o.getReferenceExpression();
    if (referenceExpression == null) return;
    String fullPackageName = getFullPackageName(referenceExpression);

    TextRange textRange = referenceExpression.getTextRange();
    int index = 0;
    for (String packageName : StringUtils.split(fullPackageName, '.')) {
      if (Character.isUpperCase(packageName.charAt(0))) {
        int startOffset = textRange.getStartOffset() + index;
        int endOffset = startOffset + packageName.length();
        String message = HaxeBundle.message("haxe.semantic.package.name.uppercase", packageName);
        createErrorAnnotation(new TextRange(startOffset, endOffset), message);
      }
      index += packageName.length() + 1;
    }
  }

  private void validateActualPathOfPackage(HaxePackageStatement packageStatement,
                                           @NotNull PsiDirectory containingDirectory) {
    PsiFileSystemItem root = PsiFileUtils.findRoot(containingDirectory);

    if (root != null) {
      List<PsiFileSystemItem> range = PsiFileUtils.getRange(root, containingDirectory);
      String rootBasedPackageName = PsiFileUtils.getListPath(range, '.');

      String fullPackageName = getFullPackageName(packageStatement);
      if (!rootBasedPackageName.equals(fullPackageName)) {
        String message = HaxeBundle
          .message("haxe.semantic.package.not.correspond.file.path", fullPackageName, rootBasedPackageName);

        Annotation annotation = createErrorAnnotation(packageStatement, message);
        annotation.registerFix(new HaxePackageNameFix(packageStatement, rootBasedPackageName));
        annotation.registerFix(new HaxeSetupModuleSourcePathsFix(packageStatement));
      }
    }
  }

  private String getFullPackageName(HaxePackageStatement o) {
    HaxeReferenceExpression reference = o.getReferenceExpression();
    return getFullPackageName(reference);
  }

  private String getFullPackageName(HaxeReferenceExpression o) {
    return o != null ? o.getText() : "";
  }


  @Override
  public void visitNewExpression(@NotNull HaxeNewExpression newExpression) {
    ResultHolder typeHolder = HaxeTypeResolver.getTypeFromType(newExpression.getType());
    SpecificTypeReference specificTypeReference = typeHolder.getType();

    if (specificTypeReference instanceof SpecificHaxeClassReference) {
      SpecificHaxeClassReference specificHaxeClass = (SpecificHaxeClassReference)specificTypeReference;
      final HaxeClassModel clazz = ((SpecificHaxeClassReference)typeHolder.getType()).getHaxeClassModel();
      if (clazz != null) {
        HaxeMethodModel constructor = clazz.getConstructor();
        if (constructor == null) {
          holder
            .createErrorAnnotation(newExpression, HaxeBundle.message("haxe.semantic.no.constructor", clazz.getName()))
            .registerFix(new HaxeCreateConstructorFromCallFix(newExpression));
        } else {
          SpecificFunctionReference constructorFunctionType = constructor.getFunctionType(specificHaxeClass.getGenericResolver());
          validateMethodCallArguments(constructorFunctionType, newExpression.getExpressionList(), newExpression);
        }
      }
    }
  }

  @Override
  public void visitCallExpression(@NotNull HaxeCallExpression o) {
    SpecificFunctionReference functionType = HaxeTypeResolver.getPsiElementType(o.getExpression()).getFunctionType();
    if (functionType != null) {
      HaxeExpressionList expressionList = o.getExpressionList();
      List<HaxeExpression> arguments = expressionList != null ? expressionList.getExpressionList() : Collections.emptyList();
      validateMethodCallArguments(functionType, arguments, o);
    }
  }

  @Override
  public void visitMethodDeclaration(@NotNull HaxeMethodDeclaration o) {
    HaxeMethodModel model = o.getModel();
    HaxeModifiersModel modifiers = model.getModifiers();
    if (model.isConstructor() && model.isStatic()) {
      createErrorAnnotation(
        modifiers.getModifierPsi(HaxePsiModifier.STATIC),
        HaxeBundle.message("semantic.static.constructor")
      )
        .registerFix(new HaxeDropModifierFix(o, HaxePsiModifier.STATIC));
    } else if (model.isStaticInit() && !modifiers.hasModifier(HaxePsiModifier.STATIC)) {
      createErrorAnnotation(o, HaxeBundle.message("haxe.semantic.static.initializer.must.be.static", model.getName()))
        .registerFix(new HaxeAddModifierFix(o, HaxePsiModifier.STATIC));
    }

    visitMethod(o);

    if (ApplicationManager.getApplication().isUnitTestMode()) {
      PsiElement body = model.getBodyPsi();
      if (body != null) {
        HaxeTypeResolver.getPsiElementType(model.getBodyPsi(), holder);
      }
    }
  }

  @Override
  public void visitAnonymousType(@NotNull HaxeAnonymousType o) {
    final HaxeAnonymousTypeBody body = o.getAnonymousTypeBody();
    if (body == null) return;
    HaxeTypeExtendsList extendsList = body.getTypeExtendsList();
    if (extendsList != null) {
      for (HaxeType extendType : extendsList.getTypeList()) {
        PsiElement resolvedItem = extendType.getReferenceExpression().resolve();
        if (resolvedItem != null) {
          if (PsiTreeUtil.isAncestor(resolvedItem, o, false)) {
            createErrorAnnotation(extendType, HaxeBundle.message("haxe.semantic.cant.extend.self"))
              .registerFix(new HaxeDropExtendDeclaration(o, extendType));
          } else if (!isAnonymousType(resolvedItem)) {
            createErrorAnnotation(extendType, HaxeBundle.message("haxe.semantic.not.anonymous.type"))
              .registerFix(new HaxeDropExtendDeclaration(o, extendType));
          }
        }
      }
    }
  }

  private boolean isAnonymousType(PsiElement item) {
    if (item instanceof HaxeTypedefDeclaration) {
      final HaxeTypeOrAnonymous typeOrAnonymous = ((HaxeTypedefDeclaration)item).getTypeOrAnonymous();
      return typeOrAnonymous != null && typeOrAnonymous.getAnonymousType() != null;
    }
    return item instanceof HaxeAnonymousType;
  }

  @Override
  public void visitType(@NotNull HaxeType o) {
    validateTypeNameCase(o.getReferenceExpression().getIdentifier());
  }

  private void validateTypeNameCase(@Nullable PsiIdentifier o) {
    if (o == null) return;
    String typeName = o.getText();
    if (Character.isLowerCase(typeName.charAt(0))) {
      String properTypeName = StringUtil.capitalize(typeName);
      createErrorAnnotation(o, HaxeBundle.message("haxe.semantic.type.name.uppercase", typeName))
        .registerFix(new HaxeRenameElementFix(o, properTypeName));
    }
  }

  @Override
  public void visitFieldDeclaration(@NotNull HaxeFieldDeclaration o) {
    HaxeModel model = o.getModel();
    if (model instanceof HaxeFieldModel) {
      validateField((HaxeFieldModel)model);
    }
  }

  private void validateField(HaxeFieldModel model) {
    if (model.hasTypeTag() && model.hasInitializer()) {
      validateTypeCompatibleWithAssignment(model.getBasePsi(), model.getTypeTagPsi(), model.getInitializerPsi());
    }

    validateFieldRedeclaration(model);

    if (model.isProperty()) {
      validateProperty(model);
    } else {
      if (model.isFinal() && !model.hasInitializer()) {
        if (model.isStatic()) {
          createErrorAnnotation(model.getBasePsi(), HaxeBundle.message("haxe.semantic.final.static.var.init", model.getName()));
        } else if (!isFieldInitializedInTheConstructor(model)) {
          createErrorAnnotation(model.getBasePsi(), HaxeBundle.message("haxe.semantic.final.var.init", model.getName()));
        }
      }
    }
  }

  private static boolean isFieldInitializedInTheConstructor(HaxeFieldModel field) {
    HaxeClassModel declaringClass = field.getDeclaringClass();
    if (declaringClass == null) return false;
    HaxeMethodModel constructor = declaringClass.getConstructor();
    if (constructor == null) return false;
    PsiElement body = constructor.getBodyPsi();
    if (body == null) return false;

    final HaxeInitVariableVisitor visitor = new HaxeInitVariableVisitor(field.getName());
    body.accept(visitor);
    return visitor.result;
  }


  private void validateFieldRedeclaration(HaxeMemberModel model) {
    HaxeClassModel declaringClass = model.getDeclaringClass();
    HaxeClassModel parentClass = declaringClass.getParentClass();
    while (parentClass != null) {
      if (PsiTreeUtil.isAncestor(parentClass.haxeClass, declaringClass.haxeClass, true)) return;
      if (declaringClass.haxeClass.equals(parentClass.haxeClass)) return;
      HaxeMemberModel parentMember = parentClass.getMember(model.getName());
      if (parentMember != null && declaringClass.haxeClass instanceof HaxeAnonymousType && isAnonymousType(parentClass.haxeClass)) {
        ResultHolder declaringType = model.getResultType();
        ResultHolder parentType = parentMember.getResultType();
        if (!parentType.canAssign(declaringType)) {
          String message = HaxeBundle.message(
            "haxe.semantic.anonymous.field.same.type.required",
            model.getName(),
            FullyQualifiedInfo.getRelativeQualifiedName(parentClass, declaringClass),
            parentType.toStringWithoutConstant(),
            wrapWithHtmlError(declaringType.toStringWithoutConstant())
          );
          createErrorAnnotation(model.getNamePsi(), message, true)
            .registerFix(new HaxeChangeComponentTypeFix(((HaxePsiField)model.getBasePsi()).getTypeTag(), declaringType, parentType));
        }
      } else if (parentMember != null && !parentMember.isStatic()) {
        String message = HaxeBundle.message("haxe.semantic.variable.redefinition.not.allowed", model.getName(), parentClass.getName());
        createErrorAnnotation(model.getBasePsi(), message);
        return;
      }
      parentClass = parentClass.getParentClass();
    }
  }

  private void validateProperty(HaxeFieldModel model) {
    HaxePropertyAccessor getterPsi = model.getGetterPsi();
    HaxePropertyAccessor setterPsi = model.getSetterPsi();

    if (getterPsi != null) {
      validatePropertyAccessor(model, getterPsi, true);
    }
    if (setterPsi != null) {
      validatePropertyAccessor(model, setterPsi, false);
    }

    if (model.isFinal()) {
      createErrorAnnotation(model.getBasePsi(), HaxeBundle.message("haxe.semantic.property.cant.be.final"))
        .registerFix(new HaxeSwitchMutabilityModifier((HaxeFieldDeclaration)model.getBasePsi()));
    } else if (model.hasInitializer() && !model.isRealVar()) {
      PsiElement initializer = model.getInitializerPsi();

      Annotation annotation = createErrorAnnotation(initializer, HaxeBundle.message("haxe.semantic.not.real.var"));

      annotation.registerFix(new HaxeAddModifierFix(model.getPsiField(), HaxePsiModifier.IS_VAR));
      annotation.registerFix(new HaxeDropInitializerFix(initializer));
    }
  }

  private void validatePropertyAccessor(HaxeFieldModel model, HaxePropertyAccessor accessor, boolean isGetter) {
    HaxeAccessorType genericAccessorType = isGetter ? HaxeAccessorType.GET : HaxeAccessorType.SET;
    HaxeAccessorType accessorType = HaxeAccessorType.fromPsi(accessor);

    final HaxeClassModel declaringClass = model.getDeclaringClass();
    final boolean isInvalidAccessorType = (isGetter && !accessorType.isValidGetter()) ||
                                          (!isGetter && !accessorType.isValidSetter());
    if (isInvalidAccessorType) {
      createErrorAnnotation(accessor, HaxeBundle.message("haxe.semantic.invalid.accessor.type", genericAccessorType.text));
    } else if (!declaringClass.isInterface() && !declaringClass.isExtern()) {
      boolean accessorMethodRequired = accessorType == genericAccessorType;
      boolean accessorMethodIsAbsent =
        accessorMethodRequired && ((isGetter && model.getGetterMethod() == null) || (!isGetter && model.getSetterMethod() == null));

      if (accessorMethodIsAbsent) {
        String accessorMethodName = genericAccessorType.text + "_" + model.getName();
        createErrorAnnotation(accessor, HaxeBundle.message("haxe.semantic.accessor.method.not.found", accessorMethodName))
          .registerFix(new HaxeCreateAccessorMethodFix(model.getPsiField(), accessor));
      }
    }
  }

  @Override
  public void visitLocalVarDeclaration(@NotNull HaxeLocalVarDeclaration o) {
    HaxeLocalVarModel model = (HaxeLocalVarModel)o.getModel();
    if (model.hasTypeTag() && model.hasInitializer()) {
      validateTypeCompatibleWithAssignment(model.getBasePsi(), model.getTypeTagPsi(), model.getInitializerPsi());
    }
  }

  @Override
  public void visitParameter(@NotNull HaxeParameter o) {
    HaxeParameterModel model = o.getModel();
    if (model.hasTypeTag() && model.hasInitializer()) {
      HaxeVarInit initializerPsi = model.getInitializerPsi();
      HaxeTypeTag typeTagPsi = model.getTypeTagPsi();

      ResultHolder initType = HaxeTypeResolver.getPsiElementType(model.getInitializerPsi());
      ResultHolder variableType = HaxeTypeResolver.getTypeFromTypeTag(model.getTypeTagPsi(), model.getTypeTagPsi());

      Annotation annotation =
        validateTypeCompatibleWithAssignment(model.getBasePsi(), variableType, initType, false);
      if (annotation != null) {
        annotation.registerFix(new HaxeDropInitializerFix(initializerPsi));
        annotation.registerFix(new HaxeChangeComponentTypeFix(typeTagPsi, variableType, initType));
      }

      if (!initType.getType().isConstant()) {
        createErrorAnnotation(initializerPsi,
                              HaxeBundle.message("haxe.semantic.parameter.default.constant.required", initType.toStringWithoutConstant()))
          .registerFix(new HaxeDropInitializerFix(o));
      }
    }

    if (model.hasOptionalPsi() && model.hasInitializer()) {
      holder
        .createWeakWarningAnnotation(model.getOptionalPsi(), HaxeBundle.message("haxe.semantic.optional.mark.redundant"))
        .registerFix(new HaxeRemoveOptionalMarkFix(o));
    }
  }

  @Override
  public void visitParameterList(@NotNull HaxeParameterList o) {
    HashMap<String, HaxeParameter> existingParameters = new HashMap<>();
    HashSet<HaxeParameter> annotatedParameters = new HashSet<>();
    for (HaxeParameter parameter : o.getParameterList()) {
      final String parameterName = parameter.getName();

      if (existingParameters.containsKey(parameterName)) {
        String message = HaxeBundle.message("haxe.semantic.duplicate.parameter.name", parameterName);
        HaxeParameter existingParameter = existingParameters.get(parameterName);

        if (!annotatedParameters.contains(existingParameter)) {
          holder.createWarningAnnotation(existingParameters.get(parameterName).getComponentName(), message);
          annotatedParameters.add(existingParameter);
        }

        holder.createWarningAnnotation(parameter.getComponentName(), message);
      } else {
        existingParameters.put(parameterName, parameter);
      }
    }
  }

  @Override
  public void visitMethod(@NotNull HaxeMethod o) {
    HaxeMethodModel model = o.getModel();
    HaxeClassModel declaringClass = model.getDeclaringClass();

    if (declaringClass != null) {
      validateInterfaceAndExternMethod(model, declaringClass);
      validateMethodOverride(model, declaringClass);

      if (!model.isConstructor()) {
        final HaxeMethodModel parentMethod = model.getParentMethod();
        if (parentMethod != null) {
          validateMethodSignatureCompatibility(model, parentMethod);
        }
      }
    }
  }

  private void validateInterfaceAndExternMethod(HaxeMethodModel model, HaxeClassModel declaringClass) {
    if ((declaringClass.isExtern() || declaringClass.isInterface())) {
      String declarationType = declaringClass.isExtern() ? "extern" : "interface";
      String message;
      if (!model.hasReturnType()) {
        message = HaxeBundle.message("haxe.semantic.return.type.required", declarationType);
        createErrorAnnotation(model.getNameOrBasePsi(), message);
      }
      if (model.hasBody()) {
        message = HaxeBundle.message("haxe.semantic.method.body.not.allowed", declarationType);
        createErrorAnnotation(model.getNameOrBasePsi(), message);
      }
      for (HaxeParameterModel parameterModel : model.getParameters()) {
        if (!parameterModel.hasTypeTag()) {
          message = HaxeBundle.message("haxe.semantic.parameter.type.required", declarationType);
          createErrorAnnotation(parameterModel.getBasePsi(), message);
        }
      }
    }
  }

  private void validateMethodOverride(HaxeMethodModel model, HaxeClassModel declaringClass) {
    final HaxeClassModel parentClass = declaringClass.getParentClass();
    final HaxeMethodModel parentMethod = parentClass != null ? parentClass.getMethod(model.getName()) : null;
    final HaxeModifiersModel parentModifiers = (parentMethod != null) ? parentMethod.getModifiers() : null;
    HaxeModifiersModel modifiers = model.getModifiers();

    if (modifiers.hasModifier(HaxePsiModifier.OVERRIDE)) {
      if (parentModifiers != null) {
        List<String> notAllowedModifiers = parentModifiers.getPresentModifiers(NOT_ALLOWED_OVERRIDE_MODIFIERS);
        if (!notAllowedModifiers.isEmpty()) {
          Annotation annotation =
            createErrorAnnotation(model.getNameOrBasePsi(), HaxeBundle.message("haxe.semantic.not.allowed.override"));

          String targetMethod = FullyQualifiedInfo.getRelativeQualifiedName(parentMethod, model);
          for (@ModifierConstant String mod : notAllowedModifiers) {
            annotation.registerFix(new HaxeDropModifierFix(parentMethod.getMethodPsi(), mod, targetMethod));
          }
        }

        final @ModifierConstant String visibility = modifiers.getVisibility();
        final @ModifierConstant String parentVisibility = parentModifiers.getVisibility();

        if (!visibility.equals(HaxePsiModifier.EMPTY) && HaxePsiModifier.hasLowerVisibilityThan(visibility, parentVisibility)) {
          final String message = HaxeBundle.message(
            "haxe.semantic.method.visibility.less.then.parent",
            model.getName(),
            wrapWithHtmlError(visibility),
            FullyQualifiedInfo.getRelativeQualifiedName(parentMethod, model),
            parentVisibility
          );

          final String parentMethodName = FullyQualifiedInfo.getRelativeQualifiedName(parentMethod, model);
          Annotation annotation = createErrorAnnotation(model.getNamePsi(), message, true);
          annotation.registerFix(new HaxeChangeVisibilityFix(model.getMethodPsi(), parentModifiers.getVisibility()));
          annotation.registerFix(new HaxeChangeVisibilityFix(parentMethod.getMethodPsi(), modifiers.getVisibility(), parentMethodName));
        }
      }

      if (parentMethod == null || parentMethod.isStatic() || parentMethod.isConstructor() || parentMethod.isStaticInit()) {
        createErrorAnnotation(modifiers.getModifierPsi(HaxePsiModifier.OVERRIDE),
                              HaxeBundle.message("haxe.semantic.overrides.nothing"))
          .registerFix(new HaxeDropModifierFix(model.getMethodPsi(), HaxePsiModifier.OVERRIDE));
      }
    }
  }

  @Override
  public void visitExternInterfaceDeclaration(@NotNull HaxeExternInterfaceDeclaration o) {
    validateHaxeClass(o);
  }

  @Override
  public void visitInterfaceDeclaration(@NotNull HaxeInterfaceDeclaration o) {
    validateTypeNameCase(o.getNameIdentifier());
    validateDuplicateFields(o);
    validateSelfExtends(o);

    List<HaxeType> implementsList = o.getHaxeImplementsList();
    if (!implementsList.isEmpty()) {
      implementsList.forEach(item -> {
        HaxeImplementsDeclaration declaration = (HaxeImplementsDeclaration)item.getParent();
        createErrorAnnotation(declaration, HaxeBundle.message("haxe.semantic.interface.cant.implement"))
          .registerFix(new HaxeChangeImplementsToExtendsFix(declaration));
      });
    }

    List<HaxeClassReferenceModel> extendsList = o.getModel().getInterfaceExtendingInterfaces();
    for (HaxeClassReferenceModel extendReference : extendsList) {
      HaxeClassModel extendsClass = extendReference.getHaxeClass();
      if (extendsClass != null && !extendsClass.isInterface()) {
        HaxeExtendsDeclaration declaration = (HaxeExtendsDeclaration)extendReference.getPsi().getParent();
        createErrorAnnotation(declaration,
                              HaxeBundle.message("haxe.semantic.not.interface", extendReference.type.getText()))
          .registerFix(new HaxeDropExtendDeclaration(o, extendReference.type));
      }
    }
  }

  @Override
  public void visitClassDeclaration(@NotNull HaxeClassDeclaration o) {
    validateHaxeClass(o);
  }

  @Override
  public void visitAbstractClassDeclaration(@NotNull HaxeAbstractClassDeclaration o) {
    validateHaxeClass(o);
  }

  @Override
  public void visitTypedefDeclaration(@NotNull HaxeTypedefDeclaration o) {
    validateHaxeClass(o);
  }

  private void validateHaxeClass(@NotNull HaxeClass o) {
    validateTypeNameCase(o.getNameIdentifier());
    validateDuplicateFields(o);

    if (o instanceof HaxeInheritanceDeclaration) {
      validateSelfExtends((HaxeInheritanceDeclaration)o);

      final HaxeClassModel model = o.getModel();
      final HaxeClassModel parentClass = model.getParentClass();
      if (parentClass != null) {
        HaxeClass targetClass = parentClass.haxeClass;
        if (parentClass.isTypedef() && parentClass.getBasePsi() instanceof AbstractHaxeTypeDefImpl) {
          final AbstractHaxeTypeDefImpl haxeTypeDef = (AbstractHaxeTypeDefImpl)parentClass.getBasePsi();
          targetClass = haxeTypeDef.getTargetClass().getHaxeClass();
        }
        if (targetClass == null || !targetClass.getModel().isClass()) {
          String type = HaxeComponentType.getName(targetClass);
          String message;
          if (type != null) {
            message = HaxeBundle.message("haxe.semantic.should.extend.class.but.extend.0", wrapWithHtmlError(type));
          } else {
            message = HaxeBundle.message("haxe.semantic.should.extend.class");
          }
          createErrorAnnotation(o.getHaxeExtendsList().get(0), message, true);
        }
      }
      final List<HaxeClassReferenceModel> interfaces = model.getImplementingInterfaces();
      if (!model.isInterface() && !interfaces.isEmpty()) {
        Set<PsiElement> validatedItems = new HashSet<>();
        for (HaxeClassReferenceModel implementedInterface : interfaces) {
          final HaxeClassModel interfaceHaxeClass = implementedInterface.getHaxeClass();
          if (interfaceHaxeClass != null && !interfaceHaxeClass.isInterface()) {
            final String message = HaxeBundle.message("haxe.semantic.not.interface", implementedInterface.type.getText());
            HaxeImplementsDeclaration declaration = (HaxeImplementsDeclaration)implementedInterface.getPsi().getParent();
            createErrorAnnotation(implementedInterface.getPsi(), message)
              .registerFix(new HaxeDropImplementsDeclarationFix(declaration));
          } else {
            validateImplementedMembers(o, implementedInterface, validatedItems);
          }
        }
      }
    }
  }

  private void validateImplementedMembers(HaxeClass haxeClass,
                                          HaxeClassReferenceModel implementedInterface,
                                          Set<PsiElement> validatedItems) {
    HaxeClassModel interfaceModel = implementedInterface.getHaxeClass();
    HaxeClassModel classModel = haxeClass.getModel();

    Set<HaxeMethodModel> missingMethods = new HashSet<>();
    Set<HaxeFieldModel> missingFields = new HashSet<>();

    if (interfaceModel != null) {
      final List<HaxeMemberModel> members = interfaceModel.getMembers();
      for (HaxeMemberModel interfaceMember : members) {
        if (!validatedItems.contains(interfaceMember.getBasePsi())) {
          validatedItems.add(interfaceMember.getBasePsi());
          if (interfaceMember instanceof HaxeMethodModel) {
            HaxeMethodModel methodModel = classModel.getMethod(interfaceMember.getName());
            if (methodModel == null || methodModel.getDeclaringClass().isInterface()) {
              missingMethods.add((HaxeMethodModel)interfaceMember);
            } else {
              validateMethodSignatureCompatibility(methodModel, (HaxeMethodModel)interfaceMember);
            }
          } else if (interfaceMember instanceof HaxeFieldModel) {
            HaxeFieldModel fieldModel = classModel.getField(interfaceMember.getName());
            if (fieldModel == null || fieldModel.getDeclaringClass().isInterface()) {
              missingFields.add((HaxeFieldModel)interfaceMember);
            } else {
              validateFieldSignatureCompatibility(fieldModel, (HaxeFieldModel)interfaceMember);
            }
          }
        }
      }
    }

    if (!missingFields.isEmpty() || !missingMethods.isEmpty()) {
      String tooltip = buildMissingMembersMessage(implementedInterface, missingMethods, missingFields);
      Annotation annotation = createErrorAnnotation(implementedInterface.getPsi().getTextRange(), tooltip, true);
      annotation.registerFix(new HaxeImplementMembersFix(haxeClass));
    }
  }

  private String buildMissingMembersMessage(HaxeClassReferenceModel implementedInterface,
                                            Set<HaxeMethodModel> methods,
                                            Set<HaxeFieldModel> fields) {
    String interfaceName = FullyQualifiedInfo.getQualifiedName(implementedInterface.getHaxeClass());
    String result = "";

    if (!methods.isEmpty()) result += buildMissingItemsList("Methods", methods);
    if (!fields.isEmpty()) result += buildMissingItemsList("Fields", fields);

    return HaxeBundle.message("haxe.semantic.not.implemented.members", interfaceName, result);
  }

  private String buildMissingItemsList(String title, Set<? extends HaxeMemberModel> members) {
    StringBuilder list = new StringBuilder();
    int index = 0;
    for (HaxeMemberModel method : members) {
      if (index > 0) list.append(", ");
      list.append(HaxeBundle.message("haxe.semantic.missing.list.item", method.getPresentableText()));
      index++;
    }

    return HaxeBundle.message("haxe.semantic.missing.list", title, list.toString());
  }

  private void validateFieldSignatureCompatibility(HaxeFieldModel field, HaxeFieldModel prototype) {
    if (isPropertyAccessDiffers(field, prototype)) {
      final String message = HaxeBundle.message(
        "haxe.semantic.field.has.different.accessors",
        FullyQualifiedInfo.getRelativeQualifiedName(prototype, field),
        wrapWithHtmlError(field.isProperty() ? field.getPropertyDeclarationPsi().getText() : "var"),
        field.isProperty() ? "var" : prototype.getPropertyDeclarationPsi().getText()
      );
      createErrorAnnotation(field.getBasePsi(), message, true);
    }

    if (!field.hasTypeTag() && !field.hasInitializer()) {
      final String message = HaxeBundle.message(
        "haxe.semantic.field.type.required",
        FullyQualifiedInfo.getRelativeQualifiedName(prototype.getDeclaringClass(), field.getDeclaringClass()),
        prototype.getTypeTagPsi().getText()
      );
      createErrorAnnotation(field.getBasePsi(), message);
    } else if (!prototype.getResultType().canAssign(field.getResultType())) {
      final String message = HaxeBundle.message(
        "haxe.semantic.incompatible.types",
        field.getResultType().toStringWithoutConstant(),
        prototype.getResultType().toStringWithoutConstant()
      );
      createErrorAnnotation(field.getBasePsi(), message, true);
    }
  }

  private boolean isPropertyAccessDiffers(HaxeFieldModel field, HaxeFieldModel prototype) {
    return (prototype.getGetterType() != field.getGetterType() || prototype.getSetterType() != field.getSetterType());
  }

  private void validateMethodSignatureCompatibility(HaxeMethodModel method, HaxeMethodModel prototype) {
    final List<HaxeParameterModel> parameters = method.getParameters();
    final List<HaxeParameterModel> prototypeParameters = prototype.getParameters();

    final int parametersCount = parameters.size();
    final int prototypeParametersCount = prototypeParameters.size();
    int minParametersCount = Math.min(parametersCount, prototypeParametersCount);

    if (parametersCount > prototypeParametersCount) {
      for (int n = minParametersCount; n < parametersCount; n++) {
        final HaxeParameterModel parameter = parameters.get(n);
        createErrorAnnotation(parameter.getBasePsi(), HaxeBundle.message("haxe.semantic.unexpected.parameter"))
          .registerFix(new HaxeDropParameterFix(parameter.getParameterPsi()));
      }
    } else if (parametersCount != prototypeParametersCount) {
      createErrorAnnotation(
        method.getNameOrBasePsi(),
        HaxeBundle.message("haxe.semantic.not.matching.arity", prototypeParametersCount, parametersCount)
      );
    }

    for (int n = 0; n < minParametersCount; n++) {
      final HaxeParameterModel parameter = parameters.get(n);
      final HaxeParameterModel prototypeParameter = prototypeParameters.get(n);

      final ResultHolder parameterType = parameter.getType();
      final ResultHolder prototypeParameterType = prototypeParameter.getType();

      if (!HaxeTypeCompatible.canAssignToFrom(parameterType, prototypeParameterType)) {
        final String message = HaxeBundle.message("haxe.semantic.incompatible.types", parameterType, prototypeParameterType);
        createErrorAnnotation(parameter.getParameterPsi(), message)
          .registerFix(new HaxeChangeComponentTypeFix(parameter.getTypeTagPsi(), parameterType, prototypeParameterType));
      }
      if (parameter.hasOptionalPsi() != prototypeParameter.hasOptionalPsi()) {
        final boolean removeOptional = parameter.hasOptionalPsi();

        String errorMessage;
        if (prototypeParameter.getDeclaringClass().isInterface()) {
          errorMessage = removeOptional ? "haxe.semantic.implemented.method.parameter.required"
                                        : "haxe.semantic.implemented.method.parameter.optional";
        } else {
          errorMessage = removeOptional ? "haxe.semantic.overwritten.method.parameter.required"
                                        : "haxe.semantic.overwritten.method.parameter.optional";
        }

        errorMessage = HaxeBundle.message(errorMessage, prototypeParameter.getPresentableText(),
                                          FullyQualifiedInfo.getRelativeQualifiedName(prototype, method));

        final Annotation annotation = createErrorAnnotation(parameter.getBasePsi(), errorMessage);
        if (removeOptional) {
          annotation.registerFix(new HaxeRemoveOptionalMarkFix(parameter.getParameterPsi()));
        } else {
          annotation.registerFix(new HaxeAddOptionalMarkFix(parameter.getParameterPsi()));
        }
      }
    }

    final ResultHolder resultType = method.getResultType();
    final ResultHolder prototypeResultType = prototype.getResultType();

    if (!HaxeTypeCompatible.canAssignToFrom(resultType, prototypeResultType)) {
      HaxeTypeTag psi = method.getReturnTypeTagPsi();
      final String message = HaxeBundle.message(
        "haxe.semantic.incompatible.types",
        resultType.toStringWithoutConstant(),
        prototypeResultType.toStringWithoutConstant()
      );
      if (psi == null) {
        createErrorAnnotation(method.getNamePsi(), message)
          .registerFix(new HaxeAddMethodReturnTypeFix(method.getMethodPsi(), prototypeResultType));
      } else {
        createErrorAnnotation(psi, message)
          .registerFix(new HaxeChangeComponentTypeFix(psi, resultType, prototypeResultType));
      }
    }
  }

  private void validateDuplicateFields(HaxeClass o) {
    HaxeClassModel model = o.getModel();

    Map<String, HaxeMemberModel> existingMembers = new HashMap<>();
    Set<HaxeMemberModel> annotatedMembers = new HashSet<>();

    for (HaxeMemberModel member : model.getMembersSelf()) {
      final String memberName = member.getName();
      if (existingMembers.containsKey(memberName)) {
        String message = HaxeBundle.message("haxe.semantic.duplicate.member.name", member.getName());

        HaxeMemberModel existingMember = existingMembers.get(memberName);
        if (!annotatedMembers.contains(existingMember)) {
          createErrorAnnotation(existingMember.getNamePsi(), message);
          annotatedMembers.add(existingMember);
        }

        createErrorAnnotation(member.getNamePsi(), message);
      } else {
        existingMembers.put(memberName, member);
      }
    }
  }


  private void validateSelfExtends(@NotNull HaxeInheritanceDeclaration o) {
    HaxeInheritList inherits = o.getInheritList();
    if (inherits != null) {
      List<HaxeExtendsDeclaration> extendsList = inherits.getExtendsDeclarationList();
      for (HaxeExtendsDeclaration extendsDeclaration : extendsList) {
        if (extendsDeclaration != null) {
          HaxeType extendType = extendsDeclaration.getType();
          if (extendType != null) {
            PsiElement resolvedItem = extendType.getReferenceExpression().resolve();
            if (resolvedItem != null && resolvedItem.equals(o)) {
              createErrorAnnotation(extendType, HaxeBundle.message("haxe.semantic.cant.extend.self"))
                .registerFix(new HaxeDropExtendDeclaration(o, extendType));
            }
          }
        }
      }
    }
  }

  @Override
  public void visitAssignExpression(@NotNull HaxeAssignExpression element) {
    final PsiElement left = element.getFirstChild();
    final PsiElement right = element.getLastChild();

    if (left != null && right != null) {
      final ResultHolder leftResult = HaxeTypeResolver.getPsiElementType(left);
      final ResultHolder rightResult = HaxeTypeResolver.getPsiElementType(right);

      if (leftResult.isUnknown()) {
        leftResult.setType(rightResult.getType());
      }
      leftResult.removeConstant();

      if (!leftResult.canAssign(rightResult)) {
        annotateNotCompatibleTypes(right, leftResult, rightResult, true);
      }
    }
  }

  @Override
  public void visitStringLiteralExpression(@NotNull HaxeStringLiteralExpression o) {
    if (isSingleQuotesRequired(o)) {
      holder.createWarningAnnotation(o, HaxeBundle.message("haxe.semantic.string.interpolation.quotes"));
    }
  }

  private static boolean isSingleQuotesRequired(HaxeStringLiteralExpression o) {
    if (o.getParent() instanceof HaxeObjectLiteralElement) return false;
    return (o.getLongTemplateEntryList().size() > 0 || o.getShortTemplateEntryList().size() > 0) &&
           o.getFirstChild().textContains('"');
  }

  private void validateTypeCompatibleWithAssignment(PsiElement annotationTarget,
                                                    HaxeTypeTag typeTag,
                                                    HaxeVarInit initializer) {
    ResultHolder initType = HaxeTypeResolver.getPsiElementType(initializer);
    ResultHolder variableType = HaxeTypeResolver.getTypeFromTypeTag(typeTag, typeTag);
    validateTypeCompatibleWithAssignment(annotationTarget, variableType, initType, true);
  }


  @Nullable
  private Annotation validateTypeCompatibleWithAssignment(PsiElement annotationTarget,
                                                          ResultHolder type,
                                                          ResultHolder initializerType,
                                                          boolean addCastFix) {
    if (!type.canAssign(initializerType)) {
      return annotateNotCompatibleTypes(annotationTarget, type, initializerType, addCastFix);
    }

    return null;
  }

  private void validateMethodCallArguments(SpecificFunctionReference method, List<HaxeExpression> arguments, @NotNull PsiElement
    context) {
    final List<HaxeParameterModel> parameters = Objects.requireNonNull(method.method).getParameters();

    int parametersCount = parameters.size();
    int argumentsCount = arguments.size();
    int parameterIndex = 0;

    for (int argumentIndex = 0; argumentIndex < argumentsCount; argumentIndex++) {
      HaxeParameterModel parameter = parameterIndex >= parametersCount ? null : parameters.get(parameterIndex);
      HaxeExpression argument = arguments.get(argumentIndex);

      if (parameter == null) {
        addExcessiveArgumentsError(arguments, argument);
        return;
      }

      ResultHolder parameterType = method.getArguments().get(parameterIndex).getType();
      ResultHolder argumentType = HaxeExpressionEvaluator.evaluate(argument, new HaxeExpressionEvaluatorContext(context, null)).result;

      if (!parameterType.canAssign(argumentType)) {
        int index = getIndexOfSuitableParameterAhead(parameters, parameterIndex, argumentType);
        if (index != -1) {
          parameterIndex = index;
        } else {
          annotateNotCompatibleTypes(argument, parameterType, argumentType, true);
        }
      }
      parameterIndex++;
    }

    int requiredParametersCount = (int)parameters.stream()
      .filter(parameter -> !(parameter.isOptional() || parameter.hasInitializer()))
      .count();

    if (requiredParametersCount > argumentsCount) {
      String message = HaxeBundle.message("haxe.semantic.0.arguments.expected.got.1", requiredParametersCount, argumentsCount);
      createErrorAnnotation(context, message);
    }
  }

  private Annotation annotateNotCompatibleTypes(PsiElement annotationTarget,
                                                ResultHolder destination,
                                                ResultHolder source,
                                                boolean addCastFix) {
    String message = HaxeBundle
      .message("haxe.semantic.incompatible.types", wrapWithHtmlError(source.toStringWithoutConstant()),
               destination.toStringWithoutConstant());


    Annotation annotation = createErrorAnnotation(annotationTarget, message, true);

    if (addCastFix) {
      annotation.registerFix(new HaxeAddCastFix(annotationTarget, source.getType(), destination.getType()));
    }

    return annotation;
  }

  private String wrapWithHtmlError(String string) {
    String color = UIUtil.isUnderDarcula() ? "FF6B68" : "red";
    return "<font color='" + color + "'><b>" + string + "</b></font>";
  }

  private void addExcessiveArgumentsError(List<HaxeExpression> arguments, HaxeExpression firstExcessiveArgument) {
    HaxeExpression lastArgument = arguments.get(arguments.size() - 1);
    TextRange textRange = new TextRange(firstExcessiveArgument.getTextOffset(), lastArgument.getTextRange().getEndOffset());

    createErrorAnnotation(textRange, HaxeBundle.message("haxe.semantic.more.arguments.than.expected"))
      .registerFix(new HaxeRemoveExcessiveArgumentsFix(arguments, firstExcessiveArgument));
  }

  private int getIndexOfSuitableParameterAhead(List<HaxeParameterModel> parameters, int currentIndex, ResultHolder argumentType) {
    int parametersCount = parameters.size();
    if (!parameters.get(currentIndex).isOptional()) return -1;

    int index = currentIndex + 1;
    if (index >= parametersCount) return -1;

    do {
      HaxeParameterModel parameterModel = parameters.get(index);
      if (parameterModel.getType().canAssign(argumentType)) {
        return index;
      } else if (!parameterModel.isOptional()) {
        return -1;
      }
      index++;
    }
    while (index < parametersCount);

    return -1;
  }

  private Annotation createErrorAnnotation(@NotNull PsiElement elt, @Nullable String message) {
    return createErrorAnnotation(elt.getTextRange(), message, false);
  }

  private Annotation createErrorAnnotation(@NotNull PsiElement elt, @Nullable String message, boolean htmlTooltip) {
    return createErrorAnnotation(elt.getTextRange(), message, htmlTooltip);
  }

  private Annotation createErrorAnnotation(@NotNull TextRange range, @Nullable String message) {
    return createErrorAnnotation(range, message, false);
  }

  private Annotation createErrorAnnotation(@NotNull TextRange range, @Nullable String message, boolean htmlTooltip) {
    if (message != null && htmlTooltip && !XmlStringUtil.isWrappedInHtml(message)) {
      message = XmlStringUtil.wrapInHtml(message);
    }
    return holder.createAnnotation(HighlightSeverity.ERROR, range, message, message);
  }
}