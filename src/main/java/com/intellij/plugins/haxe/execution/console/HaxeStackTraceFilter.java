package com.intellij.plugins.haxe.execution.console;

import com.intellij.execution.filters.Filter;
import com.intellij.execution.filters.HyperlinkInfo;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.search.GlobalSearchScope;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Links the {@code path line N} span of a Haxe stack frame to the file. Serves
 * the run console and the Analyze Stack Trace dialog alike.
 */
public final class HaxeStackTraceFilter implements Filter {

  private final Project project;
  private final GlobalSearchScope scope;

  public HaxeStackTraceFilter(@NotNull Project project, @NotNull GlobalSearchScope scope) {
    this.project = project;
    this.scope = scope;
  }

  @Override
  public @Nullable Result applyFilter(@NotNull String text, int entireLength) {
    HaxeStackFrameLine frame = HaxeStackFrameLine.parse(text);
    if (frame == null) return null;
    VirtualFile file = HaxeStackFrameFiles.find(project, scope, frame.path());
    if (file == null) return null;
    HyperlinkInfo target = HaxeConsoleLinks.target(project, file, Math.max(0, frame.line() - 1), 0);
    return HaxeConsoleLinks.lineSpan(text, entireLength, frame.pathStart(), frame.lineEnd(), target);
  }
}
