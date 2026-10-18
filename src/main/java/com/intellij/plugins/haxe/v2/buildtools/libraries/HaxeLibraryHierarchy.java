package com.intellij.plugins.haxe.v2.buildtools.libraries;

import com.intellij.plugins.haxe.v2.buildsystem.HaxeBuildFileInfo.HaxeLibDependency;
import com.intellij.plugins.haxe.v2.buildtools.libraries.HaxeLibraryGraph.Library;
import com.intellij.plugins.haxe.v2.buildtools.libraries.HaxeLibraryGraph.Requirement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Every library a build file's declarations reach, as nodes and edges: the
 * declared libraries, then what each requires, breadth first through the
 * sync's graph. A library reached twice is one node and is expanded once,
 * so a dependency loop ends where it closes instead of recursing. A
 * requirement no sync resolved is a node too, so the picture shows what is
 * missing; the graph says whether a name resolved.
 */
public final class HaxeLibraryHierarchy {

  /** A requirement edge between two libraries, with the version the requirement asks for when it pins one. */
  public record Edge(@NotNull String from, @NotNull String to, @Nullable String requiredVersion) {
  }

  private final List<HaxeLibDependency> declared;
  private final List<String> nodes;
  private final List<Edge> edges;

  private HaxeLibraryHierarchy(@NotNull List<HaxeLibDependency> declared, @NotNull List<String> nodes, @NotNull List<Edge> edges) {
    this.declared = List.copyOf(declared);
    this.nodes = List.copyOf(nodes);
    this.edges = List.copyOf(edges);
  }

  @NotNull
  public static HaxeLibraryHierarchy of(@NotNull HaxeLibraryGraph graph, @NotNull List<HaxeLibDependency> declared) {
    List<String> nodes = new ArrayList<>();
    List<Edge> edges = new ArrayList<>();
    Set<String> visited = new HashSet<>();
    Deque<String> pending = new ArrayDeque<>();
    for (HaxeLibDependency library : declared) {
      if (visited.add(HaxeLibDependency.key(library.name()))) pending.add(library.name());
    }
    while (!pending.isEmpty()) {
      String name = pending.remove();
      Library library = graph.library(name);
      nodes.add(name);
      if (library == null) continue;
      for (Requirement requirement : library.requires()) {
        edges.add(new Edge(name, requirement.name(), requirement.version()));
        if (visited.add(HaxeLibDependency.key(requirement.name()))) pending.add(requirement.name());
      }
    }
    return new HaxeLibraryHierarchy(declared, nodes, edges);
  }

  /** What the build file declares, in its order. */
  @NotNull
  public List<HaxeLibDependency> declared() {
    return declared;
  }

  /** Every reached library name once, as first reached: declared ones first, then by distance. */
  @NotNull
  public List<String> nodes() {
    return nodes;
  }

  /** The requirement edges, one per declaration in a reached library's haxelib.json. */
  @NotNull
  public List<Edge> edges() {
    return edges;
  }

}
