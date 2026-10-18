package com.intellij.plugins.haxe.v2.buildtools.libraries;

import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.Project;
import com.intellij.plugins.haxe.v2.buildsystem.HaxeBuildFileInfo.HaxeDefine;
import com.intellij.plugins.haxe.v2.buildsystem.HaxeBuildFileInfo.HaxeLibDependency;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The libraries the last sync resolved and how they depend on each other.
 * A node is one library as {@code haxelib path} printed it; its edges are
 * the dependencies the library's haxelib.json declares, each with the
 * version it requires. Per container the sync also records, for every
 * library a build file declares, the libraries that declaration brings in:
 * the closure {@code haxelib path} prints, the declared library first.
 * Resolve contexts use the closures to give a file of a transitively reached
 * library the builds that reach it; the tool window shows the edges. In
 * memory only: empty until the first sync lands.
 */
@Service(Service.Level.PROJECT)
public final class HaxeLibraryGraph {

  /** A dependency a haxelib.json declares: the name and the version it requires, null when any version does. */
  public record Requirement(@NotNull String name, @Nullable String version) {
  }

  /**
   * One resolved library: its name as haxelib printed it, the version
   * haxelib selected (null when it printed none), whether it serves from a
   * git or hg checkout, what its haxelib.json requires, the defines its
   * extraParams.hxml sets, and the folder that haxelib.json sits in (null
   * when none was found).
   */
  public record Library(@NotNull String name,
                        @Nullable String version,
                        boolean scmCheckout,
                        @NotNull List<Requirement> requires,
                        @NotNull List<HaxeDefine> extraDefines,
                        @Nullable String root) {
  }

  /** By lower-cased name. */
  private volatile Map<String, Library> libraries = Map.of();
  /** Container id, then lower-cased declared library, to the names the declaration brings in. */
  private volatile Map<String, Map<String, List<String>>> closures = Map.of();
  private volatile boolean synced;

  @NotNull
  public static HaxeLibraryGraph getInstance(@NotNull Project project) {
    return project.getService(HaxeLibraryGraph.class);
  }

  /** Whether a sync has landed since the project opened; before the first one the graph is empty, not resolved. */
  public boolean hasSynced() {
    return synced;
  }

  /** The resolved library of the name, whatever its case; null when no sync resolved it. */
  @Nullable
  public Library library(@NotNull String name) {
    return libraries.get(HaxeLibDependency.key(name));
  }

  /** Every resolved library, in sync order. */
  @NotNull
  public List<Library> libraries() {
    return List.copyOf(libraries.values());
  }

  /** The libraries whose haxelib.json requires the named one, in sync order. */
  @NotNull
  public List<Library> dependentsOf(@NotNull String name) {
    String wanted = HaxeLibDependency.key(name);
    List<Library> dependents = new ArrayList<>();
    for (Library library : libraries.values()) {
      boolean requiresIt = library.requires().stream().anyMatch(requirement -> HaxeLibDependency.key(requirement.name()).equals(wanted));
      if (requiresIt) dependents.add(library);
    }
    return dependents;
  }

  /**
   * Every library a container's declared library brings in, the declared one
   * first, in {@code haxelib path} order; empty before a sync or for a
   * library the container does not declare.
   */
  @NotNull
  public List<String> closureOf(@NotNull String containerId, @NotNull String declaredLibrary) {
    Map<String, List<String>> byDeclared = closures.getOrDefault(containerId, Map.of());
    return byDeclared.getOrDefault(HaxeLibDependency.key(declaredLibrary), List.of());
  }

  /** Replaces the whole picture with a sync's result; keys may come in any case. */
  public void replaceAll(@NotNull Map<String, Library> byName, @NotNull Map<String, Map<String, List<String>>> closuresByContainer) {
    Map<String, Library> normalizedLibraries = new LinkedHashMap<>();
    byName.forEach((name, library) -> normalizedLibraries.put(HaxeLibDependency.key(name), library));
    Map<String, Map<String, List<String>>> normalizedClosures = new LinkedHashMap<>();
    closuresByContainer.forEach((containerId, byDeclared) -> {
      Map<String, List<String>> normalized = new LinkedHashMap<>();
      byDeclared.forEach((declared, members) -> normalized.put(HaxeLibDependency.key(declared), List.copyOf(members)));
      normalizedClosures.put(containerId, Map.copyOf(normalized));
    });
    libraries = Map.copyOf(normalizedLibraries);
    closures = Map.copyOf(normalizedClosures);
    synced = true;
  }

}
