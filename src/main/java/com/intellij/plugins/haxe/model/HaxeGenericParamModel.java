/*
 * Copyright 2000-2013 JetBrains s.r.o.
 * Copyright 2014-2015 AS3Boyan
 * Copyright 2014-2014 Elias Ku
 * Copyright 2018-2019 Eric Bishton
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
package com.intellij.plugins.haxe.model;

import com.intellij.plugins.haxe.lang.psi.*;
import com.intellij.plugins.haxe.lang.psi.impl.HaxeTypeParameterDeclaration;
import com.intellij.plugins.haxe.lang.psi.impl.HaxeTypeParameterMultiType;
import com.intellij.plugins.haxe.model.type.*;
import com.intellij.psi.util.PsiTreeUtil;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * HaxeGenericParamModel (genericParam) appears on type and method *declarations*.
 * {@link HaxeTypeParameterModel} appears on references (e.g. in inherit lists).
 */
public class HaxeGenericParamModel  extends  HaxeClassModel{
  final private HaxeGenericListPart part;
  @Getter final private String name;
  @Getter final private int index;
  @Getter final private HaxeModel owner;
  final private HaxeTypeOrAnonymous defaultType;
  final private HaxeFunctionType defaultFunction;



  public HaxeGenericParamModel(@NotNull HaxeGenericListPart part) {
    this(part, findIndex(part));
  }

  private HaxeModel findOwner(@NotNull HaxeGenericListPart part) {
    HaxeMethod method = PsiTreeUtil.getParentOfType(part, HaxeMethod.class);
    if (method != null) {
      return method.getModel();
    }

    HaxeClass type = PsiTreeUtil.getParentOfType(part, HaxeClass.class);
    if (type != null) {
      return type.getModel();
    }

    return null;
  }

  private static int findIndex(@NotNull HaxeGenericListPart part) {
    HaxeGenericParam type = PsiTreeUtil.getParentOfType(part, HaxeGenericParam.class);
    if(type == null) return 0;
    return type.getGenericListPartList().indexOf(part);
  }

  public HaxeGenericParamModel(@NotNull HaxeGenericListPart part, int index) {
    super(part);
    this.index = index;
    this.part = part;
    this.name = part.getComponentName().getText();

    HaxeGenericDefaultType defaultPart = part.getGenericDefaultType();
    this.defaultType = defaultPart == null ? null : defaultPart.getTypeOrAnonymous();
    this.defaultFunction = defaultPart == null ? null : defaultPart.getFunctionType();

    owner = findOwner(part);
  }


  public  boolean hasDefault() {
    return defaultType != null || defaultFunction !=  null;
  }


  public HaxeGenericListPart getPsi() { return part; }

  @Nullable
  public ResultHolder getConstraint(@Nullable HaxeGenericResolver resolver) {
    if (null == resolver) {
      resolver = new HaxeGenericResolver();
    }

    //TODO make sure we search parent classes for their constraints as we inherit those
    // maybe make a getConstraints method with all available constraints

    HaxeGenericConstraintPart constraintPart = part.getGenericConstraintPart();
    if (constraintPart != null) {
      HaxeTypeListPart constraint = constraintPart.getTypeListPart();
      if (constraint == null) {
        HaxeTypeList list = constraintPart.getTypeList();
        if (list != null) {
          // no need to  use multiType if only one value
          if (list.getTypeListPartList().size() == 1) {
            HaxeTypeListPart typeListPart = list.getTypeListPartList().get(0);
            HaxeFunctionType functionType = typeListPart.getFunctionType();
            if (functionType != null) {
              return SpecificFunctionReference.create(new HaxeSpecificFunction(functionType, HaxeGenericSpecialization.EMPTY))
                .createHolder();
            }
            HaxeTypeOrAnonymous anonymous = typeListPart.getTypeOrAnonymous();
            if (anonymous != null) {
              return HaxeTypeResolver.getTypeFromTypeOrAnonymous(anonymous);
            }
          }
          HaxeTypeParameterMultiType type = convertToMultiTypeParameter(list, resolver);
          return new ResultHolder(SpecificHaxeClassReference.withoutGenerics(new HaxeClassReference(type.getModel(), part)));
        }
      }
      if (constraint != null) {
        HaxeTypeOrAnonymous toa = constraint.getTypeOrAnonymous();
        if (toa != null) {
          HaxeType type = toa.getType();
          if (null != type) {

            ResultHolder result = HaxeTypeResolver.getTypeFromType(type);
            if (!result.isUnknown()) {
              return result;
            } else {
              HaxeReferenceExpression reference = type.getReferenceExpression();
              if (HaxeTypeResolver.isTypeParameter(reference)) {
                // we dont want to resolve typeParameter constraints as the definition of this type parameter might need to inherit the type
                return new ResultHolder(
                  SpecificHaxeClassReference.withoutGenerics(new HaxeClassReference(type.getText(), type, true)));
                //return HaxeTypeResolver.getTypeFromTypeOrAnonymous(toa);
              }
            }
          } else {
            // Anonymous struct for a constraint.
            // TODO: Turn the anonymous structure into a ResolveResult.
            return HaxeTypeResolver.getTypeFromTypeOrAnonymous(toa, resolver); //temp solution
          }
        }
        HaxeFunctionType functionType = constraint.getFunctionType();
        if (functionType != null) {
          return HaxeTypeResolver.getTypeFromFunctionType(functionType);
        }
      }
    }
    return null;
  }

