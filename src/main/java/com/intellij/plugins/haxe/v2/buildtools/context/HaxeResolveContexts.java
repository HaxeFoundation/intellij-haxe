package com.intellij.plugins.haxe.v2.buildtools.context;

import com.intellij.openapi.components.Service;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.LibraryOrderEntry;
import com.intellij.openapi.roots.OrderEntry;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.plugins.haxe.v2.buildsystem.*;
import com.intellij.plugins.haxe.v2.buildsystem.HaxeBuildFileInfo.HaxeLibDependency;
import com.intellij.plugins.haxe.v2.buildtools.*;
import com.intellij.plugins.haxe.v2.buildtools.libraries.HaxeLibrarySync;
import com.intellij.plugins.haxe.v2.buildtools.settings.HaxeEnvironmentStore;
import com.intellij.plugins.haxe.v2.buildtools.settings.HaxeResolveContextStore;
import com.intellij.psi.util.CachedValueProvider;
import com.intellij.psi.util.CachedValuesManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.HashSet;
import com.intellij.plugins.haxe.v2.buildtools.libraries.HaxeLibraryGraph;
import java.util.Set;

/**
 * Which build each file is analysed under. Ownership is by path: among the
 * project's known build files whose sources contain the file, those with the
 * most specific source directory are its closest owners. The user's pin wins
 * while its build contains the file, then the followed context (the build a
 * navigation carried over) while it is among the closest owners. Otherwise
 * the closest owners (two hxml with the same {@code -cp src}) are told apart
 * by the build whose {@code -main} class the file is, then the project's
 * active build file, then the Build command of the file's container, then
 * tool-window order. A file no build contains falls back to its module's
 * default build ({@link #defaultBuildFileFor}).
 * <p>
 * The builds' source directories are cached for the project and dropped only
 * by what can change them: the build inputs ({@link HaxeBuildInputs}), a
 * change of the project roots and an edit of an hxml or xml file. A build
 * writing its output does not.
 */
@Service(Service.Level.PROJECT)
public final class HaxeResolveContexts {

  /**
   * One build, the source directories it compiles and the haxelibs its selected
   * section declares; {@code mainFileSuffix} is {@code /pkg/Main.hx} for an hxml
   * {@code -main}.
   */
  private record Ownership(@NotNull HaxeBuildFile buildFile,
                           @NotNull List<String> sourceDirectories,
                           @NotNull Set<String> haxelibs,
                           @Nullable String mainFileSuffix) {
    /** Length of the longest source directory containing the path, or -1 when none does. */
    int specificity(@NotNull String pathAsPrefix) {
      return HaxeBuildOwnership.specificity(sourceDirectories, pathAsPrefix);
    }

    /** Whether the file is this build's main class. */
    boolean isMain(@NotNull String path) {
      if (mainFileSuffix == null) return false;
      return sourceDirectories.stream().anyMatch(directory -> path.equals(directory + mainFileSuffix));
    }
  }

  private final Project project;

  public HaxeResolveContexts(@NotNull Project project) {
    this.project = project;
  }

  @NotNull
  public static HaxeResolveContexts getInstance(@NotNull Project project) {
    return project.getService(HaxeResolveContexts.class);
  }

  /**
   * The context a file is analysed under: the user's pin, else the followed
   * context - each only while its build still contains the file - else the
   * automatic choice; null when neither a build nor the file's module names
   * one. Call in a read action.
   */
  // TODO: multiverse: the provider's getPreferredContext answers this
  @Nullable
  public HaxeResolveContext contextFor(@NotNull VirtualFile file) {
    List<Ownership> owners = owners(file);
    Ownership chosen = chosenAmong(file, owners);
    if (chosen != null) {
      return new HaxeResolveContext(chosen.buildFile());
    }
    if (!owners.isEmpty()) {
      return new HaxeResolveContext(preferred(file, owners).buildFile());
    }
    // honorExclusion = false: a generated file in an excluded folder (lime's export) still gets its module
    Module module = ProjectFileIndex.getInstance(project).getModuleForFile(file, false);
    HaxeBuildFile fallback = module == null ? null : defaultBuildFileFor(module);
    return fallback == null ? null : new HaxeResolveContext(fallback);
  }

  /** Whether the file's context is a pin that still applies (its build contains the file). Call in a read action. */
  public boolean isPinned(@NotNull VirtualFile file) {
    return pinnedAmong(file, owners(file)) != null;
  }

  /** Whether the file shows the context a navigation gave it (no pin, and that build is among its closest owners). Call in a read action. */
  public boolean isFollowed(@NotNull VirtualFile file) {
    List<Ownership> owners = owners(file);
    return pinnedAmong(file, owners) == null && followedAmong(file, owners) != null;
  }

