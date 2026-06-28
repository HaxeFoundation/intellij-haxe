package com.intellij.plugins.haxe.lang.psi.fakes.impl;

import com.intellij.navigation.ItemPresentation;
import com.intellij.plugins.haxe.HaxeComponentType;
import com.intellij.plugins.haxe.lang.psi.HaxeComponentName;
import com.intellij.plugins.haxe.lang.psi.HaxeMethod;
import com.intellij.plugins.haxe.lang.psi.HaxeNamedComponent;
import com.intellij.plugins.haxe.lang.psi.HaxeReference;
import com.intellij.plugins.haxe.lang.psi.fakes.HaxeFakeNamedComponent;
import com.intellij.plugins.haxe.model.HaxeMethodModel;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class HaxeFakeTargetSpecificSyntax extends HaxeFakeNamedComponent {

    public final PsiElement reference;
    public final HaxeMethod resolved;
    public final String name;

    public HaxeFakeTargetSpecificSyntax(String name, HaxeReference reference, HaxeMethod resolved) {
        this.reference = reference;
        this.resolved = resolved;
        this.name = name;
    }

    @Override
    public HaxeComponentType getComponentType() {
        return HaxeComponentType.METHOD;
    }

    @Override
    public PsiElement getParent() {
        return reference;
    }

    public HaxeNamedComponent getDocsPsi(){
        return resolved;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public @NotNull PsiElement getNavigationElement() {
        return resolved != null ? resolved : reference;
    }

    @Override
    public @Nullable HaxeComponentName getComponentName() {
        if (resolved != null) {
            return resolved.getComponentName();
        }
        return null;
    }

    @Nullable
    public HaxeMethodModel getModel() {
        return resolved != null ? resolved.getModel() : null;
    }

    @Override
    public ItemPresentation getPresentation() {
        return super.getPresentation();
    }
}
