package com.intellij.plugins.haxe.ide.inlay.all;

import com.intellij.codeInsight.hints.declarative.InlayHintsProvider;
import com.intellij.plugins.haxe.ide.inlay.HaxeInlayTestBase;
import org.junit.Test;

public class HaxeAllInlayTest extends HaxeInlayTestBase {

    InlayHintsProvider hintsProvider = new AllInlayHintsProvider();

    @Override
    protected String getBasePath() {
        return "/inlay/all/";
    }

    @Override
    public void setUp() throws Exception {
        useHaxeToolkit();
        super.setUp();
        setTestStyleSettings(2);
    }

    @Test
    public void testParameterMonomorph() throws Exception {
        doTest(hintsProvider);
    }
}
