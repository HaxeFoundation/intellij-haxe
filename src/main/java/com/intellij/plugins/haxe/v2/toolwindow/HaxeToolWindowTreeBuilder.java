package com.intellij.plugins.haxe.v2.toolwindow;

import com.intellij.openapi.project.Project;
import com.intellij.plugins.haxe.HaxeBundle;
import com.intellij.plugins.haxe.config.HaxeTarget;
import com.intellij.plugins.haxe.haxelib.HaxelibGitSpec;
import com.intellij.plugins.haxe.haxelib.HaxelibSemVer;
import com.intellij.plugins.haxe.v2.buildsystem.HaxeBuildFile;
import com.intellij.plugins.haxe.v2.buildsystem.HaxeBuildFileInfo.HaxeDefine;
import com.intellij.plugins.haxe.v2.buildsystem.HaxeBuildFileInfo.HaxeLibDependency;
import com.intellij.plugins.haxe.v2.buildsystem.HaxeBuildFileType;
import com.intellij.plugins.haxe.v2.buildtools.settings.HaxeTargetOptions;
import com.intellij.plugins.haxe.v2.buildtools.settings.HaxeTargetSelectionStore;
import com.intellij.plugins.haxe.v2.runconfig.HaxeProgramLaunches;
import com.intellij.plugins.haxe.v2.testing.HaxeTestFrameworks;
import com.intellij.plugins.haxe.v2.toolwindow.HaxeToolWindowModelBuilder.ContainerEntry;
import com.intellij.plugins.haxe.v2.toolwindow.HaxeToolWindowModelBuilder.EnvironmentData;
import com.intellij.plugins.haxe.v2.toolwindow.HaxeToolWindowModelBuilder.FileEntry;
import com.intellij.plugins.haxe.v2.toolwindow.HaxeToolWindowModelBuilder.InstalledLibrary;
import com.intellij.plugins.haxe.v2.toolwindow.tree.HaxeToolWindowNodes.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.tree.DefaultMutableTreeNode;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Turns the model builder's container entries into the tool window's Swing
 * tree, Gradle-style: one project node holding the project root's groups and
 * the modules. A pure transformation of the prepared model plus the target
 * selection store - no read action needed.
 */
final class HaxeToolWindowTreeBuilder {

  private final Project project;

  HaxeToolWindowTreeBuilder(@NotNull Project project) {
    this.project = project;
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
    parent.add(buildGroupNode(container, installedLibraries));
    if (!container.tools().isEmpty()) {
      parent.add(toolsGroupNode(container));
    }
  }

  @NotNull
  private static DefaultMutableTreeNode compilationGroupNode(@NotNull ContainerEntry container) {
    DefaultMutableTreeNode compilationNode = new DefaultMutableTreeNode(new CompilationGroupNode(container.id()));
    compilationNode.add(new DefaultMutableTreeNode(container.compileCommand()));
    compilationNode.add(new DefaultMutableTreeNode(container.server()));
    return compilationNode;
  }

  @NotNull
  private static DefaultMutableTreeNode environmentNode(@NotNull ContainerEntry container) {
    EnvironmentData environment = container.environment();
    EnvironmentNode environmentRow =
      new EnvironmentNode(container.id(), container.displayName(), environment.activeBuildFileDefines());
    DefaultMutableTreeNode environmentNode = new DefaultMutableTreeNode(environmentRow);

    EnvSdkNode sdkRow = new EnvSdkNode(container.id(), environment.sdkDisplay(), environment.sdkMissing());
    environmentNode.add(new DefaultMutableTreeNode(sdkRow));

    EnvLanguageLevelNode levelRow = new EnvLanguageLevelNode(container.id(), environment.languageLevelDisplay());
    environmentNode.add(new DefaultMutableTreeNode(levelRow));

    EnvDefinesNode definesRow = new EnvDefinesNode(container.id(), environment.defines().size());
    DefaultMutableTreeNode definesNode = new DefaultMutableTreeNode(definesRow);
    for (EnvDefineNode define : environment.defines()) {
      definesNode.add(new DefaultMutableTreeNode(define));
    }
    environmentNode.add(definesNode);

    EnvCustomTargetNode customTargetRow = new EnvCustomTargetNode(container.id(), environment.customTarget());
    environmentNode.add(new DefaultMutableTreeNode(customTargetRow));
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
    boolean frameworkDetected = HaxeTestFrameworks.detectedFramework(fileEntry.info().libraries()) != null;
    return new BuildFileRow(fileEntry.buildFile(), container.id(), active, fileEntry.manual(), tests, frameworkDetected);
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
    fileNode.add(definesGroupNode(entry));
    fileNode.add(librariesGroupNode(entry, installedLibraries));
    fileNode.add(actionsGroupNode(entry));
    if (row.testsFile()) {
      fileNode.add(testsGroupNode(entry));
    }
    return fileNode;
  }

