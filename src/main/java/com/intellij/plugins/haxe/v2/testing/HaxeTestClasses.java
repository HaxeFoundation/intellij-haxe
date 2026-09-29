package com.intellij.plugins.haxe.v2.testing;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.fileTypes.FileTypeRegistry;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileVisitor;
import com.intellij.plugins.haxe.HaxeFileType;
import com.intellij.plugins.haxe.lang.psi.HaxeClass;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * The suite classes a framework accepts in a file or under a directory,
 * spelled as the generated single-run main reaches them. Call in a read
 * action.
 */
public final class HaxeTestClasses {

  private HaxeTestClasses() {
  }

  @NotNull
  public static List<String> inFile(@NotNull PsiFile file, @NotNull HaxeTestFramework framework) {
    List<String> references = new ArrayList<>();
    for (HaxeClass haxeClass : PsiTreeUtil.findChildrenOfType(file, HaxeClass.class)) {
      if (!framework.isTestClass(haxeClass)) continue;
      String reference = templateReference(haxeClass);
      if (reference != null) references.add(reference);
    }
    return references;
  }

  /**
   * Every haxe file below the directory (excluded roots such as export
   * output skipped), sorted by reference so the same selection always spells
   * the same run. A classpath outside the project content - a shared
   * {@code -cp ../lib}, a haxelib dev path - is walked as-is.
   */
  @NotNull
  public static List<String> underDirectory(@NotNull Project project, @NotNull VirtualFile directory, @NotNull HaxeTestFramework framework) {
    List<String> references = new ArrayList<>();
    PsiManager psiManager = PsiManager.getInstance(project);
    ProjectFileIndex fileIndex = ProjectFileIndex.getInstance(project);
    if (fileIndex.isInContent(directory)) {
      fileIndex.iterateContentUnderDirectory(directory, file -> collectSuites(psiManager, file, framework, references));
    }
    else {
      // TODO: bound this walk - a large shared classpath loads every file below it into the VFS
      VfsUtilCore.iterateChildrenRecursively(directory, HaxeTestClasses::isVisited,
                                             file -> collectSuites(psiManager, file, framework, references),
                                             VirtualFileVisitor.NO_FOLLOW_SYMLINKS);
    }
    references.sort(null);
    return references;
  }

  /** Ignored names (.git, build output patterns) are skipped outside the content as the index skips them inside. */
  private static boolean isVisited(@NotNull VirtualFile child) {
    return !FileTypeRegistry.getInstance().isFileIgnored(child);
  }

  /** Adds the suites of one Haxe file to {@code into}; always continues the iteration. */
  private static boolean collectSuites(@NotNull PsiManager psiManager,
                                       @NotNull VirtualFile file,
                                       @NotNull HaxeTestFramework framework,
                                       @NotNull List<String> into) {
    if (file.isDirectory() || file.getFileType() != HaxeFileType.INSTANCE) return true;
    PsiFile psiFile = psiManager.findFile(file);
    if (psiFile != null) into.addAll(inFile(psiFile, framework));
    return true;
  }

  /**
   * The spelling the generated main reaches the class by. A module's
   * non-primary class is not reachable by its bare dotted name from other
   * modules — it needs the module in the path ({@code pack.Module.Class}).
   */
  @Nullable
  public static String templateReference(@NotNull HaxeClass haxeClass) {
    String qualifiedName = haxeClass.getQualifiedName();
    String className = haxeClass.getName();
    if (qualifiedName == null || className == null) return null;
    PsiFile file = haxeClass.getContainingFile();
    VirtualFile virtualFile = file == null ? null : file.getVirtualFile();
    String moduleName = virtualFile == null ? null : virtualFile.getNameWithoutExtension();
    if (moduleName == null || moduleName.equals(className)) return qualifiedName;
    int classStart = qualifiedName.length() - className.length();
    return qualifiedName.substring(0, classStart) + moduleName + "." + className;
  }
}
