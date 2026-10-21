package com.intellij.plugins.haxe.v2.buildtools;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.plugins.haxe.lang.psi.HaxeClass;
import com.intellij.plugins.haxe.util.HaxeResolveUtil;
import com.intellij.plugins.haxe.v2.buildsystem.HaxeBuildFile;
import com.intellij.plugins.haxe.v2.buildsystem.HaxeBuildFileInspector;
import com.intellij.plugins.haxe.v2.buildsystem.HaxeBuildFileType;
import com.intellij.plugins.haxe.v2.buildsystem.HxmlFileParser;
import com.intellij.plugins.haxe.v2.buildsystem.ProjectXmlParser;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * The class a build starts from: an hxml's {@code -main} type path looked up
 * in the class paths of the section that declares it, a Lime project's
 * {@code <app main>} in its declared sources, in the order the compiler
 * searches them. A project-wide lookup by name would take whichever module's
 * class of that name it met first.
 */
public final class HaxeMainClasses {

  private HaxeMainClasses() {
  }

  /** The class the build's selected section or target starts from; null when it names none or no class path holds it. Read action. */
  @Nullable
  public static HaxeClass mainClassOf(@NotNull Project project, @NotNull HaxeBuildFile buildFile) {
    String typePath = mainTypePath(project, buildFile);
    if (typePath == null) return null;
    return find(project, buildFile, -1, typePath) instanceof HaxeClass mainClass ? mainClass : null;
  }

  /** The type path the build starts from: an hxml section's {@code -main}, a Lime project's {@code <app main>}; else null. */
  @Nullable
  private static String mainTypePath(@NotNull Project project, @NotNull HaxeBuildFile buildFile) {
    String content = HaxeBuildSections.selectedSectionContent(project, buildFile);
    if (content == null) return null;
    if (buildFile.type() == HaxeBuildFileType.HXML) return HxmlFileParser.mainClass(content);
    return LimeProjects.isLimeFamily(buildFile.type()) ? ProjectXmlParser.parseAppMain(content) : null;
  }

  /**
   * The class {@code typePath} names in the hxml section with the given
   * index (-1: the selected section), or in a project file's declared
   * sources, else the module file of that path; null when no class path
   * holds it. Call in a read action.
   */
  @Nullable
  public static PsiElement find(@NotNull Project project,
                                @NotNull HaxeBuildFile buildFile,
                                int sectionIndex,
                                @NotNull String typePath) {
    List<String> classpaths = classpathsOf(project, buildFile, sectionIndex);
    if (classpaths == null) return null;

    PsiManager psiManager = PsiManager.getInstance(project);
    for (String directory : searchOrder(project, buildFile, classpaths)) {
      PsiElement type = typeIn(psiManager, directory, typePath);
      if (type != null) return type;
    }
    return null;
  }

  /** The declared class paths: the hxml section's (-1: the selected one), a project file's sources; null when unreadable. */
  @Nullable
  private static List<String> classpathsOf(@NotNull Project project, @NotNull HaxeBuildFile buildFile, int sectionIndex) {
    if (buildFile.type() != HaxeBuildFileType.HXML) return HaxeBuildSections.inspectSelected(project, buildFile).classpaths();
    List<String> sections = HaxeBuildFileInspector.sectionContents(project, buildFile.file());
    if (sections.isEmpty()) return null;
    int index = sectionIndex >= 0 && sectionIndex < sections.size()
                ? sectionIndex
                : HaxeBuildSections.selectedIndex(project, buildFile.file(), sections);
    return HxmlFileParser.parse(sections.get(index)).classpaths();
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
