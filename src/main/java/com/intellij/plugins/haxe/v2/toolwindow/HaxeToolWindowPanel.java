package com.intellij.plugins.haxe.v2.toolwindow;

import com.intellij.execution.executors.DefaultRunExecutor;
import com.intellij.ide.CommonActionsManager;
import com.intellij.ide.DefaultTreeExpander;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.ActionToolbar;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.CommonShortcuts;
import com.intellij.openapi.actionSystem.DataSink;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.ModuleListener;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.SimpleToolWindowPanel;
import com.intellij.plugins.haxe.v2.buildtools.HaxeBuildConfigListener;
import com.intellij.plugins.haxe.v2.buildtools.server.HaxeCompilationServerListener;
import com.intellij.plugins.haxe.v2.buildtools.info.HaxeDefineContextService;
import com.intellij.plugins.haxe.v2.buildtools.server.HaxeCompilationServerManager;
import com.intellij.plugins.haxe.v2.buildtools.settings.*;
import com.intellij.plugins.haxe.v2.toolwindow.actions.*;
import com.intellij.plugins.haxe.v2.toolwindow.HaxeToolWindowModelBuilder.ContainerEntry;
import com.intellij.plugins.haxe.v2.toolwindow.tree.*;
import com.intellij.plugins.haxe.v2.toolwindow.tree.HaxeToolWindowNodes.*;
import com.intellij.plugins.haxe.v2.testing.run.HaxeTestRunConfigurations;
import com.intellij.pom.Navigatable;
import com.intellij.ui.PopupHandler;
import com.intellij.ui.ScrollPaneFactory;
import com.intellij.ui.SimpleColoredComponent;
import com.intellij.ui.TreeSpeedSearch;
import com.intellij.ui.awt.RelativePoint;
import com.intellij.ui.treeStructure.Tree;
import com.intellij.util.concurrency.AppExecutorUtil;
import com.intellij.util.ui.tree.TreeUtil;
import lombok.CustomLog;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeExpansionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.awt.Component;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Content panel of the Haxe tool window: a toolbar (sync, purge caches, execute
 * command, settings) above a tree of modules, their build files and each file's
 * target, defines and library dependencies.
 */
@CustomLog
public final class HaxeToolWindowPanel extends SimpleToolWindowPanel implements Disposable {

  public static final String TOOLBAR_PLACE = "HaxeToolWindowToolbar";
  public static final String TREE_POPUP_PLACE = "HaxeToolWindowTreePopup";

  private final Project project;
  private final DefaultTreeModel treeModel = new DefaultTreeModel(new DefaultMutableTreeNode());
  private final Tree tree = new Tree(treeModel);
  private final HaxeToolWindowModelBuilder modelBuilder;
  private final HaxeToolWindowTreeBuilder treeBuilder;
  private boolean initialExpansionDone;
  // last scan's tests build files per container (EDT only), for the container-row unit-test action
  private final Map<String, List<String>> testsPathsByContainer = new HashMap<>();
  private @Nullable String projectRootContainerId;

  public HaxeToolWindowPanel(@NotNull Project project) {
    super(true, true);
    this.project = project;
    this.modelBuilder = new HaxeToolWindowModelBuilder(project, this::refreshTree);
    this.treeBuilder = new HaxeToolWindowTreeBuilder(project);

    tree.setRootVisible(false);
    tree.setShowsRootHandles(true);
    tree.setCellRenderer(new HaxeToolWindowTreeRenderer());
    // per-row tooltips come from the renderer; a plain JTree never shows them unless registered
    ToolTipManager.sharedInstance().registerComponent(tree);
    tree.addMouseListener(new TreeClickHandler());
    PopupHandler.installPopupMenu(tree, createTreePopupGroup(), TREE_POPUP_PLACE);
    TreeSpeedSearch.installOn(tree, true, HaxeToolWindowPanel::speedSearchText);
    new TreeEnterAction().registerCustomShortcutSet(CommonShortcuts.ENTER, tree, this);
    new TreeDeleteAction().registerCustomShortcutSet(CommonShortcuts.getDelete(), tree, this);
    tree.addTreeExpansionListener(new ExpansionPersister());

    setToolbar(createToolbar());
    setContent(ScrollPaneFactory.createScrollPane(tree));

    project.getMessageBus().connect(this).subscribe(ModuleListener.TOPIC, new ModuleListener() {
      @Override
      public void modulesAdded(@NotNull Project project, @NotNull List<? extends Module> modules) {
        refreshTree();
      }

      @Override
      public void moduleRemoved(@NotNull Project project, @NotNull Module module) {
        refreshTree();
      }
    });
    project.getMessageBus().connect(this)
      .subscribe(HaxeCompilationServerListener.TOPIC, (HaxeCompilationServerListener)this::refreshTree);
    project.getMessageBus().connect(this)
      .subscribe(HaxeBuildConfigListener.TOPIC, (HaxeBuildConfigListener)this::refreshTree);
    // store mutations can originate outside this panel (the define quickfix
    // edits environment overrides) - the tree must follow those too
    project.getMessageBus().connect(this)
      .subscribe(HaxeBuildSettingsListener.TOPIC, (HaxeBuildSettingsListener)this::refreshTree);

    refreshTree();
  }

