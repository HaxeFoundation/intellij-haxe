package com.intellij.plugins.haxe.execution.console;

import com.intellij.execution.filters.ConsoleFilterProvider;
import com.intellij.execution.filters.Filter;
import com.intellij.openapi.project.Project;
import com.intellij.plugins.haxe.config.HaxeProjectSettings;
import com.intellij.psi.search.GlobalSearchScope;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class HaxeConsoleFilterProvider implements ConsoleFilterProvider {

  @Override
  public Filter @NotNull [] getDefaultFilters(@NotNull Project project) {
    List<Filter> filters = new ArrayList<>();

    if (HaxeProjectSettings.getInstance(project).getDetectCodeReferencesInConsole()) {
      GlobalSearchScope scope = GlobalSearchScope.allScope(project);

      filters.add(new HaxeCompilerMessageFilter(project));
      filters.add(new HaxeStackTraceFilter(project, scope));
      filters.add(new HaxeUtestFailureFilter(project, scope));
    }
    return filters.toArray(Filter[]::new);
  }
}
