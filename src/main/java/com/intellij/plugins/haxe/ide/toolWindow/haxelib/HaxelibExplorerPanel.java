package com.intellij.plugins.haxe.ide.toolWindow.haxelib;

import com.intellij.icons.AllIcons;
import com.intellij.ide.CommonActionsManager;
import com.intellij.ide.DefaultTreeExpander;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.ActionToolbar;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.plugins.haxe.util.HaxeReadActions;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.plugins.haxe.HaxeBundle;
import com.intellij.plugins.haxe.haxelib.*;
import com.intellij.plugins.haxe.ide.toolWindow.HaxelibConsoleWindowFactory;
import com.intellij.openapi.project.ProjectUtil;
import com.intellij.ui.content.Content;
import com.intellij.ui.ColoredTreeCellRenderer;
import com.intellij.ui.DocumentAdapter;
import com.intellij.ui.JBColor;
import com.intellij.ui.OnePixelSplitter;
import com.intellij.ui.PopupHandler;
import com.intellij.ui.SearchTextField;
import com.intellij.ui.SimpleTextAttributes;
import com.intellij.ui.TreeSpeedSearch;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.treeStructure.Tree;
import com.intellij.util.concurrency.AppExecutorUtil;
import com.intellij.util.ui.components.BorderLayoutPanel;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import javax.swing.JComponent;
import javax.swing.JTree;
import javax.swing.event.DocumentEvent;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeWillExpandListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The Haxelib tool window's permanent Explorer tab: a library TREE (each
 * library expands to its versions, installed ones marked) beside the selected
 * library's tabbed details, with an always-visible vertical filter strip on
 * the left edge. The installed picture comes from the MODULE's haxelib
 * repository (a local {@code .haxelib} when one exists), catalog and
 * per-library metadata from the session-cached {@link HaxelibCacheManager};
 * Refresh is the explicit force-update. All haxelib calls run on pooled
 * threads; a library's version children materialize on first expand.
 */
public final class HaxelibExplorerPanel extends BorderLayoutPanel implements Disposable {

  /** One library with its LOCAL state (empty versions = not installed). */
  record LibraryRow(@NotNull String name,
                    @NotNull Set<String> installedVersions,
                    @Nullable String selectedVersion) {
    boolean installed() {
      return !installedVersions.isEmpty();
    }

    boolean dev() {
      return installedVersions.contains(HaxelibSemVer.DEV);
    }

    boolean git() {
      return installedVersions.contains(HaxelibSemVer.GIT_SCM);
    }

    /** Whether a plain RELEASE version is installed (dev/git pseudo-versions aside). */
    boolean installedRelease() {
      return installedVersions.stream().anyMatch(v -> !HaxelibSemVer.isPseudoVersion(v));
    }
  }

  /** One version under a library node. */
  record VersionEntry(@NotNull String library,
                      @NotNull String version,
                      @Nullable String date,
                      @Nullable String note,
                      boolean installed,
                      boolean current) {
  }

  private final Project project;
  private final SearchTextField searchField = new SearchTextField();
  private final DefaultMutableTreeNode root = new DefaultMutableTreeNode();
  private final DefaultTreeModel treeModel = new DefaultTreeModel(root);
  private final Tree tree = new Tree(treeModel);
  private final HaxelibDetailsPane details =
    new HaxelibDetailsPane(HaxeBundle.message("haxelib.explorer.tab.overview"));
  private final HaxelibExplorerFilters filters = new HaxelibExplorerFilters(this::refilter);
  private final HaxelibDocsLoader docsLoader;

  private volatile List<LibraryRow> allRows = List.of();
  // name -> latest release from already-cached info; rebuilt on every
  // refilter, feeds the behind-latest filter and the orange suffix
  private Map<String, String> latestVersions = Map.of();
  // libraries whose haxelib info fetch is already queued or running
  private final Set<String> infoHydrationPending = ConcurrentHashMap.newKeySet();
  private final AtomicBoolean refilterQueued = new AtomicBoolean();
  private ShownSelection lastShownSelection;
  private boolean restoringTree;
  // a Show-in-Explorer reveal whose library row (or version child) has not
  // materialized yet; refilter retries it until the row exists
  @Nullable private String pendingRevealLibrary;
  @Nullable private String pendingRevealVersion;
  // separate guards: a selection click must not discard an in-flight catalog
  // load's second stage (one shared counter did exactly that)
  private final AtomicInteger loadGeneration = new AtomicInteger();
  private final AtomicInteger selectionGeneration = new AtomicInteger();
  private volatile boolean disposed;

