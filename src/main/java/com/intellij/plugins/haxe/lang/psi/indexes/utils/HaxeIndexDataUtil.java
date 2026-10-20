package com.intellij.plugins.haxe.lang.psi.indexes.utils;

import com.intellij.plugins.haxe.HaxeComponentType;
import com.intellij.plugins.haxe.lang.psi.indexes.filebased.data.HaxeComponentIndexData;
import com.intellij.plugins.haxe.model.*;
import org.jspecify.annotations.NonNull;

public class HaxeIndexDataUtil {
    // members use declared-only visibility: this runs inside file-based
    // indexers, where isPublic()'s parent-class resolve reads other files'
    // index data mid-indexing - forbidden by the platform
    public static @NonNull HaxeComponentIndexData createIndexData(HaxeFieldModel model) {
        HaxeComponentIndexData data = new HaxeComponentIndexData();
        data.setFqn(model.getQualifiedInfo());
        data.setName(model.getName());
        data.setType(HaxeComponentType.FIELD);
        data.setPublic(model.isDeclaredPublic());
        return data;
    }

    public static @NonNull HaxeComponentIndexData createIndexData(HaxeMethodModel model) {
        HaxeComponentIndexData data = new HaxeComponentIndexData();
        data.setFqn(model.getQualifiedInfo());
        data.setName(model.getName());
        data.setType(HaxeComponentType.METHOD);
        data.setPublic(model.isDeclaredPublic());
        data.setVisibilityInherited(model.isVisibilityInheritedFromParent());
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
    public static @NonNull HaxeComponentIndexData createIndexData(HaxeModuleModel model) {
        HaxeComponentIndexData data = new HaxeComponentIndexData();
        data.setFqn(model.getQualifiedInfo());
        data.setName(model.getName());
        data.setType( HaxeComponentType.MODULE);
        data.setPublic(true);
        return data;
    }
}
