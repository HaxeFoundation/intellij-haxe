package com.intellij.plugins.haxe.v2.diagram;

import com.intellij.diagram.DiagramBuilder;
import com.intellij.diagram.DiagramDataModel;
import com.intellij.diagram.DiagramEdge;
import com.intellij.diagram.DiagramNode;
import com.intellij.diagram.DiagramProvider;
import com.intellij.diagram.DiagramRelationshipInfo;
import com.intellij.diagram.DiagramRelationshipInfoAdapter;
import com.intellij.diagram.presentation.DiagramLineType;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.ModificationTracker;
import com.intellij.plugins.haxe.util.HaxeReadActions;
import com.intellij.plugins.haxe.v2.buildsystem.HaxeBuildFileInfo.HaxeDefine;
import com.intellij.plugins.haxe.v2.buildsystem.HaxeBuildFileInfo.HaxeLibDependency;
import com.intellij.plugins.haxe.v2.buildsystem.HaxeBuildFileInfo;
import com.intellij.plugins.haxe.v2.buildsystem.HxmlIncludeTree;
import com.intellij.plugins.haxe.v2.buildtools.HaxeBuildEventListener;
import com.intellij.plugins.haxe.v2.buildtools.libraries.HaxeLibraryGraph;
import com.intellij.plugins.haxe.v2.buildtools.libraries.HaxeLibraryHierarchy;
import com.intellij.plugins.haxe.v2.buildtools.libraries.HaxeLibrarySync;
import com.intellij.plugins.haxe.v2.diagram.HaxeLibraryDiagramElement.Define;
import com.intellij.plugins.haxe.v2.diagram.HaxeLibraryIncludeFiles.LibraryInclude;
import lombok.CustomLog;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * The nodes and edges of one build file's project diagram, rebuilt from the
 * build file and the sync's graph on every refresh: the build file, dashed
 * arrows to the hxml files it includes, arrows from each file to the
 * libraries its own lines declare (labelled with a pinned version), and from
 * each library to what it requires (labelled with the required version when
 * one is pinned). For a build whose tool merges the libraries' include files
 * (lime, nme), each library gets a dashed arrow to its include file, which
 * lists the defines the build takes from it and points on to the libraries it
 * brings in ({@link HaxeLibraryIncludeFiles}). Rooted at a library instead,
 * the model shows that library and what it requires.
 * {@link HxmlIncludeTree} and {@link HaxeLibraryHierarchy} do the walking,
 * each with its loop guard.
 * <p>
 * The graph is empty until the first sync of the session lands, so a diagram
 * built before that asks for a sync. The modification counter moves only
 * when a sync lands; an editor on screen is refreshed then, a hidden one
 * refreshes itself when activated, from the moved counter. A counter that
 * moved on every refresh would refresh without end.
 */
@CustomLog
final class HaxeLibraryDiagramDataModel extends DiagramDataModel<HaxeLibraryDiagramElement> {

  private static final String DECLARES = "DECLARES";
  private static final String REQUIRES = "REQUIRES";
  private static final DiagramRelationshipInfo INCLUDES = relationship("INCLUDES", DiagramLineType.DASHED, null);

  /** The version an arrow carries, in the diagram's default color. */
  private record VersionLabel(@NotNull String text) implements DiagramRelationshipInfo.Label {
    @Override
    public @NotNull String getText() {
      return text;
    }

    @Override
    public @Nullable java.awt.Color getColor() {
      return null;
    }
  }

  /** What one refresh reads under the read lock: the root's include tree (null for a library root), the library walk and the libraries' include files. */
  private record Sources(@Nullable HxmlIncludeTree tree, @NotNull HaxeLibraryHierarchy hierarchy, @NotNull List<LibraryInclude> includes) {
  }

  private final @Nullable HaxeLibraryDiagramElement root;
  private final HaxeLibraryGraph graph;
  private final AtomicBoolean syncRequested = new AtomicBoolean();
  /** The sources a background refresh read for the graph to apply next; null when none is pending. */
  private final AtomicReference<Sources> prefetchedSources = new AtomicReference<>();
  private final Map<String, HaxeLibraryDiagramNode> nodes = new LinkedHashMap<>();
  private final List<DiagramEdge<HaxeLibraryDiagramElement>> edges = new ArrayList<>();
  /** By node key: the defines each file's box lists, read with the sources so drawing a box reads no file. */
  private final Map<String, List<Define>> fileDefines = new LinkedHashMap<>();

  HaxeLibraryDiagramDataModel(@NotNull Project project,
                              @NotNull DiagramProvider<HaxeLibraryDiagramElement> provider,
                              @Nullable HaxeLibraryDiagramElement root) {
    super(project, provider);
    this.root = root;
    this.graph = HaxeLibraryGraph.getInstance(project);
    project.getMessageBus().connect(this).subscribe(HaxeBuildEventListener.TOPIC, new HaxeBuildEventListener() {
      @Override
      public void librariesSynced() {
        incModificationCount();
        refreshDiagramIfShownLater();
      }
    });
    refreshDataModel();
  }