  public HaxelibExplorerPanel(@NotNull Project project) {
    this.project = project;
    this.docsLoader = new HaxelibDocsLoader(project);

    tree.setRootVisible(false);
    tree.setShowsRootHandles(true);
    tree.setCellRenderer(new NodeRenderer());
    tree.addTreeSelectionListener(e -> {
      // rebuilds clear the selection before restoring it; reacting to the
      // transient events would restart the details render on every rebuild
      if (!restoringTree) {
        showSelection();
      }
    });
    tree.addTreeWillExpandListener(new LazyVersionLoader());
    // type-to-highlight while the tree is focused; no auto-expand - that
    // would fire a lazy info fetch per library the search passes over
    TreeSpeedSearch.installOn(tree, false, HaxelibExplorerPanel::speedSearchText);
    PopupHandler.installPopupMenu(tree, HaxelibExplorerActions.createGroup(this), "HaxelibExplorerPopup");

    searchField.addDocumentListener(new DocumentAdapter() {
      @Override
      protected void textChanged(@NotNull DocumentEvent e) {
        refilter();
      }
    });

    BorderLayoutPanel treePanel = new BorderLayoutPanel();
    treePanel.addToTop(createToolbarRow());
    treePanel.addToCenter(new JBScrollPane(tree));
    treePanel.addToLeft(createFilterStrip());

    OnePixelSplitter splitter = new OnePixelSplitter(false, 0.4f);
    splitter.setFirstComponent(treePanel);
    splitter.setSecondComponent(details.getComponent());
    addToCenter(splitter);

    reload(false);
  }

  @NotNull
  private BorderLayoutPanel createToolbarRow() {
    DefaultActionGroup actions = new DefaultActionGroup();
    actions.add(HaxelibExplorerActions.createAddLibraryAction(this));
    actions.add(new RefreshAction());
    DefaultTreeExpander treeExpander = new DefaultTreeExpander(() -> tree);
    actions.add(CommonActionsManager.getInstance().createCollapseAllAction(treeExpander, tree));
    ActionToolbar toolbar = ActionManager.getInstance()
      .createActionToolbar("HaxelibExplorer", actions, true);
    toolbar.setTargetComponent(this);

    BorderLayoutPanel row = new BorderLayoutPanel();
    row.addToCenter(searchField);
    row.addToRight(toolbar.getComponent());
    return row;
  }

  @NotNull
  private JComponent createFilterStrip() {
    ActionToolbar strip = ActionManager.getInstance()
      .createActionToolbar("HaxelibExplorerFilters", filters.createToggleGroup(), false);
    strip.setTargetComponent(this);
    return strip.getComponent();
  }

  // ------------------------------------------------------------- loading

  /** Loads installed (fast, repo-scoped) then the online catalog, updating the tree after each stage. */
  private void reload(boolean force) {
    int expected = loadGeneration.incrementAndGet();
    lastShownSelection = null;
    details.showMessage(HaxeBundle.message("haxelib.explorer.loading"));
    AppExecutorUtil.getAppExecutorService().execute(() -> {
      HaxelibCacheManager manager = cacheManager();
      if (manager == null) {
        onUi(loadGeneration, expected, () -> details.showMessage(HaxeBundle.message("haxelib.explorer.no.sdk")));
        return;
      }
      if (force) {
        manager.reload();
      }
      HaxelibInstalledIndex installed = manager.getInstalledIndex();
      onUi(loadGeneration, expected, () -> setRows(buildRows(installed, Map.of())));
      hydrateLibraryInfo(manager, installed.getInstalledLibraries());

      Map<String, Set<String>> catalog = manager.getAvailableLibraries();
      onUi(loadGeneration, expected, () -> setRows(buildRows(installed, catalog)));
    });
  }

  /**
   * Fetches missing haxelib info for installed libraries in the background:
   * the latest-version filters and the orange suffix can only answer from
   * cached info, and without this a library the user never expanded would
   * silently fail their conditions. Each answer refreshes the tree, so rows
   * appear while the sweep is still running.
   */
  private void hydrateLibraryInfo(@NotNull HaxelibCacheManager manager, @NotNull Set<String> names) {
    List<String> missing = names.stream()
      .filter(name -> manager.getCachedLibraryInfo(name) == null && infoHydrationPending.add(name))
      .toList();
    if (missing.isEmpty()) return;
    AppExecutorUtil.getAppExecutorService().execute(() -> {
      for (String name : missing) {
        if (disposed) return;
        manager.getLibraryInfo(name);
        infoHydrationPending.remove(name);
        onInfoHydrated();
      }
    });
  }

