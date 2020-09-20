/*
 * Copyright 2000-2013 JetBrains s.r.o.
 * Copyright 2014-2015 AS3Boyan
 * Copyright 2014-2014 Elias Ku
 * Copyright 2017-2018 Ilya Malanin
 * Copyright 2018-2020 Eric Bishton
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

import com.intellij.codeInsight.daemon.impl.HighlightRangeExtension;
import com.intellij.codeInspection.InspectionToolProvider;
import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.lang.annotation.Annotation;
import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.Annotator;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Condition;
import com.intellij.openapi.util.TextRange;
import com.intellij.plugins.haxe.HaxeBundle;
import com.intellij.plugins.haxe.HaxeLanguage;
import com.intellij.plugins.haxe.ide.generation.OverrideImplementMethodFix;
import com.intellij.plugins.haxe.ide.quickfix.CreateGetterSetterQuickfix;
import com.intellij.plugins.haxe.ide.quickfix.HaxeSwitchMutabilityModifier;
import com.intellij.plugins.haxe.lang.lexer.HaxeTokenTypes;
import com.intellij.plugins.haxe.lang.psi.*;
import com.intellij.plugins.haxe.model.*;
import com.intellij.plugins.haxe.model.fixer.*;
import com.intellij.plugins.haxe.model.type.*;
import com.intellij.plugins.haxe.util.*;
import com.intellij.psi.*;
import com.intellij.util.containers.ContainerUtil;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;

import static com.intellij.plugins.haxe.ide.annotator.HaxeStandardAnnotation.returnTypeMismatch;
import static com.intellij.plugins.haxe.ide.annotator.HaxeStandardAnnotation.typeMismatch;
import static com.intellij.plugins.haxe.ide.annotator.SemanticAnnotatorInspections.*;
import static com.intellij.plugins.haxe.lang.psi.HaxePsiModifier.*;

public class HaxeSemanticAnnotator implements Annotator, HighlightRangeExtension {

  @Override
  public boolean isForceHighlightParents(@NotNull PsiFile file) {
    // XXX: Maybe more complex logic will be required if we only want this to be true when
    // doing semantic annotations.
    return (file.getLanguage().isKindOf(HaxeLanguage.INSTANCE));
  }

  public static SemanticAnnotatorInspections.Registrar getInspectionProvider() {
    return new SemanticAnnotatorInspections.Registrar();
  }

  @Override
  public void annotate(@NotNull PsiElement element, @NotNull AnnotationHolder holder) {
    if (element.getLanguage() == HaxeLanguage.INSTANCE) {
      analyzeSingle(element, new HaxeAnnotationHolder(holder));
    }
  }

  private static void analyzeSingle(final PsiElement element, HaxeAnnotationHolder holder) {
    if (element instanceof HaxePackageStatement) {
      PackageChecker.check((HaxePackageStatement)element, holder);
    } else if (element instanceof HaxeMethod) {
      MethodChecker.check((HaxeMethod)element, holder);
    } else if (element instanceof HaxeClass) {
      ClassChecker.check((HaxeClass)element, holder);
    } else if (element instanceof HaxeType) {
      TypeChecker.check((HaxeType)element, holder);
    } else if (element instanceof HaxeFieldDeclaration) {
      FieldChecker.check((HaxeFieldDeclaration)element, holder);
    } else if (element instanceof HaxeLocalVarDeclaration) {
      LocalVarChecker.check((HaxeLocalVarDeclaration)element, holder);
    } else if (element instanceof HaxeStringLiteralExpression) {
      StringChecker.check((HaxeStringLiteralExpression)element, holder);
    } else if (element instanceof HaxeTypeCheckExpr) {
      TypeCheckExpressionChecker.check((HaxeTypeCheckExpr)element, holder);
    } else if (element instanceof HaxeAssignExpression) {
      AssignExpressionChecker.check((HaxeAssignExpression)element, holder);
    } else if (element instanceof HaxeIsTypeExpression) {
      IsTypeExpressionChecker.check((HaxeIsTypeExpression)element, holder);
    }
  }
}

/**
 * List of inspections that need to be displayed in the settings dialogs.
 */
enum SemanticAnnotatorInspections {

  ASSIGNMENT_TYPE_COMPATIBILITY_CHECK(new AssignmentTypeCompatibilityInspection()),
  DUPLICATE_CLASS_MODIFIERS(new DuplicateClassModifierInspection()),
  DUPLICATE_FIELDS(new DuplicateFieldInspection()),
  FIELD_REDEFINITION(new FieldRedefinitionInspection()),
  FINAL_FIELD_IS_INITIALIZED(new FinalFieldIsInitializedInspection()),
  INCOMPATIBLE_INITIALIZATION(new IncompatibleInitializationInspection()),
  INCOMPATIBLE_TYPE_CHECKS(new IncompatibleTypeChecksInspection()),
  INHERITED_INTERFACE_METHOD_SIGNATURE(new InheritedInterfaceMethodSignatureInspection()),
  INTERFACE_METHOD_SIGNATURE(new InterfaceMethodSignatureInspection()),
  INVALID_TYPE_NAME(new InvalidTypeNameInspection()),
  IS_TYPE_INSPECTION(new IsTypeExpressionInspection()),
  METHOD_OVERRIDE_CHECK(new MethodOverrideInspection()),
  METHOD_SIGNATURE_COMPATIBILITY(new MethodSignatureCompatiblityInspection()),
  MISSING_INTERFACE_METHODS(new MissingInterfaceMethodInspection()),
  MISSING_TYPE_TAG_ON_EXTERN_AND_INTERFACE(new MissingTypeTagOnExternAndInterfaceInspection()),
  OPTIONAL_WITH_INITIALIZER(new InitializerOnOptionalMethodArgumentInspection()),
  PACKAGE_NAME_CHECK(new PackageNameInspection()),
  PARAMETER_INITIALIZER_TYPES(new ParameterInitializerTypeInspection()),
  PARAMETER_ORDERING_CHECK(new ParameterOrderingInspection()),
  PROPERTY_ACCESSOR_EXISTENCE(new PropertyAccessorExistenceInspection()),
  PROPERTY_ACCESSOR_VALID(new PropertyAccessorValidInspection()),
  PROPERTY_CANNOT_BE_FINAL(new PropertyCannotBeFinalInspection()),
  PROPERTY_IS_NOT_REAL_VARIABLE(new PropertyIsNotARealVarialeInspection()),
  REPEATED_PARAMETER_NAME_CHECK(new ParameterNameDuplicatedInspection()),
  STRING_INTERPOLATION_QUOTE_CHECK(new StringInterpolationQuoteInspection()),
  SUPERCLASS_TYPE_COMPATIBILITY(new SuperclassTypeCompatibilityInspection()),
  SUPERINTERFACE_TYPE(new SuperInterfaceTypeCompatibilityInspection()),
  ;

  HaxeAnnotatorInspection inspection;

  public boolean isEnabled(PsiElement element) {return inspection.isEnabled(element);}

  SemanticAnnotatorInspections(@NotNull HaxeAnnotatorInspection inspection) {
    this.inspection = inspection;
  }

