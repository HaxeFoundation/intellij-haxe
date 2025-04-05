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
package com.intellij.plugins.haxe.model.type;

import com.intellij.plugins.haxe.lang.psi.*;
import com.intellij.plugins.haxe.model.*;
import com.intellij.plugins.haxe.model.evaluator.HaxeExpressionEvaluatorContext;
import com.intellij.plugins.haxe.util.UsefulPsiTreeUtil;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class SpecificEnumValueReference extends SpecificTypeReference {

  public final HaxeEnumValueDeclaration declaration;
  public final SpecificHaxeClassReference enumClass;
  public final HaxeGenericResolver resolver;
  public final Object constantValue;

  SpecificFunctionReference constructor = null;

  // TODO: Need the parameter values???

  // TODO: Make this work for abstract enums, too...

  public SpecificEnumValueReference(@NotNull HaxeEnumValueDeclaration enumValue, @NotNull PsiElement context,
                                    @Nullable HaxeGenericResolver resolver) {
    this(enumValue, context, resolver, null);
  }

  public SpecificEnumValueReference(@NotNull HaxeEnumValueDeclaration enumValue, @NotNull PsiElement context,
                                    @NotNull HaxeGenericResolver resolver, @Nullable Object constantValue) {
    super(context);
    HaxeEnumDeclaration enumElement =
      UsefulPsiTreeUtil.getParentOfType(enumValue, HaxeEnumDeclaration.class);
    HaxeEnumModelImpl enumModel = new HaxeEnumModelImpl(enumElement);

    HaxeClassReference reference = enumModel.getReference();
    this.enumClass = SpecificHaxeClassReference.withGenerics(reference, resolver.getSpecificsFor(reference));
    this.declaration = enumValue;
    this.resolver = enumClass.getGenericResolver();
    this.constantValue = constantValue;
  }

  @NotNull
  public HaxeResolveResult asResolveResult() {
    return HaxeResolveResult.create(enumClass.getHaxeClass());
  }

  public SpecificEnumValueReference clone() {
    return new SpecificEnumValueReference(this.declaration, this.context, this.resolver, this.constantValue);
  }

  @NotNull
  public SpecificHaxeClassReference getEnumClass() {
    return enumClass;
  }

  @NotNull
  public HaxeEnumValueModel getModel() {
    return (HaxeEnumValueModel)declaration.getModel();
  }

  @NotNull
  public ResultHolder getType() {
    HaxeEnumValueModel model = getModel();
    ResultHolder type = model.getResultType(resolver);

    // When there is no type hint/tag on the enum, the resolver returns another reference, so we need to return the EnumValue class.
    if (null == type || type.getType() instanceof SpecificEnumValueReference) {
      type = SpecificHaxeClassReference.getEnumValue(context).createHolder();
    }

    return type;
  }


  @Override
  public SpecificTypeReference withConstantValue(Object constantValue) {
    return new SpecificEnumValueReference(this.declaration, this.context, this.resolver, constantValue);
  }

  @Override
  public String toPresentationString(boolean showOnlyConstraintForTypeParam) {
    StringBuilder out = new StringBuilder(this.enumClass.getClassName());
    out.append(".");
    out.append(toShortPresentationString());
    return out.toString();
  }

  public String toShortPresentationString() {
    StringBuilder out = new StringBuilder(this.declaration.getName());
    ResultHolder[] specifics = resolver.getSpecifics();
    if (specifics.length > 0) {
      out.append("<");
      for (int n = 0; n < specifics.length; n++) {
        if (n > 0) out.append(", ");
        ResultHolder specific = specifics[n];
        if (specific == null) {
          out.append(UNKNOWN);
        } else if (!specific.getType().equals(this)) {
          out.append(specific.toStringWithoutConstant());
        }else {
          out.append("...");
        }
      }
      out.append(">");
    }
    // TODO: Add the parameter list to EnumValue presentation?
    return out.toString();
  }

  @Override
  public String toString() {
    return toShortPresentationString();
  }

  @Override
  public String toStringWithoutConstant() {
    return toShortPresentationString();
  }

  @Override
  public String toStringWithConstant() {
    return toShortPresentationString() + " = " + constantValue;
  }


  @Nullable
  public SpecificFunctionReference getConstructor() {
    if(getModel() instanceof  HaxeEnumValueConstructorModel constructorModel) {
      if (constructor == null) {
        HaxeClassModel declaringEnum = constructorModel.getDeclaringEnum();
        if (declaringEnum != null) {
          ResultHolder resultHolder = declaringEnum.getInstanceType();
          List<HaxeArgument> arguments = convertParameterList(constructorModel);
          constructor = new SpecificFunctionReference(arguments, resultHolder, null, context, null);
        }
      }
    }
  return constructor;
  }

  private static List<HaxeArgument> convertParameterList(HaxeEnumValueConstructorModel model) {
    List<HaxeArgument> arguments = new ArrayList<>();
    @NotNull List<HaxeParameter> list = model.getConstructorParameters().getParameterList();
    for (int i = 0; i < list.size(); i++) {
      HaxeParameter parameter = list.get(i);
      arguments.add(mapToArgument(parameter, i));
    }
    return arguments;
  }

  private static HaxeArgument mapToArgument(HaxeParameter parameter, int index) {
    boolean optional = parameter.getOptionalMark() != null;
    String name = parameter.getComponentName().getName();
    ResultHolder type = HaxeTypeResolver.getTypeFromTypeTag(parameter.getTypeTag(), parameter.getContext());
    return new HaxeArgument(parameter, index, optional, false, type, name);
  }
  public SpecificEnumValueReference withResolver(HaxeGenericResolver resolver) {
    return new SpecificEnumValueReference( declaration, context, resolver,  constantValue) ;
  }

  @Override
  public SpecificTypeReference withElementContext(PsiElement element) {
    return new SpecificEnumValueReference( declaration, element, resolver,  constantValue) ;
  }

  @Override
  public PsiElement getTypePsi() {
    return declaration;
  }
}
