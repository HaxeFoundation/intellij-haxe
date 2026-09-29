package com.intellij.plugins.haxe.v2.toolwindow.tree;

import com.intellij.execution.runners.ExecutionUtil;
import com.intellij.icons.AllIcons;
import com.intellij.plugins.haxe.HaxeBundle;
import com.intellij.plugins.haxe.haxelib.HaxelibGitSpec;
import com.intellij.plugins.haxe.v2.buildtools.settings.DefineEffect;
import com.intellij.plugins.haxe.v2.toolwindow.tree.HaxeToolWindowNodes.*;
import com.intellij.ui.ColoredTreeCellRenderer;
import com.intellij.ui.SimpleTextAttributes;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;

/**
 * Renders the Haxe tool window tree. Most rows take one of three shapes - a
 * label, a label with its grayed value (plus a ▾ marker where clicking opens a
 * chooser), or a group label with its child count; rows with more to say
 * (build files, the server, the SDK, defines, libraries) render themselves.
 * Missing libraries and SDKs are shown in error color.
 */
public final class HaxeToolWindowTreeRenderer extends ColoredTreeCellRenderer {

  private static final SimpleTextAttributes STRIKEOUT_ATTRIBUTES =
    new SimpleTextAttributes(SimpleTextAttributes.STYLE_STRIKEOUT, null);
  private static final String CHOOSER_MARKER = " ▾";

  @Override
  public void customizeCellRenderer(@NotNull JTree tree,
                                    Object value,
                                    boolean selected,
                                    boolean expanded,
                                    boolean leaf,
                                    int row,
                                    boolean hasFocus) {
    if (!(value instanceof DefaultMutableTreeNode node)) return;

    Object userObject = node.getUserObject();
    // the renderer instance is shared across rows - a stale tooltip must not leak
    setToolTipText(tooltipFor(userObject));
    switch (userObject) {
      case ModuleNode module -> labelRow(AllIcons.Nodes.Module, module.name());
      case ProjectNode project -> labelRow(AllIcons.Nodes.Project, project.name());
      case BuildFileRow fileRow -> renderBuildFile(fileRow);
      case TargetNode target ->
        valueRow(AllIcons.RunConfigurations.Application, HaxeBundle.message("haxe.toolwindow.node.target"),
                 target.displayName(), target.selectable());
      case SectionNode section ->
        valueRow(AllIcons.RunConfigurations.Compound, HaxeBundle.message("haxe.toolwindow.node.section"), section.displayName(), true);
      case GroupNode group -> countRow(groupIcon(group.kind()), groupLabel(group.kind()), group.count());
      case DefineNode define -> renderDefine(define.name(), define.value());
      case EnvironmentNode ignored -> labelRow(AllIcons.General.Settings, HaxeBundle.message("haxe.toolwindow.node.environment"));
      case BuildGroupNode buildGroup ->
        countRow(AllIcons.Nodes.Folder, HaxeBundle.message("haxe.toolwindow.node.build"), buildGroup.count());
      case CompilationGroupNode ignored -> labelRow(AllIcons.General.Settings, HaxeBundle.message("haxe.toolwindow.node.compilation"));
      case CompilationServerNode server -> renderServer(server);
      case ActionsGroupNode actionsGroup ->
        countRow(AllIcons.Nodes.ConfigFolder, HaxeBundle.message("haxe.toolwindow.node.actions"), actionsGroup.count());
      case TestsGroupNode ignored -> labelRow(AllIcons.Nodes.TestSourceFolder, HaxeBundle.message("haxe.toolwindow.node.tests.group"));
      case ActionNode action -> valueRow(actionIcon(action.name()), action.name(), action.presentableCommand(), false);
      case ToolsGroupNode toolsGroup ->
        countRow(AllIcons.General.ExternalTools, HaxeBundle.message("haxe.toolwindow.node.tools"), toolsGroup.count());
      case ToolNode tool -> valueRow(AllIcons.General.ExternalTools, tool.name(), tool.detail(), false);
      case ProgramNode program ->
        valueRow(AllIcons.Actions.Execute, HaxeBundle.message("haxe.toolwindow.node.program"), program.kind(), false);
      case TestRunNode ignored ->
        labelRow(AllIcons.RunConfigurations.TestState.Run, HaxeBundle.message("haxe.toolwindow.node.run.unit.tests"));
      case EnvSdkNode sdk -> renderSdk(sdk);
      case EnvCompileCommandNode compileCommand ->
        valueRow(AllIcons.Actions.Compile, HaxeBundle.message("haxe.toolwindow.node.compile.command"), compileCommand.display(), true);
      case EnvLanguageLevelNode level ->
        valueRow(AllIcons.Nodes.Property, HaxeBundle.message("haxe.toolwindow.node.environment.language.level"), level.displayName(), true);
      case EnvDefinesNode defines ->
        countRow(AllIcons.Nodes.Folder, HaxeBundle.message("haxe.toolwindow.node.environment.defines"), defines.count());
      case EnvCustomTargetNode customTarget ->
        valueRow(AllIcons.Nodes.Property, HaxeBundle.message("haxe.toolwindow.node.environment.custom.target"),
                 customTargetDisplay(customTarget), true);
      case EnvDefineNode define -> renderEnvironmentDefine(define);
      case LibraryNode library -> renderLibrary(library);
      case null -> { }
      default -> append(String.valueOf(userObject));
    }
  }