  /**
   * A hydration answer changes which rows SHOW only under the
   * latest-version narrowing filters; plain browsing just refreshes the
   * suffix colouring with a repaint, leaving the user's tree expansion and
   * scroll position untouched.
   */
  private void onInfoHydrated() {
    boolean filtersOnInfo = filters.isActive(HaxelibExplorerFilters.Filter.ONLY_UPDATES)
                            || filters.isActive(HaxelibExplorerFilters.Filter.BEHIND_LATEST);
    if (filtersOnInfo) {
      scheduleRefilter();
      return;
    }
    ApplicationManager.getApplication().invokeLater(() -> {
      if (!disposed) {
        latestVersions = collectLatestVersions(cacheManager());
        tree.repaint();
      }
    });
  }

  /** Coalesces background-triggered refilters into one queued EDT pass. */
  private void scheduleRefilter() {
    if (!refilterQueued.compareAndSet(false, true)) return;
    ApplicationManager.getApplication().invokeLater(() -> {
      refilterQueued.set(false);
      if (!disposed) {
        refilter();
      }
    });
  }

  /**
   * Re-reads the installed picture (cheap) and rebuilds — the after-mutation
   * refresh. The mutated library's cached {@code haxelib info} is dropped
   * too: an install/remove changes the release picture its overview shows.
   */
  void reloadAfterMutation(@NotNull String libraryName) {
    HaxelibCacheManager manager = cacheManager();
    if (manager != null) {
      manager.refreshInstalled();
      manager.refreshLibraryInfo(libraryName);
    }
    reload(false);
  }

  @NotNull
  private static List<LibraryRow> buildRows(@NotNull HaxelibInstalledIndex installed,
                                            @NotNull Map<String, Set<String>> catalog) {
    List<LibraryRow> rows = new ArrayList<>();
    for (String name : installed.getInstalledLibraries()) {
      rows.add(new LibraryRow(name, installed.getInstalledVersions(name), installed.getSelectedVersion(name)));
    }
    for (String name : catalog.keySet()) {
      if (!installed.getInstalledLibraries().contains(name)) {
        rows.add(new LibraryRow(name, Set.of(), null));
      }
    }
    // installed first, alphabetical within each group
    Comparator<LibraryRow> installedFirstByName = Comparator.comparing((LibraryRow row) -> !row.installed())
      .thenComparing(LibraryRow::name, String.CASE_INSENSITIVE_ORDER);
    rows.sort(installedFirstByName);
    return rows;
  }

  private void setRows(@NotNull List<LibraryRow> rows) {
    allRows = rows;
    refilter();
  }

  private void refilter() {
    Set<String> expandedLibraries = collectExpandedLibraries();
    String selectedKey = selectionKey(tree.getSelectionPath());

    String query = searchField.getText().trim().toLowerCase(Locale.ROOT);
    // resolved once - the row loop must not repeat the module/SDK lookup
    HaxelibCacheManager manager = cacheManager();
    latestVersions = collectLatestVersions(manager);
    restoringTree = true;
    try {
      root.removeAllChildren();
      for (LibraryRow row : allRows) {
        if (accepted(row, query, manager)) {
          DefaultMutableTreeNode libraryNode = new DefaultMutableTreeNode(row);
          // a placeholder gives the node its expand handle; real children
          // materialize on expand (the release list needs one info fetch)
          libraryNode.add(new DefaultMutableTreeNode(LOADING_PLACEHOLDER));
          root.add(libraryNode);
        }
      }
      treeModel.reload();
      restoreTreeState(expandedLibraries, selectedKey);
      applyPendingReveal();
    }
    finally {
      restoringTree = false;
    }
    showSelection();
  }

  // -------------------------------------------------- reveal from outside

  /** Opens the Haxelib tool window and reveals the library (and version entry) in its Explorer tab. */
  public static void reveal(@NotNull Project project, @NotNull String libraryName, @Nullable String version) {
    ToolWindow toolWindow = ToolWindowManager.getInstance(project).getToolWindow(HaxelibConsoleWindowFactory.TOOL_WINDOW_ID);
    if (toolWindow == null) return;
    toolWindow.activate(() -> {
      for (Content content : toolWindow.getContentManager().getContents()) {
        if (content.getComponent() instanceof HaxelibExplorerPanel explorer) {
          explorer.select(libraryName, version);
          return;
        }
      }
    });
  }

  /** Selects the library once a rebuild shows it — for the add flow, whose row does not exist until haxelib ran. */
  void revealAfterReload(@NotNull String libraryName) {
    pendingRevealLibrary = libraryName;
    pendingRevealVersion = null;
  }

