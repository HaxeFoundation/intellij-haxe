package com.intellij.plugins.haxe.ide;

import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.plugins.haxe.compilation.HaxeCompilerError;
import junit.framework.TestCase;

/**
 * @author: Fedor.Korotkov
 */
public class HaxeCompilerErrorParsingTest extends TestCase {
  public void testUserProperiesErrorWin() {
    final String error =
      "C:/Users/fedor.korotkov/workspace/haxe-bubble-breaker/src/Main.hx:5: characters 0-21 : Class not found : StringTools212";
    final String rootPath = "C:/Users/fedor.korotkov/workspace/haxe-bubble-breaker";
    final HaxeCompilerError compilerError = HaxeCompilerError.create(rootPath, error);

    assertNotNull(compilerError);
    assertEquals("C:/Users/fedor.korotkov/workspace/haxe-bubble-breaker/src/Main.hx", compilerError.getPath());
    assertEquals("Class not found : StringTools212", compilerError.getErrorMessage());
    assertEquals(5, compilerError.getLine());
  }

  public void testNMEErrorWin() {
    final String error = "src/Main.hx:5: characters 0-21 : Class not found : StringTools212";
    final String rootPath = "C:/Users/fedor.korotkov/workspace/haxe-bubble-breaker";
    final HaxeCompilerError compilerError = HaxeCompilerError.create(rootPath, error);

    assertNotNull(compilerError);
    assertEquals("C:/Users/fedor.korotkov/workspace/haxe-bubble-breaker/src/Main.hx", compilerError.getPath());
    assertEquals("Class not found : StringTools212", compilerError.getErrorMessage());
    assertEquals(5, compilerError.getLine());
  }
}
