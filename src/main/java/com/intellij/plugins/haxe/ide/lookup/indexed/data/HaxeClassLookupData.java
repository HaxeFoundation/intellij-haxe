package com.intellij.plugins.haxe.ide.lookup.indexed.data;

import com.intellij.plugins.haxe.HaxeComponentType;
import com.intellij.plugins.haxe.lang.psi.indexes.filebased.data.HaxeComponentIndexData;
import com.intellij.plugins.haxe.model.FullyQualifiedInfo;
import com.intellij.plugins.haxe.model.HaxeClassModel;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.util.function.Supplier;

public class HaxeClassLookupData {

    private HaxeClassModel classModel = null;
    private Supplier<HaxeClassModel> modelSupplier = null;

    public final FullyQualifiedInfo qualifiedInfo;
    public final HaxeComponentType type;
    public final String name;
    public final Icon icon;

    public HaxeClassLookupData(@NotNull HaxeClassModel model) {
        this.qualifiedInfo = model.getQualifiedInfo();
        this.name = model.getName();
        this.type = model.haxeClass.getComponentType();
        this.icon = type.getIcon();
        this.classModel = model;
    }

    public HaxeClassLookupData(@NotNull HaxeComponentIndexData indexData, @NotNull Supplier<HaxeClassModel> supplier) {
        this.qualifiedInfo = indexData.getFqn();
        this.name = indexData.getName();
        this.type = indexData.getType();
        this.icon = type.getIcon();
        this.modelSupplier = supplier;
    }

    /** Compiler-catalog entries: no index data exists, only the FQN and a lazy blueprint model. */
    public HaxeClassLookupData(@NotNull FullyQualifiedInfo qualifiedInfo,
                               @NotNull String name,
                               @NotNull HaxeComponentType type,
                               @NotNull Supplier<HaxeClassModel> supplier) {
        this.qualifiedInfo = qualifiedInfo;
        this.name = name;
        this.type = type;
        this.icon = type.getIcon();
        this.modelSupplier = supplier;
    }

    public HaxeClassModel getClassModel() {
        if (classModel == null) {
            classModel = modelSupplier.get();
        }
        return classModel;
    }
}