  public static class Registrar implements InspectionToolProvider {
    @NotNull
    @Override
    public Class<? extends LocalInspectionTool>[] getInspectionClasses() {
      SemanticAnnotatorInspections[] constants = SemanticAnnotatorInspections.class.getEnumConstants();
      int length = constants == null ? 0 : constants.length;
      //noinspection unchecked
      Class<? extends HaxeAnnotatorInspection>[] classes = new Class[length];

      for (int i = 0; i < length; i++) {
        SemanticAnnotatorInspections sai = constants[i];
        classes[i] = sai.inspection.getClass();
      }

      return classes;
    }
  }

  // We *have* to use discrete classes for each inspection because the dialog
  // mechanism works directly with the class (not the instance!) and has to find
  // a no-argument constructor.  It won't work with a single class and a closure,
  // either, because the closure is not available at the time that the class is loaded.

  public static class AssignmentTypeCompatibilityInspection extends HaxeAnnotatorInspection {
    public AssignmentTypeCompatibilityInspection() {
      super("haxe.inspections.assignment.type.compatibility.name",
            "haxe.inspections.assignment.type.compatibility.description");
    }
  }

  public static class DuplicateClassModifierInspection extends HaxeAnnotatorInspection {
    public DuplicateClassModifierInspection() {
      super("haxe.inspections.duplicate.class.modifier.name",
            "haxe.inspections.duplicate.class.modifier.description");
    }
  }

  public static class DuplicateFieldInspection extends HaxeAnnotatorInspection {
    public DuplicateFieldInspection() {
      super("haxe.inspections.duplicated.field.name",
            "haxe.inspections.duplicated.field.description");
    }
  }

  public static class FieldRedefinitionInspection extends HaxeAnnotatorInspection {
    public FieldRedefinitionInspection() {
      super("haxe.inspections.field.redefinition.inspection.name",
            "haxe.inspections.field.redefinition.inspection.description");
    }
  }

  public static class FinalFieldIsInitializedInspection extends HaxeAnnotatorInspection {
    public FinalFieldIsInitializedInspection() {
      super("haxe.inspections.final.field.is.initialized.inspection.name",
            "haxe.inspections.final.field.is.initialized.inspection.description");
    }
  }

  public static class IncompatibleInitializationInspection extends HaxeAnnotatorInspection {
    public IncompatibleInitializationInspection() {
      super("haxe.inspections.incompatible.initialization.inspection.name",
            "haxe.inspections.incompatible.initialization.inspection.description");
    }
  }

  public static class IncompatibleTypeChecksInspection extends HaxeAnnotatorInspection {
    public IncompatibleTypeChecksInspection() {
      super("haxe.inspections.incompatible.type.checks.inspection.name");
    }
  }

  public static class InheritedInterfaceMethodSignatureInspection extends HaxeAnnotatorInspection {
    public InheritedInterfaceMethodSignatureInspection() {
      super("haxe.inspections.inherited.interface.method.signature.name",
            "haxe.inspections.inherited.interface.method.signature.description");
    }
  }

  public static class InitializerOnOptionalMethodArgumentInspection extends HaxeAnnotatorInspection {
    public InitializerOnOptionalMethodArgumentInspection() {
      super("haxe.inspections.initializer.on.optional.method.argument.name",
            "haxe.inspections.initializer.on.optional.method.argument.description");
    }
  }

  public static class InterfaceMethodSignatureInspection extends HaxeAnnotatorInspection {
    public InterfaceMethodSignatureInspection() {
      super("haxe.inspections.interface.methods.signature.name",
            "haxe.inspections.interface.methods.signature.description");
    }
  }

  public static class InvalidTypeNameInspection extends HaxeAnnotatorInspection {
    public InvalidTypeNameInspection() {
      super("haxe.inspections.type.name.casing.name",
            "haxe.inspections.type.name.casing.description");
    }
  }

  public static class IsTypeExpressionInspection extends HaxeAnnotatorInspection {
    public IsTypeExpressionInspection() {
      super("haxe.inspections.is.type.expression.inspection.name",
            "haxe.inspections.is.type.expression.inspection.description");
    }
  }

  public static class MethodOverrideInspection extends HaxeAnnotatorInspection {
    public MethodOverrideInspection() {
      super("haxe.inspections.method.override.name",
            "haxe.inspections.method.override.description");
    }
  }

  public static class MethodSignatureCompatiblityInspection extends HaxeAnnotatorInspection {
    public MethodSignatureCompatiblityInspection() {
      super("haxe.inspections.method.signature.compatiblity.name",
            "haxe.inspections.method.signature.compatiblity.description");
    }
  }

  public static class MissingInterfaceMethodInspection extends HaxeAnnotatorInspection {
    public MissingInterfaceMethodInspection() {
      super("haxe.inspections.missing.interface.methods.name",
            "haxe.inspections.missing.interface.methods.description");
    }
  }

  public static class MissingTypeTagOnExternAndInterfaceInspection extends HaxeAnnotatorInspection {
    public MissingTypeTagOnExternAndInterfaceInspection() {
      super("haxe.inspections.missing.type.tag.on.extern.or.interface.name",
            "haxe.inspections.missing.type.tag.on.extern.or.interface.description");
    }
  }

  public static class PackageNameInspection extends HaxeAnnotatorInspection {
    public PackageNameInspection() {
      super("haxe.inspections.package.name.name",
            "haxe.inspections.package.name.description");
    }
  }

  public static class ParameterInitializerTypeInspection extends HaxeAnnotatorInspection {
    public ParameterInitializerTypeInspection() {
      super("haxe.inspections.parameter.initializer.type.name",
            "haxe.inspections.parameter.initializer.type.description");
    }
  }

  public static class ParameterNameDuplicatedInspection extends HaxeAnnotatorInspection {
    public ParameterNameDuplicatedInspection() {
      super("haxe.inspections.parameter.name.duplicated.name",
            "haxe.inspections.parameter.name.duplicated.description");
    }
  }

  public static class ParameterOrderingInspection extends HaxeAnnotatorInspection {
    public ParameterOrderingInspection() {
      super("haxe.inspections.parameter.ordering.name",
            "haxe.inspections.parameter.ordering.description");
    }
  }

  public static class PropertyAccessorExistenceInspection extends HaxeAnnotatorInspection {
    public PropertyAccessorExistenceInspection() {
      super("haxe.inspections.property.accessor.existence.name",
            "haxe.inspections.property.accessor.existence.description");
    }
  }

  public static class PropertyAccessorValidInspection extends HaxeAnnotatorInspection {
    public PropertyAccessorValidInspection() {
      super("haxe.inspections.property.accessor.valid.name",
            "haxe.inspections.property.accessor.valid.description");
    }
  }

  public static class PropertyCannotBeFinalInspection extends HaxeAnnotatorInspection {
    public PropertyCannotBeFinalInspection() {
      super("haxe.inspections.property.cannot.be.final.name",
            "haxe.inspections.property.cannot.be.final.description");
    }
  }

  public static class PropertyIsNotARealVarialeInspection extends HaxeAnnotatorInspection {
    public PropertyIsNotARealVarialeInspection() {
      super("haxe.inspections.property.is.not.a.real.variable.name",
            "haxe.inspections.property.is.not.a.real.variable.description");
    }
  }

  public static class StringInterpolationQuoteInspection extends HaxeAnnotatorInspection {
    public StringInterpolationQuoteInspection() {
      super("haxe.inspections.string.interpolation.quote.name",
            "haxe.inspections.string.interpolation.quote.description");
    }
  }

