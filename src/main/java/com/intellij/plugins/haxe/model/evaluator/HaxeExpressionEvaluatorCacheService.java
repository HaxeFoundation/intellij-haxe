package com.intellij.plugins.haxe.model.evaluator;

import com.intellij.openapi.util.RecursionGuard;
import com.intellij.openapi.util.RecursionManager;
import com.intellij.plugins.haxe.model.type.HaxeGenericResolver;
import com.intellij.plugins.haxe.model.type.ResultHolder;
import com.intellij.plugins.haxe.model.type.SpecificTypeReference;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.intellij.plugins.haxe.model.evaluator.HaxeExpressionEvaluator._handle;

/**
 * To avoid unnecessary re-evaluation of elements used by  other expressions (ex. functions without type tags etc)
 * We cache the evaluation result until a Psi change happens, we dont want to cache for longer as ResultHolder
 * contains SpecificTypeReference elements both as the type and as generics and these contain PsiElements
 * that might become invalid
 */
public class HaxeExpressionEvaluatorCacheService  {

  private volatile  Map<EvaluationKey, ResultHolder> cacheMap = new ConcurrentHashMap<>();
  public static boolean skipCaching = false;// just convenience flag for debugging


  public @NotNull ResultHolder handleWithResultCaching(@NotNull final PsiElement element,
                                                       final HaxeExpressionEvaluatorContext context,
                                                       final HaxeGenericResolver resolver) {

    if(skipCaching){
      ResultHolder holder = _handle(element, context, resolver);
      if(holder == null) return SpecificTypeReference.getUnknown(element).createHolder();
      return holder;
    }

    EvaluationKey key = new EvaluationKey(element, resolver == null ? "NO_RESOLVER" : resolver.toCacheString());
    if (cacheMap.containsKey(key)) {
      return cacheMap.get(key);
    }
    else {
      ResultHolder holder = _handle(element, context, resolver);
      if(holder == null) return SpecificTypeReference.getUnknown(element).createHolder();
      if (!holder.isUnknown() && !holder.containsUnknownTypes()) {
        cacheMap.put(key, holder);
      }
      return holder;
    }
  }


  public void clearCaches() {
    synchronized(this) {
      cacheMap.clear();
    }
  }
}

record EvaluationKey( PsiElement element, String evalParamString) {
}