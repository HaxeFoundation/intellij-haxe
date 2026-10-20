package com.intellij.plugins.haxe.util.ui;

import com.intellij.openapi.fileChooser.FileChooser;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import org.jetbrains.annotations.Nullable;

/**
 * Shared browse-button wiring for path fields (run-configuration editors,
 * tool-window dialogs): one place for the chooser behavior, so a UX tweak
 * reaches every field (before extraction, the "open at the current path"
 * improvement had reached only two of the three copies).
 */
public final class HaxePathFieldChoosers {
  private HaxePathFieldChoosers() {
  }

  /**
   * Wires the field's browse button: a file chooser (opening at the field's
   * current path) whose choice replaces the field text, with
   * system-dependent separators. Also caps the field's PREFERRED width — a
   * long path must not size the whole dialog (the form still stretches the
   * field to the dialog's actual width).
   */
  public static void browseInto(Project project, TextFieldWithBrowseButton field, FileChooserDescriptor descriptor) {
    field.getTextField().setColumns(25);

    field.addActionListener(e -> {
      VirtualFile file = FileChooser.chooseFile(descriptor, project, currentSelection(field));
      if (file != null) {
        field.setText(FileUtil.toSystemDependentName(file.getPath()));
      }
    });
  }

  // The chooser opens at the field's current path (or its nearest existing
  // ancestor) instead of the default location. Null (empty/unresolvable text,
  // e.g. a module-relative path) falls back to the chooser's own default.
  private static @Nullable VirtualFile currentSelection(TextFieldWithBrowseButton field) {
    String text = field.getText().trim();
    if (text.isEmpty()) {
      return null;
    }
    try {
      Path path = Path.of(text);
      if (!path.isAbsolute()) {
        return null;
      }
      while (path != null && !Files.exists(path)) {
        path = path.getParent();
      }
      return path != null ? LocalFileSystem.getInstance().findFileByNioFile(path) : null;
    } catch (InvalidPathException e) {
      return null;
    }
  }
}
