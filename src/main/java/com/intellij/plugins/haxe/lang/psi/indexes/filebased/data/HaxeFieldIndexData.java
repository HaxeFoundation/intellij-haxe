package com.intellij.plugins.haxe.lang.psi.indexes.filebased.data;

import com.intellij.plugins.haxe.HaxeComponentType;
import com.intellij.plugins.haxe.model.HaxeFieldModel;
import com.intellij.plugins.haxe.model.HaxeMethodModel;

public class HaxeFieldIndexData extends HaxeComponentIndexData {
    public HaxeFieldIndexData(HaxeFieldModel model) {
        this.fqn = model.getQualifiedInfo();
        this.name = model.getName();
        this.type = HaxeComponentType.FIELD;
    }
}
