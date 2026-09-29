package com.intellij.plugins.haxe.model.evaluator;

import com.intellij.openapi.project.Project;
import com.intellij.plugins.haxe.model.HaxeFilenameCandidateCache;
import com.intellij.psi.util.PsiModificationTracker;

/**
 * TMP workaround for a problem
 * <p>
 * Looks like project serivce and project listener entries in plugin.xml creates different instances
 * so to avoid problems with caches not beeing cleared we create a listener that  finds the service and clears the cache.
 */
public class HaxeExpressionEvaluatorCacheChangeListener implements PsiModificationTracker.Listener {
  private final Project myProject;

  public HaxeExpressionEvaluatorCacheChangeListener(Project project) {
    myProject = project;
  }

  public void modificationCountChanged() {
    myProject.getService(HaxeExpressionEvaluatorCacheService.class).clearCaches();
    myProject.getService(HaxeCallExpressionEvaluatorCacheService.class).clearCaches();

    HaxeFilenameCandidateCache.getInstance(myProject).clearCaches();
    HaxeUntypedParameterBindingCache.getInstance(myProject).clearCaches();
  }
}

