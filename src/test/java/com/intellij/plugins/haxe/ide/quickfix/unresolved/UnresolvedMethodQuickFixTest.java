package com.intellij.plugins.haxe.ide.quickfix.unresolved;

import com.intellij.plugins.haxe.ide.quickfix.HaxeQuickFixTestBase;
import org.jetbrains.annotations.NotNull;

public class UnresolvedMethodQuickFixTest extends HaxeQuickFixTestBase {


  @Override
  protected @NotNull String getBasePath() {
    return "/unresolved/method";
  }

  public void testCreateMethodAssign() {
    doSingleTest("_create_method_assign.hx");
  }

  public void testCreateMethodInIf() {
    doSingleTest("_create_method_if.hx");
  }

  public void testCreateMethodVoid() {
    doSingleTest("_create_method_void.hx");
  }

  public void testCreateMethodOtherClass() {
    doSingleTest("_create_method_other_class.hx");
  }

  public void testCreateMethodOtherClassRef() {
    doSingleTest("_create_method_other_class_ref.hx");
  }
}
