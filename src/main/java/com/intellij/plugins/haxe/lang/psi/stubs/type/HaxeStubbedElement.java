package com.intellij.plugins.haxe.lang.psi.stubs.type;

import com.intellij.psi.stubs.StubBase;

/**
 * Conviniece interface making it easier access to stub elements without the need to cast objects to types that extends StubBasedPsiElement
 * the reason why we are use this one and not StubBasedPsiElement,  is that grammarkit generates interfaces with StubBasedPsiElement
 * and we get issues with conclicting generics when used on common interfaces like HaxeClass, HaxeMethod and HaxePsiField
 */
public interface HaxeStubbedElement<Stub extends StubBase> {
    Stub getHaxeStub();

    default Stub getHaxeStub(Class<Stub> type) {
        Stub stub = getHaxeStub();
        if (type.isInstance(stub)) {
            return type.cast(stub);
        }
        return null;
    }
}
