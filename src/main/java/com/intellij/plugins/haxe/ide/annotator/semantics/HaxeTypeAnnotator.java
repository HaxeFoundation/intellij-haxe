package com.intellij.plugins.haxe.ide.annotator.semantics;

import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.Annotator;
import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.plugins.haxe.HaxeBundle;
import com.intellij.plugins.haxe.lang.psi.*;
import com.intellij.plugins.haxe.model.HaxeClassModel;
import com.intellij.plugins.haxe.model.HaxeDocumentModel;
import com.intellij.plugins.haxe.model.fixer.HaxeFixer;
import com.intellij.plugins.haxe.model.type.HaxeTypeResolver;
import com.intellij.plugins.haxe.model.type.SpecificHaxeClassReference;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiIdentifier;
import org.jetbrains.annotations.NotNull;

import static com.intellij.plugins.haxe.ide.annotator.HaxeSemanticAnnotatorInspections.INVALID_TYPE_NAME;
import static com.intellij.plugins.haxe.lang.psi.HaxePsiModifier.DYNAMIC;

public class HaxeTypeAnnotator implements Annotator {

  @Override
  public void annotate(@NotNull PsiElement element, @NotNull AnnotationHolder holder) {
    if (element instanceof HaxeType haxeType) {
      check(haxeType, holder);
    }
  }

  static public void check(final HaxeType type, final AnnotationHolder holder) {
    checkValidClassName(type.getReferenceExpression().getIdentifier(), holder);
    checkValidTypeParameters(type, holder);
  }

  //TODO mlo : extract to dumbAware?
  static public void checkValidClassName(final PsiIdentifier identifier, final AnnotationHolder holder) {
    if (identifier == null) return;
    if (!INVALID_TYPE_NAME.isEnabled(identifier)) return;

    final String typeName = getTypeName(identifier);
    if (!HaxeClassModel.isValidClassName(typeName)) {
      holder.newAnnotation(HighlightSeverity.ERROR, "Type name must start by upper case")
        .range(identifier)
        .withFix(new HaxeFixer("Change name") {
          @Override
          public void run() {
            HaxeDocumentModel.fromElement(identifier).replaceElementText(
              identifier,
              typeName.substring(0, 1).toUpperCase() + typeName.substring(1)
            );
          }
        }).create();
    }
  }

  static public void checkValidTypeParameters(final HaxeType type, final AnnotationHolder holder) {
    PsiElement context = type.getContext();
    if (context == null || context.getParent() == null) return;
    if (context instanceof HaxeNewExpression) {
      if (type.getTypeParam() != null) {
        checkTypeParametersForType(type, holder);
      }
    }
    else if (context.getParent() instanceof HaxeTypeTag) {
      if (context.getParent().getParent() instanceof HaxeParameter) {
        // if HaxeType is part of a method parameter then  type specifics are possibly inherited
        //  this check is currently only checking assignment to variables and arguments when calling methods
        return;
      }
      checkTypeParametersForType(type, holder);
    }
  }

  private static void checkTypeParametersForType(HaxeType type, AnnotationHolder holder) {
    SpecificHaxeClassReference haxeClassReference = HaxeTypeResolver.getTypeFromType(type).getClassType();
    if (haxeClassReference != null) {
      if (HaxeTypeResolver.isTypeParameter(type.getReferenceExpression())) {
        // ignoring  check if type is Type Parameter itself.
        return;
      }
      HaxeClass haxeClass = haxeClassReference.getHaxeClass();
      if (haxeClass != null) {
        // Dynamic is special and does not require Type parameter to de specified
        if (DYNAMIC.equalsIgnoreCase(haxeClass.getName())) return;
        int typeParameterCount = type.getTypeParam() == null ? 0 : type.getTypeParam().getTypeList().getTypeListPartList().size();
        int classParameterCount = countTypeParameters(haxeClass);

        if (typeParameterCount != classParameterCount) {
          String typeName = getTypeName(type.getReferenceExpression().getIdentifier());
          holder.newAnnotation(HighlightSeverity.ERROR,
                               HaxeBundle.message("haxe.inspections.parameter.count.mismatch.description", typeName, classParameterCount,
                                                  typeParameterCount))
            .range(type)
            .create();
        }
      }
    }
  }


  static private int countTypeParameters(HaxeClass haxeClass) {
    HaxeGenericParam param = haxeClass.getGenericParam();
    if (param == null) return 0;
    return param.getGenericListPartList().size();
  }

  private static String getTypeName(PsiIdentifier identifier) {
    return identifier.getText();
  }
}