  /** Filters the tree to the library and selects it; a version entry is selected once its child materializes. */
  private void select(@NotNull String libraryName, @Nullable String version) {
    pendingRevealLibrary = libraryName;
    pendingRevealVersion = version;
    // a text CHANGE refilters (and applies the reveal) through the document
    // listener; an unchanged query fires nothing, hence the direct call
    searchField.setText(libraryName);
    applyPendingReveal();
  }

  /** Selects the pending library's row when it exists; a wanted version entry not yet materialized stays pending for its lazy load. */
  private void applyPendingReveal() {
    if (pendingRevealLibrary == null) return;
    DefaultMutableTreeNode node = libraryNode(pendingRevealLibrary);
    if (node == null) return;
    TreePath path = new TreePath(node.getPath());
    tree.setSelectionPath(path);
    tree.scrollPathToVisible(path);
    if (pendingRevealVersion != null) {
      // triggers the lazy load when needed; setVersionChildren finishes the reveal
      tree.expandPath(path);
      if (selectVersionChild(node, pendingRevealVersion)) {
        pendingRevealVersion = null;
      }
    }
    pendingRevealLibrary = null;
  }

  /** Finishes a reveal after the lazy load: selects the wanted version entry among the fresh children. */
  private void applyPendingVersionSelection(@NotNull DefaultMutableTreeNode node, @NotNull LibraryRow row) {
    if (pendingRevealVersion == null || !searchField.getText().trim().equalsIgnoreCase(row.name())) return;
    String wanted = pendingRevealVersion;
    pendingRevealVersion = null;
    selectVersionChild(node, wanted);
  }

  /** Selects the library node's version child, when materialized; false leaves the library row selected. */
  private boolean selectVersionChild(@NotNull DefaultMutableTreeNode node, @NotNull String version) {
    for (int i = 0; i < node.getChildCount(); i++) {
      DefaultMutableTreeNode child = (DefaultMutableTreeNode)node.getChildAt(i);
      if (child.getUserObject() instanceof VersionEntry entry && version.equals(entry.version())) {
        TreePath childPath = new TreePath(child.getPath());
        tree.setSelectionPath(childPath);
        tree.scrollPathToVisible(childPath);
        return true;
      }
    }
    return false;
  }

  // ------------------------------------------------- expansion preservation

  /**
   * Rebuilds happen on every filter change and after every mutation; the
   * user's expansion and selection survive them by STABLE identity (library
   * name, library|version) — re-expanding goes through the lazy loader, so
   * version children refresh from the new installed state in place.
   */
  private void restoreTreeState(@NotNull Set<String> expandedLibraries, @Nullable String selectedKey) {
    for (int i = 0; i < root.getChildCount(); i++) {
      DefaultMutableTreeNode libraryNode = (DefaultMutableTreeNode)root.getChildAt(i);
      if (!(libraryNode.getUserObject() instanceof LibraryRow row)) continue;
      TreePath libraryPath = new TreePath(libraryNode.getPath());
      if (expandedLibraries.contains(row.name())) {
        tree.expandPath(libraryPath);
      }
      restoreSelection(libraryNode, row, libraryPath, selectedKey);
    }
  }

  private void restoreSelection(@NotNull DefaultMutableTreeNode libraryNode, @NotNull LibraryRow row,
                                @NotNull TreePath libraryPath, @Nullable String selectedKey) {
    if (selectedKey == null) return;
    if (selectedKey.equals(row.name())) {
      tree.setSelectionPath(libraryPath);
      return;
    }
    if (!selectedKey.startsWith(row.name() + "|")) return;
    // a version selection: the exact child when the (cached-info) expand
    // already materialized it, else the library node stands in
    for (int i = 0; i < libraryNode.getChildCount(); i++) {
      DefaultMutableTreeNode child = (DefaultMutableTreeNode)libraryNode.getChildAt(i);
      if (child.getUserObject() instanceof VersionEntry entry
          && selectedKey.equals(entry.library() + "|" + entry.version())) {
        tree.setSelectionPath(new TreePath(child.getPath()));
        return;
      }
    }
    tree.setSelectionPath(libraryPath);
  }

  @NotNull
  private Set<String> collectExpandedLibraries() {
    Set<String> names = new LinkedHashSet<>();
    var expanded = tree.getExpandedDescendants(new TreePath(root));
    while (expanded != null && expanded.hasMoreElements()) {
      Object last = ((DefaultMutableTreeNode)expanded.nextElement().getLastPathComponent()).getUserObject();
      if (last instanceof LibraryRow row) {
        names.add(row.name());
      }
    }
    return names;
  }

