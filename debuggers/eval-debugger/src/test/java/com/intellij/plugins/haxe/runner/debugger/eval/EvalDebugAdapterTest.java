package com.intellij.plugins.haxe.runner.debugger.eval;

import static com.intellij.plugins.haxe.runner.debugger.eval.EvalDebugAdapter.stripTrailingSemicolons;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Eval debugger: debug adapter")
public class EvalDebugAdapterTest {

  @Test
  @DisplayName("strips one trailing semicolon")
  public void stripsOneTrailingSemicolon() {
    // the eval VM's expression parser rejects a trailing ';' (Unexpected ;)
    assertEquals("this.member = 1", stripTrailingSemicolons("this.member = 1;"));
    assertEquals("member", stripTrailingSemicolons("member;"));
  }

  @Test
  @DisplayName("strips semicolons with surrounding whitespace")
  public void stripsSemicolonsWithSurroundingWhitespace() {
    assertEquals("x + y", stripTrailingSemicolons("  x + y ; "));
    assertEquals("a", stripTrailingSemicolons("a ; ;"));
  }

  @Test
  @DisplayName("leaves clean expressions untouched")
  public void leavesCleanExpressionsUntouched() {
    assertEquals("this.member = 1", stripTrailingSemicolons("this.member = 1"));
    assertEquals("arr[0]", stripTrailingSemicolons("arr[0]"));
  }

  @Test
  @DisplayName("does not touch interior semicolons")
  public void doesNotTouchInteriorSemicolons() {
    // no valid single expression has one, but if present it is the VM's to reject
    assertEquals("f(a; b)", stripTrailingSemicolons("f(a; b);"));
  }

  @Test
  @DisplayName("null passes through")
  public void nullPassesThrough() {
    assertNull(stripTrailingSemicolons(null));
  }
}
