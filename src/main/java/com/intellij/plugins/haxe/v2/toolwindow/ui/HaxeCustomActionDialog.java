package com.intellij.plugins.haxe.v2.toolwindow.ui;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.plugins.haxe.HaxeBundle;
import com.intellij.plugins.haxe.util.ui.HaxeDialogHints;
import com.intellij.plugins.haxe.v2.buildtools.*;
import com.intellij.plugins.haxe.v2.buildtools.settings.*;
import com.intellij.plugins.haxe.v2.buildtools.settings.HaxeCustomActionsStore.RunBeforeRefusal;
import com.intellij.plugins.haxe.v2.buildtools.settings.HaxeCustomActionsStore.CustomAction;
import com.intellij.plugins.haxe.v2.buildtools.settings.HaxeCustomActionsStore.ActionReference;
import com.intellij.plugins.haxe.v2.buildsystem.HaxeBuildFile;
import com.intellij.plugins.haxe.util.HaxeReadActions;
import com.intellij.plugins.haxe.v2.buildtools.settings.HaxeCustomActionsStore.Launch;
import com.intellij.plugins.haxe.v2.runconfig.HaxeLaunchKind;
import com.intellij.plugins.haxe.v2.runconfig.HaxeLaunchKind.Parameter;
import com.intellij.plugins.haxe.v2.runconfig.HaxeProgramLaunches;
import com.intellij.plugins.haxe.v2.toolwindow.HaxeDetectedTools;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.function.Function;
import java.util.HashSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.Map;
import java.awt.BorderLayout;

/**
 * Add/edit dialog for a custom tool window action: a name, the command line to
 * run and its working directory, and for a build file's action the optional
 * run configuration it creates or starts ({@link HaxeLaunchSection}). A blank
 * work directory falls back to the row
 * kind's default (the build file's work directory for actions, the container root
 * for tools); {@code ${moduleRoot}} and {@code ${projectRoot}} expand in both
 * the command and the work directory. The layout lives in the matching .form
 * (labels bind their bundle keys there).
 */
public final class HaxeCustomActionDialog extends DialogWrapper {

  /** The values being edited; action rows and tool rows share the dialog. */
  public record EditedCommand(@NotNull String name, @NotNull String command, @NotNull String workDirectory) {
  }

  /** The width the hint wraps at: the dialog's width less the label column, on the narrow side so no line is clipped. */
  private static final int HINT_WIDTH = 400;

  private final String nameRequiredKey;
  private final String nameTakenKey;
  /** The names the edited action or tool may not take; see {@link #takenNames(Project, HaxeBuildFile, String)} and {@link #takenToolNames}. */
  private final Set<String> takenNames;
  /** The launch section of a build file's action; null for a tool. */
  private final HaxeLaunchSection launchSection;
  /** The run-before section of a build file's action; null for a tool. */
  private final HaxeRunBeforeSection runBeforeSection;

  private JPanel panel;
  private JBTextField nameField;
  private JBTextField commandField;
  private JBTextField workDirectoryField;
  private JPanel runBeforePanel;
  private JPanel launchPanel;
  private JTextPane hintArea;

  private HaxeCustomActionDialog(@NotNull Project project, @Nullable EditedCommand initial, boolean adding,
                                 @NotNull String addTitleKey, @NotNull String editTitleKey,
                                 @Nullable String hintKey, @NotNull String nameRequiredKey, @NotNull String nameTakenKey,
                                 @NotNull Set<String> takenNames, @Nullable HaxeLaunchSection launchSection,
                                 @Nullable HaxeRunBeforeSection runBeforeSection) {
    super(project);
    this.nameRequiredKey = nameRequiredKey;
    this.nameTakenKey = nameTakenKey;
    this.takenNames = takenNames;
    this.launchSection = launchSection;
    this.runBeforeSection = runBeforeSection;
    setTitle(HaxeBundle.message(adding ? addTitleKey : editTitleKey));
    HaxeDialogHints.style(hintArea);
    if (hintKey != null) {
      hintArea.setText(HaxeBundle.message(hintKey));
    }
    HaxeDialogHints.fitHeight(hintArea, JBUI.scale(HINT_WIDTH));
    if (launchSection != null) {
      launchPanel.add(launchSection.getComponent(), BorderLayout.CENTER);
    }
    else {
      launchPanel.setVisible(false);
    }
    if (runBeforeSection != null) {
      runBeforeSection.setCurrentName(() -> nameField.getText().trim());
      runBeforePanel.add(runBeforeSection.getComponent(), BorderLayout.CENTER);
    }
    else {
      runBeforePanel.setVisible(false);
    }
    if (initial != null) {
      nameField.setText(initial.name());
      commandField.setText(initial.command());
      workDirectoryField.setText(initial.workDirectory());
    }
    else {
      workDirectoryField.setText(HaxeCustomCommands.MODULE_ROOT_VARIABLE);
    }
    init();
  }

