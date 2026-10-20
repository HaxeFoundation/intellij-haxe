package com.intellij.plugins.haxe.v2.testing.run;

import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.io.OSAgnosticPathUtil;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.plugins.haxe.lang.psi.HaxeClass;
import com.intellij.plugins.haxe.util.HaxeReadActions;
import com.intellij.plugins.haxe.v2.testing.HaxeTestNameLocation;
import com.intellij.plugins.haxe.lang.psi.HaxeFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.PsiMethod;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Best-effort mapping of a haxe output line to the test that printed it.
 * utest's TeamCity reporter emits its whole event batch after the run, so
 * output can never interleave with test events — but `trace` prefixes every
 * line with the call site (`src/unit/MathTest.hx:78:`), and that
 * file plus line resolves through PSI to a method of a test class, whose
 * TeamCity-shaped name the batch will later carry. Lines without a position
 * prefix (plain Sys.println) and call sites outside test methods (helpers,
 * other files) stay unattributed and appear only in the run console.
 */
final class HaxeTestOutputAttributor {

  // a haxe trace position prefix: `<path>.hx:<line>: <message>` at line start
  private static final Pattern TRACE_POSITION = Pattern.compile("^(.+?\\.hx):(\\d+):");

  private final Project project;
  private final @Nullable String workDirectory;
  private final Map<String, List<MethodSpan>> spansByPath = new HashMap<>();

  /** A method's 1-based line range and the TeamCity-shaped test name of `Class.method`. */
  private record MethodSpan(int firstLine, int lastLine, @NotNull String testName) {
  }

  HaxeTestOutputAttributor(@NotNull Project project, @Nullable String workDirectory) {
    this.project = project;
    this.workDirectory = workDirectory;
  }

  /** The TeamCity test name the line's position prefix points into, or null when it maps to none. */
  @Nullable
  String testNameFor(@NotNull String line) {
    Matcher matcher = TRACE_POSITION.matcher(line);
    if (!matcher.find()) return null;
    int lineNumber = Integer.parseInt(matcher.group(2));

    // normalized BEFORE keying the cache: haxe prints the path as the build
    // file spelled it, so \ and / spellings of one file must share an entry
    String path = FileUtil.toSystemIndependentName(matcher.group(1));
    List<MethodSpan> spans = spansByPath.computeIfAbsent(path, this::methodSpans);
    for (MethodSpan span : spans) {
      if (lineNumber >= span.firstLine() && lineNumber <= span.lastLine()) {
        return span.testName();
      }
    }
    return null;
  }

  /** Output processing runs on a pooled thread without read access. */
  @NotNull
  private List<MethodSpan> methodSpans(@NotNull String path) {
    return HaxeReadActions.compute(() -> computeMethodSpans(path));
  }

  @NotNull
  private List<MethodSpan> computeMethodSpans(@NotNull String path) {
    VirtualFile file = resolveFile(path);
    if (file == null || !(PsiManager.getInstance(project).findFile(file) instanceof HaxeFile haxeFile)) {
      return List.of();
    }
    Document document = FileDocumentManager.getInstance().getDocument(file);
    if (document == null) return List.of();

    List<MethodSpan> spans = new ArrayList<>();
    for (HaxeClass haxeClass : haxeFile.getClassList()) {
      String className = HaxeTestNameLocation.teamcityNameOf(haxeClass.getQualifiedName());
      if (className == null || className.isEmpty()) continue;
      for (PsiMethod method : haxeClass.getMethods()) {
        TextRange range = method.getTextRange();
        if (range == null) continue;
        int firstLine = document.getLineNumber(range.getStartOffset()) + 1;
        int lastLine = document.getLineNumber(range.getEndOffset()) + 1;
        spans.add(new MethodSpan(firstLine, lastLine, className + "." + method.getName()));
      }
    }
    return spans;
  }

  /**
   * The compiler prints paths as it saw them: project code comes from the
   * build's relative classpaths, so its positions are relative to the
   * compile's working directory. An ABSOLUTE position is toolkit or haxelib
   * code (utest itself traces its report with one) - never test output, and
   * not resolved at all. The resolved file must sit in project content, so a
   * relative classpath escaping the project cannot attach output either.
   */
  @Nullable
  private VirtualFile resolveFile(@NotNull String path) {
    if (workDirectory == null || OSAgnosticPathUtil.isAbsolute(path)) return null;
    VirtualFile file = LocalFileSystem.getInstance().findFileByPath(workDirectory + "/" + path);
    return file != null && ProjectFileIndex.getInstance(project).isInContent(file) ? file : null;
  }
}
