package com.intellij.plugins.haxe.resolve;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Resolve: module")
public class HaxeModuleTest extends HaxeResolveHighlightingTestBase {

    @Override
    protected String getBasePath() {
        return "/resolve/modules/";
    }

    @Test
    @DisplayName("module import")
    public void testModuleImport() {
        doTest(
                "modules/ModuleWithMainClass.hx",
                "modules/ModuleWithoutMainClass.hx",
                "other/OtherModule.hx",
                "umods/UsingModule.hx"
                );
    }

}
