package com.intellij.plugins.haxe.lang.parser.declarations;

/**
 * @author fedor.korotkov
 */
public class TypedefDeclarationTest extends DeclarationTestBase {
  public TypedefDeclarationTest() {
    super("typedef");
  }

  public void testSimple() throws Throwable {
    doTest(true);
  }

  public void testPoints() throws Throwable {
    doTest(true);
  }
}
