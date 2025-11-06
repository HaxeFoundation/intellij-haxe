package com.intellij.plugins.haxe.actions.move.updown.declarations;

import com.intellij.plugins.haxe.actions.move.updown.HaxeMoveTestBase;
import org.junit.Test;

public class HaxeModuleMoveTest extends HaxeMoveTestBase {

    @Override
    protected String getBasePath() {
        return "/move/updown/declarations/module";
    }



    @Test
    public void testSwapPlace() throws Throwable {
        doTest();
    }
    @Test
    public void testSwapPlaceWithMetaAndDocs() throws Throwable {
        doTest();
    }
    @Test
    public void testWithMetaAndDocsSingleLine() throws Throwable {
        doTest();
    }
    @Test
    public void testMoveLine() throws Throwable {
        doTest();
    }
}
