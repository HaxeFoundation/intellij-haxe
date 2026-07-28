package com.intellij.plugins.haxe.lang.completion;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Completion: module")
public class ModuleCompletionTest extends HaxeCompletionTestBase {
  public ModuleCompletionTest() {
    super("completion", "module");
  }

  @Test
  @DisplayName("module suggested in import statement")
  public void testModuleSuggestedInImportStatement() throws Throwable {
    myFixture.configureByFiles("com/mypackage/MyModule.hx", "com/mypackage/OtherModule.hx");
    doTest();
  }

  @Test
  @DisplayName("module member completion from import")
  public void testModuleMemberCompletionFromImport() throws Throwable {
    myFixture.configureByFiles("com/mypackage/MyModule.hx");
    doTest();
  }

  @Test
  @DisplayName("module completion from wildcard import")
  public void testModuleCompletionFromWildcardImport() throws Throwable {
    myFixture.configureByFiles("com/mypackage/MyModule.hx", "com/mypackage/OtherModule.hx");
    doTest();
  }
}
