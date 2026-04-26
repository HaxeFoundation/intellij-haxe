package com.intellij.plugins.haxe.lang.psi.stubs.stub;

import com.intellij.plugins.haxe.lang.psi.HaxePsiModifier;

public interface StubWithModifiers {
    public Boolean hasMetaModifier(@HaxePsiModifier.ModifierConstant String modifier);
    public Boolean hasKeywordModifier(String modifier);
}
