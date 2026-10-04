package com.intellij.plugins.haxe.v2.toolwindow.actions;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.InputValidator;
import com.intellij.openapi.ui.InputValidatorEx;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.plugins.haxe.HaxeBundle;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Shared input dialogs for environment define actions: "name or name=value"
 * for a SET entry, a bare name for a REMOVE entry.
 */
final class DefinePrompt {

  record DefineInput(@NotNull String name, @NotNull String value) {
  }

  private static final InputValidator NAME_NOT_BLANK = new InputValidator() {
    @Override
    public boolean checkInput(String input) {
      return isDefineName(input.split("=", 2)[0]);
    }

    @Override
    public boolean canClose(String input) {
      return checkInput(input);
    }
  };

  /** Accepts a bare define name only; "name=value" belongs to Add Define. */
  private static final InputValidatorEx UNDEFINE_NAME = DefinePrompt::undefineNameError;

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

  /** The trimmed name of the define to hide with a REMOVE entry, or null when cancelled. */
  @Nullable
  static String showUndefineName(@NotNull Project project) {
    String input = Messages.showInputDialog(project,
                                            HaxeBundle.message("haxe.toolwindow.add.undefine.message"),
                                            HaxeBundle.message("haxe.toolwindow.add.undefine.title"),
                                            null,
                                            null,
                                            UNDEFINE_NAME);
    return StringUtil.isEmptyOrSpaces(input) ? null : input.trim();
  }

  @Nullable
  private static String undefineNameError(@NotNull String entered) {
    boolean valid = !entered.contains("=") && isDefineName(entered);
    return valid ? null : HaxeBundle.message("haxe.toolwindow.add.undefine.invalid");
  }

  /** Whether the trimmed text is usable as a define name */
  private static boolean isDefineName(@NotNull String entered) {
    String name = entered.trim();
    // any whitespace character inside the name
    return !name.isEmpty() && !name.matches(".*\\s.*");
  }
}
