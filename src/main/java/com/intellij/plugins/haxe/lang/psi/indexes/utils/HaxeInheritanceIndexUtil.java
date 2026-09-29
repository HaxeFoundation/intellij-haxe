package com.intellij.plugins.haxe.lang.psi.indexes.utils;

import com.intellij.plugins.haxe.lang.psi.HaxeType;

import static com.intellij.plugins.haxe.util.HaxeResolveUtil.getSimpleName;

public class HaxeInheritanceIndexUtil {

    public static String superTypeSimpleName(HaxeType haxeType) {
        String text = haxeType.getReferenceExpression().getText();
        return getSimpleName(text);
    }
}
