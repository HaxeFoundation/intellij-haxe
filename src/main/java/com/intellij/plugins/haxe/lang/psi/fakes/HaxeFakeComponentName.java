package com.intellij.plugins.haxe.lang.psi.fakes;

import com.intellij.plugins.haxe.lang.psi.HaxeIdentifier;
import com.intellij.plugins.haxe.lang.psi.impl.HaxeComponentNameImpl;
import org.jetbrains.annotations.NotNull;

public class HaxeFakeComponentName extends HaxeComponentNameImpl {
    private final HaxeIdentifier identifier;

    public HaxeFakeComponentName(HaxeIdentifier identifier) {
        super(identifier.getNode());
        this.identifier = identifier;
    }

    @Override
    public @NotNull HaxeIdentifier getIdentifier() {
        return identifier;
    }

    @Override
    public boolean isSynthetic() {
        return true;
    }
}
