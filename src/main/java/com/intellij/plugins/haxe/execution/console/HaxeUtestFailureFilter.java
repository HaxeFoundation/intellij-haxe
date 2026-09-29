package com.intellij.plugins.haxe.execution.console;

import com.intellij.execution.filters.Filter;
import com.intellij.execution.filters.HyperlinkInfo;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.search.GlobalSearchScope;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Links the {@code path:line} of a utest assertion failure (see
 * {@link HaxeUtestFailureLine}) to the file. Only a path that resolves
 * to a file links, so other {@code word:123:} text stays plain.
 */
public final class HaxeUtestFailureFilter implements Filter {

  private final Project project;
  private final GlobalSearchScope scope;

  public HaxeUtestFailureFilter(@NotNull Project project, @NotNull GlobalSearchScope scope) {
    this.project = project;
    this.scope = scope;
  }

  @Override
  public @Nullable Result applyFilter(@NotNull String text, int entireLength) {
    HaxeUtestFailureLine failure = HaxeUtestFailureLine.parse(text);
    if (failure == null) return null;
    VirtualFile file = HaxeStackFrameFiles.find(project, scope, failure.path());
    if (file == null) return null;
    HyperlinkInfo target = HaxeConsoleLinks.target(project, file, Math.max(0, failure.line() - 1), 0);
    return HaxeConsoleLinks.lineSpan(text, entireLength, failure.pathStart(), failure.lineEnd(), target);
  }
}
