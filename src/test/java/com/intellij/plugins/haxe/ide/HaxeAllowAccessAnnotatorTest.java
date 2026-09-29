package com.intellij.plugins.haxe.ide;

import com.intellij.plugins.haxe.HaxeCodeInsightFixtureTestCase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;



@DisplayName("Annotation: allow access annotator")
public class HaxeAllowAccessAnnotatorTest extends HaxeCodeInsightFixtureTestCase {
    @Override
    public void setUp() throws Exception {
        // for use when idempotence check problems occur and we need consistent results.
        //Registry.get("platform.random.idempotence.check.rate").setValue(1, getTestRootDisposable());
        useHaxeToolkit();
        super.setUp();
        setTestStyleSettings(2);
    }

    @Override
    protected String getBasePath() {
        return "/annotation.access/";
    }

    @Override
    protected String fixturePrefix() {
        return "accesscontrol/";
    }

    private void doTest(String... additionalFiles) throws Exception {
        doHighlightingTest(true, false, true, null, additionalFiles);
    }


    @Test
    @DisplayName("static access")
    public void testStaticAccess() throws Exception {
        doTest();
    }

    @Test
    @DisplayName("meta access")
    public void testMetaAccess() throws Exception {
        doTest("accesscontrol/AccessMetaTestClass.hx");
    }


    @Test
    @DisplayName("test method allow on property")
    public void testTestMethodAllowOnProperty() throws Exception {
        doTest("accesscontrol/PrivateStaticMembers.hx");
    }
    @Test
    @DisplayName("test class allow on field")
    public void testTestClassAllowOnField() throws Exception {
        doTest("accesscontrol/PrivateStaticMembers.hx");
    }
    @Test
    @DisplayName("test class allow on class")
    public void testTestClassAllowOnClass() throws Exception {
        doTest("accesscontrol/PrivateStaticMembers.hx");
    }
    @Test
    @DisplayName("test module level")
    public void testTestModuleLevel() throws Exception {
        doTest("accesscontrol/PrivateStaticMembers.hx");
    }


}
