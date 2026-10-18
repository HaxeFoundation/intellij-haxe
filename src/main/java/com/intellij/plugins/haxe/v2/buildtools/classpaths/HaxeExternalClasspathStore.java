package com.intellij.plugins.haxe.v2.buildtools.classpaths;

import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.RootsChangeRescanningInfo;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.roots.ex.ProjectRootManagerEx;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.plugins.haxe.v2.buildtools.classpaths.HaxeExternalClasspaths.ExternalClasspath;
import com.intellij.psi.PsiManager;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Every module's external classpaths ({@link HaxeExternalClasspaths}) as the
 * last library sync found them, read by the resolve and use scope enlargers,
 * the synthetic libraries and the Haxe roots. In-memory only: every project
 * open runs a library sync, which publishes them before its haxelib lookups.
 * A change makes the platform re-read the project roots, then drops the
 * resolve scopes and rehighlights.
 */
@Service(Service.Level.PROJECT)
public final class HaxeExternalClasspathStore {

  private final Project project;
  private volatile Map<String, List<ExternalClasspath>> byModuleName = Map.of();

  public HaxeExternalClasspathStore(@NotNull Project project) {
    this.project = project;
  }

  @NotNull
  public static HaxeExternalClasspathStore getInstance(@NotNull Project project) {
    return project.getService(HaxeExternalClasspathStore.class);
  }

  /** Replaces every module's external classpaths; a change refreshes the roots, the scopes and the highlighting. */
  public void replaceAll(@NotNull Map<String, List<ExternalClasspath>> classpathsByModuleName) {
    if (classpathsByModuleName.equals(byModuleName)) return;
    byModuleName = Map.copyOf(classpathsByModuleName);
    refreshLater();
  }

  /** The module's external classpath folders. */
  @NotNull
  public List<VirtualFile> directoriesOf(@NotNull String moduleName) {
    return validDirectories(byModuleName.getOrDefault(moduleName, List.of()).stream());
  }

  /** Every module's external classpath folders, each once. */
  @NotNull
  public List<VirtualFile> allDirectories() {
    return validDirectories(allClasspaths());
  }

  /** The external classpath folders no module's content holds. */
  @NotNull
  public List<VirtualFile> outsideModuleDirectories() {
    return validDirectories(allClasspaths().filter(ExternalClasspath::outsideModules));
  }

  /** The modules whose external classpaths hold the file. */
  @NotNull
  public List<String> modulesReaching(@NotNull VirtualFile file) {
    List<String> moduleNames = new ArrayList<>();
    for (Map.Entry<String, List<ExternalClasspath>> entry : byModuleName.entrySet()) {
      boolean reaches = entry.getValue().stream().anyMatch(classpath -> VfsUtilCore.isAncestor(classpath.directory(), file, false));
      if (reaches) {
        moduleNames.add(entry.getKey());
      }
    }
    return moduleNames;
  }

  @NotNull
  private Stream<ExternalClasspath> allClasspaths() {
    return byModuleName.values()
      .stream()
      .flatMap(List::stream);
  }

  @NotNull
  private static List<VirtualFile> validDirectories(@NotNull Stream<ExternalClasspath> classpaths) {
    return classpaths
      .map(ExternalClasspath::directory)
      .filter(VirtualFile::isValid)
      .distinct()
      .toList();
  }

  private void refreshLater() {
    ApplicationManager.getApplication().invokeLater(this::refresh, ModalityState.nonModal(), project.getDisposed());
  }

  /**
   * The roots change makes the platform re-read the synthetic libraries and
   * index new ones, and clears the Haxe roots and resolver caches; the PSI
   * cache drop clears the resolve scopes, which follow PSI changes only.
   */
  private void refresh() {
    // no model edit: the event alone makes the platform re-read the providers
    Runnable noModelEdit = () -> {};
    ProjectRootManagerEx rootManager = ProjectRootManagerEx.getInstanceEx(project);
    WriteAction.run(() -> rootManager.makeRootsChange(noModelEdit, RootsChangeRescanningInfo.RESCAN_DEPENDENCIES_IF_NEEDED));
    readSyntheticLibraries();
    PsiManager.getInstance(project).dropPsiCaches();
    DaemonCodeAnalyzer.getInstance(project).restart("haxe: external classpaths changed");
  }

  /**
   * The file index reads the synthetic libraries again on its next file
   * query, but its package lookups do not wait for that: one asked first
   * caches the old package roots until the next roots change. A file query
   * right after the change makes the package of an outside folder resolve.
   */
  private void readSyntheticLibraries() {
    List<VirtualFile> outsideDirectories = outsideModuleDirectories();
    if (outsideDirectories.isEmpty()) return;
    ProjectFileIndex fileIndex = ProjectFileIndex.getInstance(project);
    ReadAction.computeBlocking(() -> fileIndex.isInLibrary(outsideDirectories.getFirst()));
  }
}
