package com.intellij.plugins.haxe.lang.parser.declarations;

import com.intellij.plugins.haxe.lang.parser.HaxeParsingTestBase;

abstract public class DeclarationTestBase extends HaxeParsingTestBase {
  public DeclarationTestBase(String subFolder) {
    super("parsing", "haxe", "declarations", subFolder);
  }
}
