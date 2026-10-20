package com.intellij.plugins.haxe.v2.runconfig;

import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.plugins.haxe.HaxeDebuggerBundle;
import com.intellij.plugins.haxe.v2.buildtools.HaxeDebugAdditions.Debugger;
import com.intellij.plugins.haxe.v2.runconfig.HaxeActionBeforeRunTaskProvider.Task;
import com.intellij.ui.DocumentAdapter;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBTextField;
import com.intellij.ui.dsl.listCellRenderer.BuilderKt;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import java.util.List;

/**
 * Configures a "Run Haxe action" before-launch step: the build file, the action to
 * run and extra arguments. The debug preview shows the additions of the owning
 * configuration's debugger. Applies to the task on OK. A test configuration's
 * step is derived from its tests file and filter: the dialog then shows the
 * step read-only and OK leaves it as computed.
 */
final class HaxeActionBeforeRunDialog extends DialogWrapper {

  private final Project project;
  private final Task task;
  private final Debugger debugger;
  private final boolean derived;

  private JPanel panel;
  private TextFieldWithBrowseButton fileField;
  private ComboBox<String> actionCombo;
  private JBTextField argumentsField;
  private JBCheckBox injectDebugCheckBox;
  private JBLabel injectDebugPreview;
  private JBLabel derivedNote;

  HaxeActionBeforeRunDialog(@NotNull Project project, @NotNull Task task, @NotNull Debugger debugger, boolean derived) {
    super(project);
    this.project = project;
    this.task = task;
    this.debugger = debugger;
    this.derived = derived;
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
    derivedNote.setVisible(derived);
    fileField.setEnabled(!derived);
    actionCombo.setEnabled(!derived);
    argumentsField.setEnabled(!derived);
    init();
  }

  /** The two styled labels; the form instantiates the rest. */
  private void createUIComponents() {
    injectDebugPreview = new JBLabel("", UIUtil.ComponentStyle.SMALL, UIUtil.FontColor.BRIGHTER);
    String derivedText = HaxeDebuggerBundle.message("haxe.before.run.dialog.derived");
    derivedNote = new JBLabel(derivedText, UIUtil.ComponentStyle.SMALL, UIUtil.FontColor.BRIGHTER);
  }

  @Override
  protected @Nullable JComponent createCenterPanel() {
    return panel;
  }

  @Override
  protected void doOKAction() {
    if (!derived) {
      applyToTask();
    }
    super.doOKAction();
  }

  private void applyToTask() {
    task.setBuildFilePath(fileField.getText().trim());
    Object action = actionCombo.getEditor().getItem();
    task.setActionName(action == null ? "" : action.toString().trim());
    task.setExtraArguments(argumentsField.getText().trim());
    task.setInjectDebugArguments(injectDebugCheckBox.isSelected());
  }

  /** The file's debug additions (from its type, selected target and the debugger), or null without a file. */
  @Nullable
  private List<String> debugAdditionsFor(@NotNull String path) {
    if (path.isEmpty()) return null;
    return HaxeActionBeforeRunTaskProvider.debugAdditions(project, path, debugger);
  }

  /** Shows the exact arguments a Debug launch would append for the chosen file's target and the debugger. */
  private void refreshDebugPreview() {
    String path = fileField.getText().trim();
    List<String> additions = debugAdditionsFor(path);
    boolean hasAdditions = additions != null && !additions.isEmpty();
    if (hasAdditions) {
      String preview = HaxeDebuggerBundle.message("haxe.before.run.dialog.inject.debug.preview", String.join(" ", additions));
      injectDebugPreview.setText(preview);
    }
    else {
      injectDebugPreview.setText(HaxeDebuggerBundle.message("haxe.before.run.dialog.inject.debug.none"));
    }
    injectDebugCheckBox.setEnabled(hasAdditions && !derived);
  }

  private void refillActionCombo(@Nullable String selectedAction) {
    HaxeActionComboUtil.refillActionCombo(project, actionCombo, fileField.getText().trim(), selectedAction);
  }
}