  /**
   * Refreshes the diagram the way its Refresh button does, once the editor
   * is on screen: data reload, node presentations, and a layout that keeps
   * the placed nodes where they are. A hidden editor refreshes itself when
   * activated. The sync announces on a background thread.
   */
  private void refreshDiagramIfShownLater() {
    DiagramBuilder builder = getBuilder();
    if (builder == null) return;
    Runnable refresh = () -> {
      if (builder.getView().getJComponent().isShowing()) DiagramDataModel.refreshDataModelInSmartMode(builder);
    };
    ApplicationManager.getApplication().invokeLater(refresh, ModalityState.nonModal(), getProject().getDisposed());
  }

  /** A diagram built before the first sync of the session asks for one, once; the sync's end refreshes it. */
  private void requestSyncIfNeverSynced() {
    if (graph.hasSynced() || !syncRequested.compareAndSet(false, true)) return;
    Project project = getProject();
    ApplicationManager.getApplication().invokeLater(() -> HaxeLibrarySync.sync(project, null), ModalityState.nonModal(), project.getDisposed());
  }

  @Override
  public @NotNull ModificationTracker getModificationTracker() {
    return myModificationTracker;
  }

  @Override
  public @NotNull Collection<? extends DiagramNode<HaxeLibraryDiagramElement>> getNodes() {
    return nodes.values();
  }

  @Override
  public @NotNull Collection<? extends DiagramEdge<HaxeLibraryDiagramElement>> getEdges() {
    return edges;
  }

  @Override
  public @NotNull String getNodeName(@NotNull DiagramNode<HaxeLibraryDiagramElement> node) {
    return node.getIdentifyingElement().key();
  }

  /** Nodes come from the walk alone; an element dropped onto the diagram is answered with its node when it is part of it. */
  @Override
  public @Nullable DiagramNode<HaxeLibraryDiagramElement> addElement(@Nullable HaxeLibraryDiagramElement element) {
    return element == null ? null : nodes.get(nodeKey(element));
  }

  /**
   * The defines a file's box lists - a build file's own, or the ones the build
   * takes from a library's file - or null for a library's box.
   */
  @Nullable
  List<Define> fileDefines(@NotNull HaxeLibraryDiagramElement element) {
    return fileDefines.get(nodeKey(element));
  }

  /**
   * The graph applies a refresh on the EDT, where the file reads behind the
   * sources are prohibited; this background half of the refresh reads them
   * first and {@link #refreshDataModel} picks them up.
   */
  @Override
  public @NotNull CompletableFuture<Void> refreshDataModelAsync(@NotNull ProgressIndicator indicator) {
    if (root != null) prefetchedSources.set(HaxeReadActions.compute(this::sources));
    return super.refreshDataModelAsync(indicator);
  }

  @Override
  public void refreshDataModel() {
    nodes.clear();
    edges.clear();
    fileDefines.clear();
    if (root == null) return;
    Sources sources = currentSources();
    HaxeLibraryDiagramNode rootNode = addNode(root);
    for (String library : sources.hierarchy().nodes()) {
      HaxeLibraryDiagramElement element = HaxeLibraryDiagramElement.library(library);
      if (!nodeKey(element).equals(nodeKey(root))) {
        nodes.putIfAbsent(nodeKey(element), new HaxeLibraryDiagramNode(getProvider(), graph, element));
      }
    }
    if (sources.tree() != null) {
      addFileEdges(sources.tree(), rootNode);
    }
    for (HaxeLibraryHierarchy.Edge requirement : sources.hierarchy().edges()) {
      HaxeLibraryDiagramNode from = nodes.get(nodeKey(HaxeLibraryDiagramElement.library(requirement.from())));
      if (from != null) addLibraryEdge(from, requirement.to(), REQUIRES, requirement.requiredVersion());
    }
    addLibraryIncludes(sources.includes());
    if (log.isDebugEnabled()) {
      log.debug("project diagram " + root.key() + ": " + nodes.size() + " nodes, " + edges.size() + " edges, synced=" + graph.hasSynced());
    }
    requestSyncIfNeverSynced();
  }

  /** The sources a background refresh read, else read now: the first build runs inside the editor's write action, where file reads are allowed. */
  @NotNull
  private Sources currentSources() {
    Sources prefetched = prefetchedSources.getAndSet(null);
    return prefetched != null ? prefetched : HaxeReadActions.compute(this::sources);
  }

