package com.intellij.plugins.haxe.v2.diagram;

import com.intellij.diagram.*;
import com.intellij.diagram.extras.DiagramExtras;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.plugins.haxe.HaxeBundle;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The Diagrams-plugin provider for a build file's project diagram: the build
 * file as the root, the hxml files it includes, arrows to the libraries each
 * file declares and from each library to what its haxelib.json requires.
 * Registered only beside the Diagrams plugin (see the optional descriptor).
 */
public final class HaxeLibraryDiagramProvider extends DiagramProvider<HaxeLibraryDiagramElement> {

  public static final String ID = "HaxeLibraryDependencies";

  private final HaxeLibraryDiagramElementManager elementManager = new HaxeLibraryDiagramElementManager();
  private final HaxeLibraryDiagramVfsResolver vfsResolver = new HaxeLibraryDiagramVfsResolver();
  private final HaxeLibraryDiagramExtras extras = new HaxeLibraryDiagramExtras();

  public HaxeLibraryDiagramProvider() {
    elementManager.setUmlProvider(this);
  }

  @Override
  public @NotNull String getID() {
    return ID;
  }

  @Override
  public @NotNull String getPresentableName() {
    return HaxeBundle.message("haxe.library.diagram.name");
  }

  @Override
  public @NotNull DiagramDataModel<HaxeLibraryDiagramElement> createDataModel(@NotNull Project project,
                                                                            @Nullable HaxeLibraryDiagramElement element,
                                                                            @Nullable VirtualFile file,
                                                                            @NotNull DiagramPresentationModel presentationModel) {
    return new HaxeLibraryDiagramDataModel(project, this, element);
  }

  @Override
  public @NotNull DiagramVisibilityManager createVisibilityManager() {
    return EmptyDiagramVisibilityManager.INSTANCE;
  }

  @Override
  public @NotNull DiagramNodeContentManager createNodeContentManager() {
    return new HaxeLibraryDiagramContentManager();
  }

  @Override
  public DiagramCategory @NotNull [] getAllContentCategories() {
    return HaxeLibraryDiagramContentManager.CATEGORIES;
  }

  @Override
  public @NotNull DiagramElementManager<HaxeLibraryDiagramElement> getElementManager() {
    return elementManager;
  }

  @Override
  public @NotNull DiagramVfsResolver<HaxeLibraryDiagramElement> getVfsResolver() {
    return vfsResolver;
  }

  @Override
  public @NotNull DiagramExtras<HaxeLibraryDiagramElement> getExtras() {
    return extras;
  }

  /** The edges carry their own relationship; nothing is derived between arbitrary node pairs. */
  @SuppressWarnings("unchecked")
  @Override
  public @NotNull DiagramRelationshipManager<HaxeLibraryDiagramElement> getRelationshipManager() {
    return (DiagramRelationshipManager<HaxeLibraryDiagramElement>)DiagramRelationshipManager.NO_RELATIONSHIP_MANAGER;
  }
}
