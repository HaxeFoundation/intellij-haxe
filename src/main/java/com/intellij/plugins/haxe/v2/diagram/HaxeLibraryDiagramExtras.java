package com.intellij.plugins.haxe.v2.diagram;

import com.intellij.diagram.DiagramBuilder;
import com.intellij.diagram.DiagramNode;
import com.intellij.diagram.extras.DiagramExtras;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataSink;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.plugins.haxe.haxelib.HaxelibRepositoryLayout;
import com.intellij.plugins.haxe.v2.buildtools.libraries.HaxeLibraryGraph;
import com.intellij.pom.Navigatable;
import com.intellij.psi.PsiManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;

/**
 * Gives the diagram's source actions (Jump to Source, F4, the node's context
 * menu) the file behind each selected node: a build or include file opens
 * itself, a library its haxelib.json - where its requirement arrows come from
 * - or, without one, its folder in the Project view.
 */
final class HaxeLibraryDiagramExtras extends DiagramExtras<HaxeLibraryDiagramElement> {

  @Override
  public void uiDataSnapshot(@NotNull DataSink sink,
                             @NotNull List<DiagramNode<HaxeLibraryDiagramElement>> nodes,
                             @NotNull DiagramBuilder builder) {
    Project project = builder.getProject();
    List<HaxeLibraryDiagramElement> elements = nodes.stream().map(DiagramNode::getIdentifyingElement).toList();
    sink.lazy(CommonDataKeys.NAVIGATABLE_ARRAY, () -> navigatablesOf(project, elements));
  }

  /** The file a node stands for: the build or include file itself, a library's manifest, else the library's folder; null when nothing is on disk. */
  @Nullable
  static VirtualFile sourceFileOf(@NotNull HaxeLibraryDiagramElement element, @NotNull HaxeLibraryGraph graph) {
    if (element.isBuildFile()) return LocalFileSystem.getInstance().findFileByPath(element.key());
    VirtualFile root = HaxeLibraryDiagramSources.libraryRoot(graph, element.key());
    if (root == null) return null;
    VirtualFile manifest = root.findChild(HaxelibRepositoryLayout.MANIFEST_FILE_NAME);
    return manifest != null ? manifest : root;
  }

  /** Null when no selected node has a file on disk. Read action. */
  private static Navigatable @Nullable [] navigatablesOf(@NotNull Project project, @NotNull List<HaxeLibraryDiagramElement> elements) {
    HaxeLibraryGraph graph = HaxeLibraryGraph.getInstance(project);
    Navigatable[] navigatables = elements.stream()
      .map(element -> sourceFileOf(element, graph))
      .filter(Objects::nonNull)
      .map(file -> navigatableOf(project, file))
      .filter(Objects::nonNull)
      .toArray(Navigatable[]::new);
    return navigatables.length == 0 ? null : navigatables;
  }

  /** A file opens in the editor; a folder is selected in the Project view. */
  @Nullable
  private static Navigatable navigatableOf(@NotNull Project project, @NotNull VirtualFile file) {
    if (file.isDirectory()) return PsiManager.getInstance(project).findDirectory(file);
    return new OpenFileDescriptor(project, file);
  }
}