  @NotNull
  private JComponent createToolbar() {
    DefaultActionGroup group = new DefaultActionGroup();
    group.add(new HaxeSyncProjectAction(this::refreshTree));
    group.add(new HaxePurgeCachesAction(this::refreshTree));
    group.addSeparator();
    group.add(new HaxeAddModuleAction());
    group.add(new HaxeRemoveNodeAction(this));
    group.addSeparator();
    group.add(new HaxeExecuteCommandAction());
    group.addSeparator();
    group.add(new HaxeSettingsActionGroup());

    group.addSeparator();
    group.add(new HaxeHideEmptyModulesAction(this::refreshTree));
    CommonActionsManager commonActions = CommonActionsManager.getInstance();
    DefaultTreeExpander treeExpander = new DefaultTreeExpander(tree);
    group.add(commonActions.createExpandAllAction(treeExpander, tree));
    group.add(commonActions.createCollapseAllAction(treeExpander, tree));

    ActionToolbar toolbar = ActionManager.getInstance().createActionToolbar(TOOLBAR_PLACE, group, true);
    toolbar.setTargetComponent(this);
    return toolbar.getComponent();
  }

  @NotNull
  private DefaultActionGroup createTreePopupGroup() {
    DefaultActionGroup group = new DefaultActionGroup();
    group.add(new HaxeRunActionNodeAction(this));
    group.add(new HaxeRunProgramAction(this, false));
    group.add(new HaxeRunProgramAction(this, true));
    group.add(new HaxeProfileProgramAction(this));
    group.add(new HaxeRunUnitTestsAction(this));
    group.add(new HaxeDebugUnitTestsAction(this));
    group.add(new HaxeSetAsCompileCommandAction(this));
    group.add(new HaxeAddCustomActionAction(this));
    group.add(new HaxeEditCustomActionAction(this));
    group.add(new HaxeRemoveCustomActionAction(this));
    group.addSeparator();
    group.add(new HaxeAddToolAction(this));
    group.add(new HaxeEditToolAction(this));
    group.add(new HaxeRemoveToolAction(this));
    group.add(new HaxeSetActiveBuildFileAction(this));
    group.add(new HaxeMarkTestsBuildFileAction(this));
    group.add(new HaxeSetWorkDirectoryAction(this));
    group.add(new HaxeReloadBuildFileAction(this));
    group.add(new HaxeAddBuildFileAction(this));
    group.add(new HaxeRemoveBuildFileAction(this));
    group.add(new HaxeSelectTargetAction(this));
    group.add(new HaxeSelectSectionAction(this));
    group.add(new HaxeInstallLibraryAction(this));
    group.add(new HaxeInstallAllMissingLibrariesAction(this));
    group.add(new HaxeShowInHaxelibExplorerAction(this));
    group.add(new HaxeConfigureEnvironmentAction(this));
    group.add(new HaxeConfigureCompileCommandAction(this));
    group.add(new HaxeRunCompileCommandAction(this));
    group.add(new HaxeSelectEnvironmentSdkAction(this));
    group.add(new HaxeAddDefineAction(this));
    group.add(new HaxeEditDefineAction(this));
    group.add(new HaxeRemoveDefineAction(this));
    return group;
  }

