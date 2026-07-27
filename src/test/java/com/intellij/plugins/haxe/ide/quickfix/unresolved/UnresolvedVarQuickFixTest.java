package com.intellij.plugins.haxe.ide.quickfix.unresolved;

import org.junit.jupiter.api.Test;

import com.intellij.plugins.haxe.ide.quickfix.HaxeQuickFixTestBase;
import org.jetbrains.annotations.NotNull;

public class UnresolvedVarQuickFixTest extends HaxeQuickFixTestBase {



  @Override
  protected @NotNull String getBasePath() {
    return "/unresolved/var";
  }


  @Test
  public void testCreateVarAssign() {
    doSingleTest("_create_var_assign.hx");
  }
  @Test
  public void testCreateVarAssignFunction() {
    doSingleTest("_create_var_assign_function.hx");
  }

  @Test
  public void testCreateVarInIf() {
    doSingleTest("_create_var_if.hx");
  }


}