  @Nullable
  private static String selectionKey(@Nullable TreePath path) {
    if (path == null) return null;
    Object userObject = ((DefaultMutableTreeNode)path.getLastPathComponent()).getUserObject();
    if (userObject instanceof LibraryRow row) return row.name();
    if (userObject instanceof VersionEntry entry) return entry.library() + "|" + entry.version();
    return null;
  }

  @NotNull
  private static String speedSearchText(@NotNull TreePath path) {
    Object userObject = ((DefaultMutableTreeNode)path.getLastPathComponent()).getUserObject();
    if (userObject instanceof LibraryRow row) return row.name();
    if (userObject instanceof VersionEntry entry) return entry.version();
    return String.valueOf(userObject);
  }

  /** A library node stays while ANY of its versions would be visible under the version filter. */
  private boolean accepted(@NotNull LibraryRow row, @NotNull String query, @Nullable HaxelibCacheManager manager) {
    boolean anyVersionShown =
      (filters.isActive(HaxelibExplorerFilters.Filter.INSTALLED) && row.installedRelease())
      || (filters.isActive(HaxelibExplorerFilters.Filter.NOT_INSTALLED) && hasNotInstalledVersion(row, manager))
      || (filters.isActive(HaxelibExplorerFilters.Filter.DEV) && row.dev())
      || (filters.isActive(HaxelibExplorerFilters.Filter.GIT) && row.git());
    if (!anyVersionShown) return false;
    if (filters.isActive(HaxelibExplorerFilters.Filter.ONLY_UPDATES) && !hasKnownUpdate(row)) return false;
    if (filters.isActive(HaxelibExplorerFilters.Filter.BEHIND_LATEST) && !behindLatest(row)) return false;
    return query.isEmpty() || row.name().toLowerCase(Locale.ROOT).contains(query);
  }

  /** The filter applied to each version node: visible while its kind's toggle is on. */
  private boolean versionShown(@NotNull VersionEntry entry) {
    if (HaxelibSemVer.DEV.equals(entry.version())) return filters.isActive(HaxelibExplorerFilters.Filter.DEV);
    if (HaxelibSemVer.GIT_SCM.equals(entry.version())) return filters.isActive(HaxelibExplorerFilters.Filter.GIT);
    return filters.isActive(entry.installed() ? HaxelibExplorerFilters.Filter.INSTALLED
                                              : HaxelibExplorerFilters.Filter.NOT_INSTALLED);
  }

  // without fetched info the release list is unknown; keep the node visible
  // and let the version filter prune children when they materialize
  private boolean hasNotInstalledVersion(@NotNull LibraryRow row, @Nullable HaxelibCacheManager manager) {
    HaxelibLibraryInfo info = manager == null ? null : manager.getCachedLibraryInfo(row.name());
    if (info == null) return true;
    return info.releases().stream().anyMatch(release -> !row.installedVersions().contains(release.version()));
  }

  /** Whether a newer release exists than anything installed (answered from cached info only). */
  private boolean hasKnownUpdate(@NotNull LibraryRow row) {
    if (!row.installed() || row.dev() || row.git()) return false;
    String latest = latestVersions.get(row.name());
    return latest != null && !row.installedVersions().contains(latest);
  }

  /** Whether the CURRENT selection is a release older than the latest — the forgotten-pin case. */
  private boolean behindLatest(@NotNull LibraryRow row) {
    String selected = row.selectedVersion();
    if (selected == null || HaxelibSemVer.isPseudoVersion(selected)) return false;
    String latest = latestVersions.get(row.name());
    return latest != null && !latest.equals(selected);
  }

  // the filters and suffix colouring never trigger a server sweep over the
  // whole catalog: only libraries whose info is already cached contribute
  @NotNull
  private Map<String, String> collectLatestVersions(@Nullable HaxelibCacheManager manager) {
    if (manager == null) return Map.of();
    Map<String, String> latest = new HashMap<>();
    for (LibraryRow row : allRows) {
      HaxelibLibraryInfo info = manager.getCachedLibraryInfo(row.name());
      if (info != null && !info.latestVersion().isEmpty()) {
        latest.put(row.name(), info.latestVersion());
      }
    }
    return latest;
  }

  // ----------------------------------------------------- version children

  private static final String LOADING_PLACEHOLDER = HaxeBundle.message("haxelib.explorer.loading.versions");