  @NotNull
  private HaxeTypeParameterMultiType convertToMultiTypeParameter(HaxeTypeList list, @Nullable HaxeGenericResolver resolver) {
    List<HaxeTypeListPart> partList = list.getTypeListPartList();
    List<HaxeTypeOrAnonymous> mapped = partList.stream().map(HaxeTypeListPart::getTypeOrAnonymous).toList();

    List<HaxeAnonymousType> anonymousTypes = mapped.stream()
      .filter(t -> t.getAnonymousType() != null)
      .map(HaxeTypeOrAnonymous::getAnonymousType).toList();

    List<HaxeAnonymousTypeBody> anonymousTypeBodies = anonymousTypes.stream().flatMap(type -> type.getAnonymousTypeBodyList().stream())
      .toList();

    List<HaxeType> typeList = mapped.stream().filter(t -> t.getType() != null).map(HaxeTypeOrAnonymous::getType).toList();
    if (!typeList.isEmpty() && !anonymousTypes.isEmpty()) {
      return HaxeTypeParameterMultiType.withTypeAndAnonymousList(part.getNode(), typeList, anonymousTypeBodies);
    } else if (anonymousTypes.isEmpty()) {
      return HaxeTypeParameterMultiType.withTypeList(part.getNode(), typeList);
    } else {
      return HaxeTypeParameterMultiType.withAnonymousList(part.getNode(), anonymousTypeBodies);
    }
  }

  public String toString() {
    return name;
  }

//TODO plural and list, we can replace tp in  multiple interfaces
  //TODO recurison guard ?
  public HaxeClass getReplacedTypeParameter() {
    HaxeClass parentClass = PsiTreeUtil.getParentOfType(part, HaxeClass.class);
    if(parentClass != null) {
      if (parentClass instanceof  HaxeTypedefDeclaration typedefDeclaration) {
        HaxeTypeOrAnonymous anonymous = typedefDeclaration.getTypeOrAnonymous();
        if(anonymous != null &&  anonymous.getType() != null) {
          HaxeType type = anonymous.getType();
          HaxeTypeParam param = type.getTypeParam();
          return getReplacedTypeParameter(type, param);
        }
      }
      else {
        List<HaxeType> extendsList = parentClass.getHaxeExtendsList();
        for (HaxeType extendedType : extendsList) {
          HaxeTypeParam param = extendedType.getTypeParam();
          return getReplacedTypeParameter(extendedType, param);
        }
      }
    }
    return null;
  }

  private @Nullable HaxeClass getReplacedTypeParameter(HaxeType extendedType, HaxeTypeParam param) {
    if (param != null) {
      HaxeTypeList list = param.getTypeList();
      List<HaxeTypeListPart> list1 = list.getTypeListPartList();
      for (int i = 0; i < list1.size(); i++) {
        HaxeTypeListPart listPart = list1.get(i);
        HaxeTypeOrAnonymous aot = listPart.getTypeOrAnonymous();
        if (aot != null) {
          ResultHolder holder = HaxeTypeResolver.getTypeFromTypeOrAnonymous(aot);
          if (holder.isTypeParameter() && holder.getClassType() != null) {
            //if typeReferences  is our typeParam we know this typparam replaces something in extended method
            if (this.getTypeParameter() == holder.getClassType().getHaxeClass()) {
              ResultHolder fromType = HaxeTypeResolver.getTypeFromType(extendedType);
              if (fromType.getClassType() != null) {
                HaxeClassModel classModel = fromType.getClassType().getHaxeClassModel();
                if (classModel != null) {
                  List<HaxeGenericParamModel> params = classModel.getGenericParams();
                  if (params.size() > i) {
                    HaxeGenericParamModel model = params.get(i);
                    return model.haxeClass;
                  }
                }
              }
            }
          }
        }
      }
    }
    return null;
  }

  public @Nullable ResultHolder getDefaultType(HaxeGenericResolver resolver) {
    if(defaultType != null) return HaxeTypeResolver.getTypeFromTypeOrAnonymous(defaultType, resolver);
    if(defaultFunction != null) return HaxeTypeResolver.getTypeFromFunctionType(defaultFunction, resolver);
    return null;
  }

  public HaxeTypeParameterDeclaration getTypeParameter() {
    return part;
  }
}
