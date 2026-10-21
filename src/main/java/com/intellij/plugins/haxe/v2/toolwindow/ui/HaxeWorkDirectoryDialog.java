package com.intellij.plugins.haxe.v2.toolwindow.ui;

import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.plugins.haxe.HaxeBundle;
import com.intellij.plugins.haxe.util.ui.HaxeDialogHints;
import com.intellij.plugins.haxe.util.ui.HaxePathFieldChoosers;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

/**
 * Sets a build file's working directory override: the directory its build
 * commands run in and its relative paths resolve against. An empty field
 * clears the override back to the type-derived default.
 * The layout lives in the matching .form (the label binds its bundle key
 * there); the hint text carries the default directory, so it is set here.
 */
public final class HaxeWorkDirectoryDialog extends DialogWrapper {

  private JPanel panel;
  private TextFieldWithBrowseButton directoryField;
  private JTextPane hintArea;

  public HaxeWorkDirectoryDialog(@NotNull Project project, @Nullable String currentOverride, @Nullable String defaultDirectory) {
    super(project);
    setTitle(HaxeBundle.message("haxe.work.directory.dialog.title"));

    var chooserDescriptor = FileChooserDescriptorFactory.singleDir()
      .withTitle(HaxeBundle.message("haxe.work.directory.dialog.chooser.title"));
    HaxePathFieldChoosers.browseInto(project, directoryField, chooserDescriptor);
    directoryField.setText(StringUtil.notNullize(currentOverride));

    HaxeDialogHints.style(hintArea);
    hintArea.setText(HaxeBundle.message("haxe.work.directory.dialog.hint", StringUtil.notNullize(defaultDirectory)));
    init();
  }

  @Override
  protected @NotNull JComponent createCenterPanel() {
    panel.setPreferredSize(JBUI.size(520, -1));
    return panel;
  }

  @Override
  public @Nullable JComponent getPreferredFocusedComponent() {
    return directoryField.getTextField();
  }

  /** The chosen override, or null for "use the default". */
  @Nullable
  public String getWorkDirectory() {
    return StringUtil.nullize(directoryField.getText().trim());
  }
}