  public static class SuperclassTypeCompatibilityInspection extends HaxeAnnotatorInspection {
    public SuperclassTypeCompatibilityInspection() {
      super("haxe.inspections.superclass.type.compatibility.name",
            "haxe.inspections.superclass.type.compatibility.description");
    }
  }

  public static class SuperInterfaceTypeCompatibilityInspection extends HaxeAnnotatorInspection {
    public SuperInterfaceTypeCompatibilityInspection() {
      super("haxe.inspections.superinterface.type.compatibility.name",
            "haxe.inspections.superinterface.type.compatibility.description");
    }
  }
}


class IsTypeExpressionChecker {
  public static void check(
    final HaxeIsTypeExpression expr,
    final HaxeAnnotationHolder holder
  ) {
    if (!IS_TYPE_INSPECTION.isEnabled(expr)) return;


  }
}

class TypeCheckExpressionChecker {
  public static void check(
    final HaxeTypeCheckExpr expr,
    final HaxeAnnotationHolder holder
  ) {
    if (!INCOMPATIBLE_TYPE_CHECKS.isEnabled(expr)) return;

    final PsiElement[] children = expr.getChildren();
    if (children.length == 2) {
      final HaxeGenericResolver resolver = HaxeGenericResolverUtil.generateResolverFromScopeParents(expr);
      final ResultHolder statementResult = HaxeTypeResolver.getPsiElementType(children[0], expr, resolver);
      ResultHolder assertionResult = SpecificTypeReference.getUnknown(expr).createHolder();
      if (children[1] instanceof HaxeTypeOrAnonymous) {
        assertionResult = HaxeTypeResolver.getTypeFromTypeOrAnonymous((HaxeTypeOrAnonymous)children[1]);
        ResultHolder resolveResult = resolver.resolve(assertionResult.getType().toStringWithoutConstant());
        if (null != resolveResult) {
          assertionResult = resolveResult;
        }
      }
      if (!assertionResult.canAssign(statementResult)) {
        final HaxeDocumentModel document = HaxeDocumentModel.fromElement(expr);
        Annotation annotation = holder.createErrorAnnotation(children[0],
                                                             HaxeBundle.message("haxe.semantic.statement.does.not.unify.with.asserted.type",
                                                                                statementResult.getType().toStringWithoutConstant(),
                                                                                assertionResult.getType().toStringWithoutConstant()));
        annotation.registerFix(new HaxeFixer(HaxeBundle.message("haxe.quickfix.remove.type.check")) {
          @Override
          public void run() {
            document.replaceElementText(expr, children[0].getText());
          }
        });
        annotation.registerFix(new HaxeFixer(HaxeBundle.message("haxe.quickfix.change.type.check.to.0", statementResult.toStringWithoutConstant())) {
          @Override
          public void run( ) {
            document.replaceElementText(children[1], statementResult.toStringWithoutConstant());
          }
        });
        // TODO: Add type conversion fixers. (eg. Wrap with Std.int(), wrap with Std.toString())
      }
    }
  }
}



class TypeTagChecker {
  public static void check(
    final PsiElement erroredElement,
    final HaxeTypeTag tag,
    final HaxeVarInit initExpression,
    boolean requireConstant,
    final HaxeAnnotationHolder holder
  ) {
    final ResultHolder varType = HaxeTypeResolver.getTypeFromTypeTag(tag, erroredElement);
    final ResultHolder initType = getTypeFromVarInit(initExpression);

    if (!varType.canAssign(initType)) {

      HaxeAnnotation annotation = typeMismatch(erroredElement, initType.toStringWithoutConstant(),
                                               varType.toStringWithoutConstant());
      if (null != initType.getClassType()) {
        annotation.withFix(new HaxeTypeTagChangeFixer(HaxeBundle.message("haxe.quickfix.change.variable.type"), tag, initType.getClassType()));
      }

      annotation.withFix(new HaxeRemoveElementFixer(HaxeBundle.message("haxe.quickfix.remove.initializer"), initExpression))
        .withFixes(HaxeExpressionConversionFixer.createStdTypeFixers(initExpression.getExpression(),
                                                                     initType.getType(), varType.getType()));
      holder.addAnnotation(annotation);

    } else if (requireConstant && initType.getType().getConstant() == null) {
      holder.createErrorAnnotation(erroredElement, HaxeBundle.message("haxe.semantic.parameter.default.type.should.be.constant", initType));
    }
  }

  @NotNull
  private static ResultHolder getTypeFromVarInit(@NotNull HaxeVarInit init) {
    HaxeExpression initExpression = init.getExpression();
    HaxeGenericResolver resolver = HaxeGenericResolverUtil.generateResolverFromScopeParents(initExpression);

    final ResultHolder abstractEnumFieldInitType = HaxeAbstractEnumUtil.getStaticMemberExpression(initExpression, resolver);
    if (abstractEnumFieldInitType != null) {
      return abstractEnumFieldInitType;
    }

    // fallback to simple init expression
    return null != initExpression ? HaxeTypeResolver.getPsiElementType(initExpression, init, resolver)
                                  : SpecificTypeReference.getInvalid(init).createHolder();
  }
}

class LocalVarChecker {
  public static void check(final HaxeLocalVarDeclaration var, final HaxeAnnotationHolder holder) {
    if (!INCOMPATIBLE_INITIALIZATION.isEnabled(var)) return;

    HaxeLocalVarModel local = new HaxeLocalVarModel(var);
    if (local.hasInitializer() && local.hasTypeTag()) {
      TypeTagChecker.check(local.getBasePsi(), local.getTypeTagPsi(), local.getInitializerPsi(), false, holder);
    }
  }
}


