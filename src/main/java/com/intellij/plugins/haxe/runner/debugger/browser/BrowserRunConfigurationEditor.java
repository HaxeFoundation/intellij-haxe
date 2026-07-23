package com.intellij.plugins.haxe.runner.debugger.browser;

import com.intellij.application.options.ModulesComboBox;
import com.intellij.icons.AllIcons;
import com.intellij.ide.actions.RevealFileAction;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.plugins.haxe.HaxeDebuggerBundle;
import com.intellij.plugins.haxe.runner.debugger.HaxeRunConfigurationEditorUtil;
import com.intellij.plugins.haxe.runner.debugger.browser.BrowserRunConfiguration.BrowserFamily;
import com.intellij.ui.components.ActionLink;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBTextField;
import com.intellij.ui.dsl.listCellRenderer.BuilderKt;
import com.intellij.util.ui.UIUtil;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTextPane;
import org.jetbrains.annotations.NotNull;

/**
 * Settings UI for the browser debug configuration: module, browser family
 * (Firefox / Chromium), the serve-vs-url content mode (checkbox toggles which
 * field is live), optional browser/node executable overrides (unchecked =
 * the default installation / PATH), and the selected family's DAP adapter
 * status with a Download link. The link is the ONLY acquisition path:
 * sessions never download, and a missing adapter fails configuration
 * validation — downloading third-party code is the user's explicit decision.
 *
 * The layout lives in the matching .form (labels bind their bundle keys
 * there); this class owns the wiring, the adapter status row and the
 * reset/apply mapping.
 */
public class BrowserRunConfigurationEditor extends SettingsEditor<BrowserRunConfiguration> {
  private JPanel panel;
  private ModulesComboBox moduleCombo;
  private ComboBox<BrowserFamily> familyCombo;
  private JBLabel adapterStatusLabel;
  /** Text link fetching the pinned adapter; the only acquisition path. */
  private ActionLink downloadLink;
  /** Icon-only link revealing the adapter store directory in the system file manager. */
  private ActionLink openStoreLink;
  private JBCheckBox serveContentCheckBox;
  private TextFieldWithBrowseButton contentRootField;
  private JBTextField urlField;
  private JBCheckBox overrideBrowserCheckBox;
  private TextFieldWithBrowseButton browserExecutableField;
  private JBCheckBox overrideNodeCheckBox;
  private TextFieldWithBrowseButton nodePathField;
  private JTextPane hintArea;

  private final Project project;

  public BrowserRunConfigurationEditor(Project project) {
    this.project = project;
    HaxeRunConfigurationEditorUtil.browseInto(project, contentRootField,
                                              FileChooserDescriptorFactory.createSingleFolderDescriptor());
    HaxeRunConfigurationEditorUtil.browseInto(project, browserExecutableField,
                                              FileChooserDescriptorFactory.singleFile());
    HaxeRunConfigurationEditorUtil.browseInto(project, nodePathField,
                                              FileChooserDescriptorFactory.singleFile());

    for (BrowserFamily family : BrowserFamily.values()) {
      familyCombo.addItem(family);
    }
    // enum names are SHOUTY; render them as ordinary names
    familyCombo.setRenderer(BuilderKt.textListCellRenderer(
      "", value -> value == BrowserFamily.FIREFOX ? "Firefox" : "Chromium"));

    serveContentCheckBox.addActionListener(e -> updateContentModeEnablement());
    overrideBrowserCheckBox.addActionListener(e -> updateOverrideEnablement());
    overrideNodeCheckBox.addActionListener(e -> updateOverrideEnablement());
    familyCombo.addActionListener(e -> updateAdapterStatus());

    // secondary information, styled like the platform's context help text
    adapterStatusLabel.setForeground(UIUtil.getContextHelpForeground());
    hintArea.setForeground(UIUtil.getContextHelpForeground());
    hintArea.setFont(UIUtil.getLabelFont(UIUtil.FontSize.SMALL));
    hintArea.setBorder(null);

    downloadLink.setText(HaxeDebuggerBundle.message("browser.runner.adapter.download"));
    downloadLink.addActionListener(e -> downloadAdapter());
    openStoreLink.setIcon(AllIcons.General.OpenDisk);
    openStoreLink.setToolTipText(RevealFileAction.getActionName());
    openStoreLink.addActionListener(e -> revealAdapterDirectory());
  }

  private void updateContentModeEnablement() {
    boolean serve = serveContentCheckBox.isSelected();
    contentRootField.setEnabled(serve);
    urlField.setEnabled(!serve);
  }

  private void updateOverrideEnablement() {
    browserExecutableField.setEnabled(overrideBrowserCheckBox.isSelected());
    nodePathField.setEnabled(overrideNodeCheckBox.isSelected());
  }

