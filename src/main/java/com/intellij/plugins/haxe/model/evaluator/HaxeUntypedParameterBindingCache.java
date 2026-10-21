package com.intellij.plugins.haxe.model.evaluator;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.LowMemoryWatcher;
import com.intellij.plugins.haxe.lang.psi.HaxeParameter;
import com.intellij.plugins.haxe.model.type.ResultHolder;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Project-scoped memo of untyped-parameter probe outcomes
 * (see {@link HaxeUntypedParameterInference}), including clean misses.
 * Holds PSI strongly, so its lifetime matches the sibling evaluator caches:
 * cleared on every PSI change ({@link HaxeExpressionEvaluatorCacheChangeListener}),
 * on low memory, and dropped with the project.
 */
public final class HaxeUntypedParameterBindingCache implements Disposable {

  private final Map<HaxeParameter, Optional<ResultHolder>> bindings = new ConcurrentHashMap<>();
  private final LowMemoryWatcher watcher = LowMemoryWatcher.register(this::clearCaches);

  @NotNull
  public static HaxeUntypedParameterBindingCache getInstance(@NotNull Project project) {
    return project.getService(HaxeUntypedParameterBindingCache.class);
  }

  @NotNull
  Map<HaxeParameter, Optional<ResultHolder>> bindings() {
    return bindings;
  }

  public void clearCaches() {
    bindings.clear();
  }

  @Override
  public void dispose() {
    watcher.stop();
    bindings.clear();
  }
}