  private final class LazyVersionLoader implements TreeWillExpandListener {
    @Override
    public void treeWillExpand(TreeExpansionEvent event) {
      if (!(event.getPath().getLastPathComponent() instanceof DefaultMutableTreeNode node)
          || !(node.getUserObject() instanceof LibraryRow row)
          || !isPlaceholderOnly(node)) {
        return;
      }
      HaxelibCacheManager manager = cacheManager();
      if (manager == null) return;
      // children swap in AFTER the expand gesture: reloading the node's
      // model inside treeWillExpand collapses it mid-gesture, which
      // intermittently left restored nodes collapsed
      HaxelibLibraryInfo cached = manager.getCachedLibraryInfo(row.name());
      if (cached != null) {
        ApplicationManager.getApplication().invokeLater(() -> populateVersions(row.name(), cached));
        return;
      }
      AppExecutorUtil.getAppExecutorService().execute(() -> {
        HaxelibLibraryInfo info = manager.getLibraryInfo(row.name());
        ApplicationManager.getApplication().invokeLater(() -> populateVersions(row.name(), info));
      });
    }

    @Override
    public void treeWillCollapse(TreeExpansionEvent event) {
    }
  }

  /** Fills the library's CURRENT node - a rebuild may have replaced the node the expand fired on. */
  private void populateVersions(@NotNull String name, @Nullable HaxelibLibraryInfo info) {
    if (disposed) return;
    DefaultMutableTreeNode node = libraryNode(name);
    if (node != null && node.getUserObject() instanceof LibraryRow row && isPlaceholderOnly(node)) {
      setVersionChildren(node, row, info);
    }
  }

  @Nullable
  private DefaultMutableTreeNode libraryNode(@NotNull String name) {
    for (int i = 0; i < root.getChildCount(); i++) {
      DefaultMutableTreeNode child = (DefaultMutableTreeNode)root.getChildAt(i);
      if (child.getUserObject() instanceof LibraryRow row && name.equals(row.name())) {
        return child;
      }
    }
    return null;
  }

  private static boolean isPlaceholderOnly(@NotNull DefaultMutableTreeNode node) {
    return node.getChildCount() == 1
           && ((DefaultMutableTreeNode)node.getChildAt(0)).getUserObject() == LOADING_PLACEHOLDER;
  }

  private void setVersionChildren(@NotNull DefaultMutableTreeNode node,
                                  @NotNull LibraryRow row,
                                  @Nullable HaxelibLibraryInfo info) {
    node.removeAllChildren();
    for (VersionEntry entry : versionEntries(row, info, gitCheckoutNote(row))) {
      if (versionShown(entry)) {
        node.add(new DefaultMutableTreeNode(entry));
      }
    }
    treeModel.reload(node);
    tree.expandPath(new TreePath(node.getPath()));
    applyPendingVersionSelection(node, row);
  }

  /** The git entry's gray suffix: what the checkout points at ("main @ 559b24c9a3"). */
  @Nullable
  private String gitCheckoutNote(@NotNull LibraryRow row) {
    HaxelibLocalDocs.GitCheckout checkout = gitCheckoutOf(row);
    return checkout == null ? null : checkout.display();
  }

  /** The row's git checkout state, or null without one. Callable from the EDT and pooled threads. */
  @Nullable
  HaxelibLocalDocs.GitCheckout gitCheckoutOf(@NotNull LibraryRow row) {
    if (!row.git()) return null;
    Path repoRoot = repositoryRoot();
    return repoRoot == null ? null : HaxelibLocalDocs.gitCheckout(repoRoot, row.name());
  }

  /** The version list under a library: dev/git and local-only versions first, then every release newest-first. */
  @NotNull
  private static List<VersionEntry> versionEntries(@NotNull LibraryRow row,
                                                   @Nullable HaxelibLibraryInfo info,
                                                   @Nullable String gitCheckoutNote) {
    List<VersionEntry> entries = new ArrayList<>();
    Set<String> covered = new LinkedHashSet<>();
    for (String pseudo : List.of(HaxelibSemVer.DEV, HaxelibSemVer.GIT_SCM)) {
      if (row.installedVersions().contains(pseudo)) {
        String note = HaxelibSemVer.GIT_SCM.equals(pseudo) ? gitCheckoutNote : null;
        entries.add(entry(row, pseudo, null, note));
        covered.add(pseudo);
      }
    }
    List<HaxelibLibraryInfo.Release> releases = info == null ? List.of() : info.releases();
    Set<String> released = new LinkedHashSet<>();
    releases.forEach(release -> released.add(release.version()));
    for (String local : row.installedVersions()) {
      if (!covered.contains(local) && !released.contains(local)) {
        entries.add(entry(row, local, null, null));
        covered.add(local);
      }
    }
    for (HaxelibLibraryInfo.Release release : releases.reversed()) {
      entries.add(entry(row, release.version(), release.date(), release.note()));
    }
    return entries;
  }

