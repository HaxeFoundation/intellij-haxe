package com.intellij.plugins.haxe.ide.quickfix.unresolved;

import org.junit.jupiter.api.Test;

import com.intellij.plugins.haxe.ide.quickfix.HaxeQuickFixTestBase;
import org.jetbrains.annotations.NotNull;

public class UnresolvedMethodQuickFixTest extends HaxeQuickFixTestBase {


  @Override
  protected @NotNull String getBasePath() {
    return "/unresolved/method";
  }

  @Test
  public void testCreateMethodAssign() {
    doSingleTest("_create_method_assign.hx");
  }

  @Test
  public void testCreateMethodInIf() {
    doSingleTest("_create_method_if.hx");
  }

  @Test
  public void testCreateMethodVoid() {
    doSingleTest("_create_method_void.hx");
  }

  @Test
  public void testCreateMethodOtherClass() {
    doSingleTest("_create_method_other_class.hx");
  }

  @Test
  public void testCreateMethodOtherClassRef() {
    doSingleTest("_create_method_other_class_ref.hx");
  }
}
