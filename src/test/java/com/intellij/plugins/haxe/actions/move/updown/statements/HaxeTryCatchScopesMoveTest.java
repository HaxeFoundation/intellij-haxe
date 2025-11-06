package com.intellij.plugins.haxe.actions.move.updown.statements;

import com.intellij.plugins.haxe.actions.move.updown.HaxeMoveTestBase;
import org.junit.Test;

public class HaxeTryCatchScopesMoveTest extends HaxeMoveTestBase {

    @Override
    protected String getBasePath() {
        return "/move/updown/statements/scopes/try-catch";
    }



    @Test
    public void testMoveFromCatchSimple() throws Throwable {
        doTest();
    }
    @Test
    public void testMoveFromCatchToCatch() throws Throwable {
        doTest();
    }
    @Test
    public void testMoveFromTry() throws Throwable {
        doTest();
    }
    @Test
    public void testMoveIntoTry() throws Throwable {
        doTest();
    }
    @Test
    public void testMoveIntoCatch() throws Throwable {
        doTest(false, true);
    }


}
