package com.intellij.plugins.haxe;

import com.intellij.lang.Language;

public class HaxeLanguage extends Language {
  public static HaxeLanguage INSTANCE = new HaxeLanguage();

  protected HaxeLanguage() {
    super(HaxeBundle.message("haxe.language.id"));
  }
}
