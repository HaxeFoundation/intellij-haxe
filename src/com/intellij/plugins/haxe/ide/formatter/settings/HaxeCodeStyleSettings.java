package com.intellij.plugins.haxe.ide.formatter.settings;

import com.intellij.psi.codeStyle.CodeStyleSettings;
import com.intellij.psi.codeStyle.CustomCodeStyleSettings;

/**
 * @author: Fedor.Korotkov
 */
public class HaxeCodeStyleSettings extends CustomCodeStyleSettings {
  public boolean SPACE_AROUND_ARROW = true;
  public boolean SPACE_BEFORE_TYPE_REFERENCE_COLON = false;
  public boolean SPACE_AFTER_TYPE_REFERENCE_COLON = false;

  protected HaxeCodeStyleSettings(CodeStyleSettings container) {
    super("HaxeCodeStyleSettings", container);
  }
}
