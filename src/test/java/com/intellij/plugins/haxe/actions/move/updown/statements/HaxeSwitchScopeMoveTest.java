package com.intellij.plugins.haxe.actions.move.updown.statements;

import com.intellij.plugins.haxe.actions.move.updown.HaxeMoveTestBase;
import org.junit.Test;

public class HaxeSwitchScopeMoveTest extends HaxeMoveTestBase {

    @Override
    protected String getBasePath() {
        return "/move/updown/statements/scopes/switch-case";
    }


    @Test
    public void testMoveVariableThroughSwitchCase() throws Throwable {
        doTestAndAlsoCheckReversed("hx", 7);
    }
    @Test
    public void testMoveIfElseThroughSwitchCase() throws Throwable {
        doTestAndAlsoCheckReversed("hx", 7);
    }


}

