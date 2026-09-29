package com.intellij.plugins.haxe.ide;

import com.intellij.plugins.haxe.HaxeCodeInsightFixtureTestCase;
import com.intellij.plugins.haxe.ide.inspections.unused.HaxeUnusedFieldInspection;
import com.intellij.plugins.haxe.ide.inspections.unused.HaxeUnusedFunctionInspection;
import com.intellij.plugins.haxe.ide.inspections.unused.HaxeUnusedLocalVarInspection;
import com.intellij.plugins.haxe.ide.inspections.unused.HaxeUnusedMethodInspection;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;



@DisplayName("Annotation: unused annotator")
public class HaxeUnusedAnnotatorTest extends HaxeCodeInsightFixtureTestCase {
    @Override
    public void setUp() throws Exception {
        useHaxeToolkit();
        super.setUp();
        setTestStyleSettings(2);
    }

    @Override
    protected String getBasePath() {
        return "/annotation.unused/";
    }

    private void doTest(String... additionalFiles) throws Exception {
        doHighlightingTest(true, false, true, null, additionalFiles);
    }

    @Test
    @DisplayName("unused fields and variables test")
    public void testUnusedFieldsAndVariablesTest() throws Exception {
        myFixture.enableInspections(
                HaxeUnusedFieldInspection.class,
                HaxeUnusedLocalVarInspection.class
        );
        doTest("UnusedFieldsTestOutside.hx");
    }

    @Test
    @DisplayName("unused methods and functions test")
    public void testUnusedMethodsAndFunctionsTest() throws Exception {
        myFixture.enableInspections(
                HaxeUnusedFunctionInspection.class,
                HaxeUnusedMethodInspection.class
        );
        doTest("UnusedMethodsTestOutside.hx");
    }

    @Test
    @DisplayName("unused method abstract impl test")
    public void testUnusedMethodAbstractImplTest() throws Exception {
        myFixture.enableInspections(
                HaxeUnusedFunctionInspection.class,
                HaxeUnusedMethodInspection.class
        );
        doTest();
    }

    @Test
    @DisplayName("unused module level function test")
    public void testUnusedModuleLevelFunctionTest() throws Exception {
        myFixture.enableInspections(
                HaxeUnusedFunctionInspection.class,
                HaxeUnusedMethodInspection.class
        );
        doTest();
    }
}
