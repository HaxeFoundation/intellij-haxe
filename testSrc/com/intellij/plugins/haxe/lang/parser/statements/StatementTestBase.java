package com.intellij.plugins.haxe.lang.parser.statements;

import com.intellij.plugins.haxe.lang.parser.HaxeParsingTestBase;

abstract public class StatementTestBase extends HaxeParsingTestBase {
  public StatementTestBase(String subFolder) {
    super("parsing", "haxe", "statements", subFolder);
  }
}