  /**
   * Gives {@code to} the context of {@code from}: a navigation landing in a
   * file several builds reach equally (a library or a shared source) shows it
   * as the file it came from sees it. Only among {@code to}'s closest owners:
   * a root hxml's implicit classpath reaches the whole project, so a file
   * with a closer owner keeps it. Nothing changes for a pinned file or when
   * the two already agree. Returns whether the context changed. Call in a
   * read action.
   */
  public boolean follow(@NotNull VirtualFile from, @NotNull VirtualFile to) {
    HaxeResolveContextStore store = HaxeResolveContextStore.getInstance(project);
    if (store.getPinnedBuildFile(to) != null) return false;
    HaxeResolveContext origin = contextFor(from);
    if (origin == null) return false;
    List<Ownership> owners = owners(to);
    if (owners.isEmpty()) return false;
    List<Ownership> closest = closestOwners(to.getPath(), owners);
    if (closest.size() < 2 || findOwner(closest, origin.path()) == null) return false;
    HaxeResolveContext current = contextFor(to);
    if (current != null && current.path().equals(origin.path())) return false;
    store.follow(to, origin.path());
    return true;
  }

  /** The user's pin, else the context a navigation gave the file - each only while its build contains the file. */
  @Nullable
  private Ownership chosenAmong(@NotNull VirtualFile file, @NotNull List<Ownership> owners) {
    Ownership pinned = pinnedAmong(file, owners);
    return pinned != null ? pinned : followedAmong(file, owners);
  }

  @Nullable
  private Ownership pinnedAmong(@NotNull VirtualFile file, @NotNull List<Ownership> owners) {
    String pinnedPath = HaxeResolveContextStore.getInstance(project).getPinnedBuildFile(file);
    return pinnedPath == null ? null : findOwner(owners, pinnedPath);
  }

  /** The followed build applies only while it stays among the file's closest owners - the rule {@link #follow} chose it by. */
  @Nullable
  private Ownership followedAmong(@NotNull VirtualFile file, @NotNull List<Ownership> owners) {
    String followedPath = HaxeResolveContextStore.getInstance(project).getFollowedBuildFile(file);
    if (followedPath == null || owners.isEmpty()) return null;
    return findOwner(closestOwners(file.getPath(), owners), followedPath);
  }

  @Nullable
  private static Ownership findOwner(@NotNull List<Ownership> owners, @Nullable String buildFilePath) {
    if (buildFilePath == null) return null;
    return owners.stream()
      .filter(ownership -> ownership.buildFile().file().getPath().equals(buildFilePath))
      .findFirst()
      .orElse(null);
  }

  /** Every build whose sources contain the file: the most specific first, tool-window order among equals. Call in a read action. */
  // TODO: multiverse: the provider's getContexts answers this
  @NotNull
  public List<HaxeResolveContext> contextsReaching(@NotNull VirtualFile file) {
    return owners(file).stream()
      .map(ownership -> new HaxeResolveContext(ownership.buildFile()))
      .toList();
  }

  /**
   * The module's default build: the project's active build file when this
   * module owns it, else the file of the module's Build command. Null when
   * the module has neither.
   */
  @Nullable
  public HaxeBuildFile defaultBuildFileFor(@NotNull Module module) {
    HaxeBuildFile active = buildFileOwnedBy(HaxeKnownBuildFiles.effectiveActivePath(project), module);
    if (active != null) return active;
    HaxeEnvironmentStore.CompileCommand command = HaxeEnvironmentStore.getInstance(project).getCompileCommand(module.getName());
    return command == null ? null : buildFileOwnedBy(command.buildFilePath(), module);
  }

  @Nullable
  private HaxeBuildFile buildFileOwnedBy(@Nullable String path, @NotNull Module module) {
    if (path == null || path.isBlank()) return null;
    HaxeBuildFile buildFile = HaxeBuildFileScanner.findBuildFile(project, path);
    if (buildFile == null) return null;
    return HaxeContainers.containerIdFor(project, buildFile.file()).equals(module.getName()) ? buildFile : null;
  }

  /**
   * The builds reaching the file: those whose sources contain it, the most
   * specific first and the cache's order (tool-window order) among equals.
   * A library or std file sits in no build's sources: a haxelib file is
   * reached by the builds declaring its lib, any other library file (the
   * std, a plain path library) by every known build, since every build
   * compiles against them.
   */
  @NotNull
  private List<Ownership> owners(@NotNull VirtualFile file) {
    String pathAsPrefix = file.getPath() + "/";
    List<Ownership> owners = new ArrayList<>();
    for (Ownership ownership : ownerships()) {
      if (ownership.specificity(pathAsPrefix) >= 0) owners.add(ownership);
    }
    if (owners.isEmpty() && ProjectFileIndex.getInstance(project).isInLibrary(file)) {
      return declaringBuilds(file);
    }
    owners.sort((a, b) -> Integer.compare(b.specificity(pathAsPrefix), a.specificity(pathAsPrefix)));
    return owners;
  }

  /**
   * The builds reaching the haxelib a library file belongs to, by declaring
   * it or a library that depends on it; every build when the file is not
   * under a synced haxelib entry or no build reaches its lib.
   */
  @NotNull
  private List<Ownership> declaringBuilds(@NotNull VirtualFile file) {
    String haxelib = haxelibOf(file);
    if (haxelib == null) return ownerships();
    String wanted = HaxeLibDependency.key(haxelib);
    List<Ownership> declaring = ownerships().stream()
      .filter(ownership -> ownership.haxelibs().contains(wanted))
      .toList();
    return declaring.isEmpty() ? ownerships() : declaring;
  }

