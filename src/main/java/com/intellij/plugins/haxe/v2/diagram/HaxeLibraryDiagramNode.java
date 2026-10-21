package com.intellij.plugins.haxe.v2.diagram;

import com.intellij.diagram.DiagramNode;
import com.intellij.diagram.DiagramProvider;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.util.UserDataHolderBase;
import com.intellij.plugins.haxe.HaxeBundle;
import com.intellij.plugins.haxe.v2.buildtools.libraries.HaxeLibraryGraph;
import com.intellij.plugins.haxe.v2.buildtools.libraries.HaxeLibraryGraph.Library;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Icon;
import java.util.Locale;
import java.util.Objects;

/**
 * One box: a file (the root build file, an included hxml or a library's
 * include file) or a library. A library's icon and tooltip are
 * read from the sync's graph when asked for, never stored: a refresh keeps
 * the node objects that stand for the same element so that the layout
 * survives, and state stored in them would survive the refresh with them.
 * Two nodes are the same when they stand for the same element. Implements
 * the node interface directly, as the plugin's node base class is internal
 * API.
 */
final class HaxeLibraryDiagramNode extends UserDataHolderBase implements DiagramNode<HaxeLibraryDiagramElement> {

  private final DiagramProvider<HaxeLibraryDiagramElement> provider;
  private final HaxeLibraryGraph graph;
  private final HaxeLibraryDiagramElement element;

  HaxeLibraryDiagramNode(@NotNull DiagramProvider<HaxeLibraryDiagramElement> provider,
                         @NotNull HaxeLibraryGraph graph,
                         @NotNull HaxeLibraryDiagramElement element) {
    this.provider = provider;
    this.graph = graph;
    this.element = element;
  }

  @Override
  public @NotNull HaxeLibraryDiagramElement getIdentifyingElement() {
    return element;
  }

  @Override
  public @NotNull DiagramProvider<HaxeLibraryDiagramElement> getProvider() {
    return provider;
  }

  @Override
  public @Nullable String getTooltip() {
    if (element.isBuildFile()) return element.key();
    Library library = graph.library(element.key());
    if (library == null) return HaxeBundle.message("haxe.library.diagram.tooltip.unresolved", element.key());
    return library.version() == null ? element.key() : element.key() + " " + library.version();
  }

  @Override
  public @Nullable Icon getIcon() {
    if (element.isBuildFile()) return AllIcons.Nodes.Module;
    return graph.library(element.key()) != null ? AllIcons.Nodes.PpLib : AllIcons.General.Warning;
  }

  @Override
  public boolean equals(Object other) {
    return other instanceof HaxeLibraryDiagramNode node && sameElement(node.element);
  }

  @Override
  public int hashCode() {
    return Objects.hash(element.kind(), element.key().toLowerCase(Locale.ROOT));
  }

  private boolean sameElement(@NotNull HaxeLibraryDiagramElement other) {
    return element.kind() == other.kind() && element.key().equalsIgnoreCase(other.key());
  }
}
