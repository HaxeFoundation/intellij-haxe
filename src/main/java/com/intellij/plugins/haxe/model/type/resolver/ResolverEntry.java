package com.intellij.plugins.haxe.model.type.resolver;

import com.intellij.plugins.haxe.lang.psi.impl.HaxeTypeParameterDeclaration;
import com.intellij.plugins.haxe.lang.psi.impl.HaxeTypeParameterScope;
import com.intellij.plugins.haxe.model.type.ResultHolder;
import lombok.With;

@With
public record ResolverEntry(String name, HaxeTypeParameterDeclaration typeParameter, ResultHolder type, HaxeTypeParameterScope scope) {
  public ResolverEntry copy() {
    return new ResolverEntry(name, typeParameter, type, scope);
  }
}
