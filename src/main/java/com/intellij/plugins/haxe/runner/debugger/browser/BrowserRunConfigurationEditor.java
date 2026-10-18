package com.intellij.plugins.haxe.runner.debugger.browser;

import com.intellij.application.options.ModulesComboBox;
import com.intellij.ide.browsers.BrowserSelector;
import com.intellij.ide.browsers.WebBrowser;
import com.intellij.ide.browsers.WebBrowserManager;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.plugins.haxe.HaxeDebuggerBundle;
import com.intellij.plugins.haxe.runner.debugger.javascript.DapAdapterStatusRow;
import com.intellij.plugins.haxe.util.ui.HaxePathFieldChoosers;
import com.intellij.plugins.haxe.runner.debugger.browser.BrowserRunConfiguration.BrowserFamily;
import com.intellij.ui.components.ActionLink;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.ui.UIUtil;
import java.awt.BorderLayout;
import java.awt.event.ItemEvent;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTextPane;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import com.intellij.plugins.haxe.runner.debugger.javascript.AdapterPin;

/**
 * Settings UI for the browser debug configuration: module, the browser
 * (the IDE's own registry combobox - the selection decides the DAP adapter
 * family), the serve-vs-url content mode (checkbox toggles which field is
 * live), an optional node executable override, and the selected browser's
 * DAP adapter status with a Download link. The link is the ONLY acquisition
 * path: sessions never download, and a missing adapter fails validation for
 * Debug (the one executor using it) — downloading third-party code is the
 * user's explicit decision.
 * An unsupported browser family (Safari, IE, ...) shows a message in the
 * status row instead of an adapter version.
 *
 * The layout lives in the matching .form (labels bind their bundle keys
 * there); this class owns the wiring, the adapter status row and the
 * reset/apply mapping.
 */
public class BrowserRunConfigurationEditor extends SettingsEditor<BrowserRunConfiguration> {
  private JPanel panel;
  private ModulesComboBox moduleCombo;
  private JBLabel adapterStatusLabel;
  /** Text link fetching the pinned adapter; the only acquisition path. */
  private ActionLink downloadLink;
  /** Icon-only link revealing the adapter store directory in the system file manager. */
  private ActionLink openStoreLink;
  private final DapAdapterStatusRow adapterStatus;
  private JBCheckBox serveContentCheckBox;
  private TextFieldWithBrowseButton contentRootField;
  private JBTextField urlField;
  /** Placeholder for the platform's browser combobox. */
  private JPanel browserSelectorPanel;
  private BrowserSelector browserSelector;
  private JBCheckBox overrideNodeCheckBox;
  private TextFieldWithBrowseButton nodePathField;
  private JTextPane hintArea;

  private final Project project;

  public BrowserRunConfigurationEditor(Project project) {
    this.project = project;
    FileChooserDescriptor folderDescriptor = FileChooserDescriptorFactory.singleDir();
    FileChooserDescriptor fileDescriptor = FileChooserDescriptorFactory.singleFile();

    HaxePathFieldChoosers.browseInto(project, contentRootField, folderDescriptor);
    HaxePathFieldChoosers.browseInto(project, nodePathField, fileDescriptor);

    serveContentCheckBox.addActionListener(e -> updateContentModeEnablement());
    overrideNodeCheckBox.addActionListener(e -> updateOverrideEnablement());
    buildBrowserSelector();

    // secondary information, styled like the platform's context help text
    hintArea.setForeground(UIUtil.getContextHelpForeground());
    hintArea.setFont(UIUtil.getLabelFont(UIUtil.FontSize.SMALL));
    hintArea.setBorder(null);

    adapterStatus = new DapAdapterStatusRow(adapterStatusLabel, downloadLink, openStoreLink, this::selectedPin, this::fireEditorStateChanged);
  }

