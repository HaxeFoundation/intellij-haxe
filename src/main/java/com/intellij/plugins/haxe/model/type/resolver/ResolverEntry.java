package com.intellij.plugins.haxe.model.type.resolver;

import com.intellij.plugins.haxe.lang.psi.impl.HaxeTypeParameterDeclaration;
import com.intellij.plugins.haxe.lang.psi.impl.HaxeTypeParameterScope;
import com.intellij.plugins.haxe.model.type.ResultHolder;
import lombok.With;

@With
public record ResolverEntry(String name, HaxeTypeParameterDeclaration typeParameter, ResultHolder type, HaxeTypeParameterScope scope, int index) {

    //NOTE: index is only used to handle special cases like genericBuild macro with Rest typeParameter
    public ResolverEntry(String name, HaxeTypeParameterDeclaration typeParameter, ResultHolder type, HaxeTypeParameterScope scope, int index) {
        this.name = name;
        this.typeParameter = typeParameter;
        this.type = type;
        this.scope = scope;
        this.index = index;
    }

    public ResolverEntry(String name, HaxeTypeParameterDeclaration typeParameter, ResultHolder type, HaxeTypeParameterScope scope) {
        this(name,typeParameter,type,scope, -1);
    }

    public ResolverEntry copy() {
    return new ResolverEntry(name, typeParameter, type, scope);
  }
}
