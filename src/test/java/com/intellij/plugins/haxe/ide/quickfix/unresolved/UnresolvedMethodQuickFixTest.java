package com.intellij.plugins.haxe.ide.quickfix.unresolved;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.intellij.plugins.haxe.ide.quickfix.HaxeQuickFixTestBase;
import org.jetbrains.annotations.NotNull;

@DisplayName("Quick fix: unresolved method")
public class UnresolvedMethodQuickFixTest extends HaxeQuickFixTestBase {


  @Override
  protected @NotNull String getBasePath() {
    return "/unresolved/method";
  }

  @Test
  @DisplayName("create method assign")
  public void testCreateMethodAssign() {
    doSingleTest("_create_method_assign.hx");
  }

  @Test
  @DisplayName("create method in if")
  public void testCreateMethodInIf() {
    doSingleTest("_create_method_if.hx");
  }

  @Test
  @DisplayName("create method void")
  public void testCreateMethodVoid() {
    doSingleTest("_create_method_void.hx");
  }

  @Test
  @DisplayName("create method other class")
  public void testCreateMethodOtherClass() {
    doSingleTest("_create_method_other_class.hx");
  }

  @Test
  @DisplayName("create method other class ref")
  public void testCreateMethodOtherClassRef() {
    doSingleTest("_create_method_other_class_ref.hx");
  }
}
