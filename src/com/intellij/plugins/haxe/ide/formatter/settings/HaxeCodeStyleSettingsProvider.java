package com.intellij.plugins.haxe.ide.formatter.settings;

import com.intellij.openapi.options.Configurable;
import com.intellij.plugins.haxe.HaxeBundle;
import com.intellij.psi.codeStyle.CodeStyleSettings;
import com.intellij.psi.codeStyle.CodeStyleSettingsProvider;
import org.jetbrains.annotations.NotNull;

/**
 * @author: Fedor.Korotkov
 */
public class HaxeCodeStyleSettingsProvider extends CodeStyleSettingsProvider {
  @Override
  public String getConfigurableDisplayName() {
    return HaxeBundle.message("haxe.title");
  }

  @NotNull
  @Override
  public Configurable createSettingsPage(CodeStyleSettings settings, CodeStyleSettings originalSettings) {
    return new HaxeCodeStyleConfigurable(settings, originalSettings);
  }
}
