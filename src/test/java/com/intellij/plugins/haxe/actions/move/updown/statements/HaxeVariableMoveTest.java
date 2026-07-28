package com.intellij.plugins.haxe.actions.move.updown.statements;

import com.intellij.plugins.haxe.actions.move.updown.HaxeMoveTestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Move statement: variable")
public class HaxeVariableMoveTest extends HaxeMoveTestBase {

    @Override
    protected String getBasePath() {
        return "/move/updown/statements/variable";
    }



    @Test
    @DisplayName("swap place")
    public void testSwapPlace() throws Throwable {
        doTest();
    }
    @Test
    @DisplayName("move into function scope")
    public void testMoveIntoFunctionScope() throws Throwable {
        doTest();
    }
    @Test
    @DisplayName("move out of function scope")
    public void testMoveOutOfFunctionScope() throws Throwable {
        doTest();
    }
    @Test
    @DisplayName("move into do while scope")
    public void testMoveIntoDoWhileScope() throws Throwable {
        doTest();
    }
    @Test
    @DisplayName("move out of do while scope")
    public void testMoveOutOfDoWhileScope() throws Throwable {
        doTest();
    }

}
