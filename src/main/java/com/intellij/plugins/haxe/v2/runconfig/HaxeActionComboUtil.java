package com.intellij.plugins.haxe.v2.runconfig;

import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.plugins.haxe.v2.buildtools.HaxeCompileCommands;
import javax.swing.DefaultComboBoxModel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** Shared combo plumbing for the action run configuration UI. */
final class HaxeActionComboUtil {

  private HaxeActionComboUtil() {
  }

  /**
   * Rebuilds an editable action combo from the given build file's available
   * actions, keeping the selection: {@code selectedAction} when given, else the
   * combo's current editor text. An unknown previous entry is re-added so a
   * custom action name survives the refill.
   */
  static void refillActionCombo(@NotNull Project project,
                                @NotNull ComboBox<String> combo,
                                @Nullable String buildFilePath,
                                @Nullable String selectedAction) {
    String previous = selectedAction != null ? selectedAction : StringUtil.notNullize((String)combo.getEditor().getItem());
    VirtualFile file = StringUtil.isEmptyOrSpaces(buildFilePath) ? null : LocalFileSystem.getInstance().findFileByPath(buildFilePath);

    DefaultComboBoxModel<String> model = new DefaultComboBoxModel<>();
    if (file != null && file.isValid()) {
      // type detection may sniff file content - EDT has no implicit read access
      ReadAction.computeBlocking(() -> HaxeCompileCommands.availableActionNames(project, file))
        .forEach(model::addElement);
    }

    if (!StringUtil.isEmptyOrSpaces(previous) && model.getIndexOf(previous) < 0) {
      model.addElement(previous);
    }

    combo.setModel(model);
    combo.setSelectedItem(StringUtil.isEmptyOrSpaces(previous) ? null : previous);
  }
}
