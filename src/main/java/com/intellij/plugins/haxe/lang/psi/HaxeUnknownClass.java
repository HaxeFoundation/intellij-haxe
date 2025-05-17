package com.intellij.plugins.haxe.lang.psi;

import com.intellij.lang.ASTNode;
import com.intellij.plugins.haxe.lang.psi.impl.AbstractHaxePsiClass;
import com.intellij.plugins.haxe.model.type.SpecificTypeReference;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class HaxeUnknownClass extends AbstractHaxePsiClass implements HaxeClass {

    public HaxeUnknownClass(@NotNull ASTNode node) {
        super(node);
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
