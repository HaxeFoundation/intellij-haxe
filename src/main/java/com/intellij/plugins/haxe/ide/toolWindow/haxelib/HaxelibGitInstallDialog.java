package com.intellij.plugins.haxe.ide.toolWindow.haxelib;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.plugins.haxe.HaxeBundle;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

/**
 * Collects the repository url and optional branch/tag/commit for installing a
 * library from git ({@code haxelib git}). Prefilled from the existing git
 * checkout when the library already has one, so re-pointing an install starts
 * from its current remote. The layout lives in the matching .form.
 */
final class HaxelibGitInstallDialog extends DialogWrapper {

  private JPanel panel;
  private JBTextField urlField;
  private JBTextField refField;

  HaxelibGitInstallDialog(@NotNull Project project, @NotNull String libraryName,
                          @Nullable String initialUrl, @Nullable String initialRef) {
    super(project);
    setTitle(HaxeBundle.message("haxelib.explorer.git.dialog.title", libraryName));
    urlField.setText(StringUtil.notNullize(initialUrl));
    refField.setText(StringUtil.notNullize(initialRef));
    init();
  }

  @Override
  protected @NotNull JComponent createCenterPanel() {
    panel.setPreferredSize(JBUI.size(480, -1));
    return panel;
  }

  @Override
  public @Nullable JComponent getPreferredFocusedComponent() {
    return urlField;
  }

  @Override
  protected @Nullable ValidationInfo doValidate() {
    if (StringUtil.isEmptyOrSpaces(urlField.getText())) {
      return new ValidationInfo(HaxeBundle.message("haxelib.explorer.git.dialog.url.required"), urlField);
    }
    return null;
  }

  @NotNull
  String getUrl() {
    return urlField.getText().trim();
  }

  /** The branch/tag/commit to check out, or null for the repository's default branch. */
  @Nullable
  String getRef() {
    return StringUtil.nullize(refField.getText().trim());
  }
}
