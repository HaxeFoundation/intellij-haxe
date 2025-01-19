package com.intellij.plugins.haxe.model;

import com.intellij.plugins.haxe.lang.psi.*;
import com.intellij.plugins.haxe.model.type.*;
import com.intellij.plugins.haxe.model.type.HaxeArgument;
import com.intellij.psi.PsiClass;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedList;
import java.util.List;


public class HaxeEnumValueConstructorModel extends HaxeMethodModel implements  HaxeEnumValueModel {

  private final HaxeEnumValueDeclarationConstructor myDeclaration;

  public HaxeEnumValueConstructorModel(@NotNull HaxeEnumValueDeclarationConstructor declaration) {
    super(declaration);
    myDeclaration = declaration;
  }


  @Override
  public boolean isStatic() {
    return true;
  }

  @Override
  public boolean isPublic() {
    return true;
  }

  public boolean isAbstractType() {
    return false;
  }

  @NotNull
  public HaxeEnumValueDeclarationConstructor getEnumValuePsi() {
    return myDeclaration;
  }

  @Nullable
  @Override
  public HaxeExposableModel getExhibitor() {
    return getDeclaringClass();
  }


  @Override
  public ResultHolder getResultType(@Nullable HaxeGenericResolver resolver) {
    PsiClass aClass = getMemberPsi().getContainingClass();
    if (aClass instanceof HaxeClass haxeClass) {

      HaxeClassReference superclassReference = new HaxeClassReference(haxeClass.getModel(), haxeClass);
      if (resolver != null) {
        SpecificHaxeClassReference reference =
          SpecificHaxeClassReference.withGenerics(superclassReference, resolver.getSpecificsFor(haxeClass));

        return reference.createHolder();
      } else {
        SpecificHaxeClassReference reference =
          SpecificHaxeClassReference.withoutGenerics(superclassReference);
        return reference.createHolder();
      }
    }
    return SpecificHaxeClassReference.getUnknown(aClass).createHolder();
  }


  @Nullable
  public HaxeClassModel getDeclaringEnum() {
    // TODO consider declaringClass (cached) instead
    PsiClass aClass = getMemberPsi().getContainingClass();
    if (aClass instanceof HaxeClass haxeClass) {
      return haxeClass.getModel();
    }
    return null;
  }

  @Override
  public SpecificFunctionReference getFunctionType(@Nullable HaxeGenericResolver resolver) {
    LinkedList<HaxeArgument> args = new LinkedList<>();
    List<HaxeParameterModel> parameters = this.getParameters();
    for (int i = 0; i < parameters.size(); i++) {
      HaxeParameterModel param = parameters.get(i);
      args.add(new HaxeArgument(param.getParameterPsi(), i, param.isOptional(), param.isRestOrMacroVarArg(), param.getType(resolver), param.getName()));
    }
    return new SpecificFunctionReference(args, getReturnType(resolver), this, getEnumValuePsi());
  }

  public ResultHolder getReturnType(@Nullable HaxeGenericResolver resolver) {
    HaxeClassModel declaringEnum = getDeclaringEnum();
    if (declaringEnum != null) {
      HaxeClassReference superclassReference = new HaxeClassReference(declaringEnum, declaringEnum.haxeClass);
      if (resolver != null) {
        @NotNull ResultHolder[] specificsFor = resolver.getSpecificsFor(declaringEnum.haxeClass);
        SpecificHaxeClassReference reference = SpecificHaxeClassReference.withGenerics(superclassReference, specificsFor);
        return reference.createHolder();
      } else {
        return declaringEnum.getInstanceType();
      }
    }

    return SpecificHaxeClassReference.getUnknown(basePsi).createHolder();
  }


  @Nullable
  public ResultHolder getParameterType(int index, HaxeGenericResolver resolver) {
    if (index < 0) return null;
    List<ResultHolder> parameters = getParameterTypes();
    if (index >= parameters.size()) return null;
    ResultHolder holder = parameters.get(index);
    if (holder.isTypeParameter()){
      ResultHolder resolve = resolver.resolve(holder);
      if(resolve != null && !resolve.isUnknown()) return resolve;
      return holder;
    }
    @NotNull ResultHolder[] specifics = resolver.getSpecifics();
    if (specifics.length> 0) {
      // drop one level of resolver as we need the resolver for the constructor argument not the result of enumValues
      SpecificHaxeClassReference classType = specifics[0].getClassType();
      if(classType != null) {
        HaxeGenericResolver subResolver = classType.getGenericResolver();
        return subResolver.resolve(holder);
      }
    }
    return resolver.resolve(holder);
  }

  private List<ResultHolder> getParameterTypes() {
    HaxeParameterList parameters = getConstructorParameters();
    if (parameters == null) return List.of();
    return parameters.getParameterList().stream()
      .map(parameter -> HaxeTypeResolver.getTypeFromTypeTag(parameter.getTypeTag(), parameters))
      .toList();
  }

  @Nullable
  public HaxeParameterList getConstructorParameters() {
    HaxeEnumValueDeclarationConstructor declaration = getEnumValuePsi();
    return declaration.getParameterList();
  }
}
