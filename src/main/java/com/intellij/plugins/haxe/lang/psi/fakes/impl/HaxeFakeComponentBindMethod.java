package com.intellij.plugins.haxe.lang.psi.fakes.impl;

import com.intellij.navigation.ItemPresentation;
import com.intellij.openapi.util.NlsSafe;
import com.intellij.plugins.haxe.HaxeComponentType;
import com.intellij.plugins.haxe.lang.psi.HaxeComponentName;
import com.intellij.plugins.haxe.lang.psi.HaxeIdentifier;
import com.intellij.plugins.haxe.lang.psi.HaxeNamedComponent;
import com.intellij.plugins.haxe.lang.psi.fakes.HaxeFakeNamedComponent;
import com.intellij.psi.PsiElement;
import icons.HaxeIcons;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class HaxeFakeComponentBindMethod extends HaxeFakeNamedComponent {


    public static final String NAME = "bind";
    private final HaxeNamedComponent boundMethodOrFunction;
    private final HaxeIdentifier parent;

    public HaxeFakeComponentBindMethod(HaxeIdentifier haxeIdentifier, HaxeNamedComponent namedComponent) {
        this.boundMethodOrFunction = namedComponent;
        this.parent = haxeIdentifier;
    }

    @Override
    public String getDocsText() {
        return """
               *Language feature*
               
               Haxe can binding arguments to functions creating new functions
               with those arguments permanently set.
               
               ```haxe
               var map = new IntMap<String>();
               $type(map.set); // Int -> String -> Void
               
               var bound = map.set.bind(0, "12"); 
               $type(bound); // () -> Void
               ```
               
               It is possible to partially apply arguments by using the
               underscore `_` to denote that an argument is not bound.
               
               ```haxe
               var partial = map.set.bind(_, "12"); // binds last argument 
               $type(partial); // Int -> Void
               ```
               
               The underscore `_ can be skipped for trailing arguments.
               
               ```haxe
               var trailing = map.set.bind(0); // binds first argument, skipping trailing args 
               $type(trailing); // String -> Void
               ```  
               """;
    }

    @Override
    public String getName() {
        return NAME;
    }

    public HaxeNamedComponent  getOriginalMethodOrFunction() {
        return  boundMethodOrFunction;
    }

    @Override
    public HaxeComponentType getComponentType() {
        return HaxeComponentType.METHOD;
    }


    @Override
    public @Nullable HaxeComponentName getComponentName() {
        return boundMethodOrFunction.getComponentName();
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
    public boolean isValid() {
        return super.isValid() && boundMethodOrFunction.isValid();
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
                return HaxeIcons.Method;
            }
        };
    }
}
