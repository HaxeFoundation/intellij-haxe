package com.intellij.plugins.haxe.resolve;

import com.intellij.lang.LanguageAnnotators;
import com.intellij.plugins.haxe.HaxeCodeInsightFixtureTestCase;
import com.intellij.plugins.haxe.HaxeLanguage;
import com.intellij.plugins.haxe.ide.annotator.HaxeUnresolvedTypeAnnotator;
import com.intellij.plugins.haxe.ide.inspections.HaxeUnresolvedSymbolInspection;
import com.intellij.util.ArrayUtil;
import org.junit.Test;

public class HaxeModuleTest extends HaxeCodeInsightFixtureTestCase {
    @Override
    public void setUp() throws Exception {
        useHaxeToolkit();
        super.setUp();
        setTestStyleSettings(2);
    }

    @Override
    protected String getBasePath() {
        return "/resolve/modules/";
    }

    public void doTest(String... additionalFiles) {
        myFixture.configureByFiles(ArrayUtil.mergeArrays(new String[]{getTestName(false) + ".hx"}, additionalFiles));
        LanguageAnnotators.INSTANCE.addExplicitExtension(HaxeLanguage.INSTANCE, new HaxeUnresolvedTypeAnnotator());
        myFixture.enableInspections(HaxeUnresolvedSymbolInspection.class);
        myFixture.testHighlighting(true, true, true);
    }

    @Test
    public void testModuleImport() {
        doTest(
                "modules/ModuleWithMainClass.hx",
                "modules/ModuleWithoutMainClass.hx",
                "other/OtherModule.hx",
                "umods/UsingModule.hx"
                );
    }

}
