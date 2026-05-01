package com.intellij.plugins.haxe.lang.psi.stubs.stub;

import com.intellij.plugins.haxe.lang.psi.HaxePsiModifier;
import com.intellij.plugins.haxe.model.HaxeCompilerMetadata;

public interface StubWithMetaAndModifiers {
    public Boolean hasMetadata(@HaxeCompilerMetadata.CompilerMetadata String modifier);
    public Boolean hasKeyword(@HaxePsiModifier.KeywordConstant String modifier);
}

