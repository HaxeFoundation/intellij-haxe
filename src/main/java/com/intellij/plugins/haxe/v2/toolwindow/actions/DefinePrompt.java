package com.intellij.plugins.haxe.v2.toolwindow.actions;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.InputValidatorEx;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.plugins.haxe.HaxeBundle;
import com.intellij.plugins.haxe.v2.buildsystem.HaxeBuildFileInfo.HaxeDefine;
import com.intellij.plugins.haxe.v2.buildtools.info.HaxeDefineContextService;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Shared input dialogs for environment define actions: "name or name=value"
 * for a SET entry, a bare name for a REMOVE entry.
 */
final class DefinePrompt {

  record DefineInput(@NotNull String name, @NotNull String value) {
  }

  /** Accepts "name" or "name=value" for a SET entry. */
  private static final InputValidatorEx DEFINE_INPUT = DefinePrompt::defineInputError;

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
                                            DEFINE_INPUT);
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

  /** The error for a SET entry ("name" or "name=value"), or null when its name can be overridden. */
  @Nullable
  static String defineInputError(@NotNull String entered) {
    return nameError(entered.split("=", 2)[0]);
  }

  /** The error for a REMOVE entry (a bare name), or null when it names a define that can be overridden. */
  @Nullable
  static String undefineNameError(@NotNull String entered) {
    if (entered.contains("=")) return HaxeBundle.message("haxe.toolwindow.add.undefine.invalid");
    return nameError(entered);
  }

  /** A name is refused when it is not one, or when it identifies the compiler: an override cannot sensibly toggle {@code haxe_ver}. */
  @Nullable
  private static String nameError(@NotNull String entered) {
    String name = entered.trim();
    if (!isDefineName(name)) return HaxeBundle.message("haxe.toolwindow.define.name.invalid");
    if (HaxeDefineContextService.isCompilerIdentityDefine(HaxeDefine.compilerName(name))) {
      return HaxeBundle.message("haxe.toolwindow.define.name.identity", name);
    }
    return null;
  }

  /** Whether the trimmed text is usable as a define name */
  private static boolean isDefineName(@NotNull String name) {
    // any whitespace character inside the name
    return !name.isEmpty() && !name.matches(".*\\s.*");
  }
}
