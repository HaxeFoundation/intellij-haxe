package com.intellij.plugins.haxe.ide.annotator.semantics;

import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.Annotator;
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
import com.intellij.plugins.haxe.model.type.SpecificHaxeClassReference;
import com.intellij.psi.PsiElement;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

import static com.intellij.plugins.haxe.ide.annotator.HaxeSemanticAnnotatorInspections.*;
import static com.intellij.plugins.haxe.ide.annotator.semantics.HaxeMethodAnnotator.checkIfMethodSignatureDiffers;
import static com.intellij.plugins.haxe.ide.annotator.semantics.HaxeMethodAnnotator.checkMethodsSignatureCompatibility;
import static com.intellij.plugins.haxe.model.type.HaxeTypeCompatible.canAssignToFrom;
import static java.util.stream.Collectors.toList;

public class HaxeClassAnnotator implements Annotator {

  @Override
  public void annotate(@NotNull PsiElement element, @NotNull AnnotationHolder holder) {
    if (element instanceof HaxeClass haxeClass) {
      check(haxeClass, holder);
    }
  }

  static public void check(final HaxeClass clazzPsi, final AnnotationHolder holder) {

    HaxeClassModel clazz = clazzPsi.getModel();
    if (clazzPsi instanceof HaxeAnonymousType && clazz.getParentClass() != null) {
      // avoiding unnecessary extra annotations when  HaxeAnonymousType is part of other non-anonymous types like typedefs etc.
      return;
    }

    checkModifiers(clazz, holder);
    checkDuplicatedFields(clazz, holder);
    checkClassName(clazz, holder);
    checkExtends(clazz, holder);
    if (!clazzPsi.isInterface() && !clazzPsi.isTypeDef()) {
      checkInterfaces(clazz, holder);
      checkInterfacesMethods(clazz, holder);
      checkInterfacesFields(clazz, holder);
    }
  }

  static private void checkModifiers(final HaxeClassModel clazz, final AnnotationHolder holder) {
    if (!DUPLICATE_CLASS_MODIFIERS.isEnabled(clazz.getBasePsi())) return;

    HaxeClassModifierList modifiers = clazz.getModifiers();
    if (null != modifiers) {
      List<HaxeClassModifier> list = modifiers.getClassModifierList();
      checkForDuplicateModifier(holder, "private",
                                list.stream()
                                  .filter((modifier) -> !Objects.isNull(modifier.getPrivateKeyWord()))
                                  .collect(toList()));
      checkForDuplicateModifier(holder, "final",
                                list.stream()
                                  .filter((modifier) -> !Objects.isNull(modifier.getFinalKeyWord()))
                                  .collect(toList()));
      if (modifiers instanceof HaxeExternClassModifierList) {
        checkForDuplicateModifier(holder, "extern", ((HaxeExternClassModifierList)modifiers).getExternKeyWordList());
      }
    }
  }

  private static void checkForDuplicateModifier(@NotNull AnnotationHolder holder,
                                                @NotNull String modifier,
                                                @Nullable List<? extends PsiElement> elements) {
    if (null != elements && elements.size() > 1) {
      for (int i = 1; i < elements.size(); ++i) {
        reportDuplicateModifier(holder, modifier, elements.get(i));
      }
    }
  }

  private static void reportDuplicateModifier(AnnotationHolder holder, String modifier, final PsiElement element) {
    final HaxeDocumentModel document = HaxeDocumentModel.fromElement(element);
    String message = HaxeBundle.message("haxe.semantic.key.must.not.be.repeated.for.class.declaration", modifier);
    holder.newAnnotation(HighlightSeverity.ERROR, message).range(element)
      .withFix(new HaxeFixer(HaxeBundle.message("haxe.quickfix.remove.duplicate", modifier)) {
        @Override
        public void run() {
          document.replaceElementText(element, "", StripSpaces.AFTER);
        }
      })
      .create();
  }


