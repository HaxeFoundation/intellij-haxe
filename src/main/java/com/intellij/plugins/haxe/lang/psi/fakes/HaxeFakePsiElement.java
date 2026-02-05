package com.intellij.plugins.haxe.lang.psi.fakes;

import com.intellij.openapi.util.Key;
import com.intellij.psi.SyntheticElement;
import com.intellij.psi.impl.FakePsiElement;

public abstract class HaxeFakePsiElement extends FakePsiElement implements SyntheticElement {
    public static final Key<HaxeFakePsiElement> FAKE_PSI_KEY = Key.create("HaxeFakePsiElement");

    public abstract String getDocs();
}