  @NotNull
  private Sources sources() {
    if (!root.isBuildFile()) {
      return new Sources(null, HaxeLibraryHierarchy.of(graph, List.of(new HaxeLibDependency(root.key(), null))), List.of());
    }
    HxmlIncludeTree tree = HaxeLibraryDiagramSources.treeOf(getProject(), root.key());
    List<HaxeLibDependency> declared = tree == null ? List.of() : tree.declaredLibraries();
    HaxeLibraryHierarchy hierarchy = HaxeLibraryHierarchy.of(graph, declared);
    return new Sources(tree, hierarchy, libraryIncludes(hierarchy));
  }

  /** The files the build's libraries add to it: extraParams.hxml always, include files when its tool merges them. Read action. */
  @NotNull
  private List<LibraryInclude> libraryIncludes(@NotNull HaxeLibraryHierarchy hierarchy) {
    HaxeBuildFileInfo mergingBuild = HaxeLibraryDiagramSources.mergingBuildInfo(getProject(), root.key());
    return HaxeLibraryIncludeFiles.walk(hierarchy.nodes(), library -> HaxeLibraryDiagramSources.libraryRoot(graph, library), mergingBuild);
  }

  /** A dashed arrow from each library to each file it adds to the build, and arrows on to the libraries that file brings in. */
  private void addLibraryIncludes(@NotNull List<LibraryInclude> includes) {
    for (LibraryInclude include : includes) {
      HaxeLibraryDiagramNode libraryNode = nodes.get(nodeKey(HaxeLibraryDiagramElement.library(include.library())));
      if (libraryNode == null) continue;
      HaxeLibraryDiagramElement includeElement = HaxeLibraryDiagramElement.buildFile(include.file().getPath());
      HaxeLibraryDiagramNode includeNode = addNode(includeElement);
      fileDefines.put(nodeKey(includeElement), definesOf(include.defines()));
      edges.add(new HaxeLibraryDiagramEdge(libraryNode, includeNode, INCLUDES));
      for (HaxeLibDependency declared : include.declares()) {
        addLibraryEdge(includeNode, declared.name(), DECLARES, declared.version());
      }
    }
  }

  /** Arrows from a file to the libraries its own lines declare, and a node plus a dashed arrow for every file it includes. */
  private void addFileEdges(@NotNull HxmlIncludeTree tree, @NotNull HaxeLibraryDiagramNode fileNode) {
    fileDefines.put(nodeKey(fileNode.getIdentifyingElement()), definesOf(tree.own().defines()));
    for (HaxeLibDependency declared : tree.own().libraries()) {
      addLibraryEdge(fileNode, declared.name(), DECLARES, declared.version());
    }
    for (HxmlIncludeTree include : tree.includes()) {
      HaxeLibraryDiagramNode includeNode = addNode(HaxeLibraryDiagramElement.buildFile(include.file().getPath()));
      edges.add(new HaxeLibraryDiagramEdge(fileNode, includeNode, INCLUDES));
      addFileEdges(include, includeNode);
    }
  }

  @NotNull
  private HaxeLibraryDiagramNode addNode(@NotNull HaxeLibraryDiagramElement element) {
    HaxeLibraryDiagramNode node = new HaxeLibraryDiagramNode(getProvider(), graph, element);
    nodes.put(nodeKey(element), node);
    return node;
  }

  /** An arrow to the library of that name, which gets a node when the walk did not reach it; a pinned version labels the arrow. */
  private void addLibraryEdge(@NotNull HaxeLibraryDiagramNode from,
                              @NotNull String toLibrary,
                              @NotNull String relationshipName,
                              @Nullable String pinnedVersion) {
    HaxeLibraryDiagramElement element = HaxeLibraryDiagramElement.library(toLibrary);
    HaxeLibraryDiagramNode to = nodes.get(nodeKey(element));
    if (to == null) to = addNode(element);
    edges.add(new HaxeLibraryDiagramEdge(from, to, relationship(relationshipName, DiagramLineType.SOLID, pinnedVersion)));
  }

  /** An arrow with a delta head, labelled above its middle when a version is pinned; absent labels stay null, never empty. */
  @NotNull
  private static DiagramRelationshipInfo relationship(@NotNull String name, @NotNull DiagramLineType lineType, @Nullable String label) {
    DiagramRelationshipInfo.Label center = label == null ? null : new VersionLabel(label);
    return new DiagramRelationshipInfoAdapter(name, lineType, 1, DiagramRelationshipInfo.NONE, DiagramRelationshipInfo.DELTA,
                                              center, null, null, null, null, null);
  }

  @NotNull
  private static List<Define> definesOf(@NotNull List<HaxeDefine> defines) {
    return defines.stream()
      .map(define -> new Define(define.name(), define.value()))
      .toList();
  }

  @NotNull
  private static String nodeKey(@NotNull HaxeLibraryDiagramElement element) {
    return element.kind() + ":" + element.key().toLowerCase(Locale.ROOT);
  }

  @Override
  public void dispose() {
  }
}
