package com.intellij.plugins.haxe.v2.testing;

import com.intellij.ide.highlighter.XmlFileType;
import com.intellij.lang.xml.XMLLanguage;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.plugins.haxe.buildsystem.hxml.HXMLFileType;
import com.intellij.plugins.haxe.buildsystem.hxml.HXMLLanguage;
import com.intellij.plugins.haxe.v2.buildsystem.HaxeBuildFile;
import com.intellij.plugins.haxe.v2.buildsystem.HaxeBuildFileInfo;
import com.intellij.plugins.haxe.v2.buildsystem.HaxeBuildFileScanner;
import com.intellij.plugins.haxe.v2.buildsystem.HaxeBuildFileType;
import com.intellij.plugins.haxe.v2.buildtools.HaxeBuildClasspaths;
import com.intellij.plugins.haxe.v2.buildtools.HaxeBuildSections;
import com.intellij.plugins.haxe.v2.buildtools.LimeProjects;
import com.intellij.plugins.haxe.v2.buildtools.settings.HaxeTestsBuildFileStore;
import com.intellij.psi.PsiFile;
import com.intellij.psi.search.FileTypeIndex;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.CachedValueProvider;
import com.intellij.psi.util.CachedValuesManager;
import com.intellij.psi.util.PsiModificationTracker;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * The tests build owning a source path, with the framework that build
 * declares — what every test entry point (gutter markers, context-menu runs)
 * needs before it can compile a selection: the build supplies classpaths,
 * defines and libraries, the framework the template and detection.
 * Ownership is by PATH: the first marked-or-conventional tests build whose
 * classpaths contain the file or directory (see {@link #candidateTestsPaths});
 * nothing claims a path outside every build. Resolved from a project-level
 * cache of the builds' classpaths, invalidated by the marked set, the VFS
 * structure and build-file content (see {@link #dependencies}) — never by
 * the whole-PSI counter, which bumps on every keystroke.
 */
public record HaxeTestContext(@NotNull HaxeTestFramework framework, @NotNull String testsBuildPath) {

  /** One tests build and the source directories it compiles. */
  private record Ownership(@NotNull HaxeTestContext context, @NotNull List<String> sourceDirectories) {
  }

  /** The file's context, or null when no marked-or-conventional tests build owns it (or its framework has no single-run support). */
  @Nullable
  public static HaxeTestContext forFile(@NotNull PsiFile file) {
    VirtualFile virtualFile = file.getVirtualFile();
    return virtualFile == null ? null : owning(file.getProject(), virtualFile);
  }

  /** The file's or directory's context: the build whose classpath contains it, or whose classpath root it is. */
  @Nullable
  public static HaxeTestContext owning(@NotNull Project project, @NotNull VirtualFile fileOrDirectory) {
    return forPath(project, fileOrDirectory.getPath());
  }

  @Nullable
  private static HaxeTestContext forPath(@NotNull Project project, @NotNull String path) {
    // TODO: a directory ABOVE a classpath root (the tests build's own directory) is claimed by nothing
    String pathAsPrefix = path + "/";
    for (Ownership ownership : ownerships(project)) {
      boolean owns = ownership.sourceDirectories().stream().anyMatch(directory -> pathAsPrefix.startsWith(directory + "/"));
      if (owns) return ownership.context();
    }
    return null;
  }

  @NotNull
  private static List<Ownership> ownerships(@NotNull Project project) {
    return CachedValuesManager.getManager(project).getCachedValue(project, () ->
      CachedValueProvider.Result.create(computeOwnerships(project), dependencies(project)));
  }

  private static Object @NotNull [] dependencies(@NotNull Project project) {
    PsiModificationTracker psiTracker = PsiModificationTracker.getInstance(project);
    return new Object[]{
      HaxeTestsBuildFileStore.getInstance(project).getModificationTracker(),
      VirtualFileManager.VFS_STRUCTURE_MODIFICATIONS,
      psiTracker.forLanguage(HXMLLanguage.INSTANCE),
      psiTracker.forLanguage(XMLLanguage.INSTANCE)};
  }

  @NotNull
  private static List<Ownership> computeOwnerships(@NotNull Project project) {
    List<Ownership> ownerships = new ArrayList<>();
    for (String testsPath : candidateTestsPaths(project)) {
      VirtualFile testsFile = LocalFileSystem.getInstance().findFileByPath(testsPath);
      if (testsFile == null || !testsFile.isValid()) continue;
      // lime-family ownership/detection reads the project xml's DECLARED
      // sources and haxelibs (no tool run) - precise enough to claim a file
      // TODO single runs for nmml tests builds: nme's app-main override is unverified
      HaxeBuildFileType type = HaxeBuildFileScanner.detectType(project, testsFile);
      boolean supported = type == HaxeBuildFileType.HXML || LimeProjects.isLimeFamily(type);
      if (!supported) continue;
      // ONE inspection per candidate: its classpaths decide ownership, its
      // -lib declarations the framework
      HaxeBuildFileInfo info = HaxeBuildSections.inspectSelected(project, new HaxeBuildFile(testsFile, type));
      // a build declaring no framework lib offers no runs - the detection
      // default must not turn an application build into a utest run
      HaxeTestFramework framework = HaxeTestFrameworks.detectedFramework(info.libraries());
      if (framework == null || framework.singleRunTemplate(false) == null) continue;
      List<String> sourceDirectories = HaxeBuildClasspaths.sourceDirectories(project, testsFile, info.classpaths());
      ownerships.add(new Ownership(new HaxeTestContext(framework, testsPath), List.copyOf(sourceDirectories)));
    }
    return List.copyOf(ownerships);
  }

  /**
   * The tests builds a path can belong to: the explicitly MARKED ones first,
   * then the CONVENTIONAL candidates among the project's hxml and
   * lime-family project files (test.hxml/tests.hxml names, files under a
   * tests/ directory) minus the explicitly EXCLUDED ones — the same per-file
   * toggle rule the tool window's Tests rows follow, so a project that never
   * explicitly marked anything still gets runs. The discovery enumerates
   * every hxml and xml file in project scope, hence the project-level cache.
   */
  @NotNull
  private static List<String> candidateTestsPaths(@NotNull Project project) {
    HaxeTestsBuildFileStore store = HaxeTestsBuildFileStore.getInstance(project);
    List<String> candidates = new ArrayList<>(store.getAllTestsFilePaths());
    List<String> excluded = store.getAllExcludedFilePaths();
    for (VirtualFile hxml : FileTypeIndex.getFiles(HXMLFileType.INSTANCE, GlobalSearchScope.projectScope(project))) {
      addConventional(candidates, excluded, hxml.getPath());
    }
    for (VirtualFile xml : FileTypeIndex.getFiles(XmlFileType.INSTANCE, GlobalSearchScope.projectScope(project))) {
      // the cheap path check gates the per-file type detection
      if (HaxeTestsBuildFileStore.isConventionalTestsPath(xml.getPath())
          && LimeProjects.isLimeFamily(HaxeBuildFileScanner.detectType(project, xml))) {
        addConventional(candidates, excluded, xml.getPath());
      }
    }
    return List.copyOf(candidates);
  }

  private static void addConventional(@NotNull List<String> candidates,
                                      @NotNull List<String> excluded,
                                      @NotNull String path) {
    boolean conventional = HaxeTestsBuildFileStore.isConventionalTestsPath(path) && !excluded.contains(path);
    if (conventional && !candidates.contains(path)) {
      candidates.add(path);
    }
  }
}
