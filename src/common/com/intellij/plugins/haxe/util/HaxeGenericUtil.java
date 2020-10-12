package com.intellij.plugins.haxe.util;

import com.intellij.plugins.haxe.model.HaxeGenericParamModel;
import com.intellij.plugins.haxe.model.HaxeMethodModel;
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
    if (nonNull(model.getGenericParams())) {

      List<HaxeGenericParamModel> genericParams = model.getGenericParams();
      ResultHolder[] specifics = classReference.getGenericResolver().getSpecifics();

      if (genericParams != null) {
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
    }
    return classReference;
  }


  public static ResultHolder[] applyConstraintsToSpecifics(HaxeMethodModel model, ResultHolder[] specifics) {
    if (nonNull(model.getGenericParams())) {
      List<HaxeGenericParamModel> genericParams = model.getGenericParams();

      if (genericParams != null) {
        Map<String, ResultHolder> nameAndConstraints = getGenericTypeParametersByName(genericParams);

        for (int i = 0; i < specifics.length; i++) {
          String name = specifics[i].getClassType().getClassName();
          boolean nameFound = genericParams.stream().anyMatch(m -> m.getName().equals(name));
          if (nameFound) {
            if (nameAndConstraints.containsKey(name)) {
              specifics[i] = nameAndConstraints.get(name);
              continue;
            }
            specifics[i] = SpecificTypeReference.getDynamic(model.getMethodPsi()).createHolder();
          }
        }
      }
    }
    return specifics;
  }

  public static SpecificHaxeClassReference replaceTypeIfGenericParameterName(HaxeMethodModel model, SpecificHaxeClassReference parameter) {
    if (nonNull(model.getGenericParams())) {
      List<HaxeGenericParamModel> genericParams = model.getGenericParams();

      if (genericParams != null) {
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
