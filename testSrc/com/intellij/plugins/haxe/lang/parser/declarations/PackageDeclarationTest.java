package com.intellij.plugins.haxe.lang.parser.declarations;

public class PackageDeclarationTest extends DeclarationTestBase {
  public PackageDeclarationTest() {
    super("package");
  }

  public void testEmpty() throws Throwable {
    doTest(true);
  }

  public void testError() throws Throwable {
    doTest(true);
  }

  public void testSimple() throws Throwable {
    doTest(true);
  }
}
