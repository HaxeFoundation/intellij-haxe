package com.intellij.plugins.haxe.lang;

import com.intellij.plugins.haxe.HaxeLanguage;
import com.intellij.plugins.haxe.ide.formatter.settings.HaxeCodeStyleSettings;
import com.intellij.psi.codeStyle.CodeStyleSettings;
import com.intellij.psi.codeStyle.CommonCodeStyleSettings;

import java.util.function.Consumer;

/**
 * Code style tweaks shared by the formatter suites: adapters that turn a
 * tweak of the common or the Haxe settings into a tweak of the whole
 * settings, which the base reformat takes, and settings equivalents of the
 * hxformat.json options the comparison fixtures use.
 */
final class HaxeCodeStyleTweaks {

  private HaxeCodeStyleTweaks() {
  }

  static Consumer<CodeStyleSettings> commonSettings(Consumer<CommonCodeStyleSettings> configure) {
    return settings -> configure.accept(settings.getCommonSettings(HaxeLanguage.INSTANCE));
  }

  static Consumer<CodeStyleSettings> haxeSettings(Consumer<HaxeCodeStyleSettings> configure) {
    return settings -> configure.accept(settings.getCustomSettings(HaxeCodeStyleSettings.class));
  }

  /** lineEnds.leftCurly=both: every type, function and block opens its brace on the next line. */
  static void allmanBraces(CodeStyleSettings settings) {
    CommonCodeStyleSettings common = settings.getCommonSettings(HaxeLanguage.INSTANCE);
    common.BRACE_STYLE = CommonCodeStyleSettings.NEXT_LINE;
    common.METHOD_BRACE_STYLE = CommonCodeStyleSettings.NEXT_LINE;
  }
}
