package com.intellij.plugins.haxe.v2.toolwindow;

import com.intellij.openapi.project.Project;
import com.intellij.plugins.haxe.HaxeBundle;
import com.intellij.plugins.haxe.config.HaxeTarget;
import com.intellij.plugins.haxe.v2.buildsystem.HaxeBuildFile;
import com.intellij.plugins.haxe.v2.buildsystem.HaxeBuildFileInfo;
import com.intellij.plugins.haxe.v2.buildsystem.HaxeBuildFileInfo.HaxeDefine;
import com.intellij.plugins.haxe.v2.buildsystem.HaxeBuildFileInfo.HaxeLibDependency;
import com.intellij.plugins.haxe.v2.buildsystem.HaxeBuildFileType;
import com.intellij.plugins.haxe.v2.buildtools.libraries.HaxeInstalledLibraries;
import com.intellij.plugins.haxe.v2.buildtools.libraries.HaxeInstalledLibraries.InstalledLibrary;
import com.intellij.plugins.haxe.v2.buildtools.libraries.HaxeInstalledLibraries.LibraryStatus;
import com.intellij.plugins.haxe.v2.buildtools.settings.DefineOverride;
import com.intellij.plugins.haxe.v2.buildtools.settings.HaxeTargetOptions;
import com.intellij.plugins.haxe.v2.buildtools.settings.HaxeTargetSelectionStore;
import com.intellij.plugins.haxe.v2.runconfig.HaxeLaunchKind;
import com.intellij.plugins.haxe.v2.runconfig.HaxeProgramLaunches;
import com.intellij.plugins.haxe.v2.testing.HaxeTestFrameworks;
import com.intellij.plugins.haxe.v2.toolwindow.HaxeToolWindowModelBuilder.ContainerEntry;
import com.intellij.plugins.haxe.v2.toolwindow.HaxeToolWindowModelBuilder.EnvironmentData;
import com.intellij.plugins.haxe.v2.toolwindow.HaxeToolWindowModelBuilder.FileEntry;
import com.intellij.plugins.haxe.v2.toolwindow.HaxeToolWindowModelBuilder.IdeOverridesData;
import com.intellij.plugins.haxe.v2.toolwindow.tree.HaxeToolWindowNodes.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.tree.DefaultMutableTreeNode;
import java.util.List;
import java.util.HashSet;
import java.util.Set;
import com.intellij.plugins.haxe.v2.buildtools.libraries.HaxeLibraryGraph;
import java.util.Objects;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Turns the model builder's container entries into the tool window's Swing
 * tree, Gradle-style: one project node holding the project root's groups and
 * the modules. A pure transformation of the prepared model plus the target
 * selection store and the library graph - no read action needed. With {@code hideEmptyNodes}, a
 * grouping row without children and an unset value row are left out.
 */
final class HaxeToolWindowTreeBuilder {

  private final Project project;
  private final boolean hideEmptyNodes;

  HaxeToolWindowTreeBuilder(@NotNull Project project, boolean hideEmptyNodes) {
    this.project = project;
    this.hideEmptyNodes = hideEmptyNodes;
  }

  /** {@code installedLibraries} null = the haxelib lookup failed: install state unknown, shown as installed. */
  @NotNull
  DefaultMutableTreeNode buildRoot(@NotNull List<ContainerEntry> containers,
                                   @Nullable Map<String, InstalledLibrary> installedLibraries) {
    DefaultMutableTreeNode root = new DefaultMutableTreeNode();
    DefaultMutableTreeNode projectNode = new DefaultMutableTreeNode(new ProjectNode(project.getName()));
    root.add(projectNode);

    for (ContainerEntry container : containers) {
      if (container.projectRoot()) {
        addContainerGroups(projectNode, container, installedLibraries);
      }
      else {
        DefaultMutableTreeNode moduleNode = new DefaultMutableTreeNode(new ModuleNode(container.displayName()));
        addContainerGroups(moduleNode, container, installedLibraries);
        projectNode.add(moduleNode);
      }
    }
    return root;
  }

