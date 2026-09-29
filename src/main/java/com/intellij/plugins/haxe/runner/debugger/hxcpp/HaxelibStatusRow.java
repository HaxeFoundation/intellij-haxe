package com.intellij.plugins.haxe.runner.debugger.hxcpp;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.project.Project;
import com.intellij.plugins.haxe.HaxeDebuggerBundle;
import com.intellij.plugins.haxe.v2.buildtools.libraries.HaxelibInstaller;
import com.intellij.ui.components.ActionLink;
import com.intellij.ui.components.JBLabel;
import org.jetbrains.annotations.NotNull;

/**
 * Run-configuration editor row showing whether a debug-server haxelib is
 * installed, with an inline Install link — the debuggee must be COMPILED with
 * the lib, so a missing install means the debug additions (or the build's own
 * {@code -lib} line) cannot resolve. The check and the install run haxelib,
 * so both happen on pooled threads; label updates hop back through the editor
 * dialog's modality.
 */
public final class HaxelibStatusRow {

  private final Project project;
  private final String libName;
  private final JBLabel statusLabel;
  private final ActionLink installLink;

  public HaxelibStatusRow(@NotNull Project project, @NotNull String libName,
                          @NotNull JBLabel statusLabel, @NotNull ActionLink installLink) {
    this.project = project;
    this.libName = libName;
    this.statusLabel = statusLabel;
    this.installLink = installLink;
    installLink.setText(HaxeDebuggerBundle.message("hxcpp.runner.editor.server.lib.install"));
    installLink.addActionListener(event -> install());
    installLink.setVisible(false);
  }

  /** Re-queries haxelib and updates the row; call from resetEditorFrom. */
  public void refresh() {
    statusLabel.setText(HaxeDebuggerBundle.message("hxcpp.runner.editor.server.lib.checking", libName));
    installLink.setVisible(false);
    ApplicationManager.getApplication().executeOnPooledThread(() -> {
      boolean installed = HaxelibInstaller.isInstalled(project, libName);
      String statusKey = installed ? "hxcpp.runner.editor.server.lib.installed" : "hxcpp.runner.editor.server.lib.missing";
      onEditorModality(() -> {
        statusLabel.setText(HaxeDebuggerBundle.message(statusKey, libName));
        installLink.setVisible(!installed);
      });
    });
  }

  private void install() {
    statusLabel.setText(HaxeDebuggerBundle.message("hxcpp.runner.editor.server.lib.installing", libName));
    installLink.setVisible(false);
    ApplicationManager.getApplication().executeOnPooledThread(() -> {
      String failure = HaxelibInstaller.install(project, libName, null, null);
      onEditorModality(() -> {
        if (failure == null) {
          refresh();
        }
        else {
          String firstLine = failure.lines().findFirst().orElse("");
          statusLabel.setText(HaxeDebuggerBundle.message("hxcpp.runner.editor.server.lib.failed", firstLine));
          installLink.setVisible(true);
        }
      });
    });
  }

  // the editor lives in a MODAL dialog: without its modality state the
  // update would only run after the dialog closes
  private void onEditorModality(@NotNull Runnable update) {
    ApplicationManager.getApplication().invokeLater(update, ModalityState.stateForComponent(statusLabel));
  }
}