  // The IDE's own browser combobox (Settings | Web Browsers entries). The
  // browser decides the adapter family, so every registered browser is
  // listed and the adapter-status row reacts to the selection - an
  // unsupported family shows a message there instead of a version.
  private void buildBrowserSelector() {
    browserSelector = new BrowserSelector();
    browserSelectorPanel.add(browserSelector.getMainComponent(), BorderLayout.CENTER);
    JComboBox<?> combo = UIUtil.findComponentOfType(browserSelector.getMainComponent(), JComboBox.class);
    if (combo != null) {
      combo.addItemListener(e -> {
        if (e.getStateChange() == ItemEvent.SELECTED) {
          updateAdapterStatus();
        }
      });
    }
  }

  private void selectBrowser(@Nullable String browserId) {
    browserSelector.setSelected(browserById(browserId));
  }

  private static @Nullable WebBrowser browserById(@Nullable String browserId) {
    if (browserId == null || browserId.isBlank()) return null;
    return WebBrowserManager.getInstance().findBrowserById(browserId);
  }

  private void updateContentModeEnablement() {
    boolean serve = serveContentCheckBox.isSelected();
    urlField.setEnabled(!serve);
  }

  private void updateOverrideEnablement() {
    nodePathField.setEnabled(overrideNodeCheckBox.isSelected());
  }

  // --- the selected browser's DAP adapter: downloaded state + version ---

  /** The family of the selected (or default) browser; null = none/unsupported. */
  private @Nullable BrowserFamily selectedFamily() {
    WebBrowser browser = browserSelector.getSelected();
    if (browser == null) {
      browser = DebugBrowser.resolve(null);
    }
    return DebugBrowser.familyOf(browser);
  }

  private void updateAdapterStatus() {
    if (selectedFamily() == null) {
      // no browser, or a family (Safari, IE, ...) no adapter can drive
      adapterStatusLabel.setText(noAdapterStatusText(browserSelector.getSelected()));
    }
    adapterStatus.update();
  }

  /** The pinned adapter for the selected browser's family; null when no adapter can drive it. */
  private @Nullable AdapterPin selectedPin() {
    BrowserFamily family = selectedFamily();
    return family == null ? null : BrowserRunConfiguration.adapterPinFor(family);
  }

  @Override
  protected void resetEditorFrom(@NotNull BrowserRunConfiguration configuration) {
    moduleCombo.fillModules(project);
    moduleCombo.setSelectedModule(configuration.getConfigurationModule().getModule());
    selectBrowser(configuration.getBrowserId());

    serveContentCheckBox.setSelected(configuration.isServeContent());
    contentRootField.setText(FileUtil.toSystemDependentName(configuration.getContentRoot()));
    urlField.setText(configuration.getUrl());
    overrideNodeCheckBox.setSelected(!configuration.getNodePath().isBlank());
    nodePathField.setText(FileUtil.toSystemDependentName(configuration.getNodePath()));

    updateContentModeEnablement();
    updateOverrideEnablement();
    updateAdapterStatus();
  }

  @Override
  protected void applyEditorTo(@NotNull BrowserRunConfiguration configuration) {
    configuration.setModule(moduleCombo.getSelectedModule());
    configuration.setBrowserId(browserSelector.getSelectedBrowserId());
    configuration.setServeContent(serveContentCheckBox.isSelected());
    configuration.setContentRoot(FileUtil.toSystemIndependentName(contentRootField.getText().trim()));
    configuration.setUrl(urlField.getText().trim());
    configuration.setNodePath(nodePathOverride());
  }

  /// An unchecked override means "use the default", regardless of field text.
  private String nodePathOverride() {
    if (!overrideNodeCheckBox.isSelected()) return "";
    return FileUtil.toSystemIndependentName(nodePathField.getText().trim());
  }

  private static String noAdapterStatusText(@Nullable WebBrowser browser) {
    if (browser == null) return HaxeDebuggerBundle.message("browser.runner.adapter.no.browser");
    return HaxeDebuggerBundle.message("browser.runner.adapter.unsupported", browser.getName());
  }

  @Override
  protected @NotNull JComponent createEditor() {
    return panel;
  }
}
