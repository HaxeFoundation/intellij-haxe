/*
 * Copyright 2000-2013 JetBrains s.r.o.
 * Copyright 2014-2023 AS3Boyan
 * Copyright 2014-2014 Elias Ku
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

/*
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
package com.intellij.plugins.haxe.util;

import com.intellij.plugins.haxe.model.HaxeGenericParamModel;
import com.intellij.plugins.haxe.model.HaxeMethodModel;
import com.intellij.plugins.haxe.model.type.HaxeTypeCompatible;
import com.intellij.plugins.haxe.model.type.ResultHolder;
import com.intellij.plugins.haxe.model.type.SpecificHaxeClassReference;
import com.intellij.plugins.haxe.model.type.SpecificTypeReference;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;

import static java.util.Objects.nonNull;
import static java.util.stream.Collectors.toMap;

public class HaxeGenericUtil {

  public static SpecificHaxeClassReference convertGenericType(HaxeMethodModel model, SpecificHaxeClassReference classReference) {
    List<HaxeGenericParamModel> genericParams = model.getGenericParams();
    if (nonNull(genericParams)) {

      ResultHolder[] specifics = classReference.getGenericResolver().getSpecifics();

      Map<String, ResultHolder> nameAndConstraints = getGenericTypeParametersByName(genericParams);
      ResultHolder[] newSpecifics = new ResultHolder[specifics.length];

      for (int i = 0; i < specifics.length; i++) {
        String name = specifics[i].getClassType().getClassName();
        boolean nameFound = genericParams.stream().anyMatch(m -> m.getName().equals(name));
        if (nameFound) {
          if (nameAndConstraints.containsKey(name)) {
            newSpecifics[i] = nameAndConstraints.get(name);
            continue;
          }
        }
        newSpecifics[i] = specifics[i];
      }
      return SpecificHaxeClassReference.withGenerics(classReference.getHaxeClassReference(), newSpecifics);
    }
    return classReference;
  }


  public static ResultHolder[] applyConstraintsToSpecifics(HaxeMethodModel model, ResultHolder[] specifics) {
    List<HaxeGenericParamModel> genericParams = model.getGenericParams();
    if (nonNull(genericParams)) {

      Map<String, ResultHolder> nameAndConstraints = getGenericTypeParametersByName(genericParams);

      for (int i = 0; i < specifics.length; i++) {
        String name = specifics[i].getClassType().getClassName();
        boolean nameFound = genericParams.stream().anyMatch(m -> m.getName().equals(name));
        if (nameFound) {
          if (nameAndConstraints.containsKey(name)) {
            specifics[i] = nameAndConstraints.get(name);

            // TODO hack to avoid EnumValue issues
            //  The EnumValue type does not have any implicit cast methods and is not an Enum type yet the compiler  in some cases treats it as if it did.
            //  We get around this issue by  replacing it with Enum<Dynamic> for now.
            if (specifics[i].getClassType().isEnumValueClass()) {
              ResultHolder dynamicType = SpecificTypeReference.getDynamic(model.getMethodPsi()).createHolder();
              specifics[i] = HaxeTypeCompatible.wrapType(dynamicType, specifics[i].getElementContext(), true).createHolder();
            }
            continue;
          }
          specifics[i] = SpecificTypeReference.getDynamic(model.getMethodPsi()).createHolder();
        }
      }
    }
    return specifics;
  }

  public static SpecificHaxeClassReference replaceTypeIfGenericParameterName(HaxeMethodModel model, SpecificHaxeClassReference parameter) {
    List<HaxeGenericParamModel> genericParams = model.getGenericParams();
    if (nonNull(genericParams)) {

      Map<String, ResultHolder> nameAndConstraints = getGenericTypeParametersByName(genericParams);

      String name = parameter.getClassName();
      boolean nameFound = genericParams.stream().anyMatch(m -> m.getName().equals(name));
      if (nameFound) {
        if (nameAndConstraints.containsKey(name)) {
          return nameAndConstraints.get(name).getClassType();
        }
        // if Type name is generic name  but no constraints return dynamic to accept any type assignment
        return SpecificTypeReference.getDynamic(model.getMethodPsi());
      }
    }
    return parameter;
  }

  @NotNull
  private static Map<String, ResultHolder> getGenericTypeParametersByName(List<HaxeGenericParamModel> genericParams) {
    return genericParams.stream()
      .filter(mo -> nonNull(mo.getConstraint(null)))
      .collect(toMap(HaxeGenericParamModel::getName, m -> m.getConstraint(null)));
  }
}