  private static VersionEntry entry(@NotNull LibraryRow row, @NotNull String version,
                                    @Nullable String date, @Nullable String note) {
    return new VersionEntry(row.name(), version, date, note,
                            row.installedVersions().contains(version),
                            version.equals(row.selectedVersion()));
  }

  // ------------------------------------------------------------ selection

  /** The render inputs the details pane shows or is loading: library state plus the version whose docs are up. */
  private record ShownSelection(@NotNull LibraryRow row, @Nullable String docsVersion) {
  }

  private void showSelection() {
    LibraryRow row = selectedLibraryRow();
    if (row == null) {
      selectionGeneration.incrementAndGet();
      lastShownSelection = null;
      details.showMessage(HaxeBundle.message("haxelib.explorer.no.selection"));
      return;
    }
    // keyed on the RENDER INPUTS, not the clicked node, and recorded at
    // REQUEST time: an equal selection - a rebuild re-firing the same node,
    // a click landing on the same library state and docs - must neither
    // flash the loading state nor bump the generation (that would discard
    // a render still in flight and restart it, looping while background
    // info hydration keeps rebuilding the tree)
    String docsVersion = docsVersionFor(row);
    ShownSelection selection = new ShownSelection(row, docsVersion);
    if (selection.equals(lastShownSelection)) return;
    lastShownSelection = selection;
    int expected = selectionGeneration.incrementAndGet();
    // one loading state, then ONE render with everything ready - an
    // intermediate overview-only render would steal the tab selection from
    // the readme
    details.showMessage(HaxeBundle.message("haxelib.explorer.loading.details"));
    HaxelibCacheManager manager = cacheManager();
    AppExecutorUtil.getAppExecutorService().execute(() -> {
      HaxelibLibraryInfo info = manager == null ? null : manager.getLibraryInfo(row.name());
      Path repoRoot = repositoryRoot();
      List<HaxelibDetailsPane.DocTab> docs = loadDocsFor(row, docsVersion, repoRoot);
      String devPath = devPathFor(row, repoRoot);
      HaxelibLocalDocs.GitCheckout gitCheckout = gitCheckoutFor(row, repoRoot);
      Runnable render = () -> details.showLibrary(row.name(), row.installedVersions(), row.selectedVersion(),
                                                  devPath, gitCheckout, info, docs);
      onUi(selectionGeneration, expected, render);
    });
  }

  /** The version's doc tabs; none without a resolvable repository or an installed version to read from. */
  @NotNull
  private List<HaxelibDetailsPane.DocTab> loadDocsFor(@NotNull LibraryRow row,
                                                      @Nullable String docsVersion,
                                                      @Nullable Path repoRoot) {
    if (docsVersion == null || repoRoot == null) return List.of();
    return docsLoader.loadDocs(repoRoot, row.name(), docsVersion);
  }

  /** The dev pseudo-version's target directory; null for non-dev rows or without a resolvable repository. */
  @Nullable
  private static String devPathFor(@NotNull LibraryRow row, @Nullable Path repoRoot) {
    if (!row.dev() || repoRoot == null) return null;
    return HaxelibLocalDocs.devPath(repoRoot, row.name());
  }

  /** The git pseudo-version's branch/commit; null for non-git rows or without a resolvable repository. */
  @Nullable
  private static HaxelibLocalDocs.GitCheckout gitCheckoutFor(@NotNull LibraryRow row, @Nullable Path repoRoot) {
    if (!row.git() || repoRoot == null) return null;
    return HaxelibLocalDocs.gitCheckout(repoRoot, row.name());
  }

  /** The version whose local files feed the doc tabs: the selected version node, else the library's current one. */
  @Nullable
  private String docsVersionFor(@NotNull LibraryRow row) {
    if (selectedUserObject() instanceof VersionEntry entry && entry.installed()) {
      return entry.version();
    }
    return row.selectedVersion();
  }

  @Nullable
  Path repositoryRoot() {
    // SDK and VFS resolution read the project model; reached from pooled
    // detail loads and from EDT context-menu actions (dev directory chooser)
    return HaxeReadActions.compute(() -> {
      Module module = haxeModule();
      if (module == null) return null;
      // haxeModule() guarantees a configured SDK; lookupSdk's fallback would probe a process
      Sdk sdk = ModuleRootManager.getInstance(module).getSdk();
      if (sdk == null) return null;
      VirtualFile moduleDir = ProjectUtil.guessModuleDir(module);
      VirtualFile root = HaxelibUtil.getLibraryBasePath(sdk, moduleDir);
      return root == null ? null : Path.of(root.getPath());
    });
  }

