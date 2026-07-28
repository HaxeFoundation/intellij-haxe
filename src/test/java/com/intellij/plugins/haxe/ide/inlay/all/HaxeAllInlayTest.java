package com.intellij.plugins.haxe.ide.inlay.all;

import com.intellij.codeInsight.hints.declarative.InlayHintsProvider;
import com.intellij.plugins.haxe.ide.inlay.HaxeInlayTestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Inlay hints: all")
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
    @DisplayName("parameter monomorph")
    public void testParameterMonomorph() throws Exception {
        doTest(hintsProvider);
    }
    @Test
    @DisplayName("constructor monomorph")
    public void testConstructorMonomorph() throws Exception {
        doTest(hintsProvider);
    }
}
