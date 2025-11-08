package com.intellij.plugins.haxe.ide.inlay;

import com.intellij.codeInsight.daemon.impl.ParameterHintsPresentationManager;
import com.intellij.openapi.editor.Inlay;
import com.intellij.plugins.haxe.HaxeCodeInsightFixtureTestCase;
import org.junit.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class HaxeMethodParameterInlayTest extends HaxeCodeInsightFixtureTestCase {

    @Override
    protected String getBasePath() {
        return "/inlay/haxe.method.parameter/";
    }

    @Override
    public void setUp() throws Exception {
        useHaxeToolkit();
        super.setUp();
        setTestStyleSettings(2);
    }

    @Test
    public void testParameterInlays() throws Exception {
        doTest();
    }


    protected void doTest() throws Exception {
        preareAndHighlight();
        compareContent();

    }

    private void preareAndHighlight() {
        String inputFile = getTestDataPath() + getTestName(false) + ".hx";
        myFixture.configureByFile(inputFile);
        myFixture.doHighlighting();
    }

    private void compareContent() throws Exception {
        String testPathAndNameBase = getTestDataPath() + getTestName(false);
        String ExpectedFile = testPathAndNameBase + "_expected.hx";
        String expected = Files.readString(Path.of(ExpectedFile));
        //seems to be an issue with windows line endings and inlays so to avoid any issues we replace them here.
        expected = expected.replaceAll("\\r\\n?", "\n");

        final ParameterHintsPresentationManager manager = ParameterHintsPresentationManager.getInstance();
        List<Inlay<?>> inlays = manager.getParameterHintsInRange(myFixture.getEditor(), 0, myFixture.getEditor().getDocument().getTextLength());

        final StringBuilder actualText = new StringBuilder(myFixture.getFile().getText());
        int offset = 0;
        for (Inlay<?> inlay : inlays) {
            String hintName = manager.getHintText(inlay);
            if (hintName == null) continue;
            String hintText = hintName.substring(0, hintName.length() - 1);
            String hintString = "/*<# " + hintText + " #>*/";
            actualText.insert(inlay.getOffset() + offset, hintString);
            offset += hintString.length();
        }
        assertEquals(expected, actualText.toString());
    }

}
