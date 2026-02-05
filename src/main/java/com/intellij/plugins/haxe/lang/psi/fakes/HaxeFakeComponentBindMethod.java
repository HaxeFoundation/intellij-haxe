package com.intellij.plugins.haxe.lang.psi.fakes;

import com.intellij.plugins.haxe.HaxeComponentType;
import com.intellij.plugins.haxe.lang.psi.HaxeComponentName;
import com.intellij.plugins.haxe.lang.psi.HaxeIdentifier;
import com.intellij.plugins.haxe.lang.psi.HaxeNamedComponent;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class HaxeFakeComponentBindMethod extends HaxeFakeNamedComponent {


    private final HaxeNamedComponent boundMethodOrFunction;
    private final HaxeIdentifier parent;

    public HaxeFakeComponentBindMethod(HaxeIdentifier haxeIdentifier, HaxeNamedComponent namedComponent) {
        this.boundMethodOrFunction = namedComponent;
        this.parent = haxeIdentifier;
    }

    @Override
    public String getDocs() {
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

    public HaxeNamedComponent  getOriginalMethodOrFunction() {
        return  boundMethodOrFunction;
    }

    @Override
    public HaxeComponentType componentType() {
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
}