  /**
   * Rebuilds the tree: scan + parse under a non-blocking read action, then the
   * haxelib installed-library lookup (external process) outside any read action,
   * then the Swing model update on the EDT.
   */
  public void refreshTree() {
    // every configuration change funnels through here - keep the parse-context
    // defines in sync (cheap no-op when nothing changed)
    HaxeDefineContextService.getInstance(project).refreshAsync();
    ReadAction.nonBlocking(modelBuilder::build)
      .inSmartMode(project)
      .expireWith(this)
      .submit(AppExecutorUtil.getAppExecutorService())
      .onSuccess(scan -> AppExecutorUtil.getAppExecutorService().execute(() -> updateTree(scan)));
  }

  /** Never lets a haxelib failure prevent the tree from updating - install state just becomes unknown. */
  private void updateTree(@NotNull List<ContainerEntry> scan) {
    Map<String, HaxeToolWindowModelBuilder.InstalledLibrary> installed = null;
    try {
      installed = HaxeToolWindowModelBuilder.fetchInstalledLibraryVersions(project);
    }
    catch (ProcessCanceledException e) {
      throw e;
    }
    catch (Exception e) {
      log.warn("haxelib lookup failed; library install state unknown", e);
    }
    DefaultMutableTreeNode root = treeBuilder.buildRoot(visibleContainers(scan), installed);
    ApplicationManager.getApplication().invokeLater(() -> {
      if (project.isDisposed()) return;
      rememberTestsPaths(scan);
      applyTreeUpdate(root);
    });
  }

  /** The hide-empty toggle removes module rows with no build files and no user configuration. */
  @NotNull
  private List<ContainerEntry> visibleContainers(@NotNull List<ContainerEntry> scan) {
    if (!HaxeToolWindowUiState.getInstance(project).isHideEmptyModules()) return scan;
    return scan.stream()
      .filter(container -> !container.emptyModule())
      .toList();
  }

  /** Keeps the scan's per-container tests build files, so container-row actions resolve them without re-scanning. */
  private void rememberTestsPaths(@NotNull List<ContainerEntry> scan) {
    testsPathsByContainer.clear();
    // reset alongside the map: a scan without a project-root container must
    // not leave the previous id answering for a container that is gone
    projectRootContainerId = null;
    for (ContainerEntry container : scan) {
      if (!container.testsPaths().isEmpty()) {
        testsPathsByContainer.put(container.id(), container.testsPaths());
      }
      if (container.projectRoot()) {
        projectRootContainerId = container.id();
      }
    }
  }

  /** The container's FIRST tests build file (marked or convention-suggested) from the last scan, or null - what a container-row run targets. */
  @Nullable
  private String testsPathFor(@NotNull String containerId) {
    List<String> paths = testsPathsByContainer.get(containerId);
    return paths == null || paths.isEmpty() ? null : paths.get(0);
  }

  /** The selection's tests build file: the row's own file, or the container's marked/suggested one from the last scan. */
  @Nullable
  public String resolveTestsPath(@Nullable Object selection) {
    return switch (selection) {
      case TestRunNode node -> node.buildFilePath();
      case ModuleNode module -> testsPathFor(module.name());
      case ProjectNode ignored -> projectRootContainerId == null ? null : testsPathFor(projectRootContainerId);
      case null, default -> null;
    };
  }

  /** User object of the tree's selected node, or null. */
  @Nullable
  public Object getSelectedUserObject() {
    TreePath path = tree.getSelectionPath();
    if (path == null) return null;
    return path.getLastPathComponent() instanceof DefaultMutableTreeNode node ? node.getUserObject() : null;
  }

  /**
   * The missing library rows of the selection's Libraries group — the group row
   * itself, or any MISSING library row in it (siblings included); empty otherwise.
   */
  @NotNull
  public List<LibraryNode> getSelectedGroupMissingLibraries() {
    TreePath path = tree.getSelectionPath();
    if (path == null || !(path.getLastPathComponent() instanceof DefaultMutableTreeNode node)) return List.of();

    DefaultMutableTreeNode groupNode = null;
    if (node.getUserObject() instanceof GroupNode group && group.kind() == GroupKind.LIBRARIES) {
      groupNode = node;
    }
    else if (node.getUserObject() instanceof LibraryNode library && !library.installed()
             && node.getParent() instanceof DefaultMutableTreeNode parent) {
      groupNode = parent;
    }
    if (groupNode == null) return List.of();

    List<LibraryNode> missing = new ArrayList<>();
    for (int i = 0; i < groupNode.getChildCount(); i++) {
      if (groupNode.getChildAt(i) instanceof DefaultMutableTreeNode child
          && child.getUserObject() instanceof LibraryNode library
          && !library.installed()) {
        missing.add(library);
      }
    }
    return missing;
  }

