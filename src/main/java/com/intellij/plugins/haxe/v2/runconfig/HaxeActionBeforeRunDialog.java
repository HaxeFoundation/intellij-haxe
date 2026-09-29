package com.intellij.plugins.haxe.v2.runconfig;

import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.plugins.haxe.HaxeDebuggerBundle;
import com.intellij.plugins.haxe.v2.runconfig.HaxeActionBeforeRunTaskProvider.Task;
import com.intellij.ui.DocumentAdapter;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBTextField;
import com.intellij.ui.dsl.listCellRenderer.BuilderKt;
import com.intellij.util.ui.FormBuilder;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import java.util.List;

/**
 * Configures a "Run Haxe action" before-launch step: the build file, the action to
 * run and extra arguments. Applies to the task on OK.
 */
final class HaxeActionBeforeRunDialog extends DialogWrapper {

  private final Project project;
  private final Task task;
  private final TextFieldWithBrowseButton fileField = new TextFieldWithBrowseButton();
  private final ComboBox<String> actionCombo = new ComboBox<>();
  private final JBTextField argumentsField = new JBTextField();
  private final JBCheckBox injectDebugCheckBox =
    new JBCheckBox(HaxeDebuggerBundle.message("haxe.before.run.dialog.inject.debug"));
  private final JBLabel injectDebugPreview = new JBLabel("", UIUtil.ComponentStyle.SMALL, UIUtil.FontColor.BRIGHTER);

  HaxeActionBeforeRunDialog(@NotNull Project project, @NotNull Task task) {
    super(project);
    this.project = project;
    this.task = task;
    setTitle(HaxeDebuggerBundle.message("haxe.before.run.dialog.title"));

    FileChooserDescriptor descriptor = FileChooserDescriptorFactory.singleFile()
      .withTitle(HaxeDebuggerBundle.message("haxe.before.run.dialog.build.file.chooser"));
    fileField.addBrowseFolderListener(project, descriptor);
    fileField.getTextField().getDocument().addDocumentListener(new DocumentAdapter() {
      @Override
      protected void textChanged(@NotNull DocumentEvent e) {
        refillActionCombo(null);
        refreshDebugPreview();
      }
    });

    actionCombo.setRenderer(BuilderKt.textListCellRenderer("", name -> name));
    actionCombo.setEditable(true);

    // build file paths are long - default the dialog to a comfortable width
    // (columns only affect the preferred size, so it stays freely resizable)
    fileField.getTextField().setColumns(45);

    fileField.setText(task.getBuildFilePath());
    refillActionCombo(task.getActionName());
    argumentsField.setText(task.getExtraArguments());
    injectDebugCheckBox.setSelected(task.isInjectDebugArguments());
    refreshDebugPreview();
    init();
  }

  @Override
  protected @Nullable JComponent createCenterPanel() {
    return FormBuilder.createFormBuilder()
      .addLabeledComponent(HaxeDebuggerBundle.message("haxe.before.run.dialog.build.file"), fileField)
      .addLabeledComponent(HaxeDebuggerBundle.message("haxe.before.run.dialog.action"), actionCombo)
      .addLabeledComponent(HaxeDebuggerBundle.message("haxe.before.run.dialog.arguments"), argumentsField)
      .addComponentToRightColumn(injectDebugCheckBox)
      .addComponentToRightColumn(injectDebugPreview)
      .getPanel();
  }

  @Override
  protected void doOKAction() {
    task.setBuildFilePath(fileField.getText().trim());
    Object action = actionCombo.getEditor().getItem();
    task.setActionName(action == null ? "" : action.toString().trim());
    task.setExtraArguments(argumentsField.getText().trim());
    task.setInjectDebugArguments(injectDebugCheckBox.isSelected());
    super.doOKAction();
  }

  /** The file's debug additions (from its type and selected target), or null without a file. */
  @Nullable
  private List<String> debugAdditionsFor(@NotNull String path) {
    if (path.isEmpty()) return null;
    return HaxeActionBeforeRunTaskProvider.debugAdditions(project, path);
  }

  /** Shows the exact arguments a Debug launch would append for the chosen file's target. */
  private void refreshDebugPreview() {
    String path = fileField.getText().trim();
    List<String> additions = debugAdditionsFor(path);
    if (additions == null || additions.isEmpty()) {
      injectDebugPreview.setText(HaxeDebuggerBundle.message("haxe.before.run.dialog.inject.debug.none"));
      injectDebugCheckBox.setEnabled(false);
    }
    else {
      String preview = HaxeDebuggerBundle.message("haxe.before.run.dialog.inject.debug.preview", String.join(" ", additions));
      injectDebugPreview.setText(preview);
      injectDebugCheckBox.setEnabled(true);
    }
  }

  private void refillActionCombo(@Nullable String selectedAction) {
    HaxeActionComboUtil.refillActionCombo(project, actionCombo, fileField.getText().trim(), selectedAction);
  }
}
