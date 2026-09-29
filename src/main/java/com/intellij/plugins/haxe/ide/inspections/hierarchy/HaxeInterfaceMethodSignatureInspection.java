package com.intellij.plugins.haxe.ide.inspections.hierarchy;

import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.plugins.haxe.lang.psi.HaxeClass;
import com.intellij.psi.PsiElementVisitor;
import org.jetbrains.annotations.NotNull;
import com.intellij.plugins.haxe.ide.annotator.HaxeProblemReporter;
import com.intellij.plugins.haxe.ide.annotator.semantics.AnnotatorUtil;
import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.plugins.haxe.HaxeBundle;
import com.intellij.plugins.haxe.HaxeComponentType;
import com.intellij.plugins.haxe.ide.generation.OverrideImplementMethodFix;
import com.intellij.plugins.haxe.lang.psi.*;
import com.intellij.plugins.haxe.model.*;
import com.intellij.plugins.haxe.model.fixer.HaxeFixer;
import com.intellij.plugins.haxe.model.type.HaxeGenericResolver;
import com.intellij.plugins.haxe.model.type.HaxeGenericResolverUtil;
import com.intellij.psi.PsiElement;
import org.apache.commons.lang3.StringUtils;
import java.util.*;
import static com.intellij.plugins.haxe.ide.annotator.semantics.AnnotatorUtil.hasMacroForCodeGeneration;
import static com.intellij.plugins.haxe.model.evaluator.assign.HaxeTypeCompatible.canAssignToFromReference;
import com.intellij.plugins.haxe.ide.inspections.HaxeInspection;

/** Implemented interface members whose signature or type does not conform. */
public class HaxeInterfaceMethodSignatureInspection extends HaxeInspection {

  private static final String ACCESSOR_DEFAULT = HaxeAccessorType.DEFAULT.text;
  private static final String ACCESSOR_NEVER = HaxeAccessorType.NEVER.text;
  private static final String ACCESSOR_NULL = HaxeAccessorType.NULL.text;
  private static final String ACCESSOR_DYNAMIC = HaxeAccessorType.DYNAMIC.text;

  @Override
  public @NotNull PsiElementVisitor buildVisitor(@NotNull ProblemsHolder holder, boolean isOnTheFly) {
    return checkVisitor(holder, HaxeClass.class, HaxeInterfaceMethodSignatureInspection::checkInterfaceMethodSignatures);
  }

  /** Interface method signature conformance, plus interface FIELD conformance. */
  public static void checkInterfaceMethodSignatures(final HaxeClass clazzPsi, final HaxeProblemReporter reporter) {
    HaxeClassModel clazz = HaxeClassInspectionUtil.modelToCheck(clazzPsi);
    if (clazz == null || clazzPsi.isInterface() || clazzPsi.isTypeDef()) return;
    HaxeClassInspectionUtil.checkImplementedInterfaces(clazz, reporter, false, true, false);
    checkInterfacesFields(clazz, reporter);
  }

  private static void checkInterfacesFields(final HaxeClassModel clazz, final HaxeProblemReporter reporter) {
    //TODO add settings for this feature

    for (HaxeClassReferenceModel reference : clazz.getImplementingInterfaces()) {
      checkInterfaceFields(clazz, reference, reporter);
    }
  }

