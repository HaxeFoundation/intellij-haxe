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
 * (Default = the container's default build file, None = the container is skipped
 * on project build), optionally one of the file's actions as command override,
 * and extra arguments such as "-clean -debug". OK hands the result to the caller
 * through {@link #selectedCompileCommand()} and {@link #isSkipSelected()}. The
 * layout lives in the matching .form (labels bind their bundle keys there).
 */
public final class HaxeCompileCommandDialog extends DialogWrapper {

  // the file combo's "skip on project build" item; no build file path is empty
  private static final String SKIP_ITEM = "";

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
                                  @NotNull Map<String, List<String>> actionNamesByFile,
                                  @Nullable String defaultFilePath) {
    super(project);
    this.project = project;
    this.containerId = containerId;
    this.actionNamesByFile = actionNamesByFile;

    setTitle(HaxeBundle.message("haxe.compile.command.dialog.title"));
    HaxeDialogHints.style(hintArea);
    String defaultItem = defaultItemText(defaultFilePath);
    fileCombo.setRenderer(BuilderKt.textListCellRenderer(defaultItem, HaxeCompileCommandDialog::fileItemText));
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

  @NotNull
  private static String defaultItemText(@Nullable String defaultFilePath) {
    if (defaultFilePath == null) return HaxeBundle.message("haxe.compile.command.dialog.default.none");
    return HaxeBundle.message("haxe.compile.command.dialog.default", PathUtil.getFileName(defaultFilePath));
  }

  @NotNull
  private static String fileItemText(@NotNull String item) {
    return SKIP_ITEM.equals(item) ? HaxeBundle.message("haxe.compile.command.dialog.none") : PathUtil.getFileName(item);
  }

  private void fillFields(@NotNull List<String> candidateFilePaths) {
    DefaultComboBoxModel<String> fileModel = new DefaultComboBoxModel<>();
    fileModel.addElement(null);
    fileModel.addElement(SKIP_ITEM);
    candidateFilePaths.forEach(fileModel::addElement);

    HaxeEnvironmentStore store = HaxeEnvironmentStore.getInstance(project);
    CompileCommand stored = store.getCompileCommand(containerId);
    if (stored != null && fileModel.getIndexOf(stored.buildFilePath()) < 0) {
      fileModel.addElement(stored.buildFilePath());
    }
    fileCombo.setModel(fileModel);
    fileCombo.setSelectedItem(selectedFileItem(store, stored));
    fileCombo.addActionListener(e -> refillCommandCombo(null));

    refillCommandCombo(stored == null ? null : stored.actionName());
    argumentsField.setText(stored == null ? "" : stored.arguments());
  }

  /** The file item the stored state selects: skip, the command's file, or the default. */
  @Nullable
  private String selectedFileItem(@NotNull HaxeEnvironmentStore store, @Nullable CompileCommand stored) {
    if (store.isBuildSkipped(containerId)) return SKIP_ITEM;
    return stored == null ? null : stored.buildFilePath();
  }

  private void refillCommandCombo(@Nullable String selectedActionName) {
    DefaultComboBoxModel<String> commandModel = new DefaultComboBoxModel<>();
    commandModel.addElement(null);
    String selectedFile = selectedBuildFilePath();
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

  /// The dialog state as a command; null (= the default, or skipping the build) when no build file is selected.
  @Nullable
  public CompileCommand selectedCompileCommand() {
    String selectedPath = selectedBuildFilePath();
    if (selectedPath == null) return null;
    String arguments = StringUtil.notNullize(argumentsField.getText()).trim();
    return new CompileCommand(selectedPath, (String)commandCombo.getSelectedItem(), arguments);
  }

  /// Whether the container is to be skipped on project build.
  public boolean isSkipSelected() {
    return SKIP_ITEM.equals(fileCombo.getSelectedItem());
  }

  /// The selected build file; null for the default and for skipping the build.
  @Nullable
  private String selectedBuildFilePath() {
    String selected = (String)fileCombo.getSelectedItem();
    return SKIP_ITEM.equals(selected) ? null : selected;
  }
}
