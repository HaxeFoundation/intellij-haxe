package com.intellij.plugins.haxe.model;

import com.intellij.openapi.project.Project;
import com.intellij.plugins.haxe.lang.psi.HaxeClassResolveCache;
import com.intellij.plugins.haxe.lang.psi.HaxeFileTypeResolverCacheService;
import com.intellij.plugins.haxe.model.evaluator.HaxeCallExpressionEvaluatorCacheService;
import com.intellij.plugins.haxe.model.evaluator.HaxeExpressionEvaluatorCacheService;
import com.intellij.plugins.haxe.model.evaluator.HaxeUntypedParameterBindingCache;
import org.jetbrains.annotations.NotNull;

/**
 * The one place that clears the plugin's own resolution caches, always all
 * of them: they feed each other, so a cache left out would keep answers
 * derived from the ones cleared. Runs on every PSI change
 * ({@link HaxeResolveCachePsiChangeListener}) and every roots change
 * ({@link HaxeResolveCacheRootsListener}).
 */
public final class HaxeResolveCaches {

  private HaxeResolveCaches() {
  }

  public static void clear(@NotNull Project project) {
    HaxeClassResolveCache.getInstance(project).clear();
    HaxeFileTypeResolverCacheService.getInstance(project).invalidate();
    project.getService(HaxeExpressionEvaluatorCacheService.class).clearCaches();
    project.getService(HaxeCallExpressionEvaluatorCacheService.class).clearCaches();
    HaxeFilenameCandidateCache.getInstance(project).clearCaches();
    HaxeUntypedParameterBindingCache.getInstance(project).clearCaches();
  }
}
