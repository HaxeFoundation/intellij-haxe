package com.intellij.plugins.haxe.actions.move.updown.declarations;

import com.intellij.plugins.haxe.actions.move.updown.HaxeMoveTestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Move statement: classes")
public class HaxeClassesMoveTest extends HaxeMoveTestBase {

    @Override
    protected String getBasePath() {
        return "/move/updown/declarations/classes";
    }



    @Test
    @DisplayName("swap place")
    public void testSwapPlace() throws Throwable {
        doTest();
    }
    @Test
    @DisplayName("swap place with meta and docs")
    public void testSwapPlaceWithMetaAndDocs() throws Throwable {
        doTest();
    }
    @Test
    @DisplayName("with meta and docs single line")
    public void testWithMetaAndDocsSingleLine() throws Throwable {
        doTest();
    }
    @Test
    @DisplayName("move line")
    public void testMoveLine() throws Throwable {
        doTest();
    }
}
