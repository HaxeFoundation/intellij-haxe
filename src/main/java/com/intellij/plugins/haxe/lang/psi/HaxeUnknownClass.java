package com.intellij.plugins.haxe.lang.psi;

import com.intellij.lang.ASTNode;
import com.intellij.plugins.haxe.lang.psi.impl.AbstractHaxePsiClass;
import com.intellij.plugins.haxe.model.type.SpecificTypeReference;
import com.intellij.psi.SyntheticElement;
import com.intellij.psi.impl.source.DummyHolderElement;
import org.jetbrains.annotations.Nullable;

public class HaxeUnknownClass extends AbstractHaxePsiClass implements HaxeClass, SyntheticElement {

    public HaxeUnknownClass(ASTNode node) {
        super(node != null ? node : new DummyHolderElement("Unknown"));
    }

    @Nullable
    @Override
    public String getName() {
        return SpecificTypeReference.UNKNOWN;
    }

    @Override
    public @Nullable HaxeGenericParam getGenericParam() {
        return null;
    }

    @Override
    public @Nullable HaxeComponentName getComponentName() {
        return null;
    }
}
