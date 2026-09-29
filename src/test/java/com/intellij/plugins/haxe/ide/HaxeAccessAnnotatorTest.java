package com.intellij.plugins.haxe.ide;

import com.intellij.plugins.haxe.HaxeCodeInsightFixtureTestCase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;



@DisplayName("Annotation: access annotator")
public class HaxeAccessAnnotatorTest extends HaxeCodeInsightFixtureTestCase {
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

    private void doTest(String... additionalFiles) throws Exception {
        doHighlightingTest(true, false, true, null, additionalFiles);
    }

    // KEYWORD access control
    @Test
    @DisplayName("access modifiers static access")
    public void testAccessModifiersStaticAccess() throws Exception {
        doTest();
    }
    @Test
    @DisplayName("access modifiers instance access")
    public void testAccessModifiersInstanceAccess() throws Exception {
        doTest();
    }
    @Test
    @DisplayName("access modifiers chain access")
    public void testAccessModifiersChainAccess() throws Exception {
        doTest();
    }
    // METADATA access control
    @Test
    @DisplayName("meta private access")
    public void testMetaPrivateAccess() throws Exception {
        doTest();
    }
}
