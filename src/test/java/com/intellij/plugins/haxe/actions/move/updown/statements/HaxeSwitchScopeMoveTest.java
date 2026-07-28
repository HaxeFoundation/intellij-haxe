package com.intellij.plugins.haxe.actions.move.updown.statements;

import com.intellij.plugins.haxe.actions.move.updown.HaxeMoveTestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Move statement: switch scope")
public class HaxeSwitchScopeMoveTest extends HaxeMoveTestBase {

    @Override
    protected String getBasePath() {
        return "/move/updown/statements/scopes/switch-case";
    }


    @Test
    @DisplayName("move variable through switch case")
    public void testMoveVariableThroughSwitchCase() throws Throwable {
        doTestAndAlsoCheckReversed("hx", 7);
    }
    @Test
    @DisplayName("move if else through switch case")
    public void testMoveIfElseThroughSwitchCase() throws Throwable {
        doTestAndAlsoCheckReversed("hx", 7);
    }


}

