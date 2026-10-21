package com.intellij.plugins.haxe.ide.references;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectUtil;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.plugins.haxe.lang.psi.HaxeStringLiteralExpression;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFileSystemItem;
import com.intellij.psi.PsiManager;
import com.intellij.psi.PsiReferenceBase;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.regex.Pattern;

/**
 * A string literal whose VALUE is a resolvable file path — navigable like a
 * URL in a string, but inside the IDE. Resolution tries three bases in
 * order: absolute (only when the text looks absolute), relative to the
 * project base dir, relative to the directory of the containing file. Soft:
 * an unresolvable path is prose, never an error — the contributor only
 * attaches this reference when {@link #resolveFile} succeeds.
 */
public class HaxeStringFilePathReference extends PsiReferenceBase<HaxeStringLiteralExpression> {

  private final String value;

  public HaxeStringFilePathReference(@NotNull HaxeStringLiteralExpression literal, @NotNull TextRange range, @NotNull String value) {
    super(literal, range, true);
    this.value = value;
  }

  @Override
  @Nullable
  public PsiElement resolve() {
    VirtualFile file = resolveFile(getElement(), value);
    if (file == null) return null;
    PsiManager psiManager = PsiManager.getInstance(getElement().getProject());
    PsiFileSystemItem item = file.isDirectory() ? psiManager.findDirectory(file) : psiManager.findFile(file);
    return item;
  }

  /** The file (or directory) the path denotes, or null — the three-base lookup. */
  @Nullable
  public static VirtualFile resolveFile(@NotNull PsiElement context, @NotNull String path) {
    String normalized = path.replace('\\', '/');
    if (normalized.isEmpty()) return null;

    if (looksAbsolute(normalized)) {
      return LocalFileSystem.getInstance().findFileByPath(normalized);
    }

    Project project = context.getProject();
    VirtualFile projectDir = ProjectUtil.guessProjectDir(project);
    if (projectDir != null) {
      VirtualFile fromProjectRoot = projectDir.findFileByRelativePath(normalized);
      if (fromProjectRoot != null) return fromProjectRoot;
    }

    VirtualFile containing = context.getContainingFile().getOriginalFile().getVirtualFile();
    VirtualFile parent = containing != null ? containing.getParent() : null;
    return parent != null ? parent.findFileByRelativePath(normalized) : null;
  }

  // a bare file name with an extension ("build.hxml") - the shape that can
  // be a path without containing any separator
  private static final Pattern NAME_WITH_EXTENSION = Pattern.compile(".+\\.[A-Za-z0-9]{1,10}");

  /**
   * Whether the text READS as a path (separator or extension). Gates the
   * link painting only: completion references attach more broadly, so a
   * bare word that happens to match a file never lights up as a link but
   * still completes on explicit request.
   */
  public static boolean looksLikePath(@NotNull String value) {
    return value.indexOf('/') >= 0 || value.indexOf('\\') >= 0 || NAME_WITH_EXTENSION.matcher(value).matches();
  }

  // a drive-letter prefix (C:/) or a leading slash - anything else is
  // treated as relative and never handed to the local file system root
  static boolean looksAbsolute(@NotNull String path) {
    String normalized = path.replace('\\', '/');
    if (normalized.startsWith("/")) return true;
    return normalized.length() > 2
           && Character.isLetter(normalized.charAt(0))
           && normalized.charAt(1) == ':'
           && normalized.charAt(2) == '/';
  }
}