class FieldChecker {
  public static void check(final HaxeFieldDeclaration var, final HaxeAnnotationHolder holder) {
    HaxeFieldModel field = new HaxeFieldModel(var);
    if (field.isProperty()) {
      checkProperty(field, holder);
    } else {
      if (FINAL_FIELD_IS_INITIALIZED.isEnabled(var)) {
        if (field.isFinal()) {
          if (!field.hasInitializer()) {
            if (!isParentInterface(var)) {
              if (field.isStatic()) {
                holder.createErrorAnnotation(var, HaxeBundle.message("haxe.semantic.final.static.var.init", field.getName()));
              } else if (!isFieldInitializedInTheConstructor(field)) {
                holder.createErrorAnnotation(var, HaxeBundle.message("haxe.semantic.final.var.init", field.getName()));
              }
            }
          } else {
            if (isParentInterface(var)) {
              holder.createErrorAnnotation(var, HaxeBundle.message("haxe.semantic.final.static.var.init.interface", field.getName()));
            }
          }
        }
      }
    }

    if (field.hasInitializer() && field.hasTypeTag()) {
      TypeTagChecker.check(field.getBasePsi(), field.getTypeTagPsi(), field.getInitializerPsi(), false, holder);
    }


    // Checking for variable redefinition.
    if (FIELD_REDEFINITION.isEnabled(var)) {
      HashSet<HaxeClassModel> classSet = new HashSet<>();
      HaxeClassModel fieldDeclaringClass = field.getDeclaringClass();
      classSet.add(fieldDeclaringClass);
      while (fieldDeclaringClass != null) {
        fieldDeclaringClass = fieldDeclaringClass.getParentClass();
        if (classSet.contains(fieldDeclaringClass)) {
          break;
        }
        else {
          classSet.add(fieldDeclaringClass);
        }
        if (fieldDeclaringClass != null) {
          for (HaxeFieldModel parentField : fieldDeclaringClass.getFields()) {
            if (parentField.getName().equals(field.getName())) {
              String message;
              if (parentField.isStatic()) {
                message = HaxeBundle.message("haxe.semantic.static.field.override", field.getName());
                holder.createWeakWarningAnnotation(field.getNameOrBasePsi(), message);
              }
              else {
                message = HaxeBundle.message("haxe.semantic.variable.redefinition", field.getName(), fieldDeclaringClass.getName());
                holder.createErrorAnnotation(field.getBasePsi(), message);
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

  private static void checkProperty(final HaxeFieldModel field, final HaxeAnnotationHolder holder) {
    final HaxeDocumentModel document = field.getDocument();

    PsiElement fieldBasePsi = field.getBasePsi();
    if (PROPERTY_ACCESSOR_VALID.isEnabled(fieldBasePsi)) {
// TODO: Bug here.  (set,get) are being marked as errors.
      if (field.getGetterPsi() != null && !field.getGetterType().isValidGetter()) {
        holder.createErrorAnnotation(field.getGetterPsi(), "Invalid getter accessor");
      }

      if (field.getSetterPsi() != null && !field.getSetterType().isValidSetter()) {
        holder.createErrorAnnotation(field.getSetterPsi(), "Invalid setter accessor");
      }
    }

    if (field.isFinal()) {
      if (PROPERTY_CANNOT_BE_FINAL.isEnabled(fieldBasePsi)) {
        holder
          .createErrorAnnotation(fieldBasePsi, HaxeBundle.message("haxe.semantic.property.cant.be.final"))
          .registerFix(new HaxeSwitchMutabilityModifier((HaxeFieldDeclaration)fieldBasePsi));
      }
    } else {
      if (PROPERTY_IS_NOT_REAL_VARIABLE.isEnabled(fieldBasePsi)) {
        final HaxeVarInit initializerPsi = field.getInitializerPsi();
        if (field.isProperty() && !field.isRealVar() && null != initializerPsi) {
          Annotation annotation = holder.createErrorAnnotation(
            initializerPsi,
            "This field cannot be initialized because it is not a real variable"
          );
          annotation.registerFix(new HaxeFixer("Remove init") {
            @Override
            public void run() {
              document.replaceElementText(initializerPsi, "", StripSpaces.BEFORE);
            }
          });
          annotation.registerFix(new HaxeFixer("Add @:isVar") {
            @Override
            public void run() {
              field.getModifiers().addModifier(IS_VAR);
            }
          });
          if (field.getSetterPsi() != null) {
            annotation.registerFix(new HaxeFixer("Make setter null") {
              @Override
              public void run() {
                document.replaceElementText(field.getSetterPsi(), "null");
              }
            });
          }
        }
      }
    }
    checkPropertyAccessorMethods(field, holder);
  }

  private static void checkPropertyAccessorMethods(final HaxeFieldModel field, final HaxeAnnotationHolder holder) {
    if (!PROPERTY_ACCESSOR_EXISTENCE.isEnabled(field.getBasePsi())) {
      return;
    }

    if (field.getDeclaringClass().isInterface()) {
      return;
    }

    if (field.getGetterType() == HaxeAccessorType.GET) {
      final String methodName = "get_" + field.getName();

      HaxeMethodModel method = field.getDeclaringClass().getMethod(methodName, null);
      if (method == null && field.getGetterPsi() != null) {
        holder
          .createErrorAnnotation(field.getGetterPsi(), "Can't find method " + methodName)
          .registerFix(new CreateGetterSetterQuickfix(field.getDeclaringClass(), field, true));
      }
    }

    if (field.getSetterType() == HaxeAccessorType.SET) {
      final String methodName = "set_" + field.getName();

      HaxeMethodModel method = field.getDeclaringClass().getMethod(methodName, null);
      if (method == null && field.getSetterPsi() != null) {
        holder
          .createErrorAnnotation(field.getSetterPsi(), "Can't find method " + methodName)
          .registerFix(new CreateGetterSetterQuickfix(field.getDeclaringClass(), field, false));
      }
    }
  }
}

class TypeChecker {
  static public void check(final HaxeType type, final HaxeAnnotationHolder holder) {
    check(type.getReferenceExpression().getIdentifier(), holder);
  }

  static public void check(final PsiIdentifier identifier, final HaxeAnnotationHolder holder) {
    if (identifier == null) return;
    if (!INVALID_TYPE_NAME.isEnabled(identifier)) return;

    final String typeName = identifier.getText();
    if (!HaxeClassModel.isValidClassName(typeName)) {
      Annotation annotation = holder.createErrorAnnotation(identifier, "Type name must start by upper case");
      annotation.registerFix(new HaxeFixer("Change name") {
        @Override
        public void run() {
          HaxeDocumentModel.fromElement(identifier).replaceElementText(
            identifier,
            typeName.substring(0, 1).toUpperCase() + typeName.substring(1)
          );
        }
      });
    }
  }
}

class ClassChecker {
  static public void check(final HaxeClass clazzPsi, final HaxeAnnotationHolder holder) {
    HaxeClassModel clazz = clazzPsi.getModel();
    checkModifiers(clazz, holder);
    checkDuplicatedFields(clazz, holder);
    checkClassName(clazz, holder);
    checkInterfaces(clazz, holder);
    checkExtends(clazz, holder);
    checkInterfacesMethods(clazz, holder);
    // TODO: checkInterfacesFields for properties and vars.
  }

  static private void checkModifiers(final HaxeClassModel clazz, final HaxeAnnotationHolder holder) {
    if (!DUPLICATE_CLASS_MODIFIERS.isEnabled(clazz.getBasePsi())) return;

    HaxeClassModifierList modifiers = clazz.getModifiers();
    if (null != modifiers) {
      List<HaxeClassModifier> list = modifiers.getClassModifierList();
      checkForDuplicateModifier(holder, "private",
                                list.stream()
                                  .filter((modifier)->!Objects.isNull(modifier.getPrivateKeyWord()))
                                  .collect(Collectors.toList()));
      checkForDuplicateModifier(holder, "final",
                                list.stream()
                                  .filter((modifier)->!Objects.isNull(modifier.getFinalKeyWord()))
                                  .collect(Collectors.toList()));
      if (modifiers instanceof HaxeExternClassModifierList) {
        checkForDuplicateModifier(holder, "extern", ((HaxeExternClassModifierList)modifiers).getExternKeyWordList());
      }
    }
  }

  private static void checkForDuplicateModifier(@NotNull HaxeAnnotationHolder holder, @NotNull String modifier, @Nullable List<? extends PsiElement> elements) {
    if (null != elements && elements.size() > 1) {
      for (int i = 1; i < elements.size(); ++i) {
        reportDuplicateModifier(holder, modifier, elements.get(i));
      }
    }
  }

  private static void reportDuplicateModifier(HaxeAnnotationHolder holder, String modifier, final PsiElement element) {
    String message = HaxeBundle.message("haxe.semantic.key.must.not.be.repeated.for.class.declaration", modifier);
    Annotation annotation = holder.createErrorAnnotation(element, message);
    final HaxeDocumentModel document = HaxeDocumentModel.fromElement(element);
    if (null != document) {
      annotation.registerFix(new HaxeFixer(HaxeBundle.message("haxe.quickfix.remove.duplicate", modifier)) {
        @Override
        public void run() {
          document.replaceElementText(element, "", StripSpaces.AFTER);
        }
      });
    }
  }


  static private void checkDuplicatedFields(final HaxeClassModel clazz, final HaxeAnnotationHolder holder) {
    if (!DUPLICATE_FIELDS.isEnabled(clazz.getBasePsi())) return;

    Map<String, HaxeMemberModel> map = new HashMap<>();
    Set<HaxeMemberModel> repeatedMembers = new HashSet<>();
    for (HaxeMemberModel member : clazz.getMembersSelf()) {
      final String memberName = member.getName();
      HaxeMemberModel repeatedMember = map.get(memberName);
      if (repeatedMember != null) {
        repeatedMembers.add(member);
        repeatedMembers.add(repeatedMember);
      } else {
        map.put(memberName, member);
      }
    }

    for (HaxeMemberModel member : repeatedMembers) {
      holder.createErrorAnnotation(member.getNameOrBasePsi(), "Duplicate class field declaration : " + member.getName());
    }


    //Duplicate class field declaration
  }

  static private void checkClassName(final HaxeClassModel clazz, final HaxeAnnotationHolder holder) {
    TypeChecker.check(clazz.getNamePsi(), holder);
  }

  private static void checkExtends(final HaxeClassModel clazz, final HaxeAnnotationHolder holder) {
    if (!SUPERCLASS_TYPE_COMPATIBILITY.isEnabled(clazz.getBasePsi())) return;

    HaxeClassModel reference = clazz.getParentClass(); // Get first in extends list, not PSI parent.
    // TODO: Need to loop over all interfaces or types.
    if (reference != null) {
      if (isAnonymousType(clazz)) {
        if (!isAnonymousType(reference)) {
          // @TODO: Move to bundle
          holder.createErrorAnnotation(clazz.haxeClass.getHaxeExtendsList().get(0), "Not an anonymous type");
        }
      } else if (clazz.isInterface()) {
        if (!reference.isInterface()) {
          // @TODO: Move to bundle
          holder.createErrorAnnotation(reference.getPsi(), "Not an interface");
        }
      } else if (!reference.isClass()) {
        // @TODO: Move to bundle
        holder.createErrorAnnotation(reference.getPsi(), "Not a class");
      }

      final String qname1 = reference.haxeClass.getQualifiedName();
      final String qname2 = clazz.haxeClass.getQualifiedName();
      if (qname1.equals(qname2)) {
        // @TODO: Move to bundle
        holder.createErrorAnnotation(clazz.haxeClass.getHaxeExtendsList().get(0), "Cannot extend self");
      }
    }
  }

  static private boolean isAnonymousType(HaxeClassModel clazz) {
    if (clazz != null && clazz.haxeClass != null) {
      HaxeClass haxeClass = clazz.haxeClass;
      if (haxeClass instanceof HaxeAnonymousType) {
        return true;
      }
      if (haxeClass instanceof HaxeTypedefDeclaration) {
        HaxeTypeOrAnonymous anonOrType = ((HaxeTypedefDeclaration)haxeClass).getTypeOrAnonymous();
        if (anonOrType != null) {
          return anonOrType.getAnonymousType() != null;
        }
      }
    }
    return false;
  }

  private static void checkInterfaces(final HaxeClassModel clazz, final HaxeAnnotationHolder holder) {
    if (!SUPERINTERFACE_TYPE.isEnabled(clazz.getBasePsi())) return;

    for (HaxeClassReferenceModel interfaze : clazz.getImplementingInterfaces()) {
      HaxeClassModel interfazeClass = interfaze.getHaxeClass();
      boolean isDynamic = null != interfazeClass ? SpecificHaxeClassReference.withoutGenerics(interfazeClass.getReference()).isDynamic() : false;
      if (interfazeClass == null || !(interfazeClass.isInterface() || isDynamic) ) {
        holder.createErrorAnnotation(interfaze.getPsi(), HaxeBundle.message("haxe.semantic.interface.error.message"));
      }
    }
  }

  private static void checkInterfacesMethods(final HaxeClassModel clazz, final HaxeAnnotationHolder holder) {
    PsiElement clazzPsi = clazz.getPsi();
    boolean checkMissingInterfaceMethods = MISSING_INTERFACE_METHODS.isEnabled(clazzPsi);
    boolean checkInterfaceMethodSignature = INTERFACE_METHOD_SIGNATURE.isEnabled(clazzPsi);
    boolean checkInheritedInterfaceMethodSignature = INHERITED_INTERFACE_METHOD_SIGNATURE.isEnabled(clazzPsi);

    if (!checkMissingInterfaceMethods && !checkInterfaceMethodSignature && !checkInheritedInterfaceMethodSignature) {
      return;
    }

    for (HaxeClassReferenceModel reference : clazz.getImplementingInterfaces()) {
      checkInterfaceMethods(clazz, reference, holder, checkMissingInterfaceMethods, checkInterfaceMethodSignature, checkInheritedInterfaceMethodSignature);
    }
  }

  private static void checkInterfaceMethods(
    final HaxeClassModel clazz,
    final HaxeClassReferenceModel intReference,
    final HaxeAnnotationHolder holder,
    final boolean checkMissingInterfaceMethods,
    final boolean checkInterfaceMethodSignature,
    final boolean checkInheritedInterfaceMethodSignature
  ) {
    final List<HaxeMethodModel> missingMethods = new ArrayList<HaxeMethodModel>();
    final List<String> missingMethodsNames = new ArrayList<String>();

    if (intReference.getHaxeClass() != null) {
      for (HaxeMethodModel intMethod : intReference.getHaxeClass().getMethods(null)) {
        if (!intMethod.isStatic()) {
          // Implemented method not necessarily located in current class
          final PsiMethod[] methods = clazz.haxeClass.findMethodsByName(intMethod.getName(), true);
          final PsiMethod psiMethod = ContainerUtil.find(methods, new Condition<PsiMethod>() {
            @Override
            public boolean value(PsiMethod method) {
              return method instanceof HaxeMethod;
            }
          });

          if (psiMethod == null) {
            if (checkMissingInterfaceMethods) {
              missingMethods.add(intMethod);
              missingMethodsNames.add(intMethod.getName());
            }
          } else {
            final HaxeMethod method = (HaxeMethod)psiMethod;
            final HaxeMethodModel methodModel = method.getModel();

            // We should check if signature in inherited method differs from method provided by interface
            if (methodModel.getDeclaringClass() != clazz) {
              if (checkInheritedInterfaceMethodSignature && MethodChecker.checkIfMethodSignatureDiffers(methodModel, intMethod)) {
                final HaxeClass parentClass = methodModel.getDeclaringClass().haxeClass;

                final String errorMessage = HaxeBundle.message(
                  "haxe.semantic.implemented.super.method.signature.differs",
                  method.getName(),
                  parentClass.getQualifiedName(),
                  intMethod.getPresentableText(HaxeMethodContext.NO_EXTENSION),
                  methodModel.getPresentableText(HaxeMethodContext.NO_EXTENSION)
                );

                holder.createErrorAnnotation(intReference.getPsi(), errorMessage);
              }
            } else {
              if (checkInterfaceMethodSignature) {
                MethodChecker.checkMethodsSignatureCompatibility(methodModel, intMethod, holder);
              }
            }
          }
        }
      }
    }

    if (missingMethods.size() > 0) {
      // @TODO: Move to bundle
      Annotation annotation = holder.createErrorAnnotation(
        intReference.getPsi(),
        "Not implemented methods: " + StringUtils.join(missingMethodsNames, ", ")
      );
      annotation.registerFix(new HaxeFixer("Implement methods") {
        @Override
        public void run() {
          OverrideImplementMethodFix fix = new OverrideImplementMethodFix(clazz.haxeClass, false);
          for (HaxeMethodModel mm : missingMethods) {
            fix.addElementToProcess(mm.getMethodPsi());
          }

          PsiElement basePsi = clazz.getBasePsi();
          Project p = basePsi.getProject();
          fix.invoke(p, FileEditorManager.getInstance(p).getSelectedTextEditor(), basePsi.getContainingFile());
        }
      });
    }
  }
}

class MethodChecker {
  static public void check(final HaxeMethod methodPsi, final HaxeAnnotationHolder holder) {
    final HaxeMethodModel currentMethod = methodPsi.getModel();
    checkTypeTagInInterfacesAndExternClass(currentMethod, holder);
    checkMethodArguments(currentMethod, holder);
    checkOverride(methodPsi, holder);
    if (HaxeSemanticAnnotatorConfig.ENABLE_EXPERIMENTAL_BODY_CHECK) {
      MethodBodyChecker.check(methodPsi, holder);
    }
    //currentMethod.getBodyPsi()
  }

  private static void checkTypeTagInInterfacesAndExternClass(final HaxeMethodModel currentMethod, final HaxeAnnotationHolder holder) {
    if (!MISSING_TYPE_TAG_ON_EXTERN_AND_INTERFACE.isEnabled(currentMethod.getBasePsi())) return;

    HaxeClassModel currentClass = currentMethod.getDeclaringClass();
    if (currentClass.isExtern() || currentClass.isInterface()) {
      if (currentMethod.getReturnTypeTagPsi() == null && !currentMethod.isConstructor()) {
        holder.createErrorAnnotation(currentMethod.getNameOrBasePsi(), HaxeBundle.message("haxe.semantic.type.required"));
      }
      for (final HaxeParameterModel param : currentMethod.getParameters()) {
        if (param.getTypeTagPsi() == null) {
          holder.createErrorAnnotation(param.getBasePsi(), HaxeBundle.message("haxe.semantic.type.required"));
        }
      }
    }
  }

  private static void checkMethodArguments(final HaxeMethodModel currentMethod, final HaxeAnnotationHolder holder) {
    PsiElement methodPsi = currentMethod.getBasePsi();
    boolean checkOptionalWithInit = OPTIONAL_WITH_INITIALIZER.isEnabled(methodPsi);
    boolean checkParameterInitializers = PARAMETER_INITIALIZER_TYPES.isEnabled(methodPsi);
    boolean checkParameterOrdering = PARAMETER_ORDERING_CHECK.isEnabled(methodPsi);
    boolean checkRepeatedParameterName = REPEATED_PARAMETER_NAME_CHECK.isEnabled(methodPsi);

    if (!checkOptionalWithInit
    &&  !checkParameterInitializers
    &&  !checkParameterOrdering
    &&  !checkRepeatedParameterName) {
      return;
    }

    boolean hasOptional = false;
    HashMap<String, PsiElement> argumentNames = new HashMap<String, PsiElement>();
    for (final HaxeParameterModel param : currentMethod.getParameters()) {
      String paramName = param.getName();

      if (checkOptionalWithInit) {
        if (param.hasOptionalPsi() && param.getVarInitPsi() != null) {
          // @TODO: Move to bundle
          holder.createWarningAnnotation(param.getOptionalPsi(), "Optional not needed when specified an init value");
        }
      }

      if (checkParameterInitializers) {
        if (param.getVarInitPsi() != null && param.getTypeTagPsi() != null) {
          TypeTagChecker.check(
            param.getBasePsi(),
            param.getTypeTagPsi(),
            param.getVarInitPsi(),
            true,
            holder
          );
        }
      }

      if (checkParameterOrdering) {
        if (param.isOptional()) {
          hasOptional = true;
        } else if (hasOptional) {
          // @TODO: Move to bundle
          holder.createWarningAnnotation(param.getBasePsi(), "Non-optional argument after optional argument");
        }
      }

      if (checkRepeatedParameterName) {
        if (argumentNames.containsKey(paramName)) {
          // @TODO: Move to bundle
          holder.createWarningAnnotation(param.getNamePsi(), "Repeated argument name '" + paramName + "'");
          holder.createWarningAnnotation(argumentNames.get(paramName), "Repeated argument name '" + paramName + "'");
        } else {
          argumentNames.put(paramName, param.getNamePsi());
        }
      }
    }
  }

  private static final String[] OVERRIDE_FORBIDDEN_MODIFIERS = {FINAL, INLINE, STATIC};
  private static void checkOverride(final HaxeMethod methodPsi, final HaxeAnnotationHolder holder) {
    final HaxeMethodModel currentMethod = methodPsi.getModel();
    final HaxeClassModel currentClass = currentMethod.getDeclaringClass();
    final HaxeModifiersModel currentModifiers = currentMethod.getModifiers();

    final HaxeClassModel parentClass = (currentClass != null) ? currentClass.getParentClass() : null;
    final HaxeMethodModel parentMethod = parentClass != null ? parentClass.getMethod(currentMethod.getName(), null) : null;
    final HaxeModifiersModel parentModifiers = (parentMethod != null) ? parentMethod.getModifiers() : null;

    if (!METHOD_OVERRIDE_CHECK.isEnabled(methodPsi)) { // TODO: This check is not granular enough.
      // If the rest of the checks are disabled, we don't want to inhibit the signature check.
      if (null != parentMethod) {
        checkMethodsSignatureCompatibility(currentMethod, parentMethod, holder);
      }
      return;
    }

    boolean requiredOverride = false;

    if (currentMethod.isConstructor()) {
      if (currentModifiers.hasModifier(STATIC)) {
        // @TODO: Move to bundle
        holder.createErrorAnnotation(currentMethod.getNameOrBasePsi(), "Constructor can't be static").registerFix(
          new HaxeModifierRemoveFixer(currentModifiers, STATIC)
        );
      }
    } else if (currentMethod.isStaticInit()) {
      if (!currentModifiers.hasModifier(STATIC)) {
        holder.createErrorAnnotation(currentMethod.getNameOrBasePsi(), "__init__ must be static").registerFix(
          new HaxeModifierAddFixer(currentModifiers, STATIC)
        );
      }
    } else if (parentMethod != null) {
      if (parentMethod.isStatic()) {
        holder.createWarningAnnotation(currentMethod.getNameOrBasePsi(), "Method '" + currentMethod.getName()
                                                                         + "' overrides a static method of a superclass");
      } else {
        requiredOverride = true;

        if (parentModifiers.hasAnyModifier(OVERRIDE_FORBIDDEN_MODIFIERS)) {
          Annotation annotation =
            holder.createErrorAnnotation(currentMethod.getNameOrBasePsi(), "Can't override static, inline or final methods");
          for (String modifier : OVERRIDE_FORBIDDEN_MODIFIERS) {
            if (parentModifiers.hasModifier(modifier)) {
              annotation.registerFix(
                new HaxeModifierRemoveFixer(parentModifiers, modifier, "Remove " + modifier + " from " + parentMethod.getFullName())
              );
            }
          }
        }

        if (HaxePsiModifier.hasLowerVisibilityThan(currentModifiers.getVisibility(), parentModifiers.getVisibility())) {
          Annotation annotation = holder.createWarningAnnotation(
            currentMethod.getNameOrBasePsi(),
            "Field " +
            currentMethod.getName() +
            " has less visibility (public/private) than superclass one."
          );
          annotation.registerFix(
            new HaxeModifierReplaceVisibilityFixer(currentModifiers, parentModifiers.getVisibility(), "Change current method visibility"));
          annotation.registerFix(
            new HaxeModifierReplaceVisibilityFixer(parentModifiers, currentModifiers.getVisibility(), "Change parent method visibility"));
        }
      }
    }

    //System.out.println(aClass);
    if (currentModifiers.hasModifier(OVERRIDE) && !requiredOverride) {
      holder.createErrorAnnotation(currentModifiers.getModifierPsi(OVERRIDE), "Overriding nothing").registerFix(
        new HaxeModifierRemoveFixer(currentModifiers, OVERRIDE)
      );
    } else if (requiredOverride) {
      if (!currentModifiers.hasModifier(OVERRIDE)) {
        holder.createErrorAnnotation(currentMethod.getNameOrBasePsi(), "Must override").registerFix(
          new HaxeModifierAddFixer(currentModifiers, OVERRIDE)
        );
      } else {
        // It is rightly overriden. Now check the signature.
        checkMethodsSignatureCompatibility(currentMethod, parentMethod, holder);
      }
    }
  }

  static void checkMethodsSignatureCompatibility(
    @NotNull final HaxeMethodModel currentMethod,
    @NotNull final HaxeMethodModel parentMethod,
    final HaxeAnnotationHolder holder
  ) {
    if (!METHOD_SIGNATURE_COMPATIBILITY.isEnabled(currentMethod.getBasePsi())) return;

    final HaxeDocumentModel document = currentMethod.getDocument();

    List<HaxeParameterModel> currentParameters = currentMethod.getParameters();
    final List<HaxeParameterModel> parentParameters = parentMethod.getParameters();
    int minParameters = Math.min(currentParameters.size(), parentParameters.size());

    if (currentParameters.size() > parentParameters.size()) {
      for (int n = minParameters; n < currentParameters.size(); n++) {
        final HaxeParameterModel currentParam = currentParameters.get(n);
        holder.createErrorAnnotation(currentParam.getBasePsi(), "Unexpected argument").registerFix(
          new HaxeFixer("Remove argument") {
            @Override
            public void run() {
              currentParam.remove();
            }
          });
      }
    } else if (currentParameters.size() != parentParameters.size()) {
      holder.createErrorAnnotation(
        currentMethod.getNameOrBasePsi(),
        "Not matching arity expected " +
        parentParameters.size() +
        " arguments but found " +
        currentParameters.size()
      );
    }

    HaxeGenericResolver scopeResolver = HaxeGenericResolverUtil.generateResolverFromScopeParents(currentMethod.getBasePsi());

    for (int n = 0; n < minParameters; n++) {
      final HaxeParameterModel currentParam = currentParameters.get(n);
      final HaxeParameterModel parentParam = parentParameters.get(n);

      // We cannot simply check that the two types are the same when they have type arguments;
      // the arguments may not resolve to the same thing.  So, we need to resolve the element
      // in the super-class before we can check assignment compatibility.
      SpecificHaxeClassReference resolvedParent = resolveSuperclassElement(scopeResolver, currentParam, parentParam);

      // Order of assignment compatibility is to parent, from subclass.
      ResultHolder currentParamType = currentParam.getType(scopeResolver);
      ResultHolder parentParamType = parentParam.getType(null == resolvedParent ? scopeResolver : resolvedParent.getGenericResolver());
      if (!HaxeTypeCompatible.canAssignToFrom(parentParamType, currentParamType)) {
        HaxeAnnotation annotation =
          typeMismatch(currentParam.getBasePsi(), currentParamType.toString(), parentParamType.toString())
          .withFix(HaxeFixer.create(HaxeBundle.message("haxe.semantic.change.type"), ()->{
            document.replaceElementText(currentParam.getTypeTagPsi(), parentParam.getTypeTagPsi().getText());
          }));
        holder.addAnnotation(annotation);
      }

      if (currentParam.hasOptionalPsi() != parentParam.hasOptionalPsi()) {
        final boolean removeOptional = currentParam.hasOptionalPsi();

        String errorMessage;
        if (parentMethod.getDeclaringClass().isInterface()) {
          errorMessage = removeOptional ? "haxe.semantic.implemented.method.parameter.required"
                                        : "haxe.semantic.implemented.method.parameter.optional";
        } else {
          errorMessage = removeOptional ? "haxe.semantic.overwritten.method.parameter.required"
                                        : "haxe.semantic.overwritten.method.parameter.optional";
        }

        errorMessage = HaxeBundle.message(errorMessage, parentParam.getPresentableText(),
                                          parentMethod.getDeclaringClass().getName() + "." + parentMethod.getName());

        final Annotation annotation = holder.createErrorAnnotation(currentParam.getBasePsi(), errorMessage);
        final String localFixName = HaxeBundle.message(removeOptional ? "haxe.semantic.method.parameter.optional.remove"
                                                                      : "haxe.semantic.method.parameter.optional.add");

        annotation.registerFix(
          new HaxeFixer(localFixName) {
            @Override
            public void run() {
              if (removeOptional) {
                currentParam.getOptionalPsi().delete();
              } else {
                PsiElement element = currentParam.getBasePsi();
                document.addTextBeforeElement(element.getFirstChild(), "?");
              }
            }
          }
        );
      }
    }

    // Check the return type...

    // Again, the super-class may resolve with different/incompatible type arguments.
    SpecificHaxeClassReference resolvedParent = resolveSuperclassElement(scopeResolver, currentMethod, parentMethod);

    ResultHolder currentResult = currentMethod.getResultType(scopeResolver);
    ResultHolder parentResult = parentMethod.getResultType(resolvedParent != null ? resolvedParent.getGenericResolver() : scopeResolver);

    // Order of assignment compatibility is to parent, from subclass.
    if (!HaxeTypeCompatible.canAssignToFrom(parentResult.getType(), currentResult.getType())) {
      PsiElement psi = currentMethod.getReturnTypeTagOrNameOrBasePsi();
      HaxeAnnotation annotation =
        returnTypeMismatch(psi, currentResult.getType().toStringWithoutConstant(), parentResult.getType().toStringWithConstant())
          .withFix(HaxeFixer.create(HaxeBundle.message("haxe.semantic.change.type"), ()->{
            document.replaceElementText(currentResult.getElementContext(), parentResult.toStringWithoutConstant());
          }));
      holder.addAnnotation(annotation);
    }
  }

  @Nullable
  private static SpecificHaxeClassReference resolveSuperclassElement(HaxeGenericResolver scopeResolver,
                                                                     HaxeModel currentElement,
                                                                     HaxeModel parentParam) {
    HaxeGenericSpecialization scopeSpecialization = HaxeGenericSpecialization.fromGenericResolver(currentElement.getBasePsi(), scopeResolver);
    HaxeClassResolveResult superclassResult = HaxeResolveUtil.getSuperclassResolveResult(parentParam.getBasePsi(),
                                                                                         currentElement.getBasePsi(),
                                                                                         scopeSpecialization);
    if (superclassResult == HaxeClassResolveResult.EMPTY) {
      // TODO: Create Unresolved annotation??
      HaxeDebugLogger.getLogger().warn("Couldn't resolve a parameter type from a subclass for " + currentElement.getName());
    }

    SpecificHaxeClassReference resolvedParent = null;
    HaxeGenericResolver superResolver = superclassResult.getGenericResolver();
    HaxeClass superClass = superclassResult.getHaxeClass();
    if (null != superClass) {
      HaxeClassReference superclassReference = new HaxeClassReference(superClass.getModel(), currentElement.getBasePsi());
      resolvedParent = SpecificHaxeClassReference.withGenerics(superclassReference, superResolver.getSpecificsFor(superClass));
    }
    return resolvedParent;
  }

  // Fast check without annotations
  static boolean checkIfMethodSignatureDiffers(HaxeMethodModel source, HaxeMethodModel prototype) {
    final List<HaxeParameterModel> sourceParameters = source.getParameters();
    final List<HaxeParameterModel> prototypeParameters = prototype.getParameters();

    if (sourceParameters.size() != prototypeParameters.size()) {
      return true;
    }

    final int parametersCount = sourceParameters.size();

    for (int n = 0; n < parametersCount; n++) {
      final HaxeParameterModel sourceParam = sourceParameters.get(n);
      final HaxeParameterModel prototypeParam = prototypeParameters.get(n);
      if (!HaxeTypeCompatible.canAssignToFrom(sourceParam.getType(), prototypeParam.getType()) ||
          sourceParam.isOptional() != prototypeParam.isOptional()) {
        return true;
      }
    }

    ResultHolder currentResult = source.getResultType();
    ResultHolder prototypeResult = prototype.getResultType();

    return !currentResult.canAssign(prototypeResult);
  }
}

class PackageChecker {
  static public void check(final HaxePackageStatement element, final HaxeAnnotationHolder holder) {
    if (!PACKAGE_NAME_CHECK.isEnabled(element)) return;

    final HaxeReferenceExpression expression = element.getReferenceExpression();
    String packageName = (expression != null) ? expression.getText() : "";
    PsiDirectory fileDirectory = element.getContainingFile().getParent();
    if (fileDirectory == null) return;
    List<PsiFileSystemItem> fileRange = PsiFileUtils.getRange(PsiFileUtils.findRoot(fileDirectory), fileDirectory);
    fileRange.remove(0);
    String actualPath = PsiFileUtils.getListPath(fileRange);
    final String actualPackage = actualPath.replace('/', '.');
    final String actualPackage2 = HaxeResolveUtil.getPackageName(element.getContainingFile());
    // @TODO: Should use HaxeResolveUtil

    for (String s : StringUtils.split(packageName, '.')) {
      if (!s.substring(0, 1).toLowerCase().equals(s.substring(0, 1))) {
        //HaxeSemanticError.addError(element, new HaxeSemanticError("Package name '" + s + "' must start with a lower case character"));
        // @TODO: Move to bundle
        holder.createErrorAnnotation(element, "Package name '" + s + "' must start with a lower case character");
      }
    }

    if (!packageName.equals(actualPackage)) {
      holder.createErrorAnnotation(
        element,
        "Invalid package name! '" + packageName + "' should be '" + actualPackage + "'").registerFix(
        new HaxeFixer("Fix package") {
          @Override
          public void run() {
            Document document =
              PsiDocumentManager.getInstance(element.getProject()).getDocument(element.getContainingFile());

            if (expression != null) {
              TextRange range = expression.getTextRange();
              document.replaceString(range.getStartOffset(), range.getEndOffset(), actualPackage);
            } else {
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

class MethodBodyChecker {
  public static void check(HaxeMethod psi, HaxeAnnotationHolder holder) {
    final HaxeMethodModel method = psi.getModel();
    // Note: getPsiElementType runs a number of checks while determining the type.
    HaxeTypeResolver.getPsiElementType(method.getBodyPsi(), holder, generateConstraintResolver(method));
  }

  @NotNull
  private static HaxeGenericResolver generateConstraintResolver(HaxeMethodModel method) {
    HaxeGenericResolver resolver = new HaxeGenericResolver();
    for (HaxeGenericParamModel param : method.getGenericParams()) {
      ResultHolder constraint = param.getConstraint(resolver);
      if (null == constraint) {
        constraint = new ResultHolder(SpecificHaxeClassReference.getDynamic(param.getPsi()));
      }
      resolver.add(param.getName(), constraint);
    }
    return resolver;
  }
}

class StringChecker {
  public static void check(HaxeStringLiteralExpression psi, HaxeAnnotationHolder holder) {
    if (!STRING_INTERPOLATION_QUOTE_CHECK.isEnabled(psi)) return;

    if (isSingleQuotesRequired(psi)) {
      holder.createWarningAnnotation(psi, "Expressions that contains string interpolation should be wrapped with single quotes");
    }
  }

  private static boolean isSingleQuotesRequired(HaxeStringLiteralExpression psi) {
    return (psi.getLongTemplateEntryList().size() > 0 || psi.getShortTemplateEntryList().size() > 0) &&
           psi.getFirstChild().textContains('"');
  }
}

class InitVariableVisitor extends HaxeVisitor {
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
    if (expression instanceof HaxeReferenceExpression) {
      final HaxeReferenceExpression reference = (HaxeReferenceExpression)expression;
      final HaxeIdentifier identifier = reference.getIdentifier();

      if (identifier.getText().equals(fieldName)) {
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

class AssignExpressionChecker {
  public static void check(HaxeAssignExpression psi, HaxeAnnotationHolder holder) {
    if (!ASSIGNMENT_TYPE_COMPATIBILITY_CHECK.isEnabled(psi)) return;

    // TODO: Think about how to use models to do this instead. :/
    PsiElement lhs = UsefulPsiTreeUtil.getFirstChildSkipWhiteSpacesAndComments(psi);
    PsiElement assignOperation = UsefulPsiTreeUtil.getNextSiblingSkipWhiteSpacesAndComments(lhs);
    PsiElement rhs = UsefulPsiTreeUtil.getNextSiblingSkipWhiteSpacesAndComments(assignOperation);

    HaxeGenericResolver lhsResolver = HaxeGenericResolverUtil.generateResolverFromScopeParents(lhs);
    HaxeGenericResolver rhsResolver = HaxeGenericResolverUtil.generateResolverFromScopeParents(rhs);
    ResultHolder lhsType = HaxeTypeResolver.getPsiElementType(lhs, psi, lhsResolver);
    ResultHolder rhsType = HaxeTypeResolver.getPsiElementType(rhs, psi, rhsResolver);

    if (!lhsType.canAssign(rhsType)) {
      HaxeAnnotation anno = typeMismatch(rhs, rhsType.toPresentationString(), lhsType.toPresentationString())
        .withFixes(HaxeExpressionConversionFixer.createStdTypeFixers(rhs, rhsType.getType(), lhsType.getType()));
      holder.addAnnotation(anno);
    }
    if (lhsType.isImmutable()) {
      // TODO: Think about providing a quick-fix for immutability; remember final markings come from metadata, too.
      holder.createErrorAnnotation(psi, HaxeBundle.message("haxe.semantic.cannot.assign.value.to.final.variable"));
    }
  }
}