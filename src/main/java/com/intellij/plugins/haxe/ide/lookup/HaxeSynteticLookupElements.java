package com.intellij.plugins.haxe.ide.lookup;

import com.intellij.plugins.haxe.lang.psi.fakes.*;
import icons.HaxeIcons;

public class HaxeSynteticLookupElements {

    // all methods/functions have a bind method
    public static HaxeSynteticLookupElement bind(HaxeFakeNamedComponent bind){
        return new HaxeSynteticLookupElement(bind, "bind", "(...)","", HaxeIcons.Method, true);
    }

    // single character strings  have a "code" member/feature returning the character code (Int)
    public static HaxeSynteticLookupElement code(HaxeFakeNamedComponent stringCode) {
        return new HaxeSynteticLookupElement(stringCode, "code", "","Int", HaxeIcons.Field, false);
    }
    public static HaxeSynteticLookupElement trace(HaxeFakeNamedComponent stringCode) {
        return new HaxeSynteticLookupElement(stringCode, "trace", "(v:Dynamic, ...customParams)","Void", HaxeIcons.Method, true);
    }

    public static HaxeSynteticLookupElement targetSpecificSyntax(HaxeFakeNamedComponent specificSyntax) {
        return new HaxeSynteticLookupElement(specificSyntax, specificSyntax.getName(), "(code:String, args:Rest<Dynamic>)","Dynamic", HaxeIcons.Method, true);
    }

}