  /** A container's groups: Compilation, Environment, Build and - when it has any - Tools. */
  private void addContainerGroups(@NotNull DefaultMutableTreeNode parent,
                                  @NotNull ContainerEntry container,
                                  @Nullable Map<String, InstalledLibrary> installedLibraries) {
    parent.add(compilationGroupNode(container));
    parent.add(environmentNode(container));
    addUnlessEmpty(parent, buildGroupNode(container, installedLibraries));
    if (!container.tools().isEmpty()) {
      parent.add(toolsGroupNode(container));
    }
  }

  @NotNull
  private static DefaultMutableTreeNode compilationGroupNode(@NotNull ContainerEntry container) {
    DefaultMutableTreeNode compilationNode = new DefaultMutableTreeNode(new CompilationGroupNode(container.id()));
    compilationNode.add(new DefaultMutableTreeNode(container.compileCommand()));
    compilationNode.add(new DefaultMutableTreeNode(container.server()));
    compilationNode.add(new DefaultMutableTreeNode(container.connectionMode()));
    compilationNode.add(new DefaultMutableTreeNode(container.serverInstance()));
    return compilationNode;
  }

  @NotNull
  private static DefaultMutableTreeNode environmentNode(@NotNull ContainerEntry container) {
    EnvironmentData environment = container.environment();
    DefaultMutableTreeNode environmentNode = new DefaultMutableTreeNode(new EnvironmentNode(container.id()));

    EnvSdkNode sdkRow = new EnvSdkNode(container.id(), environment.sdkDisplay(), environment.sdkMissing());
    environmentNode.add(new DefaultMutableTreeNode(sdkRow));

    EnvLanguageLevelNode levelRow = new EnvLanguageLevelNode(container.id(), environment.languageLevelDisplay());
    environmentNode.add(new DefaultMutableTreeNode(levelRow));
    return environmentNode;
  }

  @NotNull
  private DefaultMutableTreeNode buildGroupNode(@NotNull ContainerEntry container,
                                                @Nullable Map<String, InstalledLibrary> installedLibraries) {
    DefaultMutableTreeNode buildNode =
      new DefaultMutableTreeNode(new BuildGroupNode(container.id(), container.files().size()));
    for (FileEntry fileEntry : container.files()) {
      buildNode.add(fileNode(fileEntry, fileRow(container, fileEntry), installedLibraries));
    }
    return buildNode;
  }

  @NotNull
  private static DefaultMutableTreeNode toolsGroupNode(@NotNull ContainerEntry container) {
    DefaultMutableTreeNode toolsNode =
      new DefaultMutableTreeNode(new ToolsGroupNode(container.id(), container.tools().size()));
    for (ToolNode tool : container.tools()) {
      toolsNode.add(new DefaultMutableTreeNode(tool));
    }
    return toolsNode;
  }

  @NotNull
  private static BuildFileRow fileRow(@NotNull ContainerEntry container, @NotNull FileEntry fileEntry) {
    String path = fileEntry.buildFile().file().getPath();
    boolean active = path.equals(container.activePath());
    boolean tests = container.testsPaths().contains(path);
    boolean marked = container.markedTestsPaths().contains(path);
    boolean frameworkDetected = HaxeTestFrameworks.detectedFramework(fileEntry.info().libraries()) != null;
    return new BuildFileRow(fileEntry.buildFile(), container.id(), active, fileEntry.manual(), tests, marked, frameworkDetected,
                            fileEntry.toolFailure());
  }

  @NotNull
  private DefaultMutableTreeNode fileNode(@NotNull FileEntry entry,
                                          @NotNull BuildFileRow row,
                                          @Nullable Map<String, InstalledLibrary> installedLibraries) {
    HaxeBuildFile buildFile = entry.buildFile();
    DefaultMutableTreeNode fileNode = new DefaultMutableTreeNode(row);
    // a plain hxp script decides its own targets in code - no target row
    if (buildFile.type() != HaxeBuildFileType.HXP_SCRIPT) {
      fileNode.add(new DefaultMutableTreeNode(targetNode(entry)));
    }
    // multi-section hxml (--next chain): the row picking which compilation the tree follows
    if (!entry.sectionLabels().isEmpty()) {
      SectionNode sectionNode = new SectionNode(buildFile, entry.sectionIds(), entry.sectionLabels(),
                                                entry.sectionDescriptors(), entry.selectedSection());
      fileNode.add(new DefaultMutableTreeNode(sectionNode));
    }
    addUnlessEmpty(fileNode, definesGroupNode(entry));
    addUnlessEmpty(fileNode, ideOverridesNode(entry));
    addUnlessEmpty(fileNode, librariesGroupNode(entry, installedLibraries));
    addUnlessEmpty(fileNode, actionsGroupNode(entry));
    if (row.testsFile()) {
      fileNode.add(testsGroupNode(entry));
    }
    return fileNode;
  }

