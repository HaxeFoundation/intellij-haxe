package com.intellij.plugins.haxe.v2.display;

import com.intellij.openapi.editor.Document;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.plugins.haxe.display.protocol.Location;
import com.intellij.plugins.haxe.display.protocol.Range;
import com.intellij.plugins.haxe.lang.lexer.HaxeTokenTypes;
import com.intellij.plugins.haxe.lang.psi.HaxeAssignExpression;
import com.intellij.plugins.haxe.lang.psi.HaxeComponentName;
import com.intellij.plugins.haxe.lang.psi.HaxeExpression;
import com.intellij.plugins.haxe.lang.psi.HaxeNamedComponent;
import com.intellij.plugins.haxe.lang.psi.HaxeReference;
import com.intellij.plugins.haxe.lang.util.HaxeExpressionUtil;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.PsiReference;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.usageView.UsageInfo;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Turns the compiler's locations into the PSI that the platform's usage and
 * navigation views take. A location names a file by its path and a 0-based
 * range; a range that lies beyond the current document is dropped rather
 * than clamped. Every method needs the read lock.
 */
final class HaxeCompilerLocations {

  private HaxeCompilerLocations() {
  }

  /** The file a location names, or null when it is gone. */
  @Nullable
  static VirtualFile fileOf(@NotNull Location location) {
    VirtualFile file = LocalFileSystem.getInstance().findFileByPath(location.file());
    return file != null && file.isValid() ? file : null;
  }

  @Nullable
  static PsiFile psiFileOf(@NotNull Project project, @NotNull Location location) {
    VirtualFile file = fileOf(location);
    return file == null ? null : PsiManager.getInstance(project).findFile(file);
  }

  /**
   * The usage at the range. The compiler reports a field access as the last
   * name-length characters of the access expression, which is normally the
   * name itself. In a compound assignment ({@code field += x}) the left-hand
   * access has the position of the whole assignment, so the reported range
   * sits at the end of the assignment instead; such a range is mapped back
   * to the assignment's left-hand reference. A range that holds no reference
   * gives the bare element, so a usage the static resolver cannot explain,
   * such as a generated member, is still listed. {@code name} is the
   * referenced name, or null when unknown.
   */
  @Nullable
  static UsageInfo usageIn(@NotNull PsiFile file, @NotNull Range range, @Nullable String name) {
    TextRange textRange = textRangeIn(file, range);
    if (textRange == null) return null;
    PsiReference reference = file.findReferenceAt(textRange.getStartOffset());
    if (isNamed(reference, name)) return new UsageInfo(reference);
    PsiReference assigned = compoundAssignmentTarget(file, textRange.getEndOffset(), name);
    if (assigned != null) return new UsageInfo(assigned);
    if (reference != null) return new UsageInfo(reference);
    PsiElement element = file.findElementAt(textRange.getStartOffset());
    return element == null ? null : new UsageInfo(element);
  }

  /** Whether the reference is a Haxe reference to {@code name}, or to anything when the name is unknown. */
  private static boolean isNamed(@Nullable PsiReference reference, @Nullable String name) {
    return reference instanceof HaxeReference haxeReference && (name == null || name.equals(haxeReference.getReferenceName()));
  }

  /** The left-hand reference of the compound assignment ending at {@code end} that assigns {@code name}, else null. */
  @Nullable
  private static PsiReference compoundAssignmentTarget(@NotNull PsiFile file, int end, @Nullable String name) {
    if (end == 0) return null;
    PsiElement candidate = file.findElementAt(end - 1);
    while (candidate != null && candidate.getTextRange().getEndOffset() == end) {
      if (candidate instanceof HaxeAssignExpression assignment && isCompound(assignment)) {
        return assignedReference(assignment, name);
      }
      candidate = candidate.getParent();
    }
    return null;
  }

  private static boolean isCompound(@NotNull HaxeAssignExpression assignment) {
    IElementType operator = HaxeExpressionUtil.getAssignOperationElementType(assignment);
    return operator != null && operator != HaxeTokenTypes.OASSIGN;
  }

  @Nullable
  private static PsiReference assignedReference(@NotNull HaxeAssignExpression assignment, @Nullable String name) {
    List<HaxeExpression> sides = assignment.getExpressionList();
    if (sides.isEmpty()) return null;
    PsiReference left = sides.get(0) instanceof PsiReference reference ? reference : null;
    return isNamed(left, name) ? left : null;
  }

  /**
   * The declaration the range marks: the named component whose name sits
   * there, else the bare element at the range start.
   */
  @Nullable
  static PsiElement declarationIn(@NotNull PsiFile file, @NotNull Range range) {
    TextRange textRange = textRangeIn(file, range);
    if (textRange == null) return null;
    PsiElement element = file.findElementAt(textRange.getStartOffset());
    if (element == null) return null;
    HaxeComponentName name = PsiTreeUtil.getParentOfType(element, HaxeComponentName.class, false);
    HaxeNamedComponent named = name == null ? null : PsiTreeUtil.getParentOfType(name, HaxeNamedComponent.class);
    return named != null ? named : element;
  }

  @Nullable
  private static TextRange textRangeIn(@NotNull PsiFile file, @NotNull Range range) {
    Document document = PsiDocumentManager.getInstance(file.getProject()).getDocument(file);
    return document == null ? null : HaxeDiagnosticsFetcher.toTextRange(document, range);
  }
}
