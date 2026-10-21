package com.intellij.plugins.haxe.execution.console;

import com.intellij.execution.filters.Filter;
import com.intellij.execution.filters.HyperlinkInfo;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.search.GlobalSearchScope;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Links the {@code path:line} of a compiler message ({@code Main.hx:12: characters 4-9 : ...})
 * to the file. The compiler prints positions relative to the directory it
 * runs in, so the path resolves inside the console's launch scope first (see
 * {@link HaxeStackFrameFiles}), the project after.
 */
public final class HaxeCompilerMessageFilter implements Filter {

  // optional label ("WARNING", "[ERROR]" in the haxe 5 format), the path (an absolute
  // windows path spells forward slashes), ":line: characters|lines from-to", the message
  private static final Pattern COMPILER_MESSAGE = Pattern.compile(
    "(\\s*(?<label>\\[?\\w+\\]?)\\s+)?"
    + "(?<path>((\\w:)?/)?([a-z_\\-\\s0-9.,]+(/)?)+\\.(\\w+))"
    + ":(?<line>([0-9]+))"
    + ":\\s+(?<type>(characters|lines))\\s+"
    + "(?<column>([0-9]+))-\\d+"
    + ".*",
    Pattern.CASE_INSENSITIVE);

  private final Project project;
  private final GlobalSearchScope scope;

  public HaxeCompilerMessageFilter(@NotNull Project project, @NotNull GlobalSearchScope scope) {
    this.project = project;
    this.scope = scope;
  }

  @Override
  public @Nullable Result applyFilter(@NotNull String text, int entireLength) {
    // find() not matches(): console lines arrive with their trailing newline, which `.` never matches
    Matcher matcher = COMPILER_MESSAGE.matcher(text);
    if (!matcher.find()) return null;
    String path = matcher.group("path");
    String line = matcher.group("line");
    // a "lines" position carries no column
    int column = matcher.group("type").equalsIgnoreCase("lines") ? 0 : Integer.parseInt(matcher.group("column"));

    VirtualFile file = HaxeStackFrameFiles.find(project, scope, path);
    if (file == null) return null;
    int spanStart = text.indexOf(path);
    int spanEnd = spanStart + path.length() + 1 + line.length(); // path ':' line
    HyperlinkInfo target = HaxeConsoleLinks.target(project, file, Integer.parseInt(line) - 1, column - 1);
    return HaxeConsoleLinks.lineSpan(text, entireLength, spanStart, spanEnd, target);
  }
}
