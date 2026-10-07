package com.intellij.plugins.haxe.execution.console;

import com.intellij.execution.filters.ConsoleFilterProviderEx;
import com.intellij.execution.filters.Filter;
import com.intellij.openapi.project.Project;
import com.intellij.plugins.haxe.config.HaxeProjectSettings;
import com.intellij.psi.search.GlobalSearchScope;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * The console filters that turn Haxe positions into links. The scope is the
 * console's own: a launch hands over a {@link HaxeLaunchSearchScope}, so a
 * trace line resolves against the build that produced it first.
 */
public class HaxeConsoleFilterProvider implements ConsoleFilterProviderEx {

  @Override
  public Filter @NotNull [] getDefaultFilters(@NotNull Project project) {
    return getDefaultFilters(project, GlobalSearchScope.allScope(project));
  }

  @Override
  public Filter @NotNull [] getDefaultFilters(@NotNull Project project, @NotNull GlobalSearchScope scope) {
    List<Filter> filters = new ArrayList<>();

    if (HaxeProjectSettings.getInstance(project).getDetectCodeReferencesInConsole()) {
      filters.add(new HaxeCompilerMessageFilter(project));
      filters.add(new HaxeStackTraceFilter(project, scope));
      filters.add(new HaxeUtestFailureFilter(project, scope));
    }
    return filters.toArray(Filter[]::new);
  }
}
