package com.intellij.plugins.haxe.profiler.bridge.data;

import com.intellij.ide.util.EditSourceUtil;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.plugins.haxe.lang.psi.HaxeClass;
import com.intellij.plugins.haxe.lang.psi.HaxeNamedComponent;
import com.intellij.plugins.haxe.profiler.model.StackFrame;
import com.intellij.plugins.haxe.util.HaxeReadActions;
import com.intellij.plugins.haxe.util.HaxeResolveUtil;
import com.intellij.pom.Navigatable;
import com.intellij.profiler.api.BaseCallStackElement;
import com.intellij.psi.NavigatablePsiElement;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.search.FilenameIndex;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.concurrency.AppExecutorUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

/**
 * One frame of a profiled Haxe stack shown in the IU profiler views. A
 * frame carrying a source position navigates by it — the position comes
 * straight from the collector (or a source map) and stays exact where the
 * symbol may be bare or generated. Without one, the symbol is resolved as
 * a qualified name ({@code pack.Class.method}) against the project's Haxe
 * classes — the longest symbol prefix that names a class wins, so a
 * closure symbol ({@code Main.update.closure}) still lands on its
 * enclosing method.
 */
public final class HaxeCallStackElement extends BaseCallStackElement {

  private final String symbol;
  private final @Nullable String file;
  private final int line;

  public HaxeCallStackElement(@NotNull String symbol, @Nullable String file, int line) {
    this.symbol = symbol;
    this.file = file;
    this.line = line;
  }

  @Override
  public @NotNull String fullName() {
    return symbol;
  }

  @Override
  public boolean isNavigatable() {
    return hasLocalPosition() || symbol.indexOf('.') > 0;
  }

  /** A position navigation can use: a plain file path — a script URL (an unmapped js frame) is not one. */
  private boolean hasLocalPosition() {
    return file != null && !file.contains("://");
  }

  @Override
  public NavigatablePsiElement @NotNull [] calcNavigatables(@NotNull Project project) {
    // the IU profiler calls this from either kind of thread
    return HaxeReadActions.compute(() -> resolve(project));
  }

  /**
   * Opens the frame's source from a UI event. The PSI work (resolve and
   * descriptor building) hits indexes and the resolver, so it runs as a
   * non-blocking read action on a pooled thread once indexes are ready —
   * the EDT only performs the final PSI-free navigation. Repeat clicks on
   * the same frame coalesce into one computation.
   */
  public static void navigateToFrame(@NotNull Project project, @NotNull StackFrame frame) {
    HaxeCallStackElement element = new HaxeCallStackElement(frame.symbol(), frame.file(), frame.line());
    ReadAction.nonBlocking(() -> element.descriptor(project))
      .inSmartMode(project)
      .coalesceBy(element)
      .finishOnUiThread(ModalityState.defaultModalityState(), descriptor -> {
        if (descriptor != null) descriptor.navigate(true);
      })
      .submit(AppExecutorUtil.getAppExecutorService());
  }

  /** Runs inside the non-blocking read action, which already holds read access. */
  @Nullable
  private Navigatable descriptor(Project project) {
    NavigatablePsiElement[] navigatables = resolve(project);
    return navigatables.length > 0 ? EditSourceUtil.getDescriptor(navigatables[0]) : null;
  }

  private NavigatablePsiElement @NotNull [] resolve(Project project) {
    NavigatablePsiElement atPosition = hasLocalPosition() ? resolveByPosition(project) : null;
    if (atPosition != null) {
      return new NavigatablePsiElement[]{atPosition};
    }
    NavigatablePsiElement declaration = declarationOf(project, symbol);
    return declaration != null ? new NavigatablePsiElement[]{declaration}
                               : NavigatablePsiElement.EMPTY_NAVIGATABLE_ELEMENT_ARRAY;
  }

  /**
   * The declaration a bare qualified symbol names: the longest symbol
   * prefix that names a class wins, then its member (or the class itself)
   * — a closure symbol ({@code Main.update.2}) lands on its enclosing
   * method. Hits the resolver and indexes; call under a read action.
   */
  @Nullable
  public static NavigatablePsiElement declarationOf(@NotNull Project project, @NotNull String symbol) {
    PsiManager psiManager = PsiManager.getInstance(project);
    GlobalSearchScope scope = GlobalSearchScope.allScope(project);
    // qualified-name segments: pack.Class.method, closures with trailing numeric segments
    String[] segments = symbol.split("\\.");
    for (int memberIndex = segments.length - 1; memberIndex >= 1; memberIndex--) {
      String className = String.join(".", List.of(segments).subList(0, memberIndex));
      HaxeClass haxeClass = HaxeResolveUtil.findClassByQName(className, psiManager, scope);
      if (haxeClass == null) continue;
      NavigatablePsiElement member = findMember(haxeClass, segments[memberIndex]);
      return member != null ? member : haxeClass;
    }
    return null;
  }

  /** The named component enclosing the frame's line (or the file itself); null when the file cannot be found. */
  @Nullable
  private NavigatablePsiElement resolveByPosition(Project project) {
    VirtualFile virtualFile = findVirtualFile(project);
    if (virtualFile == null) return null;
    PsiFile psiFile = PsiManager.getInstance(project).findFile(virtualFile);
    if (psiFile == null) return null;
    Document document = PsiDocumentManager.getInstance(project).getDocument(psiFile);
    if (document == null || line <= 0 || line > document.getLineCount()) return psiFile;
    PsiElement at = psiFile.findElementAt(document.getLineStartOffset(line - 1));
    NavigatablePsiElement enclosing = PsiTreeUtil.getParentOfType(at, HaxeNamedComponent.class, false);
    return enclosing != null ? enclosing : psiFile;
  }

  /** An absolute path opens directly; a collector-relative one matches the project file whose path ends with it. */
  @Nullable
  private VirtualFile findVirtualFile(Project project) {
    String normalized = file.replace('\\', '/');
    try {
      Path path = Path.of(normalized);
      if (path.isAbsolute()) {
        return LocalFileSystem.getInstance().findFileByNioFile(path);
      }
    }
    catch (InvalidPathException notAPath) {
      return null;
    }
    String fileName = normalized.substring(normalized.lastIndexOf('/') + 1);
    for (VirtualFile candidate : FilenameIndex.getVirtualFilesByName(fileName, GlobalSearchScope.allScope(project))) {
      if (candidate.getPath().endsWith("/" + normalized)) return candidate;
    }
    return null;
  }

  @Nullable
  private static NavigatablePsiElement findMember(HaxeClass haxeClass, String name) {
    List<HaxeNamedComponent> methods = haxeClass.findHaxeMethodByName(name, null);
    if (!methods.isEmpty()) return methods.getFirst();
    return haxeClass.findHaxeFieldByName(name, null);
  }

  @Override
  public boolean equals(Object other) {
    if (this == other) return true;
    if (!(other instanceof HaxeCallStackElement element)) return false;
    return line == element.line && symbol.equals(element.symbol) && Objects.equals(file, element.file);
  }

  @Override
  public int hashCode() {
    return Objects.hash(symbol, file, line);
  }
}
