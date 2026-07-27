package com.intellij.plugins.haxe.ide.inlay.resolve;

import com.intellij.codeInsight.hints.declarative.InlayHintsProvider;
import com.intellij.plugins.haxe.ide.hint.types.HaxeInlayFieldHintsProvider;
import com.intellij.plugins.haxe.ide.hint.types.HaxeInlayLocalVariableHintsProvider;
import com.intellij.plugins.haxe.ide.inlay.HaxeInlayTestBase;
import org.junit.jupiter.api.Test;

public class HaxeTestUntypedUnificationTest extends HaxeInlayTestBase {

  InlayHintsProvider localVarHintsProvider = new HaxeInlayLocalVariableHintsProvider();
  InlayHintsProvider fieldHintsProvider = new HaxeInlayFieldHintsProvider();

  @Override
  protected  String getBasePath() {
    return "/inlay/resolve/";
  }

  @Override
  public void setUp() throws Exception {
    useHaxeToolkit();
    super.setUp();
    setTestStyleSettings(2);
  }


  @Test
  public void testNullUnificationLocalVar() throws Exception {
    doTest(localVarHintsProvider);
  }
  @Test
  public void testNullUnificationField() throws Exception {
    doTest(fieldHintsProvider);
  }


}
