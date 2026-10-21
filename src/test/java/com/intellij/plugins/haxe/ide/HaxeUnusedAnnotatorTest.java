package com.intellij.plugins.haxe.ide;

import com.intellij.plugins.haxe.ide.inspections.unused.HaxeUnusedFieldInspection;
import com.intellij.plugins.haxe.ide.inspections.unused.HaxeUnusedFunctionInspection;
import com.intellij.plugins.haxe.ide.inspections.unused.HaxeUnusedLocalVarInspection;
import com.intellij.plugins.haxe.ide.inspections.unused.HaxeUnusedMethodInspection;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;



@DisplayName("Annotation: unused annotator")
public class HaxeUnusedAnnotatorTest extends HaxeSemanticAnnotatorTestBase {
    @Override
    protected String getBasePath() {
        return "/annotation.unused/";
    }

    @Test
    @DisplayName("unused fields and variables test")
    public void testUnusedFieldsAndVariablesTest() throws Exception {
        myFixture.enableInspections(
                HaxeUnusedFieldInspection.class,
                HaxeUnusedLocalVarInspection.class
        );
        doTestNoFixWithWeakWarnings("UnusedFieldsTestOutside.hx");
    }

    @Test
    @DisplayName("unused methods and functions test")
    public void testUnusedMethodsAndFunctionsTest() throws Exception {
        myFixture.enableInspections(
                HaxeUnusedFunctionInspection.class,
                HaxeUnusedMethodInspection.class
        );
        doTestNoFixWithWeakWarnings("UnusedMethodsTestOutside.hx");
    }

    @Test
    @DisplayName("unused method abstract impl test")
    public void testUnusedMethodAbstractImplTest() throws Exception {
        myFixture.enableInspections(
                HaxeUnusedFunctionInspection.class,
                HaxeUnusedMethodInspection.class
        );
        doTestNoFixWithWeakWarnings();
    }

    @Test
    @DisplayName("unused module level function test")
    public void testUnusedModuleLevelFunctionTest() throws Exception {
        myFixture.enableInspections(
                HaxeUnusedFunctionInspection.class,
                HaxeUnusedMethodInspection.class
        );
        doTestNoFixWithWeakWarnings();
    }
}
