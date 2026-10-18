package com.intellij.plugins.haxe.lang.psi.stubs.stub;

import com.intellij.plugins.haxe.lang.psi.HaxePsiModifier;

public interface StubWithMetaAndModifiers extends StubWithMeta {
    public Boolean hasKeyword(@HaxePsiModifier.KeywordConstant String modifier);
}
