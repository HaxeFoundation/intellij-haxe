package com.intellij.plugins.haxe.runner.debugger.eval;

import static com.intellij.plugins.haxe.runner.debugger.eval.EvalDebugAdapter.stripTrailingSemicolons;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

public class EvalDebugAdapterTest {

  @Test
  public void stripsOneTrailingSemicolon() {
    // the eval VM's expression parser rejects a trailing ';' (Unexpected ;)
    assertEquals("this.member = 1", stripTrailingSemicolons("this.member = 1;"));
    assertEquals("member", stripTrailingSemicolons("member;"));
  }

  @Test
  public void stripsSemicolonsWithSurroundingWhitespace() {
    assertEquals("x + y", stripTrailingSemicolons("  x + y ; "));
    assertEquals("a", stripTrailingSemicolons("a ; ;"));
  }

  @Test
  public void leavesCleanExpressionsUntouched() {
    assertEquals("this.member = 1", stripTrailingSemicolons("this.member = 1"));
    assertEquals("arr[0]", stripTrailingSemicolons("arr[0]"));
  }

  @Test
  public void doesNotTouchInteriorSemicolons() {
    // no valid single expression has one, but if present it is the VM's to reject
    assertEquals("f(a; b)", stripTrailingSemicolons("f(a; b);"));
  }

  @Test
  public void nullPassesThrough() {
    assertNull(stripTrailingSemicolons(null));
  }
}