  // --- the selected family's DAP adapter: downloaded state + version ---

  private BrowserFamily selectedFamily() {
    Object selected = familyCombo.getSelectedItem();
    return selected instanceof BrowserFamily family ? family : BrowserFamily.FIREFOX;
  }

  private AdapterPin selectedPin() {
    return BrowserRunConfiguration.adapterPinFor(selectedFamily());
  }

  private String selectedAdapterName() {
    return BrowserRunConfiguration.adapterDisplayName(selectedFamily());
  }

  private void updateAdapterStatus() {
    AdapterPin pin = selectedPin();
    String name = selectedAdapterName() + " " + pin.version();
    boolean installed = new AdapterStore(BrowserDebugBackend.adapterStoreRoot()).isInstalled(pin);
    adapterStatusLabel.setText(installed
                               ? HaxeDebuggerBundle.message("browser.runner.adapter.downloaded", name)
                               : name + " —");
    openStoreLink.setVisible(installed);
    downloadLink.setVisible(!installed);
    downloadLink.setEnabled(true);
  }

  private void revealAdapterDirectory() {
    AdapterPin pin = selectedPin();
    RevealFileAction.openDirectory(
      BrowserDebugBackend.adapterStoreRoot().resolve(pin.id()).resolve(pin.version()).toFile());
  }

  private void downloadAdapter() {
    AdapterPin pin = selectedPin();
    downloadLink.setVisible(false);
    adapterStatusLabel.setText(HaxeDebuggerBundle.message(
      "browser.runner.adapter.downloading", selectedAdapterName() + " " + pin.version()));
    ApplicationManager.getApplication().executeOnPooledThread(() -> {
      String failure;
      try {
        new AdapterStore(BrowserDebugBackend.adapterStoreRoot()).resolveEntry(pin, null);
        failure = null;
      } catch (Exception e) {
        failure = e.getMessage();
      }
      String failureText = failure;
      // the editor lives in a MODAL dialog: without its modality state the
      // runnable would only run after the dialog closes and the status text
      // would never appear to update
      ApplicationManager.getApplication().invokeLater(() -> {
        if (failureText != null) {
          adapterStatusLabel.setText(HaxeDebuggerBundle.message("browser.runner.adapter.failed", failureText));
          downloadLink.setVisible(true);
        } else {
          updateAdapterStatus();
          // the dialog's "adapter is not downloaded" validation error must
          // clear now, not on the next manual edit - nudge a re-validation
          fireEditorStateChanged();
        }
      }, ModalityState.stateForComponent(adapterStatusLabel));
    });
  }

  @Override
  protected void resetEditorFrom(@NotNull BrowserRunConfiguration configuration) {
    moduleCombo.fillModules(project);
    moduleCombo.setSelectedModule(configuration.getConfigurationModule().getModule());
    familyCombo.setSelectedItem(configuration.getBrowserFamily());
    serveContentCheckBox.setSelected(configuration.isServeContent());
    contentRootField.setText(FileUtil.toSystemDependentName(configuration.getContentRoot()));
    urlField.setText(configuration.getUrl());
    overrideBrowserCheckBox.setSelected(!configuration.getBrowserExecutablePath().isBlank());
    browserExecutableField.setText(FileUtil.toSystemDependentName(configuration.getBrowserExecutablePath()));
    overrideNodeCheckBox.setSelected(!configuration.getNodePath().isBlank());
    nodePathField.setText(FileUtil.toSystemDependentName(configuration.getNodePath()));
    updateContentModeEnablement();
    updateOverrideEnablement();
    updateAdapterStatus();
  }

  @Override
  protected void applyEditorTo(@NotNull BrowserRunConfiguration configuration) {
    configuration.setModule(moduleCombo.getSelectedModule());
    configuration.setBrowserFamily((BrowserFamily)familyCombo.getSelectedItem());
    configuration.setServeContent(serveContentCheckBox.isSelected());
    configuration.setContentRoot(FileUtil.toSystemIndependentName(contentRootField.getText().trim()));
    configuration.setUrl(urlField.getText().trim());
    // an unchecked override means "use the default", regardless of field text
    configuration.setBrowserExecutablePath(overrideBrowserCheckBox.isSelected()
                                           ? FileUtil.toSystemIndependentName(browserExecutableField.getText().trim())
                                           : "");
    configuration.setNodePath(overrideNodeCheckBox.isSelected()
                              ? FileUtil.toSystemIndependentName(nodePathField.getText().trim())
                              : "");
  }

  @Override
  protected @NotNull JComponent createEditor() {
    return panel;
  }
}