  private void labelRow(@NotNull Icon icon, @NotNull String label) {
    setIcon(icon);
    append(label);
  }

  /** The label, then its current value grayed; a {@code chooser} row adds the dropdown marker. */
  private void valueRow(@NotNull Icon icon, @NotNull String label, @NotNull String value, boolean chooser) {
    setIcon(icon);
    append(label);
    append("  " + value, SimpleTextAttributes.GRAYED_ATTRIBUTES);
    if (chooser) {
      append(CHOOSER_MARKER, SimpleTextAttributes.GRAYED_ATTRIBUTES);
    }
  }

  private void countRow(@NotNull Icon icon, @NotNull String label, int count) {
    setIcon(icon);
    append(label);
    append(" (" + count + ")", SimpleTextAttributes.GRAYED_ATTRIBUTES);
  }

  private void renderBuildFile(@NotNull BuildFileRow fileRow) {
    setIcon(fileRow.buildFile().type().getIcon());
    String name = fileRow.buildFile().file().getName();
    if (fileRow.active()) {
      append(name, SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES);
      append("  " + HaxeBundle.message("haxe.toolwindow.node.build.file.active"), SimpleTextAttributes.GRAYED_ATTRIBUTES);
    }
    else {
      append(name);
    }
    if (fileRow.testsFile()) {
      append("  " + HaxeBundle.message("haxe.toolwindow.node.build.file.tests"), SimpleTextAttributes.GRAYED_ATTRIBUTES);
    }
  }

  private void renderServer(@NotNull CompilationServerNode server) {
    Icon icon = server.running() ? ExecutionUtil.getLiveIndicator(AllIcons.Webreferences.Server) : AllIcons.Webreferences.Server;
    valueRow(icon, HaxeBundle.message("haxe.toolwindow.node.server"), server.display(), true);
    if (server.contextFailure() != null) {
      String failing = "  " + HaxeBundle.message("haxe.toolwindow.server.context.failing");
      append(failing, SimpleTextAttributes.ERROR_ATTRIBUTES, new ServerFailureLink(server.containerId()));
    }
  }

  private void renderSdk(@NotNull EnvSdkNode sdk) {
    setIcon(AllIcons.Nodes.PpJdk);
    append(HaxeBundle.message("haxe.toolwindow.node.environment.sdk"));
    var attributes = sdk.missing() ? SimpleTextAttributes.ERROR_ATTRIBUTES : SimpleTextAttributes.GRAYED_ATTRIBUTES;
    append("  " + sdk.displayName(), attributes);
    append(CHOOSER_MARKER, SimpleTextAttributes.GRAYED_ATTRIBUTES);
  }

  private void renderDefine(@NotNull String name, @Nullable String value) {
    setIcon(AllIcons.Nodes.Property);
    append(name);
    if (value != null && !value.isEmpty()) {
      append(" = " + value, SimpleTextAttributes.GRAYED_ATTRIBUTES);
    }
  }

  private void renderEnvironmentDefine(@NotNull EnvDefineNode define) {
    if (define.effect() == DefineEffect.REMOVE) {
      setIcon(AllIcons.Nodes.Property);
      append(define.name(), STRIKEOUT_ATTRIBUTES);
      append("  " + HaxeBundle.message("haxe.environment.effect.remove"), SimpleTextAttributes.GRAYED_ATTRIBUTES);
      return;
    }
    renderDefine(define.name(), define.value());
    if (define.inBuildFile()) {
      append("  " + HaxeBundle.message("haxe.toolwindow.node.define.overriding"), SimpleTextAttributes.GRAYED_ATTRIBUTES);
    }
  }

  private void renderLibrary(@NotNull LibraryNode library) {
    setIcon(AllIcons.Nodes.PpLib);
    var attributes = library.installed() ? SimpleTextAttributes.REGULAR_ATTRIBUTES : SimpleTextAttributes.ERROR_ATTRIBUTES;
    append(library.name(), attributes);
    if (library.displayVersion() != null) {
      append(" : " + library.displayVersion(), SimpleTextAttributes.GRAYED_ATTRIBUTES);
    }
    if (!library.installed()) {
      // a resolved version means the library name IS installed - only the pinned version is absent
      String missing = library.resolvedVersion() != null
                       ? HaxeBundle.message("haxe.toolwindow.node.library.version.missing")
                       : HaxeBundle.message("haxe.toolwindow.node.library.missing");
      append("  " + missing, SimpleTextAttributes.GRAYED_ATTRIBUTES);
    }
  }