  /** The text speed search matches against - the row's primary label as rendered. */
  @NotNull
  private static String speedSearchText(@NotNull TreePath path) {
    if (!(path.getLastPathComponent() instanceof DefaultMutableTreeNode treeNode)) return "";
    return treeNode.getUserObject() instanceof HaxeToolWindowNode node ? node.speedSearchText() : "";
  }

  /** Supplies the selection's jump-to-source target so F4 / Jump to Source works on tree rows. */
  @Override
  public void uiDataSnapshot(@NotNull DataSink sink) {
    super.uiDataSnapshot(sink);
    Navigatable navigatable = HaxeToolWindowNavigation.forSelection(project, getSelectedUserObject());
    if (navigatable != null) {
      sink.set(CommonDataKeys.NAVIGATABLE, navigatable);
    }
  }

  /** The build file row enclosing the selection (an action or child row), or null. */
  @Nullable
  public BuildFileRow getSelectedBuildFileRowAncestor() {
    TreePath path = tree.getSelectionPath();
    while (path != null) {
      if (path.getLastPathComponent() instanceof DefaultMutableTreeNode node
          && node.getUserObject() instanceof BuildFileRow row) {
        return row;
      }
      path = path.getParentPath();
    }
    return null;
  }

  /** The Environment row of the selection, walking up from any of its child rows. */
  @Nullable
  public EnvironmentNode getSelectedEnvironmentNode() {
    TreePath path = tree.getSelectionPath();
    while (path != null) {
      if (path.getLastPathComponent() instanceof DefaultMutableTreeNode node
          && node.getUserObject() instanceof EnvironmentNode environmentNode) {
        return environmentNode;
      }
      path = path.getParentPath();
    }
    return null;
  }

  /** Anchor point for popups on the selected row, falling back to the tree's corner. */
  @NotNull
  public RelativePoint getSelectionPopupPoint() {
    TreePath path = tree.getSelectionPath();
    var bounds = path != null ? tree.getPathBounds(path) : null;
    if (bounds == null) return new RelativePoint(tree, new Point(0, 0));
    return new RelativePoint(tree, new Point(bounds.x, bounds.y + bounds.height));
  }

  /**
   * Replaces the model while preserving the user's expansion and selection state.
   * Rows are matched by stable identity (file path, module name), so volatile parts
   * of a row - counts, the active marker, versions - do not reset the view. The
   * first fill restores the expansion persisted in the workspace file, so the view
   * survives an IDE restart the way the Maven/Gradle tool windows do.
   */
  private void applyTreeUpdate(@NotNull DefaultMutableTreeNode newRoot) {
    Set<String> expandedKeys = collectExpandedKeys();
    String selectedKey = chainKey(tree.getSelectionPath());

    treeModel.setRoot(newRoot);
    if (!initialExpansionDone) {
      initialExpansionDone = true;
      Set<String> savedKeys = HaxeToolWindowUiState.getInstance(project).getExpandedKeys();
      if (savedKeys.isEmpty()) {
        // nothing saved yet: project node, modules, Environment/Build groups and build files visible
        TreeUtil.expand(tree, 4);
      }
      else {
        expandAndSelect(newRoot, savedKeys, null);
      }
      return;
    }
    expandAndSelect(newRoot, expandedKeys, selectedKey);
  }

  private void expandAndSelect(@NotNull DefaultMutableTreeNode root,
                               @NotNull Set<String> expandedKeys,
                               @Nullable String selectedKey) {
    forEachNode(root, node -> {
      TreePath path = new TreePath(node.getPath());
      String key = chainKey(path);
      if (expandedKeys.contains(key)) {
        tree.expandPath(path);
      }
      if (key != null && key.equals(selectedKey)) {
        tree.setSelectionPath(path);
      }
    });
  }

