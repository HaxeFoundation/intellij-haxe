package com.intellij.plugins.haxe.v2.buildtools;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.plugins.haxe.lang.psi.HaxeClass;
import com.intellij.plugins.haxe.util.HaxeResolveUtil;
import com.intellij.plugins.haxe.v2.buildsystem.HaxeBuildFile;
import com.intellij.plugins.haxe.v2.buildsystem.HaxeBuildFileInspector;
import com.intellij.plugins.haxe.v2.buildsystem.HxmlFileParser;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * The class an hxml build starts from: its {@code -main} type path looked up
 * in the class paths of the section that declares it, in the order the
 * compiler searches them. A project-wide lookup by name would take whichever
 * module's class of that name it met first.
 */
public final class HaxeMainClasses {

  private HaxeMainClasses() {
  }

  /**
   * The class {@code typePath} names in the hxml section with the given
   * index (-1: the selected section), else the module file of that path;
   * null when no class path of the section holds it. Call in a read action.
   */
  @Nullable
  public static PsiElement find(@NotNull Project project,
                                @NotNull HaxeBuildFile buildFile,
                                int sectionIndex,
                                @NotNull String typePath) {
    List<String> sections = HaxeBuildFileInspector.sectionContents(project, buildFile.file());
    if (sections.isEmpty()) return null;
    int index = sectionIndex >= 0 && sectionIndex < sections.size()
                ? sectionIndex
                : HaxeBuildSections.selectedIndex(project, buildFile.file(), sections);
    List<String> classpaths = HxmlFileParser.parse(sections.get(index)).classpaths();

    PsiManager psiManager = PsiManager.getInstance(project);
    for (String directory : searchOrder(project, buildFile, classpaths)) {
      PsiElement type = typeIn(psiManager, directory, typePath);
      if (type != null) return type;
    }
    return null;
  }

  /**
   * The section's class path directories in the compiler's search order: the
   * last declared {@code -cp} first, then the work directory (the implicit
   * class path), then the module's library roots.
   */
  @NotNull
  private static Set<String> searchOrder(@NotNull Project project,
                                         @NotNull HaxeBuildFile buildFile,
                                         @NotNull List<String> classpaths) {
    VirtualFile anchor = HaxeBuildWorkDirectories.anchor(project, buildFile.file());
    if (anchor == null) return Set.of();
    Set<String> directories = new LinkedHashSet<>();
    for (VirtualFile declared : HaxeBuildClasspaths.resolvedDirectories(anchor, classpaths).reversed()) {
      directories.add(declared.getPath());
    }
    directories.addAll(HaxeBuildClasspaths.sourceDirectories(project, buildFile, classpaths));
    return directories;
  }

  /**
   * The type path's class in one class path directory: the class of module
   * {@code pack/Name.hx} (the module file when it declares no such class), or
   * the type {@code Name} declared in module {@code pack.hx}.
   */
  @Nullable
  private static PsiElement typeIn(@NotNull PsiManager psiManager, @NotNull String directory, @NotNull String typePath) {
    String typeName = StringUtil.getShortName(typePath);
    PsiFile module = moduleFile(psiManager, directory, typePath);
    if (module != null) {
      HaxeClass declared = HaxeResolveUtil.findComponentDeclaration(module, typeName);
      return declared != null ? declared : module;
    }
    String enclosingModulePath = StringUtil.getPackageName(typePath);
    if (enclosingModulePath.isEmpty()) return null;
    PsiFile enclosingModule = moduleFile(psiManager, directory, enclosingModulePath);
    return enclosingModule == null ? null : HaxeResolveUtil.findComponentDeclaration(enclosingModule, typeName);
  }

  @Nullable
  private static PsiFile moduleFile(@NotNull PsiManager psiManager, @NotNull String directory, @NotNull String modulePath) {
    String path = directory + "/" + modulePath.replace('.', '/') + ".hx";
    VirtualFile file = LocalFileSystem.getInstance().findFileByPath(path);
    return file == null ? null : psiManager.findFile(file);
  }
}
