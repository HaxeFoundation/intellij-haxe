package com.intellij.plugins.haxe.v2.diagram;

import com.intellij.diagram.DiagramEdgeBase;
import com.intellij.diagram.DiagramNode;
import com.intellij.diagram.DiagramRelationshipInfo;
import org.jetbrains.annotations.NotNull;

/** An arrow of the diagram; the base class carries everything and only asks to be subclassed. */
final class HaxeLibraryDiagramEdge extends DiagramEdgeBase<HaxeLibraryDiagramElement> {

  HaxeLibraryDiagramEdge(@NotNull DiagramNode<HaxeLibraryDiagramElement> source,
                         @NotNull DiagramNode<HaxeLibraryDiagramElement> target,
                         @NotNull DiagramRelationshipInfo relationship) {
    super(source, target, relationship);
  }
}
