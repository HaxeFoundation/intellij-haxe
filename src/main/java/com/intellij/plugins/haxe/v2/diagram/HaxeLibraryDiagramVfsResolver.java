package com.intellij.plugins.haxe.v2.diagram;

import com.intellij.diagram.DiagramVfsResolver;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** Persists a diagram's root as its qualified name and brings it back on reopen. */
final class HaxeLibraryDiagramVfsResolver implements DiagramVfsResolver<HaxeLibraryDiagramElement> {

  @Override
  public @NotNull String getQualifiedName(@Nullable HaxeLibraryDiagramElement element) {
    return element == null ? "" : element.qualifiedName();
  }

  @Override
  public @Nullable HaxeLibraryDiagramElement resolveElementByFQN(@NotNull String qualifiedName, @NotNull Project project) {
    return HaxeLibraryDiagramElement.fromQualifiedName(qualifiedName);
  }
}