  /** The lib of the synced haxelib entry whose roots hold the file, or null. */
  @Nullable
  private String haxelibOf(@NotNull VirtualFile file) {
    for (OrderEntry entry : ProjectFileIndex.getInstance(project).getOrderEntriesForFile(file)) {
      if (!(entry instanceof LibraryOrderEntry libraryEntry)) continue;
      String haxelib = HaxeLibrarySync.haxelibNameOf(libraryEntry.getLibraryName());
      if (haxelib != null) return haxelib;
    }
    return null;
  }

  /**
   * Among the closest owners (every build reaching a library file): the file's
   * -main build, else the active build file, else the Build command of the
   * file's container, else the first in tool-window order.
   */
  @NotNull
  private Ownership preferred(@NotNull VirtualFile file, @NotNull List<Ownership> owners) {
    String path = file.getPath();
    List<Ownership> candidates = closestOwners(path, owners);
    for (Ownership candidate : candidates) {
      if (candidate.isMain(path)) return candidate;
    }
    Ownership active = findOwner(candidates, HaxeKnownBuildFiles.effectiveActivePath(project));
    if (active != null) return active;
    Ownership command = findOwner(candidates, compileCommandPath(file));
    return command != null ? command : candidates.getFirst();
  }

  /** The build file of the Build command set for the file's container, or null. */
  @Nullable
  private String compileCommandPath(@NotNull VirtualFile file) {
    String containerId = HaxeContainers.containerIdFor(project, file);
    HaxeEnvironmentStore.CompileCommand command = HaxeEnvironmentStore.getInstance(project).getCompileCommand(containerId);
    return command == null ? null : command.buildFilePath();
  }

  /** The owners tied for the most specific source directory containing the path; all owners when none contains it (a library file). */
  @NotNull
  private static List<Ownership> closestOwners(@NotNull String path, @NotNull List<Ownership> owners) {
    List<Ownership> closest = HaxeBuildOwnership.closest(path, owners, Ownership::sourceDirectories);
    return closest.isEmpty() ? owners : closest;
  }

  @NotNull
  private List<Ownership> ownerships() {
    return CachedValuesManager.getManager(project).getCachedValue(project, () ->
      CachedValueProvider.Result.create(computeOwnerships(), HaxeBuildInputs.getInstance(project).dependencies()));
  }

  /** One inspection per known build file: its classpaths decide ownership, its main class the tie-break. */
  @NotNull
  private List<Ownership> computeOwnerships() {
    List<Ownership> ownerships = new ArrayList<>();
    for (HaxeBuildFile buildFile : HaxeKnownBuildFiles.all(project)) {
      // a plain hxp script generates its compiler arguments in code - nothing to derive statically
      if (buildFile.type() == HaxeBuildFileType.HXP_SCRIPT) continue;
      HaxeBuildFileInfo info = HaxeBuildSections.inspectSelected(project, buildFile);
      List<String> sourceDirectories = HaxeBuildClasspaths.sourceDirectories(project, buildFile, info.classpaths());
      Set<String> haxelibs = reachableLibraries(buildFile, info);
      ownerships.add(new Ownership(buildFile, List.copyOf(sourceDirectories), haxelibs, mainFileSuffix(buildFile)));
    }
    return List.copyOf(ownerships);
  }

  /**
   * The libraries the build reaches, lower-cased: the ones it declares and,
   * once a library sync recorded them, everything those bring in.
   */
  @NotNull
  private Set<String> reachableLibraries(@NotNull HaxeBuildFile buildFile, @NotNull HaxeBuildFileInfo info) {
    HaxeLibraryGraph graph = HaxeLibraryGraph.getInstance(project);
    String containerId = HaxeContainers.containerIdFor(project, buildFile.file());
    Set<String> reachable = new HashSet<>();
    for (HaxeLibDependency library : info.libraries()) {
      reachable.add(library.key());
      for (String member : graph.closureOf(containerId, library.name())) {
        reachable.add(HaxeLibDependency.key(member));
      }
    }
    return Set.copyOf(reachable);
  }

  /** The hxml's {@code -main} class as a path suffix under a source directory; null without one. */
  // TODO: a Lime-family suffix from <app main="..."/> (HaxeMainClasses.mainTypePath reads it);
  //  two project.xml sharing a source folder tie on the active file until then
  @Nullable
  private String mainFileSuffix(@NotNull HaxeBuildFile buildFile) {
    if (buildFile.type() != HaxeBuildFileType.HXML) return null;
    String content = HaxeBuildSections.selectedSectionContent(project, buildFile);
    String mainClass = content == null ? null : HxmlFileParser.mainClass(content);
    return mainClass == null ? null : "/" + mainClass.replace(".", "/") + ".hx";
  }
}
