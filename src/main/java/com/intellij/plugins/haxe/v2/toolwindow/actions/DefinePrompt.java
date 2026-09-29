package com.intellij.plugins.haxe.v2.toolwindow.actions;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.InputValidator;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.plugins.haxe.HaxeBundle;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Shared "name or name=value" input dialog for environment define actions.
 */
final class DefinePrompt {

  record DefineInput(@NotNull String name, @NotNull String value) {
  }

  private static final InputValidator NAME_NOT_BLANK = new InputValidator() {
    @Override
    public boolean checkInput(String input) {
      String name = input.split("=", 2)[0].trim();
      // a define name as haxe accepts it - no whitespace, not empty
      return !name.isEmpty() && !name.matches(".*\\s.*");
    }

    @Override
    public boolean canClose(String input) {
      return checkInput(input);
    }
  };

  private DefinePrompt() {
  }

  @Nullable
  static DefineInput show(@NotNull Project project, @Nullable String initialValue) {
    String input = Messages.showInputDialog(project,
                                            HaxeBundle.message("haxe.toolwindow.define.prompt"),
                                            HaxeBundle.message("haxe.toolwindow.define.title"),
                                            null,
                                            initialValue,
                                            NAME_NOT_BLANK);
    if (StringUtil.isEmptyOrSpaces(input)) return null;

    String[] parts = input.trim().split("=", 2);
    return new DefineInput(parts[0].trim(), parts.length > 1 ? parts[1].trim() : "");
  }
}
