package com.intellij.plugins.haxe.lang.psi.indexes.utils;

import com.intellij.plugins.haxe.lang.psi.HaxeType;

public class HaxeInheritanceIndexUtil {

    public static String getClassNameCandidate(HaxeType haxeType) {
        // we are using getReferenceExpression here as we dont want generics as part of the candidate name
        return haxeType.getReferenceExpression().getText();
    }
    public static boolean containsDotSeparator(String classNameCandidate) {
        return classNameCandidate.indexOf('.') != -1;
    }
}
