package com.intellij.plugins.haxe.lang.parser.declarations;

public class ImportDeclarationTest extends DeclarationTestBase {
  public ImportDeclarationTest() {
    super("import");
  }

  public void testEmpty() throws Throwable {
    doTest(true);
  }

  public void testMulti() throws Throwable {
    doTest(true);
  }

  public void testSimple() throws Throwable {
    doTest(true);
  }
}