  private static void checkInterfaceFields(
    final HaxeClassModel clazz,
    final HaxeClassReferenceModel intReference,
    final HaxeProblemReporter reporter) {

    final List<HaxeFieldModel> missingFields = new ArrayList<>();
    final List<String> missingFieldNames = new ArrayList<>();

    if (intReference.getHaxeClassModel() != null) {
      List<HaxeFieldDeclaration> fieldsInThisClass = clazz.haxeClass.getFieldSelf(clazz.getGenericResolver(null));
      List<HaxeNamedComponent> allFields = clazz.haxeClass.getHaxeFieldAll(HaxeComponentType.INTERFACE);
      for (HaxeFieldModel intField : intReference.getHaxeClassModel().getFields()) {
        if (!intField.isStatic()) {


          String interfaceFieldName = intField.getName();
          Optional<HaxeNamedComponent> fieldResultAll = allFields.stream()
            .filter(method -> interfaceFieldName.equals(method.getName()))
            .findFirst();
          Optional<HaxeFieldDeclaration> fieldResultClassOnly = fieldsInThisClass.stream()
            .filter(method -> interfaceFieldName.equals(method.getName()))
            .findFirst();

          if (fieldResultAll.isEmpty()) {
            missingFields.add(intField);
            missingFieldNames.add(interfaceFieldName);
          }
          else  if (fieldResultClassOnly.isPresent()){
            final HaxeFieldDeclaration fieldDeclaration = fieldResultClassOnly.get();

            if (intField.isProperty()) {
              String intGetterText = intField.getGetterText();
              String intSetterText = intField.getSetterText();
              HaxePropertyDeclaration propertyDeclaration = fieldDeclaration.getPropertyDeclaration();

              if (propertyDeclaration == null) {
                // some combinations are compatible with normal variables
                if (ACCESSOR_DEFAULT.equals(intGetterText) && (ACCESSOR_NEVER.equals(intSetterText) || ACCESSOR_NULL.equals(intSetterText))) {
                  continue;
                }
                if (ACCESSOR_NEVER.equals(intGetterText) && ACCESSOR_NULL.equals(intSetterText)) {
                  continue;
                }

                annotateDifferentAccess(intReference, reporter, fieldDeclaration, fieldDeclaration);
              }
              else {
                HaxePropertyAccessor getter = propertyDeclaration.getPropertyAccessorList().getFirst();
                HaxePropertyAccessor setter = propertyDeclaration.getPropertyAccessorList().get(1);

                if (intGetterText != null && getter != null) {
                  // never is just restricting visibility for interface (class may use different access)
                  // null only specifies access allowed from within the defining class (class may use different access)
                  // dynamic: Like get/set access, but does not verify the existence of the accessor field.
                  if (!ACCESSOR_NEVER.equals(intGetterText) && !ACCESSOR_NULL.equals(intGetterText) && !ACCESSOR_DYNAMIC.equals(intGetterText)) {
                    if (!intGetterText.equals(getter.getText())) {
                      annotateDifferentAccess(intReference, reporter, fieldDeclaration, getter.getElement());
                    }
                  }
                }

                if (intSetterText != null && setter != null) {
                  // never is just restricting visibility for interface (class may use different access)
                  // null only specifies access allowed from within the defining class (class may use different access )
                  // dynamic: Like get/set access, but does not verify the existence of the accessor field.
                  if (!ACCESSOR_NEVER.equals(intSetterText) && !ACCESSOR_NULL.equals(intSetterText) && !ACCESSOR_DYNAMIC.equals(intSetterText)) {
                    if (!intSetterText.equals(setter.getText())) {
                      annotateDifferentAccess(intReference, reporter, fieldDeclaration, setter.getElement());
                    }
                  }
                }
              }
            }

            HaxeFieldDeclaration intFieldDeclaration = (HaxeFieldDeclaration)intField.getPsiField();
            HaxeMutabilityModifier modifier = intFieldDeclaration.getMutabilityModifier();

            HaxeMutabilityModifier mutabilityModifier = fieldDeclaration.getMutabilityModifier();
            if (!modifier.textMatches(mutabilityModifier)) {

              String message = HaxeBundle.message("haxe.semantic.field.different.mutability",
                                                  fieldDeclaration.getName(),
                                                  intReference.getHaxeClassModel().getName());

              reporter.problem(HighlightSeverity.ERROR, message)
                .range(fieldDeclaration.getNode().getTextRange())
                .create();
            }

            HaxeFieldModel model = (HaxeFieldModel) fieldDeclaration.getModel();
            HaxeGenericResolver classFieldResolver = HaxeGenericResolverUtil.generateResolverFromScopeParents(model.getBasePsi());
            HaxeGenericResolver interfaceFieldResolver = HaxeGenericResolverUtil.generateResolverFromScopeParents(intField.getBasePsi());

            boolean typesAreCompatible =
                    canAssignToFromReference(intField.getResultType(interfaceFieldResolver), model.getResultType(classFieldResolver));

            if (!typesAreCompatible) {
              annotateDifferentType(intReference, reporter, fieldDeclaration);
            }
          }
        }
      }

      if (!missingFields.isEmpty()) {
        String fixPopupText = HaxeBundle.message("haxe.quickfix.implement.fields");
        String errorMessage = HaxeBundle.message("haxe.semantic.fields.not.implemented", StringUtils.join(missingFieldNames, ", "));

        reporter.problem(HighlightSeverity.ERROR, errorMessage)
          .range(intReference.getPsi())
          .withFix(new HaxeFixer(fixPopupText) {
            @Override
            public void run() {
              OverrideImplementMethodFix fix = new OverrideImplementMethodFix(clazz.haxeClass, false);
              for (HaxeFieldModel field : missingFields) {
                fix.addElementToProcess(field.getPsiField());
              }

              PsiElement basePsi = clazz.getBasePsi();
              Project p = basePsi.getProject();
              fix.invoke(p, FileEditorManager.getInstance(p).getSelectedTextEditor(), basePsi.getContainingFile());
            }
          })
          .create();
      }
    }
  }

  private static void annotateDifferentType(HaxeClassReferenceModel intReference, HaxeProblemReporter reporter, HaxeFieldDeclaration fieldDeclaration) {
    boolean macroCodegen = hasMacroCodeGen(fieldDeclaration);

    reporter.problem(macroCodegen ? HighlightSeverity.WEAK_WARNING : HighlightSeverity.ERROR,
                         "Field " + fieldDeclaration.getName() + " has different type than in  "
                         + intReference.getHaxeClassModel().getName())
      .range(fieldDeclaration.getNode().getTextRange())
      .create();

  }

  private static boolean hasMacroCodeGen(HaxeFieldDeclaration fieldDeclaration) {
    HaxeClass containingClass = (HaxeClass)fieldDeclaration.getContainingClass();
    if (containingClass == null) return false;

    HaxeClassModel model = containingClass.getModel();
    return  AnnotatorUtil.hasMacroForCodeGeneration(model);
  }

  private static void annotateDifferentAccess(HaxeClassReferenceModel intReference,
                                              HaxeProblemReporter reporter,
                                              HaxeFieldDeclaration fieldDeclaration,
                                              PsiElement rangeElement) {

    boolean macroCodegen = hasMacroCodeGen(fieldDeclaration);

    String message = HaxeBundle.message("haxe.semantic.field.different.access",
                                        fieldDeclaration.getName(),
                                        intReference.getHaxeClassModel().getName());

    reporter.problem(macroCodegen ? HighlightSeverity.WEAK_WARNING : HighlightSeverity.ERROR, message)
      .range(rangeElement)
      .create();
  }
}
