package com.intellij.plugins.haxe.lang.parser;

import com.intellij.plugins.haxe.lang.psi.HaxeModule;
import com.intellij.plugins.haxe.lang.psi.HaxeNamedComponent;
import com.intellij.psi.PsiElement;
import com.intellij.psi.impl.source.tree.LazyParseablePsiElement;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.util.CachedValuesManager;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class HaxePsiMetaElement extends LazyParseablePsiElement implements HaxeLazyWithOwner {

    public HaxePsiMetaElement(@NotNull IElementType type, @Nullable CharSequence buffer) {
        super(type, buffer);
    }


    public @Nullable HaxeNamedComponent getOwner() {
        return CachedValuesManager.getProjectPsiDependentCache(this, HaxePsiMetaElement::_getOwner);
    }

    private static @Nullable HaxeNamedComponent _getOwner(PsiElement element) {
        HaxeNamedComponent namedComponent = PsiTreeUtil.getNextSiblingOfType(element, HaxeNamedComponent.class);
        if(namedComponent != null) return namedComponent;
        // if not member of a class, try to see if its a module member (can be  more than just class, ex. field, method)
        HaxeModule module = PsiTreeUtil.getNextSiblingOfType(element, HaxeModule.class);
        if(module != null) {
            PsiElement firstChild = module.getFirstChild();
            if(firstChild instanceof HaxeNamedComponent namedComponent2 ) return namedComponent2;
            return PsiTreeUtil.getNextSiblingOfType(firstChild, HaxeNamedComponent.class);
        }
        return null;
    }

}
