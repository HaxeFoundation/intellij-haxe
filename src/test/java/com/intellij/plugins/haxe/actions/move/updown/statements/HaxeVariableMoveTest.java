package com.intellij.plugins.haxe.actions.move.updown.statements;

import com.intellij.plugins.haxe.actions.move.updown.HaxeMoveTestBase;
import org.junit.Test;

public class HaxeVariableMoveTest extends HaxeMoveTestBase {

    @Override
    protected String getBasePath() {
        return "/move/updown/statements/variable";
    }



    @Test
    public void testSwapPlace() throws Throwable {
        doTest();
    }
    @Test
    public void testMoveIntoFunctionScope() throws Throwable {
        doTest();
    }
    @Test
    public void testMoveOutOfFunctionScope() throws Throwable {
        doTest();
    }
    @Test
    public void testMoveIntoDoWhileScope() throws Throwable {
        doTest();
    }
    @Test
    public void testMoveOutOfDoWhileScope() throws Throwable {
        doTest();
    }

}
