/*
 * Copyright 2020 Eric Bishton
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

import com.intellij.codeInspection.InspectionToolProvider;
import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;

/**
 * List of inspections that need to be displayed in the settings dialogs.
 */
public enum HaxeSemanticAnnotatorInspections {

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
  IS_TYPE_INSPECTION_4dot1_COMPATIBLE(new IsTypeExpressionInspection4dot1Compatible()),
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

  final HaxeAnnotatorInspection inspection;

  public boolean isEnabled(PsiElement element) {
    return inspection.isEnabled(element);
  }

  HaxeSemanticAnnotatorInspections(@NotNull HaxeAnnotatorInspection inspection) {
    this.inspection = inspection;
  }

  public static class Registrar implements InspectionToolProvider {
    @NotNull
    @Override
    public Class<? extends LocalInspectionTool>[] getInspectionClasses() {
      HaxeSemanticAnnotatorInspections[] constants = HaxeSemanticAnnotatorInspections.class.getEnumConstants();
      int length = constants == null ? 0 : constants.length;
      //noinspection unchecked
      Class<? extends HaxeAnnotatorInspection>[] classes = new Class[length];

      for (int i = 0; i < length; i++) {
        HaxeSemanticAnnotatorInspections sai = constants[i];
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

  public static class IsTypeExpressionInspection4dot1Compatible extends HaxeAnnotatorInspection {
    public IsTypeExpressionInspection4dot1Compatible() {
      super("haxe.inspections.is.type.expression.inspection.4dot1.compatible.name",
            "haxe.inspections.is.type.expression.inspection.4dot1.compatible.description");
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
      super("haxe.inspections.method.signature.compatibility.name",
            "haxe.inspections.method.signature.compatibility.description");
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
