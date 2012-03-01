package com.intellij.plugins.haxe.ide.formatter.settings;

import com.intellij.application.options.CodeStyleAbstractConfigurable;
import com.intellij.application.options.CodeStyleAbstractPanel;
import com.intellij.psi.codeStyle.CodeStyleSettings;
import org.jetbrains.annotations.NotNull;

/**
 * @author: Fedor.Korotkov
 */
public class HaxeCodeStyleConfigurable extends CodeStyleAbstractConfigurable {
  public HaxeCodeStyleConfigurable(@NotNull CodeStyleSettings settings, CodeStyleSettings cloneSettings) {
    super(settings, cloneSettings, "haXe");
  }

  @Override
  protected CodeStyleAbstractPanel createPanel(CodeStyleSettings settings) {
    return new HaxeCodeStyleMainPanel(getCurrentSettings(), settings);
  }

  @Override
  public String getHelpTopic() {
    return null;
  }
}