  /** What a row means, shown as its tooltip; null for self-explanatory rows. */
  @Nullable
  private static String tooltipFor(@Nullable Object userObject) {
    return switch (userObject) {
      case CompilationGroupNode ignored -> HaxeBundle.message("haxe.toolwindow.tooltip.compilation");
      case EnvCompileCommandNode ignored -> HaxeBundle.message("haxe.toolwindow.tooltip.compile.command");
      case CompilationServerNode server -> serverTooltip(server);
      case EnvironmentNode ignored -> HaxeBundle.message("haxe.toolwindow.tooltip.environment");
      case EnvSdkNode ignored -> HaxeBundle.message("haxe.toolwindow.tooltip.environment.sdk");
      case EnvLanguageLevelNode ignored -> HaxeBundle.message("haxe.toolwindow.tooltip.environment.language.level");
      case EnvDefinesNode ignored -> HaxeBundle.message("haxe.toolwindow.tooltip.environment.defines");
      case EnvCustomTargetNode ignored -> HaxeBundle.message("haxe.environment.dialog.custom.target.tooltip");
      case BuildGroupNode ignored -> HaxeBundle.message("haxe.toolwindow.tooltip.build.files");
      case TargetNode target ->
        HaxeBundle.message(target.selectable() ? "haxe.toolwindow.tooltip.target.selectable" : "haxe.toolwindow.tooltip.target");
      case GroupNode group ->
        HaxeBundle.message(group.kind() == GroupKind.LIBRARIES ? "haxe.toolwindow.tooltip.libraries" : "haxe.toolwindow.tooltip.defines");
      case ActionsGroupNode ignored -> HaxeBundle.message("haxe.toolwindow.tooltip.actions");
      case TestsGroupNode ignored -> HaxeBundle.message("haxe.toolwindow.tooltip.tests.group");
      case LibraryNode library -> libraryTooltip(library);
      case SectionNode ignored -> HaxeBundle.message("haxe.toolwindow.tooltip.section");
      case ProgramNode program -> HaxeBundle.message("haxe.toolwindow.tooltip.program", program.kind());
      case TestRunNode ignored -> HaxeBundle.message("haxe.toolwindow.tooltip.run.unit.tests");
      case null, default -> null;
    };
  }

  @NotNull
  private static String serverTooltip(@NotNull CompilationServerNode server) {
    if (server.contextFailure() != null) {
      return HaxeBundle.message("haxe.toolwindow.tooltip.server.context.failing", server.contextFailure());
    }
    return server.connectEligible() ? HaxeBundle.message("haxe.toolwindow.tooltip.server")
                                    : HaxeBundle.message("haxe.toolwindow.tooltip.server.not.connectable");
  }

  /** A git-pinned library shows only {@code git#ref} in the row; the tooltip carries the repository url. */
  @Nullable
  private static String libraryTooltip(@NotNull LibraryNode library) {
    HaxelibGitSpec gitSpec = HaxelibGitSpec.parse(library.version());
    return gitSpec == null ? null : HaxeBundle.message("haxe.toolwindow.tooltip.library.git", gitSpec.url());
  }

  @NotNull
  private static String customTargetDisplay(@NotNull EnvCustomTargetNode customTarget) {
    return customTarget.customTarget() != null
           ? customTarget.customTarget()
           : HaxeBundle.message("haxe.toolwindow.node.target.unspecified");
  }

  /** Actions that only produce output get the build hammer; ones that run something keep the play icon. */
  @NotNull
  private static Icon actionIcon(@NotNull String actionName) {
    boolean buildAction = actionName.equalsIgnoreCase("build")
                          || actionName.equalsIgnoreCase("compile")
                          || actionName.equalsIgnoreCase("clean");
    return buildAction ? AllIcons.Actions.Compile : AllIcons.Actions.Execute;
  }

  @NotNull
  private static Icon groupIcon(@NotNull GroupKind kind) {
    return kind == GroupKind.LIBRARIES ? AllIcons.Nodes.PpLibFolder : AllIcons.Nodes.Folder;
  }

  @NotNull
  private static String groupLabel(@NotNull GroupKind kind) {
    return kind == GroupKind.LIBRARIES
           ? HaxeBundle.message("haxe.toolwindow.node.libraries")
           : HaxeBundle.message("haxe.toolwindow.node.defines");
  }
}
