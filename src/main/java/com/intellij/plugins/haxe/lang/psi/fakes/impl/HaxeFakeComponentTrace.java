package com.intellij.plugins.haxe.lang.psi.fakes.impl;

import com.intellij.navigation.ItemPresentation;
import com.intellij.openapi.util.NlsSafe;
import com.intellij.plugins.haxe.HaxeComponentType;
import com.intellij.plugins.haxe.lang.psi.HaxeComponentName;
import com.intellij.plugins.haxe.lang.psi.HaxeIdentifier;
import com.intellij.plugins.haxe.lang.psi.fakes.HaxeFakeComponentName;
import com.intellij.plugins.haxe.lang.psi.fakes.HaxeFakeNamedComponent;
import com.intellij.psi.PsiElement;
import icons.HaxeIcons;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class HaxeFakeComponentTrace extends HaxeFakeNamedComponent {


    public static final String NAME = "trace";
    private final HaxeFakeComponentName componentName;
    private final HaxeIdentifier parent;

    public HaxeFakeComponentTrace(HaxeIdentifier parent) {
        this.componentName = new HaxeFakeComponentName(parent);
        this.parent = parent;
    }

    @Override
    public String getDocsText() {
        return """
               *Language feature*
               
               Convenience method that redirects input to `haxe.Log.trace`
               
               The code `trace("hello", "warning", 123);` will be transformed to call `haxe.Log.trace` 
               with position information and custom parameters.
               
               If this call was made in a file and class called `Test` in a method named `Main` on line 6 
               it would be compiled to something like this:
               
               ```haxe
               haxe.Log.trace("hello", {
                     fileName : "Test.hx",
                     lineNumber : 6,
                     className : "Test",
                     methodName : "main",
                     customParams : ["warning",123]
                 });
                 ```
              To trace without the default position information `haxe.Log.trace(msg, null)` can be used.
              
              
              @See https://haxe.org/manual/debugging-trace-log.html 
              @see https://haxe.org/manual/debugging-posinfos.html
               """;
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public HaxeComponentType getComponentType() {
        return HaxeComponentType.METHOD;
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
                return HaxeIcons.Method;
            }
        };
    }
}
