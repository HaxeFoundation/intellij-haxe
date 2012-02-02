package com.intellij.plugins.haxe.lang.parser.declarations;

/**
 * @author fedor.korotkov
 */
public class EnumDeclarationTest extends DeclarationTestBase {
  public EnumDeclarationTest() {
    super("enum");
  }

  public void testAxis() throws Throwable {
    doTest(true);
  }

  public void testCell() throws Throwable {
    doTest(true);
  }

  public void testColor3() throws Throwable {
    doTest(true);
  }
}
