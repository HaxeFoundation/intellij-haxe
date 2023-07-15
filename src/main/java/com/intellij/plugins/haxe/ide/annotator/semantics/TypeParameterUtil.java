package com.intellij.plugins.haxe.ide.annotator.semantics;

import com.intellij.plugins.haxe.lang.psi.HaxeMethod;
import com.intellij.plugins.haxe.model.HaxeClassModel;
import com.intellij.plugins.haxe.model.HaxeGenericParamModel;
import com.intellij.plugins.haxe.model.HaxeMethodModel;
import com.intellij.plugins.haxe.model.type.HaxeGenericResolver;
import com.intellij.plugins.haxe.model.type.ResultHolder;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Stream;


public class TypeParameterUtil {

  @NotNull
  public static Map<String, ResultHolder> createTypeParameterConstraintMap(HaxeMethod method, HaxeGenericResolver resolver) {
    Map<String, ResultHolder> typeParamMap = new HashMap<>();

    HaxeMethodModel methodModel = method.getModel();
    if (methodModel != null) {
      List<HaxeGenericParamModel> params = methodModel.getGenericParams();
      for (HaxeGenericParamModel model : params) {
        ResultHolder constraint = model.getConstraint(resolver);
        typeParamMap.put(model.getName(), constraint);
      }
      HaxeClassModel declaringClass = methodModel.getDeclaringClass();
      if (declaringClass != null) {
        List<HaxeGenericParamModel> classParams = declaringClass.getGenericParams();
        for (HaxeGenericParamModel model : classParams) {
          ResultHolder constraint = model.getConstraint(resolver);
          typeParamMap.put(model.getName(), constraint);
        }
      }
    }
    return typeParamMap;
  }

  public static boolean containsTypeParameter(@NotNull ResultHolder parameterType, @NotNull Map<String, ResultHolder> typeParamMap) {
    if (!parameterType.isClassType()) return false;

    ResultHolder[] specifics = parameterType.getClassType().getSpecifics();
    if (specifics.length == 0){
      return typeParamMap.containsKey(parameterType.getClassType().getClassName());
    }

    return Arrays.stream(specifics)
      .filter(ResultHolder::isClassType)
      .flatMap(TypeParameterUtil::getSpecificsIfClass)
      .map( holder ->  holder.getClassType().getClassName())
      .anyMatch(typeParamMap::containsKey);
  }
  public static Optional<ResultHolder> findConstraintForTypeParameter(@NotNull ResultHolder parameterType, @NotNull Map<String, ResultHolder> typeParamMap) {
    if (!parameterType.isClassType()) return Optional.empty();

    ResultHolder[] specifics = parameterType.getClassType().getSpecifics();
    if (specifics.length == 0){
      String className = parameterType.getClassType().getClassName();
      return typeParamMap.containsKey(className) ? Optional.ofNullable(typeParamMap.get(className)) : Optional.empty();
    }
    HaxeGenericResolver resolver = parameterType.getClassType().getGenericResolver();

    Arrays.stream(specifics)
      .filter(ResultHolder::isClassType)
      .flatMap(TypeParameterUtil::getSpecificsIfClass)
      .map( holder ->  holder.getClassType().getClassName())
      .filter(typeParamMap::containsKey)
      .filter( s ->  typeParamMap.get(s) != null)
      .forEach( s ->  resolver.add(s, typeParamMap.get(s)));


    return Optional.ofNullable(resolver.resolve(parameterType));

  }

  private static Stream<ResultHolder> getSpecificsIfClass(@NotNull ResultHolder holder) {
    @NotNull ResultHolder[] specifics = holder.getClassType().getSpecifics();
    if (specifics.length == 0) return Stream.of(holder);
    return Arrays.stream(specifics)
      .filter(ResultHolder::isClassType)
      .filter(h -> !h.equals(holder) )// avoid recursive loop (classes can have itself as type parameter)
      .flatMap(TypeParameterUtil::getSpecificsIfClass);
  }
}
