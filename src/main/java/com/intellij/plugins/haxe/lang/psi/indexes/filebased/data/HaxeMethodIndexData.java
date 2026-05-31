package com.intellij.plugins.haxe.lang.psi.indexes.filebased.data;

import com.intellij.plugins.haxe.HaxeComponentType;
import com.intellij.plugins.haxe.model.HaxeMethodModel;

public class HaxeMethodIndexData  extends HaxeComponentIndexData {
    public HaxeMethodIndexData(HaxeMethodModel model) {
        this.fqn = model.getQualifiedInfo();
        this.name = model.getName();
        this.type = HaxeComponentType.METHOD;
    }
}