  /**
   * A build file's custom action: the hint documents work directory and
   * placeholder expansion; the launch section offers the kinds with the
   * defaults derived from the file's current target output ({@code launchOutput},
   * null when the file produces nothing launchable).
   */
  @NotNull
  public static HaxeCustomActionDialog forAction(@NotNull Project project,
                                                 @NotNull HaxeBuildFile buildFile,
                                                 @Nullable String launchOutput,
                                                 @Nullable CustomAction initial) {
    return forAction(project, buildFile, launchOutput, initial, initial == null);
  }

  /** A copy of a row as a NEW action: the Add title over pre-filled values. */
  @NotNull
  public static HaxeCustomActionDialog forDuplicate(@NotNull Project project,
                                                    @NotNull HaxeBuildFile buildFile,
                                                    @Nullable String launchOutput,
                                                    @NotNull CustomAction template) {
    return forAction(project, buildFile, launchOutput, template, true);
  }

  @NotNull
  private static HaxeCustomActionDialog forAction(@NotNull Project project,
                                                  @NotNull HaxeBuildFile buildFile,
                                                  @Nullable String launchOutput,
                                                  @Nullable CustomAction initial,
                                                  boolean adding) {
    EditedCommand edited = initial == null
                           ? null
                           : new EditedCommand(initial.name(), initial.command(), initial.workDirectory());
    Function<HaxeLaunchKind, Map<Parameter, String>> defaults = kind -> launchDefaults(project, buildFile, launchOutput, kind);
    HaxeLaunchSection launchSection = new HaxeLaunchSection(project, defaults, initial == null ? null : initial.launch());
    Set<String> takenNames = takenNames(project, buildFile, adding || initial == null ? null : initial.name());
    String path = buildFile.file().getPath();
    List<ActionReference> runBefore = initial == null ? List.of() : initial.runBefore();
    HaxeRunBeforeSection runBeforeSection = new HaxeRunBeforeSection(name -> runBeforeCandidates(project, buildFile, name),
                                                                     reference -> HaxeActionReferences.label(project, path, reference),
                                                                     reference -> !HaxeActionReferences.exists(project, path, reference),
                                                                     buildFilePath -> HaxeActionReferences.location(project, buildFilePath),
                                                                     (name, reference) -> refusalMessage(project, buildFile, name, reference),
                                                                     runBefore);
    return new HaxeCustomActionDialog(project, edited, adding,
                                      "haxe.custom.action.dialog.add.title", "haxe.custom.action.dialog.edit.title",
                                      null, "haxe.custom.action.dialog.name.required", "haxe.custom.action.dialog.name.taken",
                                      takenNames, launchSection, runBeforeSection);
  }

  /**
   * The actions offered to run before the edited one (by the name the dialog
   * shows): its own build file's built-in and custom actions, then every
   * other build file's, without the edited action itself. A pick that would
   * loop is refused with a message ({@link #refusalMessage}) rather than
   * left out silently.
   */
  @NotNull
  private static List<ActionReference> runBeforeCandidates(@NotNull Project project,
                                                           @NotNull HaxeBuildFile buildFile,
                                                           @NotNull String editedName) {
    String path = buildFile.file().getPath();
    List<ActionReference> candidates = new ArrayList<>();
    HaxeCompileCommands.availableActionNames(project, buildFile.file()).forEach(name -> candidates.add(new ActionReference(null, name)));
    for (HaxeBuildFile other : HaxeReadActions.compute(() -> HaxeKnownBuildFiles.all(project))) {
      String otherPath = other.file().getPath();
      if (otherPath.equals(path)) continue;
      HaxeCompileCommands.availableActionNames(project, other.file()).forEach(name -> candidates.add(new ActionReference(otherPath, name)));
    }
    candidates.removeIf(candidate -> isEdited(candidate, editedName));
    return candidates;
  }

  /** Why the pick cannot run before the edited action, or null when it may. */
  @Nullable
  private static String refusalMessage(@NotNull Project project,
                                       @NotNull HaxeBuildFile buildFile,
                                       @NotNull String editedName,
                                       @NotNull ActionReference candidate) {
    String path = buildFile.file().getPath();
    RunBeforeRefusal refusal = HaxeCustomActionsStore.getInstance(project).refusal(candidate.qualifiedFor(path), path, editedName);
    if (refusal == null) return null;
    String label = HaxeActionReferences.label(project, path, candidate);
    return switch (refusal) {
      case RUNS_IT_FIRST -> HaxeBundle.message("haxe.custom.action.dialog.run.before.refused.runs.first", label, editedName);
      case LOOPS -> HaxeBundle.message("haxe.custom.action.dialog.run.before.refused.loops", label);
    };
  }

  private static boolean isEdited(@NotNull ActionReference candidate, @NotNull String editedName) {
    return candidate.buildFile() == null && candidate.action().equals(editedName);
  }

