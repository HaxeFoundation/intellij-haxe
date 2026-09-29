package com.intellij.plugins.haxe.lang;

import com.intellij.lexer.Lexer;
import com.intellij.plugins.haxe.HaxeLightFixtureTestCase;
import com.intellij.plugins.haxe.lang.lexer.HaxeLexer;
import com.intellij.psi.tree.IElementType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The editor's incremental highlighter re-lexes from a mid-file token
 * boundary using only that boundary's saved int state. Whether a '<' is an
 * operator or an XML-literal start depends on the preceding significant
 * token, so that context is folded into the state - without it, a restart
 * before "Map<Int , String>" relexed the '<' as XML and the highlighting
 * stayed broken while the PSI (always lexed from offset 0) looked fine.
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
}
