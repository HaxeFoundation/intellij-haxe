package com.intellij.plugins.haxe.ide.lookup;

import com.intellij.plugins.haxe.lang.psi.fakes.*;
import icons.HaxeIcons;

public class HaxeSyntheticLookupElements {

    // all methods/functions have a bind method
    public static HaxeSyntheticLookupElement bind(HaxeFakeNamedComponent bind){
        return new HaxeSyntheticLookupElement(bind, "bind", "(...)","", HaxeIcons.Method, true);
    }

    // single character strings  have a "code" member/feature returning the character code (Int)
    public static HaxeSyntheticLookupElement code(HaxeFakeNamedComponent stringCode) {
        return new HaxeSyntheticLookupElement(stringCode, "code", "","Int", HaxeIcons.Field, false);
    }

}
