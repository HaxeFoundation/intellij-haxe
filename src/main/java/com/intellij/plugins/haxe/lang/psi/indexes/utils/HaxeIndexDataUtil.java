package com.intellij.plugins.haxe.lang.psi.indexes.utils;

import com.intellij.plugins.haxe.HaxeComponentType;
import com.intellij.plugins.haxe.lang.psi.indexes.filebased.data.HaxeComponentIndexData;
import com.intellij.plugins.haxe.model.*;
import org.jspecify.annotations.NonNull;

public class HaxeIndexDataUtil {
    public static @NonNull HaxeComponentIndexData createIndexData(HaxeFieldModel model) {
        HaxeComponentIndexData data = new HaxeComponentIndexData();
        data.setFqn(model.getQualifiedInfo());
        data.setName(model.getName());
        data.setType(HaxeComponentType.FIELD);
        data.setPublic(model.isPublic());
        return data;
    }

    public static @NonNull HaxeComponentIndexData createIndexData(HaxeMethodModel model) {
        HaxeComponentIndexData data = new HaxeComponentIndexData();
        data.setFqn(model.getQualifiedInfo());
        data.setName(model.getName());
        data.setType(HaxeComponentType.METHOD);
        data.setPublic(model.isPublic());
        return data;
    }

    public static @NonNull HaxeComponentIndexData createIndexData(HaxeClassModel model) {
        HaxeComponentIndexData data = new HaxeComponentIndexData();
        data.setFqn(model.getQualifiedInfo());
        data.setName(model.getName());
        data.setType( HaxeComponentType.typeOf(model.getPsi()));
        data.setPublic(model.isPublic());
        return data;
    }
}
