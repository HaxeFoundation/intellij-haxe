package com.intellij.plugins.haxe.lang.psi.indexes.filebased.data;

import com.intellij.plugins.haxe.HaxeComponentType;
import com.intellij.plugins.haxe.model.FullyQualifiedInfo;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class HaxeComponentIndexData {
    List<String> targets = new ArrayList<>();
    String name;
    HaxeComponentType type;
    FullyQualifiedInfo fqn;
    boolean isPublic;
}
