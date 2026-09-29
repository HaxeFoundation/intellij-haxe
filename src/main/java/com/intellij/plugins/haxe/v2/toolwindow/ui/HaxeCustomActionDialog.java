package com.intellij.plugins.haxe.v2.toolwindow.ui;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.plugins.haxe.HaxeBundle;
import com.intellij.plugins.haxe.util.ui.HaxeDialogHints;
import com.intellij.plugins.haxe.v2.buildtools.HaxeCustomCommands;
import com.intellij.plugins.haxe.v2.buildtools.settings.HaxeCustomActionsStore.CustomAction;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

/**
 * Add/edit dialog for a custom tool window action: a name, the command line to
 * run and its working directory. A blank work directory falls back to the row
 * kind's default (the build file's directory for actions, the container root
 * for tools); {@code ${moduleRoot}} and {@code ${projectRoot}} expand in both
 * the command and the work directory. The layout lives in the matching .form
 * (labels bind their bundle keys there).
 */
public final class HaxeCustomActionDialog extends DialogWrapper {

  /** The values being edited; action rows and tool rows share the dialog. */
  public record EditedCommand(@NotNull String name, @NotNull String command, @NotNull String workDirectory) {
  }

  private final String nameRequiredKey;

  private JPanel panel;
  private JBTextField nameField;
  private JBTextField commandField;
  private JBTextField workDirectoryField;
  private JTextPane hintArea;

  private HaxeCustomActionDialog(@NotNull Project project, @Nullable EditedCommand initial,
                                 @NotNull String addTitleKey, @NotNull String editTitleKey,
                                 @Nullable String hintKey, @NotNull String nameRequiredKey) {
    super(project);
    this.nameRequiredKey = nameRequiredKey;
    setTitle(HaxeBundle.message(initial == null ? addTitleKey : editTitleKey));
    HaxeDialogHints.style(hintArea);
    if (hintKey != null) {
      hintArea.setText(HaxeBundle.message(hintKey));
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

  /** A build file's custom action: the hint documents work directory and placeholder expansion. */
  @NotNull
  public static HaxeCustomActionDialog forAction(@NotNull Project project, @Nullable CustomAction initial) {
    EditedCommand edited = initial == null
                           ? null
                           : new EditedCommand(initial.name(), initial.command(), initial.workDirectory());
    return new HaxeCustomActionDialog(project, edited,
                                      "haxe.custom.action.dialog.add.title", "haxe.custom.action.dialog.edit.title",
                                      null, "haxe.custom.action.dialog.name.required");
  }

  /** A container's custom tool: no target placeholders, defaults to the container root. */
  @NotNull
  public static HaxeCustomActionDialog forTool(@NotNull Project project, @Nullable EditedCommand initial) {
    return new HaxeCustomActionDialog(project, initial,
                                      "haxe.custom.tool.dialog.add.title", "haxe.custom.tool.dialog.edit.title",
                                      "haxe.custom.tool.dialog.hint", "haxe.custom.tool.dialog.name.required");
  }

  @Override
  protected @NotNull JComponent createCenterPanel() {
    panel.setPreferredSize(JBUI.size(480, -1));
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
    if (StringUtil.isEmptyOrSpaces(commandField.getText())) {
      return new ValidationInfo(HaxeBundle.message("haxe.custom.action.dialog.command.required"), commandField);
    }
    return null;
  }

  @NotNull
  public CustomAction getAction() {
    EditedCommand edited = getEditedCommand();
    return new CustomAction(edited.name(), edited.command(), edited.workDirectory());
  }

  /** The edited values, independent of which store they land in. */
  @NotNull
  public EditedCommand getEditedCommand() {
    return new EditedCommand(nameField.getText().trim(), commandField.getText().trim(),
                             workDirectoryField.getText().trim());
  }
}
