package com.intellij.plugins.haxe.lang.psi.impl;

import com.intellij.lang.ASTNode;
import com.intellij.plugins.haxe.lang.psi.HaxeModule;
import com.intellij.plugins.haxe.model.HaxeModelTarget;
import com.intellij.plugins.haxe.model.HaxeModuleModel;
import org.jetbrains.annotations.NotNull;

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
}