  /** Mirrors every expand/collapse into the workspace-file state; the last write holds the full current set. */
  private final class ExpansionPersister implements TreeExpansionListener {
    @Override
    public void treeExpanded(TreeExpansionEvent event) {
      persist();
    }

    @Override
    public void treeCollapsed(TreeExpansionEvent event) {
      persist();
    }

    private void persist() {
      HaxeToolWindowUiState.getInstance(project).setExpandedKeys(collectExpandedKeys());
    }
  }

  @NotNull
  private Set<String> collectExpandedKeys() {
    Set<String> keys = new HashSet<>();
    if (treeModel.getRoot() == null) return keys;
    Enumeration<TreePath> expanded = tree.getExpandedDescendants(new TreePath(treeModel.getRoot()));
    while (expanded != null && expanded.hasMoreElements()) {
      String key = chainKey(expanded.nextElement());
      if (key != null) {
        keys.add(key);
      }
    }
    return keys;
  }

  private static void forEachNode(@NotNull DefaultMutableTreeNode node, @NotNull Consumer<DefaultMutableTreeNode> visitor) {
    visitor.accept(node);
    for (int i = 0; i < node.getChildCount(); i++) {
      forEachNode((DefaultMutableTreeNode)node.getChildAt(i), visitor);
    }
  }

  /** Stable identity of a row's ancestry, independent of volatile row content. */
  @Nullable
  private static String chainKey(@Nullable TreePath path) {
    if (path == null) return null;
    StringBuilder key = new StringBuilder();
    for (Object component : path.getPath()) {
      if (!(component instanceof DefaultMutableTreeNode node) || node.getUserObject() == null) continue;
      key.append('|').append(nodeKey(node.getUserObject()));
    }
    return key.toString();
  }

  @NotNull
  private static String nodeKey(@NotNull Object userObject) {
    return userObject instanceof HaxeToolWindowNode node ? node.expansionKey() : String.valueOf(userObject);
  }

  private final class TreeClickHandler extends MouseAdapter {
    @Override
    public void mousePressed(MouseEvent e) {
      selectRowForPopup(e);
    }

    @Override
    public void mouseReleased(MouseEvent e) {
      selectRowForPopup(e);
    }

    /**
     * A right-click acts on the row under the cursor: select it BEFORE the
     * popup handler evaluates the menu actions (a JTree does not select on
     * right-click by itself, leaving every selection-driven action hidden).
     * Both pressed and released matter - the popup trigger fires on press or
     * release depending on the platform.
     */
    private void selectRowForPopup(MouseEvent e) {
      if (!e.isPopupTrigger()) return;
      TreePath path = tree.getPathForLocation(e.getX(), e.getY());
      if (path != null && !tree.isPathSelected(path)) {
        tree.setSelectionPath(path);
      }
    }

    @Override
    public void mouseClicked(MouseEvent e) {
      if (!SwingUtilities.isLeftMouseButton(e)) return;
      TreePath path = tree.getPathForLocation(e.getX(), e.getY());
      if (path == null || !(path.getLastPathComponent() instanceof DefaultMutableTreeNode node)) return;

      int clicks = e.getClickCount();
      Object userObject = node.getUserObject();
      RelativePoint point = new RelativePoint(e.getComponent(), e.getPoint());

      if (clicks == 1) {
        interactWithNode(userObject, point, fragmentTagAt(e, path));
      } else if (clicks == 2) {
        activateNode(userObject);
      }
    }

    /** The renderer fragment tag under the click — tags mark clickable fragments (the server failure link). */
    @Nullable
    private Object fragmentTagAt(@NotNull MouseEvent e, @NotNull TreePath path) {
      Rectangle bounds = tree.getPathBounds(path);
      if (bounds == null || !(path.getLastPathComponent() instanceof DefaultMutableTreeNode node)) return null;
      Component renderer = tree.getCellRenderer()
        .getTreeCellRendererComponent(tree, node, tree.isPathSelected(path), tree.isExpanded(path),
                                      node.isLeaf(), tree.getRowForPath(path), false);
      if (!(renderer instanceof SimpleColoredComponent colored)) return null;
      renderer.setSize(bounds.width, bounds.height);
      return colored.getFragmentTagAt(e.getX() - bounds.x);
    }
  }

