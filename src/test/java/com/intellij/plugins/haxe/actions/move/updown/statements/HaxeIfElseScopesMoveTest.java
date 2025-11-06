package com.intellij.plugins.haxe.actions.move.updown.statements;

import com.intellij.plugins.haxe.actions.move.updown.HaxeMoveTestBase;
import org.junit.Test;

public class HaxeIfElseScopesMoveTest extends HaxeMoveTestBase {

    @Override
    protected String getBasePath() {
        return "/move/updown/statements/scopes/if-else";
    }



    @Test
    public void testMoveInToIf() throws Throwable {
        doTest();
    }
    @Test
    public void testMoveOutFromIf() throws Throwable {
        doTest();
    }
    @Test
    public void testMoveFromElseIf() throws Throwable {
        doTest();
    }


}
