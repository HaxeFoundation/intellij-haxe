package com.intellij.plugins.haxe.ide;

import com.intellij.codeInspection.InspectionProfileEntry;
import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.plugins.haxe.ide.inspections.duplicates.HaxeDuplicateClassModifierInspection;
import com.intellij.plugins.haxe.ide.inspections.duplicates.HaxeDuplicateFieldInspection;
import com.intellij.plugins.haxe.ide.inspections.duplicates.HaxeParameterNameDuplicatedInspection;
import com.intellij.plugins.haxe.ide.inspections.hierarchy.*;
import com.intellij.plugins.haxe.ide.inspections.members.*;
import com.intellij.plugins.haxe.ide.inspections.operators.HaxeBinaryOperatorApplicabilityInspection;
import com.intellij.plugins.haxe.ide.inspections.operators.HaxeIsTypeExpressionInspection;
import com.intellij.plugins.haxe.ide.inspections.operators.HaxeUnaryOperatorApplicabilityInspection;
import com.intellij.plugins.haxe.ide.inspections.resolve.HaxeUnresolvedTypeInspection;
import com.intellij.plugins.haxe.ide.inspections.style.HaxeInvalidTypeNameInspection;
import com.intellij.plugins.haxe.ide.inspections.style.HaxePackageNameInspection;
import com.intellij.plugins.haxe.ide.inspections.style.HaxeStringInterpolationQuoteInspection;
import com.intellij.plugins.haxe.ide.inspections.typing.HaxeAssignmentTypeCompatibilityInspection;
import com.intellij.plugins.haxe.ide.inspections.typing.HaxeIncompatibleInitializationInspection;
import com.intellij.plugins.haxe.ide.inspections.typing.HaxeIncompatibleTypeChecksInspection;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * The semantic inspections a highlighting test enables as a block — the
 * fixture-side equivalent of their plugin.xml registrations. Kept as one
 * list so sibling test classes stay in sync.
 */
public final class HaxeInspectionTestTools {

  private static final List<Class<? extends LocalInspectionTool>> SEMANTIC_INSPECTIONS = List.of(
    HaxeAssignmentTypeCompatibilityInspection.class,
    HaxeBinaryOperatorApplicabilityInspection.class,
    HaxeDuplicateClassModifierInspection.class,
    HaxeDuplicateFieldInspection.class,
    HaxeFieldRedefinitionInspection.class,
    HaxeFinalFieldIsInitializedInspection.class,
    HaxeIncompatibleInitializationInspection.class,
    HaxeIncompatibleTypeChecksInspection.class,
    HaxeInheritedInterfaceMethodSignatureInspection.class,
    HaxeInterfaceMethodSignatureInspection.class,
    HaxeInvalidTypeNameInspection.class,
    HaxeIsTypeExpressionInspection.class,
    HaxeMethodOverrideInspection.class,
    HaxeMethodSignatureCompatibilityInspection.class,
    HaxeMissingInterfaceMethodInspection.class,
    HaxeMissingTypeTagOnExternAndInterfaceInspection.class,
    HaxePackageNameInspection.class,
    HaxeParameterInitializerTypeInspection.class,
    HaxeParameterNameDuplicatedInspection.class,
    HaxePropertyAccessorExistenceInspection.class,
    HaxePropertyAccessorValidInspection.class,
    HaxePropertyCannotBeFinalInspection.class,
    HaxePropertyIsNotARealVariableInspection.class,
    HaxeStringInterpolationQuoteInspection.class,
    HaxeSuperclassTypeCompatibilityInspection.class,
    HaxeSuperInterfaceTypeCompatibilityInspection.class,
    HaxeUnaryOperatorApplicabilityInspection.class,
    HaxeUnresolvedTypeInspection.class);

  private HaxeInspectionTestTools() {
  }

  /** All semantic inspections minus the unset ones, freshly instantiated. */
  public static InspectionProfileEntry[] semanticInspections(@Nullable Set<Class<? extends LocalInspectionTool>> unsetInspections)
    throws Exception {
    List<InspectionProfileEntry> tools = new ArrayList<>();
    for (Class<? extends LocalInspectionTool> c : SEMANTIC_INSPECTIONS) {
      if (null != unsetInspections && unsetInspections.contains(c)) continue;
      tools.add(c.getDeclaredConstructor().newInstance());
    }
    return tools.toArray(new InspectionProfileEntry[0]);
  }
}