  /**
   * The build file's own action names (the build step resolves them before
   * any custom one) and its other custom actions' names (the store keeps one
   * action per name); {@code editedName} is the edited action's current name,
   * still free to keep.
   */
  @NotNull
  public static Set<String> takenNames(@NotNull Project project, @NotNull HaxeBuildFile buildFile, @Nullable String editedName) {
    Set<String> taken = new HashSet<>(HaxeBuildSystem.of(buildFile.type()).actionNames(project, buildFile));
    for (CustomAction custom : HaxeCustomActionsStore.getInstance(project).getActions(buildFile.file().getPath())) {
      if (!custom.name().equals(editedName)) taken.add(custom.name());
    }
    return Set.copyOf(taken);
  }

  /** A container's custom tool: no target placeholders, defaults to the container root, no launch; the container's other tool names are taken. */
  @NotNull
  public static HaxeCustomActionDialog forTool(@NotNull Project project, @NotNull String containerId, @Nullable EditedCommand initial) {
    Set<String> takenNames = takenToolNames(project, containerId, initial == null ? null : initial.name());
    return new HaxeCustomActionDialog(project, initial, initial == null,
                                      "haxe.custom.tool.dialog.add.title", "haxe.custom.tool.dialog.edit.title",
                                      "haxe.custom.tool.dialog.hint", "haxe.custom.tool.dialog.name.required",
                                      "haxe.custom.tool.dialog.name.taken", takenNames, null, null);
  }

  /**
   * The names of the container's detected tools (fixed rows) and of its other
   * custom tools (the store keeps one tool per name); {@code editedName} is
   * the edited tool's current name, still free to keep.
   */
  @NotNull
  private static Set<String> takenToolNames(@NotNull Project project, @NotNull String containerId, @Nullable String editedName) {
    Set<String> taken = new HashSet<>();
    VirtualFile rootDir = HaxeContainers.containerRootDir(project, containerId);
    if (rootDir != null) taken.addAll(HaxeDetectedTools.namesIn(rootDir));
    for (HaxeCustomToolsStore.CustomTool tool : HaxeCustomToolsStore.getInstance(project).getTools(containerId)) {
      if (!tool.name().equals(editedName)) taken.add(tool.name());
    }
    return Set.copyOf(taken);
  }

  @NotNull
  private static Map<Parameter, String> launchDefaults(@NotNull Project project,
                                                       @NotNull HaxeBuildFile buildFile,
                                                       @Nullable String launchOutput,
                                                       @NotNull HaxeLaunchKind kind) {
    if (launchOutput == null) return Map.of();
    return HaxeProgramLaunches.defaultParameters(project, buildFile, kind, launchOutput);
  }

  /** The form's launch and run-before slots; the sections' panels land in them when the dialog edits a build file's action. */
  private void createUIComponents() {
    runBeforePanel = new JPanel(new BorderLayout());
    launchPanel = new JPanel(new BorderLayout());
  }

  @Override
  protected @NotNull JComponent createCenterPanel() {
    panel.setPreferredSize(JBUI.size(560, -1));
    return panel;
  }

  @Override
  public @Nullable JComponent getPreferredFocusedComponent() {
    return nameField;
  }

  @Override
  protected @Nullable ValidationInfo doValidate() {
    if (StringUtil.isEmptyOrSpaces(nameField.getText())) {
      return new ValidationInfo(HaxeBundle.message(nameRequiredKey), nameField);
    }
    String name = nameField.getText().trim();
    if (takenNames.contains(name)) {
      return new ValidationInfo(HaxeBundle.message(nameTakenKey, name), nameField);
    }
    // a bare trigger of an existing configuration and a chain of other actions need no command of their own
    boolean triggersExisting = launchSection != null && launchSection.launch() instanceof Launch.Existing;
    boolean chainsActions = runBeforeSection != null && !runBeforeSection.actions().isEmpty();
    if (StringUtil.isEmptyOrSpaces(commandField.getText()) && !triggersExisting && !chainsActions) {
      // a tool has no run-before actions to offer instead
      String messageKey = runBeforeSection == null
                          ? "haxe.custom.action.dialog.command.required"
                          : "haxe.custom.action.dialog.command.or.run.before.required";
      return new ValidationInfo(HaxeBundle.message(messageKey), commandField);
    }
    return null;
  }

  @NotNull
  public CustomAction getAction() {
    EditedCommand edited = getEditedCommand();
    Launch launch = launchSection == null ? null : launchSection.launch();
    List<ActionReference> runBefore = runBeforeSection == null ? List.of() : runBeforeSection.actions();
    return new CustomAction(edited.name(), edited.command(), edited.workDirectory(), launch, runBefore);
  }

  /** The edited values, independent of which store they land in. */
  @NotNull
  public EditedCommand getEditedCommand() {
    return new EditedCommand(nameField.getText().trim(), commandField.getText().trim(),
                             workDirectoryField.getText().trim());
  }
}
