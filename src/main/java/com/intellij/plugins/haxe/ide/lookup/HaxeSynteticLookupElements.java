package com.intellij.plugins.haxe.ide.lookup;

import com.intellij.plugins.haxe.lang.psi.fakes.HaxeFakeComponentBindMethod;
import com.intellij.plugins.haxe.lang.psi.fakes.HaxeFakeComponentStringCode;
import icons.HaxeIcons;

public class HaxeSynteticLookupElements {

    // all methods/functions have a bind method
    public static HaxeLookupElement bind(HaxeFakeComponentBindMethod bind){
        return new HaxeSynteticLookupElement(bind, "bind", "(...)","", HaxeIcons.Method, true);
    }

    // single character strings  have a "code" member/feature returning the character code (Int)
    public static HaxeLookupElement code(HaxeFakeComponentStringCode stringCode) {
        return new HaxeSynteticLookupElement(stringCode, "code", "","Int", HaxeIcons.Field, false);
    }
}
