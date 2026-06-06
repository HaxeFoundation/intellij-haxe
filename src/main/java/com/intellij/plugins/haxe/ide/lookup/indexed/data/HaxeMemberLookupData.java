package com.intellij.plugins.haxe.ide.lookup.indexed.data;

import com.intellij.plugins.haxe.HaxeComponentType;
import com.intellij.plugins.haxe.lang.psi.indexes.filebased.data.HaxeComponentIndexData;
import com.intellij.plugins.haxe.model.*;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.util.*;
import java.util.function.Supplier;

import static com.intellij.plugins.haxe.model.HaxeMethodContext.NO_EXTENSION;
import static com.intellij.plugins.haxe.util.HaxePresentableUtil.buildTypeText;

public class HaxeMemberLookupData {

    private HaxeBaseMemberModel model = null;
    private Supplier<HaxeMemberModel> modelSupplier = null;

    public final FullyQualifiedInfo qualifiedInfo;
    public final HaxeComponentType type;
    public final String name;
    public final Icon icon;

    public HaxeMemberLookupData(@NotNull HaxeBaseMemberModel model) {
        this.qualifiedInfo = model.getQualifiedInfo();
        this.name = model.getName();
        this.type = model.getNamedComponentPsi().getComponentType();
        this.icon = type.getIcon();
        this.model = model;
    }

    public HaxeMemberLookupData(@NotNull HaxeComponentIndexData indexData, @NotNull Supplier<HaxeMemberModel> supplier) {
        this.qualifiedInfo = indexData.getFqn();
        this.name = indexData.getName();
        this.type = indexData.getType();
        this.icon = type.getIcon();
        this.modelSupplier = supplier;
    }

    public HaxeBaseMemberModel getModel() {
        if (model == null) {
            model = modelSupplier.get();
        }
        return model;
    }
}
