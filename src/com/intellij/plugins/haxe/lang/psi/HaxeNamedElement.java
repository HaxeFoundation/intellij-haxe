package com.intellij.plugins.haxe.lang.psi;

import com.intellij.navigation.NavigationItem;
import com.intellij.psi.PsiNameIdentifierOwner;
import com.intellij.psi.PsiNamedElement;

/**
 * @author: Fedor.Korotkov
 */
public interface HaxeNamedElement extends HaxePsiCompositeElement, PsiNamedElement, NavigationItem, PsiNameIdentifierOwner {
}
