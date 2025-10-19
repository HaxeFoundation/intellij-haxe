package com.intellij.plugins.haxe.lang.parser;

import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.Nullable;

public interface HaxeLazyWithOwner {
    @Nullable PsiElement getOwner();
}