  @NotNull
  private TargetNode targetNode(@NotNull FileEntry entry) {
    HaxeBuildFile buildFile = entry.buildFile();
    if (!HaxeTargetOptions.isTargetSelectable(buildFile.type())) {
      HaxeTarget target = entry.info().target();
      String display = target != null ? target.toString()
                                      : HaxeBundle.message("haxe.toolwindow.node.target.unspecified");
      return new TargetNode(buildFile, display, false);
    }
    String selectedId = HaxeTargetSelectionStore.getInstance(project).getSelectedTargetId(buildFile.file());
    return new TargetNode(buildFile, HaxeTargetOptions.displayNameFor(buildFile.type(), selectedId), true);
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

  @NotNull
  private static DefaultMutableTreeNode librariesGroupNode(@NotNull FileEntry entry,
                                                           @Nullable Map<String, InstalledLibrary> installedLibraries) {
    List<HaxeLibDependency> libraries = entry.info().libraries();
    DefaultMutableTreeNode librariesNode = new DefaultMutableTreeNode(new GroupNode(GroupKind.LIBRARIES, libraries.size()));
    for (HaxeLibDependency library : libraries) {
      librariesNode.add(new DefaultMutableTreeNode(libraryNode(entry.buildFile(), library, installedLibraries)));
    }
    return librariesNode;
  }

  @NotNull
  private static LibraryNode libraryNode(@NotNull HaxeBuildFile buildFile,
                                         @NotNull HaxeLibDependency library,
                                         @Nullable Map<String, InstalledLibrary> installedLibraries) {
    if (installedLibraries == null) {
      return new LibraryNode(buildFile, library.name(), library.version(), null, true);
    }
    InstalledLibrary installedLibrary = installedLibraries.get(library.name().toLowerCase(Locale.ROOT));
    if (installedLibrary == null) {
      return new LibraryNode(buildFile, library.name(), library.version(), null, false);
    }
    // a pinned version must itself be installed - the name alone is not
    // enough. A git checkout is listed as version "git", never as its
    // url#ref spec.
    // TODO: compare a git pin's #ref against the installed checkout
    //       (HaxelibLocalDocs.gitCheckout) and flag mismatches
    String requiredVersion = HaxelibGitSpec.parse(library.version()) != null ? HaxelibSemVer.GIT_SCM : library.version();
    boolean installed = requiredVersion == null || installedLibrary.versions().contains(requiredVersion);
    return new LibraryNode(buildFile, library.name(), library.version(), installedLibrary.selectedVersion(), installed);
  }

  @NotNull
  private static DefaultMutableTreeNode actionsGroupNode(@NotNull FileEntry entry) {
    String launchKind = HaxeProgramLaunches.launchKind(entry.info(), entry.buildFile().type());
    int count = entry.actions().size() + (launchKind != null ? 1 : 0);
    DefaultMutableTreeNode actionsNode = new DefaultMutableTreeNode(
      new ActionsGroupNode(entry.buildFile().file().getPath(), count));
    for (ActionNode action : entry.actions()) {
      actionsNode.add(new DefaultMutableTreeNode(action));
    }
    if (launchKind != null) {
      ProgramNode program =
        new ProgramNode(entry.buildFile(), launchKind, entry.info().target(), entry.info().targetOutput());
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
