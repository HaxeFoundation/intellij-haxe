package com.intellij.plugins.haxe.lang.psi.fakes;

import com.intellij.navigation.ItemPresentation;
import com.intellij.openapi.util.NlsSafe;
import com.intellij.plugins.haxe.HaxeComponentType;
import com.intellij.plugins.haxe.lang.psi.HaxeComponentName;
import com.intellij.plugins.haxe.lang.psi.HaxeIdentifier;
import com.intellij.psi.PsiElement;
import icons.HaxeIcons;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class HaxeFakeComponentStringCode extends HaxeFakeNamedComponent {


    private final HaxeFakeComponentName componentName;
    private final HaxeIdentifier parent;

    public HaxeFakeComponentStringCode(HaxeIdentifier parent) {
        this.componentName = new HaxeFakeComponentName(parent);
        this.parent = parent;
    }

    @Override
    public String getDocs() {
        return """
               *Language feature*
               
               Obtains the character code of a single character.
               ```haxe
               "x".code // 120
               ```
               can be used to inline the character code at compile time.
               Note that this only works on String literals of length 1.
               """;
    }

    @Override
    public HaxeComponentType componentType() {
        return HaxeComponentType.FIELD;
    }


    @Override
    public @Nullable HaxeComponentName getComponentName() {
        return componentName;
    }

    @Override
    public PsiElement getParent() {
        return parent;
    }

    @Override
    public @NotNull PsiElement getNavigationElement() {
        return parent;
    }

    @Override
    public @Nullable ItemPresentation getPresentation() {
        return new ItemPresentation() {

            @Override
            public @NlsSafe @Nullable String getPresentableText() {
                return parent.getText();
            }

            @Override
            public @Nullable Icon getIcon(boolean unused) {
                return HaxeIcons.Field;
            }
        };
    }
}
