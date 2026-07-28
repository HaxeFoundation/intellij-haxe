package com.intellij.plugins.haxe.actions.move.updown.statements;

import com.intellij.plugins.haxe.actions.move.updown.HaxeMoveTestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Move statement: try catch scopes")
public class HaxeTryCatchScopesMoveTest extends HaxeMoveTestBase {

    @Override
    protected String getBasePath() {
        return "/move/updown/statements/scopes/try-catch";
    }



    @Test
    @DisplayName("move from catch simple")
    public void testMoveFromCatchSimple() throws Throwable {
        doTest();
    }
    @Test
    @DisplayName("move from catch to catch")
    public void testMoveFromCatchToCatch() throws Throwable {
        doTest();
    }
    @Test
    @DisplayName("move from try")
    public void testMoveFromTry() throws Throwable {
        doTest();
    }
    @Test
    @DisplayName("move into try")
    public void testMoveIntoTry() throws Throwable {
        doTest();
    }
    @Test
    @DisplayName("move into catch")
    public void testMoveIntoCatch() throws Throwable {
        doTest(false, true);
    }


}
