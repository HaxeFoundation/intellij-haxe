package com.intellij.plugins.haxe.lang;

import com.intellij.lexer.Lexer;
import com.intellij.plugins.haxe.HaxeLightFixtureTestCase;
import com.intellij.plugins.haxe.lang.lexer.HaxeLexer;
import com.intellij.psi.tree.IElementType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.FieldSource;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.params.provider.Arguments.arguments;

/**
 * The editor's incremental highlighter re-lexes from a mid-file token
 * boundary using only that boundary's saved int state. Whether a '<' is an
 * operator or an XML-literal start depends on the preceding significant
 * token, so that context is folded into the state - without it, a restart
 * before "Map<Int , String>" relexed the '<' as XML and the highlighting
 * stayed broken while the PSI (always lexed from offset 0) looked fine.
 * Conditional compilation folds its region into the state the same way
 * (CC_BLOCK), so a restart inside a dead branch never lexes it as live.
 */
@DisplayName("Lexing: incremental restart")
public class HaxeLexerRestartTest extends HaxeLightFixtureTestCase {

  @Override
  protected String getBasePath() {
    return "/formatter/";
  }

  @Test
  @DisplayName("restart before type parameters keeps the operator context")
  public void testRestartBeforeTypeParametersKeepsTheOperatorContext() {
    String text = "class Foo { var lookup:Map<Int , String>; }";
    Lexer full = new HaxeLexer(getProject());
    full.start(text);

    int lessThanStart = -1;
    int stateAtLessThan = -1;
    IElementType fullLexType = null;
    while (full.getTokenType() != null) {
      if ("<".contentEquals(full.getTokenSequence())) {
        lessThanStart = full.getTokenStart();
        stateAtLessThan = full.getState();
        fullLexType = full.getTokenType();
        break;
      }
      full.advance();
    }
    assertTrue(lessThanStart > 0, "the sample must contain a '<' token");
    assertNotEquals(0, stateAtLessThan,
                    "the state before '<' must carry the value context, or the highlighter restarts blind");

    Lexer restarted = new HaxeLexer(getProject());
    restarted.start(text, lessThanStart, text.length(), stateAtLessThan);
    assertEquals(fullLexType, restarted.getTokenType(),
                 "a restart at the recorded state must lex '<' exactly like the full pass");
  }

  // (name, source, whether top-level code after the conditional must be restartable)
  static final List<Arguments> RESTART_SOURCES = List.of(
    arguments("top-level dead branch", """
      class A {}
      #if never
      class Dead { var x:Int; }
      #else
      class Live {}
      #end
      class B {}""", true),
    arguments("nested conditionals", """
      #if never
      #if js
      var a = 1;
      #end
      var b = 2;
      #end
      class After {}""", true),
    arguments("elseif chain in class body", """
      class C {
      \t#if never
      \tvar a:Int;
      \t#elseif never_either
      \tvar b:Int;
      \t#else
      \tvar c:Int;
      \t#end
      }
      class D {}""", true),
    arguments("strings and templates around a dead branch", """
      class E {
      \tfunction f():Void {
      \t\tvar s = "before";
      \t\t#if never
      \t\tvar t = 'tmp ${1 + 1}';
      \t\t#end
      \t\tvar u = "after";
      \t}
      }""", true),
    arguments("inline expression conditional", """
      class F {
      \tfunction f():Void {
      \t\tvar mode = #if debug "d" #else "r" #end;
      \t\tvar tail = 1;
      \t}
      }""", true),
    arguments("cross branch operator soup", """
      class G {
      \tfunction f():Void {
      \t\tvar x = 1 #if never < #else > #end 2;
      \t}
      }
      class H {}""", true),
    // an unterminated #if keeps the file in CC state to EOF - nothing to restart, but it must not diverge either
    arguments("missing #end at eof", """
      class I {}
      #if never
      var dead:Int;""", false),
    arguments("stray #end without #if", """
      class J {}
      #end
      class K {}""", true));

  @ParameterizedTest(name = "{0}")
  @FieldSource("RESTART_SOURCES")
  @DisplayName("lexer restart at zero state boundaries reproduces the stream")
  public void testLexerRestartAtZeroStateBoundariesReproducesTheStream(String name, String source, boolean expectRestartableTail) {
    checkRestartAtEveryZeroStateBoundary(source, expectRestartableTail);
  }

  /**
   * The editor's incremental highlighter restarts lexing only at boundaries
   * whose saved state equals a fresh lexer's - so at every such boundary a
   * cold start must reproduce the remaining stream exactly. A zero state
   * inside a conditional would make a restart lex dead code as live; the
   * CC_BLOCK lexer state exists to prevent that.
   */
  private void checkRestartAtEveryZeroStateBoundary(String source, boolean expectRestartableTail) {
    checkRestartAtEveryZeroStateBoundary(source, expectRestartableTail, () -> new HaxeLexer(getProject()));
    checkRestartAtEveryZeroStateBoundary(source, expectRestartableTail, () -> HaxeLexer.forHighlighting(getProject()));
  }

  private void checkRestartAtEveryZeroStateBoundary(String source, boolean expectRestartableTail, Supplier<Lexer> lexerFactory) {
    Lexer lexer = lexerFactory.get();
    lexer.start(source);
    List<LexStep> full = new ArrayList<>();
    while (lexer.getTokenType() != null) {
      String type = lexer.getTokenType().toString();
      int start = lexer.getTokenStart();
      int end = lexer.getTokenEnd();
      lexer.advance();
      full.add(new LexStep(start, end, type, lexer.getState()));
    }
    assertFalse(full.isEmpty(), "the source must lex to something");

    int restartableBoundaries = 0;
    for (int i = 0; i < full.size() - 1; i++) {
      if (full.get(i).stateAfter() != 0) continue;
      restartableBoundaries++;

      int offset = full.get(i).end();
      List<LexStep> restarted = lexFrom(source, offset, lexerFactory);
      List<LexStep> remainder = full.subList(i + 1, full.size());
      assertEquals(remainder.size(), restarted.size(), "restart at " + offset + ": token count diverged");
      for (int j = 0; j < remainder.size(); j++) {
        LexStep expected = remainder.get(j);
        LexStep actual = restarted.get(j);
        boolean sameToken = expected.start() == actual.start()
                            && expected.end() == actual.end()
                            && expected.type().equals(actual.type());
        assertTrue(sameToken, "restart at " + offset + " diverged: expected " + expected + " but got " + actual);
      }
    }
    if (expectRestartableTail) {
      assertTrue(restartableBoundaries > 0, "expected at least one restartable boundary - otherwise this source proves nothing");
    }
  }

  private List<LexStep> lexFrom(String source, int offset, Supplier<Lexer> lexerFactory) {
    Lexer lexer = lexerFactory.get();
    lexer.start(source, offset, source.length(), 0);
    List<LexStep> steps = new ArrayList<>();
    while (lexer.getTokenType() != null) {
      steps.add(new LexStep(lexer.getTokenStart(), lexer.getTokenEnd(), lexer.getTokenType().toString(), -1));
      lexer.advance();
    }
    return steps;
  }

  private record LexStep(int start, int end, String type, int stateAfter) {}
}
