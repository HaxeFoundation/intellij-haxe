package com.intellij.plugins.haxe.lang.psi.fakes;

import com.intellij.openapi.util.Key;
import com.intellij.plugins.haxe.lang.psi.HaxeNamedComponent;
import com.intellij.psi.SyntheticElement;
import com.intellij.psi.impl.FakePsiElement;
import com.intellij.psi.meta.PsiMetaOwner;
import com.intellij.psi.meta.PsiPresentableMetaData;

public abstract class HaxeFakePsiElement extends FakePsiElement implements SyntheticElement //, PsiPresentableMetaData, PsiMetaOwner {
{
    public static final Key<HaxeFakePsiElement> FAKE_PSI_KEY = Key.create("HaxeFakePsiElement");

    public abstract String getDocsText();
    public abstract HaxeNamedComponent getDocsPsi();
}