  static private void checkDuplicatedFields(final HaxeClassModel clazz, final AnnotationHolder holder) {
    if (!DUPLICATE_FIELDS.isEnabled(clazz.getBasePsi())) return;

    Map<String, HaxeMemberModel> map = new HashMap<>();
    Set<HaxeMemberModel> repeatedMembers = new HashSet<>();
    for (HaxeMemberModel member : clazz.getMembersSelf()) {
      final String memberName = member.getName();
      HaxeMemberModel repeatedMember = map.get(memberName);
      if (repeatedMember != null) {
        repeatedMembers.add(member);
        repeatedMembers.add(repeatedMember);
      }
      else {
        map.put(memberName, member);
      }
    }

    for (HaxeMemberModel member : repeatedMembers) {
      holder.newAnnotation(HighlightSeverity.ERROR, "Duplicate class field declaration : " + member.getName())
        .range(member.getNameOrBasePsi())
        .create();
    }


    //Duplicate class field declaration
  }

  static private void checkClassName(final HaxeClassModel clazz, final AnnotationHolder holder) {
    HaxeTypeAnnotator.checkValidClassName(clazz.getNamePsi(), holder);
  }

  private static void checkExtends(final HaxeClassModel clazz, final AnnotationHolder holder) {
    if (!SUPERCLASS_TYPE_COMPATIBILITY.isEnabled(clazz.getBasePsi())) return;

    HaxeClassModel reference = clazz.getParentClass(); // Get first in extends list, not PSI parent.
    // TODO: Need to loop over all interfaces or types.
    if (reference != null) {
      if (isAnonymousType(clazz)) {
        if (!isAnonymousType(reference)) {
          // @TODO: Move to bundle
          holder.newAnnotation(HighlightSeverity.ERROR, "Not an anonymous type").range(clazz.haxeClass.getHaxeExtendsList().get(0))
            .create();
        }
      }
      else if (clazz.isInterface()) {
        if (!reference.isInterface() && !reference.isTypedef()) {
          // @TODO: Move to bundle
          holder.newAnnotation(HighlightSeverity.ERROR, "Not an interface").range(reference.getPsi()).create();
        }
      }
      else if (clazz.isClass() ) {
         if (!reference.isClass() && !reference.isTypedef()) {
           // @TODO: Move to bundle
           holder.newAnnotation(HighlightSeverity.ERROR, "Not a class").range(reference.getPsi()).create();
         }
      }

      final String qname1 = reference.haxeClass.getQualifiedName();
      final String qname2 = clazz.haxeClass.getQualifiedName();
      if (qname1.equals(qname2)) {
        // @TODO: Move to bundle
        holder.newAnnotation(HighlightSeverity.ERROR, "Cannot extend self").range(clazz.haxeClass.getHaxeExtendsList().get(0)).create();
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

  private static void checkInterfaces(final HaxeClassModel clazz, final AnnotationHolder holder) {
    if (!SUPERINTERFACE_TYPE.isEnabled(clazz.getBasePsi())) return;

    for (HaxeClassReferenceModel interfaze : clazz.getImplementingInterfaces()) {
      HaxeClassModel interfazeClass = interfaze.getHaxeClass();
      boolean isDynamic =
        null != interfazeClass && SpecificHaxeClassReference.withoutGenerics(interfazeClass.getReference()).isDynamic();
      if (interfazeClass != null && !(interfazeClass.isInterface() || isDynamic)) {
        holder.newAnnotation(HighlightSeverity.ERROR, HaxeBundle.message("haxe.semantic.interface.error.message"))
          .range(interfaze.getPsi())
          .create();
      }
    }
  }

  private static void checkInterfacesMethods(final HaxeClassModel clazz, final AnnotationHolder holder) {
    PsiElement clazzPsi = clazz.getPsi();
    boolean checkMissingInterfaceMethods = MISSING_INTERFACE_METHODS.isEnabled(clazzPsi);
    boolean checkInterfaceMethodSignature = INTERFACE_METHOD_SIGNATURE.isEnabled(clazzPsi);
    boolean checkInheritedInterfaceMethodSignature = INHERITED_INTERFACE_METHOD_SIGNATURE.isEnabled(clazzPsi);

    if (!checkMissingInterfaceMethods && !checkInterfaceMethodSignature && !checkInheritedInterfaceMethodSignature) {
      return;
    }

    for (HaxeClassReferenceModel reference : clazz.getImplementingInterfaces()) {
      checkInterfaceMethods(clazz, reference, holder, checkMissingInterfaceMethods, checkInterfaceMethodSignature,
                            checkInheritedInterfaceMethodSignature);
    }
  }

  private static void checkInterfacesFields(final HaxeClassModel clazz, final AnnotationHolder holder) {
    //TODO add settings for this feature

    for (HaxeClassReferenceModel reference : clazz.getImplementingInterfaces()) {
      checkInterfaceFields(clazz, reference, holder);
    }
  }

  private static void checkInterfaceFields(
    final HaxeClassModel clazz,
    final HaxeClassReferenceModel intReference,
    final AnnotationHolder holder) {

    final List<HaxeFieldModel> missingFields = new ArrayList<>();
    final List<String> missingFieldNames = new ArrayList<>();

    if (intReference.getHaxeClass() != null) {
      List<HaxeFieldDeclaration> fields = clazz.haxeClass.getAllHaxeFields(HaxeComponentType.CLASS, HaxeComponentType.ENUM);
      for (HaxeFieldModel intField : intReference.getHaxeClass().getFields()) {
        if (!intField.isStatic()) {


          Optional<HaxeFieldDeclaration> fieldResult = fields.stream()
            .filter(method -> intField.getName().equals(method.getName()))
            .findFirst();

          if (fieldResult.isEmpty()) {
            missingFields.add(intField);
            missingFieldNames.add(intField.getName());
          }
          else {
            final HaxeFieldDeclaration fieldDeclaration = fieldResult.get();

            if (intField.getPropertyDeclarationPsi() != null) {
              HaxePropertyAccessor intGetter = intField.getGetterPsi();
              HaxePropertyAccessor intSetter = intField.getSetterPsi();
              HaxePropertyDeclaration propertyDeclaration = fieldDeclaration.getPropertyDeclaration();

              if (propertyDeclaration == null) {
                // some combinations are compatible with normal variables
                if (intGetter.textMatches("default") && (intSetter.textMatches("never") || intSetter.textMatches("null"))) {
                  continue;
                }
                if (intGetter.textMatches("never") && (intSetter.textMatches("null"))) {
                  continue;
                }

                // @TODO: Move error messages to  bundle
                holder.newAnnotation(HighlightSeverity.ERROR, "Field " + fieldDeclaration.getName()
                                                              + " has different property access than in  "
                                                              + intReference.getHaxeClass().getName())
                  .range(fieldDeclaration.getOriginalElement())
                  .create();
              }
              else {
                HaxePropertyAccessor getter = propertyDeclaration.getPropertyAccessorList().get(0);
                HaxePropertyAccessor setter = propertyDeclaration.getPropertyAccessorList().get(1);


                if (intGetter != null && getter != null) {
                  if (!intGetter.getText().equals(getter.getText())) {
                    holder.newAnnotation(HighlightSeverity.ERROR, "Field " + fieldDeclaration.getName()
                                                                  + " has different property access than in  "
                                                                  + intReference.getHaxeClass().getName())
                      .range(getter.getElement())
                      .create();
                  }
                }

                if (intSetter != null && setter != null) {
                  if (!intSetter.getText().equals(setter.getText())) {
                    holder.newAnnotation(HighlightSeverity.ERROR, "Field " + fieldDeclaration.getName()
                                                                  + " has different property access than in  "
                                                                  + intReference.getHaxeClass().getName())
                      .range(setter.getElement())
                      .create();
                  }
                }
              }
            }

            HaxeFieldDeclaration intFieldDeclaration = (HaxeFieldDeclaration)intField.getPsiField();
            HaxeMutabilityModifier modifier = intFieldDeclaration.getMutabilityModifier();

            HaxeMutabilityModifier mutabilityModifier = fieldDeclaration.getMutabilityModifier();
            if (!modifier.getText().equals(mutabilityModifier.getText())) {
              holder.newAnnotation(HighlightSeverity.ERROR, "Field " + fieldDeclaration.getName()
                                                            + " has different mutability than in  "
                                                            + intReference.getHaxeClass().getName())
                .range(fieldDeclaration.getNode())
                .create();
            }

            HaxeFieldModel model = new HaxeFieldModel(fieldDeclaration);
            HaxeGenericResolver classFieldResolver = HaxeGenericResolverUtil.generateResolverFromScopeParents(model.getBasePsi());
            HaxeGenericResolver interfaceFieldResolver = HaxeGenericResolverUtil.generateResolverFromScopeParents(intField.getBasePsi());

            boolean typesAreCompatible =
              canAssignToFrom(intField.getResultType(interfaceFieldResolver), model.getResultType(classFieldResolver));

            if (!typesAreCompatible) {
              holder.newAnnotation(HighlightSeverity.ERROR, "Field " + fieldDeclaration.getName()
                                                            + " has different type than in  "
                                                            + intReference.getHaxeClass().getName())
                .range(fieldDeclaration.getNode())
                .create();
            }
          }
        }
      }

      if (missingFields.size() > 0) {
        // @TODO: Move to bundle
        holder.newAnnotation(HighlightSeverity.ERROR, "Not implemented fields: " + StringUtils.join(missingFieldNames, ", "))
          .range(intReference.getPsi())
          .withFix(new HaxeFixer("Implement fields") {
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


  private static void checkInterfaceMethods(
    final HaxeClassModel clazz,
    final HaxeClassReferenceModel intReference,
    final AnnotationHolder holder,
    final boolean checkMissingInterfaceMethods,
    final boolean checkInterfaceMethodSignature,
    final boolean checkInheritedInterfaceMethodSignature
  ) {
    final List<HaxeMethodModel> missingMethods = new ArrayList<HaxeMethodModel>();
    final List<String> missingMethodsNames = new ArrayList<String>();

    if (intReference.getHaxeClass() != null) {
      List<HaxeMethodModel> methods = clazz.haxeClass.getAllHaxeMethods(HaxeComponentType.CLASS, HaxeComponentType.ENUM).stream()
        .map(HaxeMethodPsiMixin::getModel)
        .toList();

      for (HaxeMethodModel intMethod : intReference.getHaxeClass().getMethods(null)) {
        if (!intMethod.isStatic()) {

          Optional<HaxeMethodModel> methodResult = methods.stream()
            .filter(method -> intMethod.getName().equals(method.getName()))
            .findFirst();


          if (methodResult.isEmpty()) {
            if (checkMissingInterfaceMethods) {
              missingMethods.add(intMethod);
              missingMethodsNames.add(intMethod.getName());
            }
          }
          else {
            final HaxeMethodModel methodModel = methodResult.get();

            // We should check if signature in inherited method differs from method provided by interface
            if (methodModel.getDeclaringClass() != clazz) {
              if (methodModel.getDeclaringClass().isInterface()) {
                missingMethods.add(methodModel);
                missingMethodsNames.add(intMethod.getName());
              }
              else {
                if (checkInheritedInterfaceMethodSignature && checkIfMethodSignatureDiffers(methodModel, intMethod)) {
                  final HaxeClass parentClass = methodModel.getDeclaringClass().haxeClass;

                  final String errorMessage = HaxeBundle.message(
                    "haxe.semantic.implemented.super.method.signature.differs",
                    methodModel.getName(),
                    parentClass.getQualifiedName(),
                    intMethod.getPresentableText(HaxeMethodContext.NO_EXTENSION),
                    methodModel.getPresentableText(HaxeMethodContext.NO_EXTENSION)
                  );

                  holder.newAnnotation(HighlightSeverity.ERROR, errorMessage).range(intReference.getPsi()).create();
                }
              }
            }
            else {
              if (checkInterfaceMethodSignature) {
                checkMethodsSignatureCompatibility(methodModel, intMethod, holder);
              }
            }
          }
        }
      }
    }

    if (missingMethods.size() > 0) {
      // @TODO: Move to bundle
      holder.newAnnotation(HighlightSeverity.ERROR, "Not implemented methods: " + StringUtils.join(missingMethodsNames, ", "))
        .range(intReference.getPsi())
        .withFix(new HaxeFixer("Implement methods") {
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
        })
        .create();
    }
  }
}
