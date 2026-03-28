package com.intellij.plugins.haxe.lang.psi;

import com.intellij.lang.ASTNode;
import com.intellij.plugins.haxe.lang.lexer.HaxeTokenTypes;
import com.intellij.plugins.haxe.lang.psi.impl.AbstractHaxePsiClass;
import com.intellij.plugins.haxe.model.type.SpecificTypeReference;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementFactory;
import com.intellij.psi.SyntheticElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;

public class HaxeUnknownClass extends AbstractHaxePsiClass implements HaxeClass, SyntheticElement {

    private static ASTNode getNode(@NonNull PsiElement context) {
        ASTNode node = context.getNode();
        if(node != null && context.isPhysical())  return node;
        PsiElementFactory factory = JavaPsiFacade.getInstance(context.getProject()).getElementFactory();
        PsiElement unknown = factory.createDummyHolder("Unknown", HaxeTokenTypes.CLASS_DECLARATION, context);
        return unknown.getNode();
    }

    public HaxeUnknownClass(@NotNull PsiElement context) {
        super(getNode(context));
    }


    @Nullable
    @Override
    public String getName() {
        return SpecificTypeReference.UNKNOWN;
    }

    @Override
    public @Nullable HaxeGenericParam getGenericParam() {
        return null;
    }

    @Override
    public @Nullable HaxeComponentName getComponentName() {
        return null;
    }
}
