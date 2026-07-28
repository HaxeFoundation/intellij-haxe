package com.intellij.plugins.haxe.ide.quickfix.unresolved;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.intellij.plugins.haxe.ide.quickfix.HaxeQuickFixTestBase;
import org.jetbrains.annotations.NotNull;

@DisplayName("Quick fix: unresolved function")
public class UnresolvedFunctionQuickFixTest extends HaxeQuickFixTestBase {



  @Override
  protected @NotNull String getBasePath() {
    return "/unresolved/function";
  }


  @Test
  @DisplayName("create function assign")
  public void testCreateFunctionAssign() {
    doSingleTest("_create_function_assign.hx");
  }



}
