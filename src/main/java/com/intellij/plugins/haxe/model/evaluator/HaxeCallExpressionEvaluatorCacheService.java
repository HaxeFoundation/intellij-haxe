package com.intellij.plugins.haxe.model.evaluator;

import com.intellij.plugins.haxe.lang.psi.HaxeCallExpression;
import com.intellij.plugins.haxe.lang.psi.HaxeMethod;
import com.intellij.plugins.haxe.model.evaluator.callexpression.HaxeCallExpressionContext;
import com.intellij.plugins.haxe.model.evaluator.callexpression.HaxeCallExpressionContextContainer;
import com.intellij.plugins.haxe.model.evaluator.callexpression.HaxeCallExpressionEvaluation;
import com.intellij.plugins.haxe.model.type.ResultHolder;
import lombok.CustomLog;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.intellij.plugins.haxe.model.evaluator.callexpression.HaxeCallExpressionUtil.createContextForMethodCall;

/**
 * Avoids unnecessary re-evaluation of callExpressions
 */
@CustomLog
public class HaxeCallExpressionEvaluatorCacheService  {

  private volatile  Map<CallExpressionEvaluationKey, HaxeCallExpressionEvaluation> cacheMap = new ConcurrentHashMap<>();
  public static boolean skipCaching = false;// just convenience flag for debugging

  public static @Nullable HaxeCallExpressionEvaluation cachedHaxeCallExpressionEvaluation(HaxeMethod method, HaxeCallExpression callExpression) {

    HaxeCallExpressionEvaluatorCacheService service = method.getProject().getService(HaxeCallExpressionEvaluatorCacheService.class);
    return  service.callExpressionCachedEvaluation(method, callExpression);

  }


  public @Nullable HaxeCallExpressionEvaluation callExpressionCachedEvaluation(HaxeMethod method, HaxeCallExpression callExpression) {

    if(skipCaching){
      HaxeCallExpressionContextContainer contextContainer = createContextForMethodCall(callExpression, method);
      return contextContainer.evaluateContexts();
    }

    CallExpressionEvaluationKey key = new CallExpressionEvaluationKey(method, callExpression);
    if (cacheMap.containsKey(key)) {
      return cacheMap.get(key);
    }

    HaxeCallExpressionContextContainer contextContainer = createContextForMethodCall(callExpression, method);
    HaxeCallExpressionEvaluation evaluate = contextContainer.evaluateContexts();
    if(evaluate == null) return null;

    if(evaluate.isValid() && evaluate.isCompleted()) {
      HaxeCallExpressionContext context = contextContainer.getContext();
      if(context != null && context.canCache) {
        if (noUnknownResolvedValues(evaluate)) {
          cacheMap.put(key, evaluate);
        }
      }
    }

    return evaluate;
  }

  private boolean noUnknownResolvedValues( HaxeCallExpressionEvaluation evaluate) {
      if(evaluate.getReturnTypeWithoutResolve().containsUnknownTypes()) {
        return false;
      }
      for (ResultHolder parameterType : evaluate.getParameterTypes()) {
        if(parameterType.containsUnknownTypes()) {
          return false;
        }
      }
      return true;
    }


  public void clearCaches() {
    synchronized(this) {
      cacheMap.clear();
    }
  }
}

record CallExpressionEvaluationKey(HaxeMethod method, HaxeCallExpression callExpression) {
}