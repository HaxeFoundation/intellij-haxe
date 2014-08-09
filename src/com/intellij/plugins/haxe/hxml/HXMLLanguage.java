package com.intellij.plugins.haxe.hxml;

import com.intellij.lang.Language;
import com.intellij.plugins.haxe.HaxeBundle;

public class HXMLLanguage extends Language {
  public static HXMLLanguage INSTANCE = new HXMLLanguage();

  protected HXMLLanguage() {
    super(HaxeBundle.message("hxml.language.id"));
  }
}