  /** Opens the server console at the tab serving this container's SDK — the status view holds the failure detail. */
  private void openServerConsole(@NotNull String containerId) {
    HaxeServerConsoleWindowFactory.open(project, HaxeCompilationServerManager.serverIdFor(project, containerId));
  }

  /** The row's ACTIVATION — double-click and Enter share it. False when the row has none. */
  private boolean activateNode(@Nullable Object userObject) {
    switch (userObject) {
      case BuildFileRow row when row.buildFile().file().isValid() ->
        new OpenFileDescriptor(project, row.buildFile().file()).navigate(true);
      case ActionNode actionNode -> HaxeToolWindowLaunches.runAction(project, actionNode);
      case ToolNode toolNode -> HaxeToolWindowLaunches.runTool(project, toolNode);
      case ProgramNode programNode -> HaxeToolWindowLaunches.runProgram(project, programNode, DefaultRunExecutor.getRunExecutorInstance());
      case TestRunNode testRunNode -> HaxeTestRunConfigurations.run(project, testRunNode.buildFilePath());
      case null, default -> {
        return false;
      }
    }
    return true;
  }

  /** The row's chooser/toggle INTERACTION — single-click and Enter share it. False when the row has none. */
  private boolean interactWithNode(@Nullable Object userObject, @NotNull RelativePoint point, @Nullable Object fragmentTag) {
    switch (userObject) {
      case TargetNode targetNode when targetNode.selectable() -> HaxeToolWindowEditors.showTargetPopup(project, targetNode, point);
      case SectionNode sectionNode -> HaxeToolWindowEditors.showSectionPopup(project, sectionNode, point);
      case EnvSdkNode sdkNode -> HaxeToolWindowEditors.showEnvironmentSdkPopup(project, sdkNode, point);
      case EnvLanguageLevelNode levelNode -> HaxeToolWindowEditors.showLanguageLevelPopup(project, levelNode, point);
      case EnvCustomTargetNode customTargetNode -> HaxeToolWindowEditors.editCustomTarget(project, customTargetNode);
      case EnvCompileCommandNode compileCommand -> HaxeToolWindowEditors.configureCompileCommand(project, compileCommand);
      case CompilationServerNode serverNode -> {
        // the red failure text links to the server console's status view;
        // the rest of the row keeps the participation toggle
        if (fragmentTag instanceof HaxeToolWindowNodes.ServerFailureLink link) {
          openServerConsole(link.containerId());
        }
        else {
          HaxeToolWindowEditors.toggleCompilationServer(project, serverNode);
        }
      }
      case null, default -> {
        return false;
      }
    }
    return true;
  }

  /**
   * Enter mirrors the mouse: a row's activation (run, launch, open) first,
   * its chooser/toggle second, and rows with neither toggle their expansion.
   */
  private final class TreeEnterAction extends DumbAwareAction {
    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
      Object selected = getSelectedUserObject();
      if (activateNode(selected)) return;
      if (interactWithNode(selected, getSelectionPopupPoint(), null)) return;
      toggleSelectedExpansion();
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
      e.getPresentation().setEnabled(tree.getSelectionPath() != null);
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
      return ActionUpdateThread.EDT;
    }
  }

  /** Delete removes the selected removable row — build file, define override, module — after confirmation. */
  private final class TreeDeleteAction extends DumbAwareAction {
    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
      switch (getSelectedUserObject()) {
        case BuildFileRow row -> HaxeToolWindowEditors.confirmAndRemoveBuildFile(project, row);
        case EnvDefineNode define -> HaxeToolWindowEditors.confirmAndRemoveDefine(project, define);
        case ModuleNode module -> HaxeToolWindowEditors.confirmAndRemoveModule(project, module);
        case null, default -> { }
      }
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
      Object selected = getSelectedUserObject();
      boolean removable = selected instanceof BuildFileRow
                          || selected instanceof EnvDefineNode
                          || selected instanceof ModuleNode;
      e.getPresentation().setEnabled(removable);
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
      return ActionUpdateThread.EDT;
    }
  }

  private void toggleSelectedExpansion() {
    TreePath path = tree.getSelectionPath();
    if (path == null) return;
    if (tree.isExpanded(path)) {
      tree.collapsePath(path);
    }
    else {
      tree.expandPath(path);
    }
  }

  @Override
  public void dispose() {
    // message bus connection is tied to this Disposable; nothing else to release
  }
}
