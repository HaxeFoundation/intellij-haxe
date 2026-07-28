package com.intellij.plugins.haxe.actions.move.updown.statements;

import com.intellij.plugins.haxe.actions.move.updown.HaxeMoveTestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Move statement: if else scopes")
public class HaxeIfElseScopesMoveTest extends HaxeMoveTestBase {

    @Override
    protected String getBasePath() {
        return "/move/updown/statements/scopes/if-else";
    }



    @Test
    @DisplayName("move in to if")
    public void testMoveInToIf() throws Throwable {
        doTest();
    }
    @Test
    @DisplayName("move out from if")
    public void testMoveOutFromIf() throws Throwable {
        doTest();
    }
    @Test
    @DisplayName("move from else if")
    public void testMoveFromElseIf() throws Throwable {
        doTest();
    }


}
