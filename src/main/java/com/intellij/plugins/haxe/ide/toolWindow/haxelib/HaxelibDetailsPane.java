package com.intellij.plugins.haxe.ide.toolWindow.haxelib;

import com.intellij.plugins.haxe.haxelib.HaxelibLibraryInfo;
import com.intellij.plugins.haxe.haxelib.HaxelibLocalDocs.GitCheckout;
import com.intellij.ui.BrowserHyperlinkListener;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.JBTabbedPane;
import com.intellij.util.ui.HTMLEditorKitBuilder;
import java.net.URL;
import java.util.List;
import java.util.Set;
import javax.swing.JComponent;
import javax.swing.JEditorPane;
import javax.swing.text.html.HTMLDocument;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The details half of the explorer: one tab per local documentation file of
 * the shown VERSION (README/CHANGELOG/LICENSE, pre-rendered to HTML off the
 * EDT by the caller) with the Overview tab (metadata + installed state +
 * releases) LAST; the FIRST tab — the readme when one exists — is what
 * opens. Every tab gets a FRESH component: JBTabbedPane decorates inserted
 * components, and re-adding one instance accumulates its insets (the
 * shrinking-overview bug).
 */
final class HaxelibDetailsPane {

  /** One pre-rendered documentation tab; base resolves the document's relative image paths. */
  record DocTab(@Nls @NotNull String title, @NotNull String html, @Nullable URL base) {
  }

  private final JBTabbedPane tabs = new JBTabbedPane();
  private final @Nls String overviewTitle;

  HaxelibDetailsPane(@Nls @NotNull String overviewTitle) {
    this.overviewTitle = overviewTitle;
  }

  @NotNull
  JComponent getComponent() {
    return tabs;
  }

  /** One message-only tab — the empty-selection, loading and error states. */
  void showMessage(@Nls @NotNull String message) {
    tabs.removeAll();
    tabs.addTab(overviewTitle, htmlPane(HaxelibOverviewHtml.message(message), null));
  }

  void showLibrary(@NotNull String name,
                   @NotNull Set<String> installedVersions,
                   @Nullable String selectedVersion,
                   @Nullable String devPath,
                   @Nullable GitCheckout gitCheckout,
                   @Nullable HaxelibLibraryInfo info,
                   @NotNull List<DocTab> docs) {
    tabs.removeAll();
    for (DocTab doc : docs) {
      tabs.addTab(doc.title(), htmlPane(doc.html(), doc.base()));
    }
    String overviewHtml =
      HaxelibOverviewHtml.render(name, installedVersions, selectedVersion, devPath, gitCheckout, info);
    tabs.addTab(overviewTitle, htmlPane(overviewHtml, null));
    tabs.setSelectedIndex(0);
  }

  @NotNull
  private static JComponent htmlPane(@NotNull String html, @Nullable URL base) {
    JEditorPane pane = new JEditorPane();
    pane.setEditorKit(new HTMLEditorKitBuilder().build());
    pane.setEditable(false);
    if (base != null && pane.getDocument() instanceof HTMLDocument document) {
      document.setBase(base);
    }
    pane.addHyperlinkListener(BrowserHyperlinkListener.INSTANCE);
    pane.setText(html);
    pane.setCaretPosition(0);
    return new JBScrollPane(pane);
  }
}