  @NotNull
  private TargetNode targetNode(@NotNull FileEntry entry) {
    HaxeBuildFile buildFile = entry.buildFile();
    if (!HaxeTargetOptions.isTargetSelectable(buildFile.type())) {
      return new TargetNode(buildFile, declaredTargetDisplay(entry.info()), false);
    }
    String selectedId = HaxeTargetSelectionStore.getInstance(project).getSelectedTargetId(buildFile.file());
    return new TargetNode(buildFile, HaxeTargetOptions.displayNameFor(buildFile.type(), selectedId), true);
  }

  /** The target the build file selects: a stock target's name, "Custom (go)" for a custom target, else "(not specified)". */
  @NotNull
  private static String declaredTargetDisplay(@NotNull HaxeBuildFileInfo info) {
    HaxeTarget target = info.target();
    if (target != null) return target.toString();
    String customTarget = info.customTargetName();
    if (customTarget != null) return HaxeBundle.message("haxe.toolwindow.node.target.custom", customTarget);
    return HaxeBundle.message("haxe.toolwindow.node.target.unspecified");
  }

  @NotNull
  private static DefaultMutableTreeNode definesGroupNode(@NotNull FileEntry entry) {
    List<HaxeDefine> defines = entry.info().defines();
    DefaultMutableTreeNode definesNode = new DefaultMutableTreeNode(new GroupNode(GroupKind.DEFINES, defines.size()));
    for (HaxeDefine define : defines) {
      definesNode.add(new DefaultMutableTreeNode(new DefineNode(entry.buildFile(), define.name(), define.value())));
    }
    return definesNode;
  }

  /** The build file's IDE overrides: its define entries, then - for an hxml - its custom target. */
  @NotNull
  private DefaultMutableTreeNode ideOverridesNode(@NotNull FileEntry entry) {
    HaxeBuildFile buildFile = entry.buildFile();
    IdeOverridesData overrides = entry.ideOverrides();
    Set<String> buildDefineNames = entry.info().defines().stream()
      .map(HaxeDefine::name)
      .collect(Collectors.toSet());
    int count = overrides.defines().size() + (overrides.customTarget() != null ? 1 : 0);
    IdeOverridesNode overridesRow = new IdeOverridesNode(buildFile, count, buildDefineNames, overrides.customTargetApplicable());
    DefaultMutableTreeNode overridesNode = new DefaultMutableTreeNode(overridesRow);

    DefaultMutableTreeNode definesNode = new DefaultMutableTreeNode(new IdeDefinesNode(buildFile, overrides.defines().size()));
    for (DefineOverride define : overrides.defines()) {
      boolean inBuildFile = buildDefineNames.contains(define.name());
      IdeDefineNode defineRow = new IdeDefineNode(buildFile, define.name(), define.value(), define.effect(), inBuildFile);
      definesNode.add(new DefaultMutableTreeNode(defineRow));
    }
    addUnlessEmpty(overridesNode, definesNode);

    boolean showsCustomTarget = overrides.customTargetApplicable() && (overrides.customTarget() != null || !hideEmptyNodes);
    if (showsCustomTarget) {
      IdeCustomTargetNode customTargetRow = new IdeCustomTargetNode(buildFile, overrides.customTarget(), overrides.buildTarget());
      overridesNode.add(new DefaultMutableTreeNode(customTargetRow));
    }
    return overridesNode;
  }

