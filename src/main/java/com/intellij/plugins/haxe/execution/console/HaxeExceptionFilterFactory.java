package com.intellij.plugins.haxe.execution.console;

import com.intellij.execution.filters.CompositeFilter;
import com.intellij.execution.filters.ExceptionFilterFactory;
import com.intellij.execution.filters.Filter;
import com.intellij.openapi.project.Project;
import com.intellij.psi.search.GlobalSearchScope;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;

/** Makes Haxe stack frames and utest failure positions navigable in the Analyze Stack Trace dialog. */
public final class HaxeExceptionFilterFactory implements ExceptionFilterFactory {

  @Override
  public @NotNull Filter create(@NotNull GlobalSearchScope searchScope) {
    return create(Objects.requireNonNull(searchScope.getProject()), searchScope);
  }

  @Override
  public @NotNull Filter create(@NotNull Project project, @NotNull GlobalSearchScope searchScope) {
    List<Filter> filters = List.of(
      new HaxeStackTraceFilter(project, searchScope),
      new HaxeUtestFailureFilter(project, searchScope));
    return new CompositeFilter(project, filters);
  }
}
