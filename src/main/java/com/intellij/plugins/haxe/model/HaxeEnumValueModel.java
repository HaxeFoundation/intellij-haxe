package com.intellij.plugins.haxe.model;

import com.intellij.plugins.haxe.lang.psi.HaxeClass;
import com.intellij.plugins.haxe.lang.psi.HaxeEnumValueDeclaration;
import com.intellij.plugins.haxe.model.type.HaxeClassReference;
import com.intellij.plugins.haxe.model.type.HaxeGenericResolver;
import com.intellij.plugins.haxe.model.type.ResultHolder;
import com.intellij.plugins.haxe.model.type.SpecificHaxeClassReference;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiMember;
import org.jetbrains.annotations.Nullable;

/** An enum value, declared as a constructor (with arguments) or as a plain field; both models share the declaring-enum answers. */
public interface HaxeEnumValueModel extends HaxeNamedComponentModel {

  PsiMember getMemberPsi();

  ResultHolder getResultType(@Nullable HaxeGenericResolver resolver);

  @Nullable
  HaxeEnumValueDeclaration getEnumValuePsi();

  /** The enum declaring the value; null outside a Haxe class. */
  @Nullable
  default HaxeClassModel getDeclaringEnum() {
    return getMemberPsi().getContainingClass() instanceof HaxeClass haxeClass ? haxeClass.getModel() : null;
  }

  /** The declaring enum as the value's type, with the resolver's specifics when given; the models' getResultType returns this. */
  default ResultHolder declaringEnumType(@Nullable HaxeGenericResolver resolver) {
    PsiClass aClass = getMemberPsi().getContainingClass();
    if (!(aClass instanceof HaxeClass haxeClass)) return SpecificHaxeClassReference.getUnknown(aClass).createHolder();
    HaxeClassReference enumReference = new HaxeClassReference(haxeClass.getModel(), haxeClass);
    SpecificHaxeClassReference reference = resolver != null
                                           ? SpecificHaxeClassReference.withGenerics(enumReference, resolver.getSpecificsFor(haxeClass))
                                           : SpecificHaxeClassReference.withoutGenerics(enumReference);
    return reference.createHolder();
  }
}
