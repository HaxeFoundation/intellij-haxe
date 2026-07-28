package com.intellij.plugins.haxe.ide.quickfix.unresolved;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.intellij.plugins.haxe.ide.quickfix.HaxeQuickFixTestBase;
import org.jetbrains.annotations.NotNull;

@DisplayName("Quick fix: unresolved paramter")
public class UnresolvedParamterQuickFixTest extends HaxeQuickFixTestBase {



  @Override
  protected @NotNull String getBasePath() {
    return "/unresolved/parameter";
  }


  @Test
  @DisplayName("create parameter assign")
  public void testCreateParameterAssign() {
    doSingleTest("_create_parameter_assign.hx");
  }



}