  /** Adds a grouping row, unless empty rows are hidden and it has no children. */
  private void addUnlessEmpty(@NotNull DefaultMutableTreeNode parent, @NotNull DefaultMutableTreeNode group) {
    if (hideEmptyNodes && group.getChildCount() == 0) return;
    parent.add(group);
  }

  /** The declared libraries, each with the dependencies the sync's graph knows below it. */
  @NotNull
  private DefaultMutableTreeNode librariesGroupNode(@NotNull FileEntry entry,
                                                    @Nullable Map<String, InstalledLibrary> installedLibraries) {
    List<HaxeLibDependency> libraries = entry.info().libraries();
    DefaultMutableTreeNode librariesNode = new DefaultMutableTreeNode(new GroupNode(GroupKind.LIBRARIES, libraries.size()));
    HaxeLibraryGraph graph = HaxeLibraryGraph.getInstance(project);
    for (HaxeLibDependency library : libraries) {
      LibraryNode row = libraryNode(entry.buildFile(), library, installedLibraries, null);
      librariesNode.add(libraryTreeNode(row, graph, installedLibraries, new HashSet<>()));
    }
    return librariesNode;
  }

  /** The library's row with its transitive dependencies below it; a library already on the path above is not repeated. */
  @NotNull
  private static DefaultMutableTreeNode libraryTreeNode(@NotNull LibraryNode row,
                                                        @NotNull HaxeLibraryGraph graph,
                                                        @Nullable Map<String, InstalledLibrary> installedLibraries,
                                                        @NotNull Set<String> ancestry) {
    DefaultMutableTreeNode node = new DefaultMutableTreeNode(row);
    HaxeLibraryGraph.Library library = graph.library(row.name());
    String key = HaxeLibDependency.key(row.name());
    if (library == null || !ancestry.add(key)) return node;
    for (HaxeLibraryGraph.Requirement requirement : library.requires()) {
      HaxeLibDependency dependency = new HaxeLibDependency(requirement.name(), requirement.version());
      LibraryNode child = libraryNode(row.owner(), dependency, installedLibraries, row.name());
      node.add(libraryTreeNode(child, graph, installedLibraries, ancestry));
    }
    ancestry.remove(key);
    return node;
  }

  @NotNull
  private static LibraryNode libraryNode(@NotNull HaxeBuildFile buildFile,
                                         @NotNull HaxeLibDependency library,
                                         @Nullable Map<String, InstalledLibrary> installedLibraries,
                                         @Nullable String requiredBy) {
    LibraryStatus status = HaxeInstalledLibraries.status(installedLibraries, library);
    return new LibraryNode(buildFile, library.name(), library.version(), status.selectedVersion(), status.installed(), requiredBy);
  }

  @NotNull
  private DefaultMutableTreeNode actionsGroupNode(@NotNull FileEntry entry) {
    List<HaxeLaunchKind> launchKinds = HaxeProgramLaunches.launchKinds(project, entry.info(), entry.buildFile());
    int count = entry.actions().size() + launchKinds.size();
    String launchOutput = HaxeProgramLaunches.launchOutput(entry.info());
    DefaultMutableTreeNode actionsNode = new DefaultMutableTreeNode(
      new ActionsGroupNode(entry.buildFile().file().getPath(), count, launchOutput));
    for (ActionNode action : entry.actions()) {
      actionsNode.add(new DefaultMutableTreeNode(action));
    }
    for (HaxeLaunchKind launchKind : launchKinds) {
      ProgramNode program = new ProgramNode(entry.buildFile(), launchKind, Objects.requireNonNull(launchOutput));
      actionsNode.add(new DefaultMutableTreeNode(program));
    }
    return actionsNode;
  }

  /** The tests build file's own category, keeping test runs out of the crowded Actions group. */
  @NotNull
  private static DefaultMutableTreeNode testsGroupNode(@NotNull FileEntry entry) {
    String path = entry.buildFile().file().getPath();
    DefaultMutableTreeNode testsNode = new DefaultMutableTreeNode(new TestsGroupNode());
    testsNode.add(new DefaultMutableTreeNode(new TestRunNode(path)));
    return testsNode;
  }
}
