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
import com.intellij.plugins.haxe.model.type.ResultHolder;
import com.intellij.plugins.haxe.model.type.SpecificHaxeClassReference;
import com.intellij.plugins.haxe.model.type.SpecificTypeReference;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;

import static com.intellij.plugins.haxe.model.type.HaxeTypeCompatible.wrapType;
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
    ResultHolder[] newSpecifics = new ResultHolder[specifics.length];
    List<HaxeGenericParamModel> genericParams = model.getGenericParams();
    if (nonNull(genericParams)) {

      Map<String, ResultHolder> nameAndConstraints = getGenericTypeParametersByName(genericParams);

      for (int i = 0; i < specifics.length; i++) {
        String name = specifics[i].getClassType().getClassName();
        boolean nameFound = genericParams.stream().anyMatch(m -> m.getName().equals(name));
        if (nameFound) {
          if (nameAndConstraints.containsKey(name)) {
            newSpecifics[i] = nameAndConstraints.get(name);
          }else {
            newSpecifics[i] = SpecificTypeReference.getDynamic(model.getMethodPsi()).createHolder();
          }
        }else {
          // not found, just copy original
          newSpecifics[i] = specifics[i];
        }
      }
    }
    return newSpecifics;
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
