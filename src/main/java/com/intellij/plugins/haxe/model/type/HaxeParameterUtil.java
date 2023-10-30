package com.intellij.plugins.haxe.model.type;

import com.intellij.plugins.haxe.lang.psi.HaxeCallExpression;
import com.intellij.plugins.haxe.lang.psi.HaxeExpression;
import com.intellij.plugins.haxe.model.HaxeParameterModel;
import lombok.CustomLog;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@CustomLog
public class HaxeParameterUtil {

  @NotNull
  public static Map<Integer, ParameterToArgumentAndResolver> mapArgumentsToParameters(
    @NotNull HaxeCallExpression callExpression,
    @NotNull List<HaxeParameterModel> parameterModelList,
    @NotNull List<HaxeExpression> argumentList,
    boolean isStatic,
    @NotNull HaxeGenericResolver methodResolver
  ) {

    Map<Integer, ParameterToArgumentAndResolver> map = new HashMap<>();
    ResultHolder argumentTypeHolder = null;
    ResultHolder parameterTypeHolder = null;
    HaxeExpression argumentExpression = null;

    int parameterCount = parameterModelList.size();
    int argumentCount = argumentList.size();
    int startIndex = isStatic ? 1 : 0;

    int argumentCounter = startIndex;
    int counter = startIndex;

    for (; counter < parameterCount; counter++) {
      HaxeParameterModel parameter = parameterModelList.get(counter);
      parameterTypeHolder = parameter.getType();
      //TODO rewrite getType to get Type Without resolving typeParameters, and then do it before any useage, that way we can cache the value
      //parameterTypeHolder = resolveAnyGenericType(parameterTypeHolder);

      if (argumentCount < counter) {
        map.put(counter, new ParameterToArgumentAndResolver(counter, parameter, null,null,null));
      }else {
        // get

        if (argumentTypeHolder == null) {
          if (argumentCounter < argumentCount) {
            argumentExpression = argumentList.get(argumentCounter++);
            argumentTypeHolder = findArgumentType(callExpression, methodResolver, argumentExpression);
          }
        }

        if (argumentCounter <= argumentCount) {
          if (parameterTypeHolder.canAssign(argumentTypeHolder)) {
            map.put(counter, new ParameterToArgumentAndResolver(counter, parameter, argumentExpression, argumentTypeHolder, null));
            argumentTypeHolder = null;// clear after successful match so we pick up next argument in next loop iteration
          } else {
            map.put(counter, new ParameterToArgumentAndResolver(counter, parameter, null,null, null));
          }
        }
      }
    }
    if (argumentCounter < counter) {
      // normal for method calls with optional fields not used
      //log.warn("not all arguments where mapped");
    }
    return map;
  }

  private static ResultHolder resolveAnyGenericType(ResultHolder holder) {
    //TODO mlo
    return holder;
  }

  private static ResultHolder findArgumentType(@NotNull HaxeCallExpression callExpression,
                                        @NotNull HaxeGenericResolver resolver,
                                        HaxeExpression expression) {
    HaxeExpressionEvaluatorContext evaluatorContext = new HaxeExpressionEvaluatorContext(callExpression);
    HaxeExpressionEvaluator.evaluate(expression, evaluatorContext, resolver);
    ResultHolder result = evaluatorContext.result;
    return result;
  }

  public record ParameterToArgumentAndResolver(int index, @NotNull HaxeParameterModel parameter,@Nullable HaxeExpression argumentExpression, @Nullable ResultHolder argumentType, @Nullable HaxeGenericResolver resolver){}

}
