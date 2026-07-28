package com.intellij.plugins.haxe.ide.quickfix.unresolved;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.intellij.plugins.haxe.ide.quickfix.HaxeQuickFixTestBase;
import org.jetbrains.annotations.NotNull;

@DisplayName("Quick fix: unresolved var")
public class UnresolvedVarQuickFixTest extends HaxeQuickFixTestBase {



  @Override
  protected @NotNull String getBasePath() {
    return "/unresolved/var";
  }


  @Test
  @DisplayName("create var assign")
  public void testCreateVarAssign() {
    doSingleTest("_create_var_assign.hx");
  }
  @Test
  @DisplayName("create var assign function")
  public void testCreateVarAssignFunction() {
    doSingleTest("_create_var_assign_function.hx");
  }

  @Test
  @DisplayName("create var in if")
  public void testCreateVarInIf() {
    doSingleTest("_create_var_if.hx");
  }


}
