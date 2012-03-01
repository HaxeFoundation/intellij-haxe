package com.intellij.plugins.haxe.ide.formatter.settings;

import com.intellij.application.options.TabbedLanguageCodeStylePanel;
import com.intellij.plugins.haxe.HaxeLanguage;
import com.intellij.psi.codeStyle.CodeStyleSettings;

/**
 * @author: Fedor.Korotkov
 */
public class HaxeCodeStyleMainPanel extends TabbedLanguageCodeStylePanel {
  protected HaxeCodeStyleMainPanel(CodeStyleSettings currentSettings, CodeStyleSettings settings) {
    super(HaxeLanguage.INSTANCE, currentSettings, settings);
  }
}
