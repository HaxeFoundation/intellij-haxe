package com.intellij.plugins.haxe.runner.debugger.javascript;

import com.intellij.icons.AllIcons;
import com.intellij.ide.HelpTooltipKt;
import com.intellij.ide.actions.RevealFileAction;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.util.text.HtmlChunk;
import com.intellij.plugins.haxe.HaxeDebuggerBundle;
import com.intellij.ui.components.ActionLink;
import com.intellij.ui.components.JBLabel;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

/**
 * The adapter status row of a run configuration editor: the pinned adapter's
 * name and version with its state, a Download link - the ONLY acquisition
 * path, sessions never download - and, once downloaded, a link revealing the
 * store directory. The components belong to the editor's form; the row
 * drives them for whatever pin the editor currently shows. The fetch runs on
 * a pooled thread and reports under the editor's own modality: the editor
 * lives in a modal dialog, and without its state the report would wait until
 * the dialog closed. A successful download runs {@code onDownloaded} on the
 * EDT; the editors pass {@code fireEditorStateChanged} so the dialog's
 * "adapter is not downloaded" validation error clears right away instead of
 * on the next manual edit.
 */
public final class DapAdapterStatusRow {

  private final JBLabel statusLabel;
  private final ActionLink downloadLink;
  private final ActionLink openStoreLink;
  private final Supplier<@Nullable AdapterPin> pin;
  private final Runnable onDownloaded;

  public DapAdapterStatusRow(@NotNull JBLabel statusLabel,
                             @NotNull ActionLink downloadLink,
                             @NotNull ActionLink openStoreLink,
                             @NotNull Supplier<@Nullable AdapterPin> pin,
                             @NotNull Runnable onDownloaded) {
    this.statusLabel = statusLabel;
    this.downloadLink = downloadLink;
    this.openStoreLink = openStoreLink;
    this.pin = pin;
    this.onDownloaded = onDownloaded;
    // secondary information, styled like the platform's context help text
    statusLabel.setForeground(UIUtil.getContextHelpForeground());
    downloadLink.setText(HaxeDebuggerBundle.message("javascript.adapter.download"));
    downloadLink.addActionListener(e -> download());
    openStoreLink.setIcon(AllIcons.General.OpenDisk);
    openStoreLink.addActionListener(e -> reveal());
    HelpTooltipKt.setToolTipText(openStoreLink, HtmlChunk.text(RevealFileAction.getActionName()));
  }

  /** Shows the current pin's state; without a pin the links hide and the label keeps the editor's own text. */
  public void update() {
    AdapterPin current = pin.get();
    if (current == null) {
      openStoreLink.setVisible(false);
      downloadLink.setVisible(false);
      return;
    }
    boolean installed = AdapterStores.open().isInstalled(current);
    statusLabel.setText(JsDebugAdapters.statusText(current, installed));
    openStoreLink.setVisible(installed);
    downloadLink.setVisible(!installed);
    downloadLink.setEnabled(true);
  }

  private void reveal() {
    AdapterPin current = pin.get();
    if (current == null) return;
    RevealFileAction.openDirectory(AdapterStores.root().resolve(current.id()).resolve(current.version()).toFile());
  }

  private void download() {
    AdapterPin current = pin.get();
    if (current == null) return;
    downloadLink.setVisible(false);
    statusLabel.setText(JsDebugAdapters.downloadingText(current));
    ApplicationManager.getApplication().executeOnPooledThread(() -> {
      String failure = fetch(current);
      ApplicationManager.getApplication().invokeLater(() -> downloaded(failure), ModalityState.stateForComponent(statusLabel));
    });
  }

  private void downloaded(@Nullable String failure) {
    if (failure != null) {
      statusLabel.setText(HaxeDebuggerBundle.message("javascript.adapter.failed", failure));
      downloadLink.setVisible(true);
      return;
    }
    update();
    onDownloaded.run();
  }

  /** Downloads and verifies the pin; the failure's message, or null. */
  @Nullable
  private static String fetch(@NotNull AdapterPin pin) {
    try {
      AdapterStores.open().resolveEntry(pin);
      return null;
    }
    catch (Exception e) {
      return e.getMessage();
    }
  }
}
