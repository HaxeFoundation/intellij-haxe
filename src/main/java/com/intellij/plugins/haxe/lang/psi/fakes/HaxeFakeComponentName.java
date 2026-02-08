package com.intellij.plugins.haxe.lang.psi.fakes;

import com.intellij.navigation.ItemPresentation;
import com.intellij.openapi.util.NlsSafe;
import com.intellij.plugins.haxe.lang.psi.HaxeIdentifier;
import com.intellij.plugins.haxe.lang.psi.impl.HaxeComponentNameImpl;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

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

    @Override
    public @Nullable ItemPresentation getPresentation() {
        return new ItemPresentation() {

            @Override
            public @NlsSafe @Nullable String getPresentableText() {
                return identifier.getText();
            }

            @Override
            public @Nullable Icon getIcon(boolean unused) {
                return null;
            }
        };
    }
}
