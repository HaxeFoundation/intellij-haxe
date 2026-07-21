package com.intellij.plugins.haxe.runner.debugger.hashlink;

import com.intellij.openapi.util.TextRange;
import com.intellij.plugins.haxe.HaxeCodeInsightFixtureTestCase;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.StepInTarget;
import com.intellij.psi.PsiElement;
import com.intellij.xdebugger.XDebuggerUtil;
import com.intellij.xdebugger.XSourcePosition;
import java.util.List;

/**
 * The target↔highlight pairing for smart step into: the adapter reports
 * targets in EXECUTION order (bytecode order), so the PSI call names must be
 * collected in execution order too — a post-order walk, since a call executes
 * after its receiver and its arguments. With both lists in the same order,
 * same-named calls on one line ({@code a.reset(b.reset())}: two targets both
 * labeled {@code .reset}) pair with their own occurrence; the old source-order
 * collection made the inner call's target steal the leftmost name, swapping
 * the highlights — and the user, choosing by highlight, stepped into the
 * wrong method.
 */
public class HashLinkSmartStepIntoOrderTest extends HaxeCodeInsightFixtureTestCase {

  @Override
  protected String getBasePath() {
    return "";
  }

  private List<PsiElement> namesOnCaretLine(String mainBody) {
    myFixture.configureByText("Main.hx",
                              "class A { public function new() {} public function reset(v:Int):Int { return v; }"
                              + " public function first():A { return this; } public function second():A { return this; } }\n"
                              + "class B { public function new() {} public function reset():Int { return 1; } }\n"
                              + "class Main { static function main() { var a = new A(); var b = new B(); "
                              + mainBody + "<caret> } }");
    int caretLine = myFixture.getEditor().getDocument()
      .getLineNumber(myFixture.getCaretOffset());
    XSourcePosition position = XDebuggerUtil.getInstance()
      .createPosition(myFixture.getFile().getVirtualFile(), caretLine);
    assertNotNull(position);
    return HashLinkSmartStepIntoHandler.callNameElementsInExecutionOrder(getProject(), position);
  }

  public void testNestedCallsCollectInExecutionOrderInnerFirst() {
    // b.reset() is the ARGUMENT: it executes first although it is textually last
    List<PsiElement> names = namesOnCaretLine("a.reset(b.reset());");
    assertEquals(2, names.size());
    assertEquals("reset", names.get(0).getText());
    assertEquals("reset", names.get(1).getText());
    assertTrue("the inner (textually later) call executes first",
               names.get(0).getTextOffset() > names.get(1).getTextOffset());
  }

  public void testChainedCallsCollectInExecutionOrderLeftToRight() {
    List<PsiElement> names = namesOnCaretLine("a.first().second();");
    assertEquals(2, names.size());
    assertEquals("first", names.get(0).getText());
    assertEquals("second", names.get(1).getText());
  }

  public void testSameNamedTargetsPairWithTheirOwnOccurrence() {
    List<PsiElement> names = namesOnCaretLine("a.reset(b.reset());");
    // the adapter reports execution order: B.reset (the argument) first
    List<StepInTarget> targets = List.of(target(1, "B.reset"), target(2, "A.reset"));

    List<TextRange> ranges = HashLinkSmartStepIntoHandler.matchCallRanges(targets, names);

    assertEquals(2, ranges.size());
    assertEquals("B.reset highlights the inner (textually later) call",
                 names.get(0).getTextRange(), ranges.get(0));
    assertEquals("A.reset highlights the outer (textually first) call",
                 names.get(1).getTextRange(), ranges.get(1));
    assertTrue("the highlights must not be swapped",
               ranges.get(0).getStartOffset() > ranges.get(1).getStartOffset());
  }

  /**
   * Mid-line, the adapter reports only the calls still AHEAD — a suffix of the
   * line's execution order — while the PSI names cover the whole line. A
   * duplicate callee must pair with its LATER occurrence, not steal the
   * already-executed one at the start of the line.
   */
  public void testRemainingTargetsPairWithTheLaterOccurrenceOfADuplicateName() {
    // full line: first, second, first (execution order = source order for a chain)
    List<PsiElement> names = namesOnCaretLine("a.first().second().first();");
    assertEquals(3, names.size());
    // the initial first() already ran; the adapter offers the rest
    List<StepInTarget> targets = List.of(target(1, "A.second"), target(2, "A.first"));

    List<TextRange> ranges = HashLinkSmartStepIntoHandler.matchCallRanges(targets, names);

    assertEquals(2, ranges.size());
    assertEquals("second highlights its own call", names.get(1).getTextRange(), ranges.get(0));
    assertEquals("the remaining first() highlights the LAST occurrence",
                 names.get(2).getTextRange(), ranges.get(1));
  }

  public void testUnmatchableTargetGetsNoHighlightButKeepsAlignment() {
    List<PsiElement> names = namesOnCaretLine("a.first().second();");
    // an extra target the PSI knows nothing about (e.g. an inlined helper)
    List<StepInTarget> targets = List.of(target(1, "A.first"), target(2, "Hidden.helper"), target(3, "A.second"));

    List<TextRange> ranges = HashLinkSmartStepIntoHandler.matchCallRanges(targets, names);

    assertEquals(3, ranges.size());
    assertEquals(names.get(0).getTextRange(), ranges.get(0));
    assertNull("no PSI call to highlight for the unknown target", ranges.get(1));
    assertEquals(names.get(1).getTextRange(), ranges.get(2));
  }

  private static StepInTarget target(int id, String label) {
    StepInTarget target = new StepInTarget();
    target.setId(id);
    target.setLabel(label);
    return target;
  }
}