  /** The row's registered dev directory, or null without one. Callable from the EDT and pooled threads. */
  @Nullable
  String devDirectoryOf(@NotNull LibraryRow row) {
    return devPathFor(row, repositoryRoot());
  }

  // ------------------------------------------------------------- context

  @Nullable
  Object selectedUserObject() {
    TreePath path = tree.getSelectionPath();
    return path == null ? null : ((DefaultMutableTreeNode)path.getLastPathComponent()).getUserObject();
  }

  /** The library of the selection — the node itself or the parent of a version node. */
  @Nullable
  LibraryRow selectedLibraryRow() {
    TreePath path = tree.getSelectionPath();
    if (path == null) return null;
    for (Object component : path.getPath()) {
      if (((DefaultMutableTreeNode)component).getUserObject() instanceof LibraryRow row) {
        return row;
      }
    }
    return null;
  }

  @NotNull
  Project getProject() {
    return project;
  }

  /** The cache behind the first module with a valid Haxe SDK; null without one. */
  @Nullable
  private HaxelibCacheManager cacheManager() {
    Module module = haxeModule();
    return module == null ? null : HaxelibCacheManager.getInstance(module);
  }

  // TODO: a module chooser in the toolbar - the explorer follows the FIRST module with a valid Haxe SDK
  @Nullable
  private Module haxeModule() {
    // pure model reads under the lock; lookupSdk's default-SDK fallback
    // probes `haxe -help` (a process) and must never run in here. Reached
    // from the EDT (tree selection) AND the pooled tasks, hence the
    // per-thread read form.
    return HaxeReadActions.compute(() -> {
      for (Module module : ModuleManager.getInstance(project).getModules()) {
        Sdk sdk = ModuleRootManager.getInstance(module).getSdk();
        if (sdk != null && HaxelibSdkUtils.isValidHaxeSdk(sdk)) {
          return module;
        }
      }
      return null;
    });
  }

  private void onUi(@NotNull AtomicInteger guard, int expectedGeneration, @NotNull Runnable update) {
    ApplicationManager.getApplication().invokeLater(() -> {
      if (!disposed && guard.get() == expectedGeneration) {
        update.run();
      }
    });
  }

  @Override
  public void dispose() {
    disposed = true;
  }

  // ------------------------------------------------------------ rendering

  // an orange current-version suffix marks a selection pinned behind the
  // latest release, spottable while scrolling with every filter off
  private static final SimpleTextAttributes BEHIND_LATEST_ATTRIBUTES =
    new SimpleTextAttributes(SimpleTextAttributes.STYLE_PLAIN, JBColor.ORANGE);

  private final class NodeRenderer extends ColoredTreeCellRenderer {
    @Override
    public void customizeCellRenderer(@NotNull JTree tree, Object value, boolean selected, boolean expanded,
                                      boolean leaf, int row, boolean hasFocus) {
      Object userObject = ((DefaultMutableTreeNode)value).getUserObject();
      if (userObject instanceof LibraryRow library) {
        setIcon(library.installed() ? AllIcons.Nodes.PpLib : AllIcons.Nodes.PpLibFolder);
        append(library.name());
        if (library.selectedVersion() != null) {
          SimpleTextAttributes suffix = behindLatest(library) ? BEHIND_LATEST_ATTRIBUTES
                                                              : SimpleTextAttributes.GRAYED_ATTRIBUTES;
          append("  " + library.selectedVersion(), suffix);
        }
      }
      else if (userObject instanceof VersionEntry entry) {
        setIcon(entry.installed() ? AllIcons.Actions.Checked : AllIcons.Actions.Download);
        append(entry.version(), entry.current() ? SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES
                                                : SimpleTextAttributes.REGULAR_ATTRIBUTES);
        if (entry.date() != null) {
          append("  " + entry.date(), SimpleTextAttributes.GRAYED_ATTRIBUTES);
        }
        if (entry.note() != null && !entry.note().isEmpty()) {
          append("  " + entry.note(), SimpleTextAttributes.GRAYED_ITALIC_ATTRIBUTES);
        }
      }
      else {
        append(String.valueOf(userObject), SimpleTextAttributes.GRAYED_ATTRIBUTES);
      }
    }
  }

  private final class RefreshAction extends AnAction implements DumbAware {
    private RefreshAction() {
      super(() -> HaxeBundle.message("haxelib.explorer.refresh"), AllIcons.Actions.Refresh);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
      reload(true);
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
      return ActionUpdateThread.BGT;
    }
  }
}
