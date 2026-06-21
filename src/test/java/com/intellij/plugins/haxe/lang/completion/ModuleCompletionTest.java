package com.intellij.plugins.haxe.lang.completion;

import org.junit.Test;

public class ModuleCompletionTest extends HaxeCompletionTestBase {
  public ModuleCompletionTest() {
    super("completion", "module");
  }

  @Test
  public void testModuleSuggestedInImportStatement() throws Throwable {
    myFixture.configureByFiles("com/mypackage/MyModule.hx", "com/mypackage/OtherModule.hx");
    doTest();
  }

  @Test
  public void testModuleMemberCompletionFromImport() throws Throwable {
    myFixture.configureByFiles("com/mypackage/MyModule.hx");
    doTest();
  }

  @Test
  public void testModuleCompletionFromWildcardImport() throws Throwable {
    myFixture.configureByFiles("com/mypackage/MyModule.hx", "com/mypackage/OtherModule.hx");
    doTest();
  }
}
