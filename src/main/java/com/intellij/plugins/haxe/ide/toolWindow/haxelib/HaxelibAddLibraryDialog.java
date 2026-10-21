package com.intellij.plugins.haxe.ide.toolWindow.haxelib;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.plugins.haxe.HaxeBundle;
import com.intellij.plugins.haxe.haxelib.HaxelibGitSpec;
import com.intellij.plugins.haxe.haxelib.HaxelibMetadata;
import com.intellij.plugins.haxe.util.ui.HaxePathFieldChoosers;
import com.intellij.ui.DocumentAdapter;
import com.intellij.ui.components.JBRadioButton;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.io.HttpRequests;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.regex.Pattern;

/**
 * Registers a library that is NOT in the haxelib catalog, so the name cannot
 * be picked from a row: the user types it and chooses the source — a local
 * development directory ({@code haxelib dev}) or a git repository to clone
 * ({@code haxelib git}). The layout lives in the matching .form.
 */
final class HaxelibAddLibraryDialog extends DialogWrapper {

  // haxelib's allowed library-name alphabet: letters, digits, '.', '-', '_'
  private static final Pattern LIBRARY_NAME = Pattern.compile("[A-Za-z0-9_.-]+");

  private JPanel panel;
  private JBTextField nameField;
  private JBRadioButton devRadio;
  private TextFieldWithBrowseButton devDirectoryField;
  private JBRadioButton gitRadio;
  private JBTextField gitUrlField;
  private JBTextField gitRefField;

  // the last name auto-filled from a haxelib.json - anything else in the
  // field is the user's own and is never overwritten
  private String autoFilledName = "";
  // the last raw url a name fetch ran for - focus flips must not refetch
  private String fetchedNameUrl = "";

  HaxelibAddLibraryDialog(@NotNull Project project) {
    super(project);
    setTitle(HaxeBundle.message("haxelib.explorer.add.dialog.title"));

    ButtonGroup methods = new ButtonGroup();
    methods.add(devRadio);
    methods.add(gitRadio);
    devRadio.setSelected(true);
    devRadio.addActionListener(e -> updateMethodEnablement());
    gitRadio.addActionListener(e -> updateMethodEnablement());
    updateMethodEnablement();

    FileChooserDescriptor directories = FileChooserDescriptorFactory.singleDir()
      .withTitle(HaxeBundle.message("haxelib.explorer.add.dialog.dev.chooser.title"));
    HaxePathFieldChoosers.browseInto(project, devDirectoryField, directories);

    devDirectoryField.getTextField().getDocument().addDocumentListener(new DocumentAdapter() {
      @Override
      protected void textChanged(@NotNull DocumentEvent e) {
        fillNameFromDevDirectory();
      }
    });
    FocusAdapter fetchOnFocusLost = new FocusAdapter() {
      @Override
      public void focusLost(FocusEvent e) {
        fillNameFromGitRepository();
      }
    };
    gitUrlField.addFocusListener(fetchOnFocusLost);
    gitRefField.addFocusListener(fetchOnFocusLost);
    init();
  }

  /** Prefills the name discovered in a haxelib.json, never overwriting what the user typed. */
  private void fillName(@Nullable String name) {
    if (name == null || name.isBlank()) return;
    String current = getLibraryName();
    if (current.isEmpty() || current.equals(autoFilledName)) {
      autoFilledName = name.trim();
      nameField.setText(autoFilledName);
    }
  }

  private void fillNameFromDevDirectory() {
    String directory = getDevDirectory();
    if (directory.isEmpty()) return;
    try {
      Path metadata = Path.of(directory).resolve("haxelib.json");
      if (!Files.isRegularFile(metadata)) return;
      fillName(new HaxelibMetadata(Files.readString(metadata)).getName());
    }
    catch (IOException | InvalidPathException e) {
      // no readable haxelib.json - the user names the library themselves
    }
  }

  /** Best-effort: known forges serve raw files over https; elsewhere the user names the library themselves. */
  private void fillNameFromGitRepository() {
    String rawUrl = HaxelibGitSpec.rawHaxelibJsonUrl(getGitUrl(), getGitRef());
    if (rawUrl == null || rawUrl.equals(fetchedNameUrl)) return;
    fetchedNameUrl = rawUrl;
    ApplicationManager.getApplication().executeOnPooledThread(() -> {
      String name = fetchHaxelibJsonName(rawUrl);
      if (name != null) {
        // the dialog is MODAL: without its modality state the fill would
        // only run after it closes
        ApplicationManager.getApplication().invokeLater(() -> fillName(name),
                                                        ModalityState.stateForComponent(nameField));
      }
    });
  }

  @Nullable
  private static String fetchHaxelibJsonName(@NotNull String rawUrl) {
    try {
      String content = HttpRequests.request(rawUrl)
        .connectTimeout(3000)
        .readTimeout(3000)
        .readString();
      return new HaxelibMetadata(content).getName();
    }
    catch (IOException e) {
      return null;
    }
  }

  private void updateMethodEnablement() {
    devDirectoryField.setEnabled(devRadio.isSelected());
    gitUrlField.setEnabled(gitRadio.isSelected());
    gitRefField.setEnabled(gitRadio.isSelected());
  }

  @Override
  protected @NotNull JComponent createCenterPanel() {
    panel.setPreferredSize(JBUI.size(480, -1));
    return panel;
  }

  @Override
  public @Nullable JComponent getPreferredFocusedComponent() {
    return nameField;
  }

  @Override
  protected @Nullable ValidationInfo doValidate() {
    String name = getLibraryName();
    if (name.isEmpty()) {
      return new ValidationInfo(HaxeBundle.message("haxelib.explorer.add.dialog.name.required"), nameField);
    }
    if (!LIBRARY_NAME.matcher(name).matches()) {
      return new ValidationInfo(HaxeBundle.message("haxelib.explorer.add.dialog.name.invalid"), nameField);
    }
    if (isDevMethod()) {
      if (!isExistingDirectory(getDevDirectory())) {
        return new ValidationInfo(HaxeBundle.message("haxelib.explorer.add.dialog.dev.required"), devDirectoryField);
      }
    }
    else if (getGitUrl().isEmpty()) {
      return new ValidationInfo(HaxeBundle.message("haxelib.explorer.git.dialog.url.required"), gitUrlField);
    }
    return null;
  }

  private static boolean isExistingDirectory(@NotNull String text) {
    if (text.isEmpty()) return false;
    try {
      return Files.isDirectory(Path.of(text));
    }
    catch (InvalidPathException e) {
      return false;
    }
  }

  @NotNull
  String getLibraryName() {
    return nameField.getText().trim();
  }

  boolean isDevMethod() {
    return devRadio.isSelected();
  }

  @NotNull
  String getDevDirectory() {
    return devDirectoryField.getText().trim();
  }

  @NotNull
  String getGitUrl() {
    return gitUrlField.getText().trim();
  }

  /** The branch/tag/commit to check out, or null for the repository's default branch. */
  @Nullable
  String getGitRef() {
    return StringUtil.nullize(gitRefField.getText().trim());
  }
}
