package com.intellij.plugins.haxe.v2.toolwindow.ui;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.plugins.haxe.HaxeBundle;
import com.intellij.plugins.haxe.util.ui.HaxeDialogHints;
import com.intellij.plugins.haxe.v2.buildtools.settings.HaxeEnvironmentStore;
import com.intellij.plugins.haxe.v2.buildtools.settings.HaxeEnvironmentStore.CompileCommand;
import com.intellij.ui.components.JBTextField;
import com.intellij.ui.dsl.listCellRenderer.BuilderKt;
import com.intellij.util.PathUtil;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.List;
import java.util.Map;

/**
 * Configures a container's compile command: which build file compiling runs
 * (None = the container is skipped on project build), optionally one of the file's
 * actions as command override, and extra arguments such as "-clean -debug".
 * The layout lives in the matching .form (labels bind their bundle keys there).
 */
public final class HaxeCompileCommandDialog extends DialogWrapper {

  private final Project project;
  private final String containerId;
  private final Map<String, List<String>> actionNamesByFile;

  private JPanel panel;
  private ComboBox<String> fileCombo;
  private ComboBox<String> commandCombo;
  private JBTextField argumentsField;
  private JTextPane hintArea;

  public HaxeCompileCommandDialog(@NotNull Project project,
                                  @NotNull String containerId,
                                  @NotNull List<String> candidateFilePaths,
                                  @NotNull Map<String, List<String>> actionNamesByFile) {
    super(project);
    this.project = project;
    this.containerId = containerId;
    this.actionNamesByFile = actionNamesByFile;

    setTitle(HaxeBundle.message("haxe.compile.command.dialog.title"));
    HaxeDialogHints.style(hintArea);
    fileCombo.setRenderer(BuilderKt.textListCellRenderer(
      HaxeBundle.message("haxe.compile.command.dialog.none"), PathUtil::getFileName));
    commandCombo.setRenderer(BuilderKt.textListCellRenderer(
      HaxeBundle.message("haxe.compile.command.dialog.default.command"), name -> name));
    fillFields(candidateFilePaths);
    init();
  }

  @Override
  protected @NotNull JComponent createCenterPanel() {
    panel.setPreferredSize(JBUI.size(480, -1));
    return panel;
  }

  private void fillFields(@NotNull List<String> candidateFilePaths) {
    DefaultComboBoxModel<String> fileModel = new DefaultComboBoxModel<>();
    fileModel.addElement(null);
    candidateFilePaths.forEach(fileModel::addElement);

    CompileCommand stored = HaxeEnvironmentStore.getInstance(project).getCompileCommand(containerId);
    if (stored != null && fileModel.getIndexOf(stored.buildFilePath()) < 0) {
      fileModel.addElement(stored.buildFilePath());
    }
    fileCombo.setModel(fileModel);
    fileCombo.setSelectedItem(stored == null ? null : stored.buildFilePath());
    fileCombo.addActionListener(e -> refillCommandCombo(null));

    refillCommandCombo(stored == null ? null : stored.actionName());
    argumentsField.setText(stored == null ? "" : stored.arguments());
  }

  private void refillCommandCombo(@Nullable String selectedActionName) {
    DefaultComboBoxModel<String> commandModel = new DefaultComboBoxModel<>();
    commandModel.addElement(null);
    String selectedFile = (String)fileCombo.getSelectedItem();
    if (selectedFile != null) {
      actionNamesByFile.getOrDefault(selectedFile, List.of()).forEach(commandModel::addElement);
    }
    if (selectedActionName != null && commandModel.getIndexOf(selectedActionName) < 0) {
      commandModel.addElement(selectedActionName);
    }
    commandCombo.setModel(commandModel);
    commandCombo.setSelectedItem(selectedActionName);
    commandCombo.setEnabled(selectedFile != null);
  }

  /// The dialog state as a command; null (= clear) when no build file is selected.
  private CompileCommand selectedCompileCommand() {
    String selectedPath = (String)fileCombo.getSelectedItem();
    if (selectedPath == null) return null;
    String arguments = StringUtil.notNullize(argumentsField.getText()).trim();
    return new CompileCommand(selectedPath, (String)commandCombo.getSelectedItem(), arguments);
  }

  @Override
  protected void doOKAction() {
    HaxeEnvironmentStore.getInstance(project).setCompileCommand(containerId, selectedCompileCommand());
    super.doOKAction();
  }
}
