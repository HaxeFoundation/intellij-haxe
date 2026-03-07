package com.intellij.plugins.haxe.lang.psi.impl;

import com.intellij.lang.ASTNode;
import com.intellij.navigation.ItemPresentation;
import com.intellij.openapi.util.NlsSafe;
import com.intellij.plugins.haxe.lang.psi.HaxeModule;
import com.intellij.plugins.haxe.model.HaxeModelTarget;
import com.intellij.plugins.haxe.model.HaxeModuleModel;
import icons.HaxeIcons;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public abstract class HaxeModulePsiMixinImpl extends HaxePsiCompositeElementImpl implements HaxeModelTarget, HaxeModule {

    private HaxeModuleModel model;

    public HaxeModulePsiMixinImpl(@NotNull ASTNode node) {
        super(node);
    }

    @Override
    public HaxeModuleModel getModel() {
        if (model == null) {
            model = new HaxeModuleModel(this);
        }
        return model;
    }

    @Override
    public ItemPresentation getPresentation() {
        return new ItemPresentation() {
            @Override
            public @NlsSafe @Nullable String getPresentableText() {
                return getModel().getName();
            }

            @Override
            public @Nullable Icon getIcon(boolean unused) {
                return HaxeIcons.Module;
            }

            @Override
            public @NlsSafe @Nullable String getLocationString() {
                return getModel().getPackageName();
            }
        };
    }
}
