package com.intellij.plugins.haxe.ide.annotator.semantics;

import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.Annotator;
import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.plugins.haxe.lang.psi.HaxeClass;
import com.intellij.plugins.haxe.lang.psi.HaxeGenericParam;
import com.intellij.plugins.haxe.lang.psi.HaxeType;
import com.intellij.plugins.haxe.lang.psi.HaxeTypeTag;
import com.intellij.plugins.haxe.model.HaxeClassModel;
import com.intellij.plugins.haxe.model.HaxeDocumentModel;
import com.intellij.plugins.haxe.model.fixer.HaxeFixer;
import com.intellij.plugins.haxe.model.type.HaxeTypeResolver;
import com.intellij.plugins.haxe.model.type.SpecificHaxeClassReference;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiIdentifier;
import org.jetbrains.annotations.NotNull;

import java.util.stream.Stream;

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
    if (type.getContext() != null && type.getContext().getParent() instanceof HaxeTypeTag) {
      SpecificHaxeClassReference haxeClassReference = HaxeTypeResolver.getTypeFromType(type).getClassType();

      if (haxeClassReference != null) {
        HaxeClass haxeClass = haxeClassReference.getHaxeClass();
        if (haxeClass != null) {
          // Dynamic is special and does not require Type parameter to de specified
          if (DYNAMIC.equalsIgnoreCase(haxeClass.getName())) return;

          int typeParameterCount = countTypeParameters(haxeClassReference);
          int classParameterCount = countTypeParameters(haxeClass);

          if (typeParameterCount != classParameterCount) {
            String typeName = getTypeName(type.getReferenceExpression().getIdentifier());
            holder.newAnnotation(HighlightSeverity.ERROR, "Invalid number of type parameters for " + typeName)
              .range(type)
              .create();
          }
        }
      }
    }
  }

  static private int countTypeParameters(SpecificHaxeClassReference reference) {
    return (int)Stream.of(reference.getSpecifics()).filter(holder -> !holder.isUnknown()).count();
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
