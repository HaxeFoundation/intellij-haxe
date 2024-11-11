package com.intellij.plugins.haxe.ide.annotator.semantics;

import com.intellij.plugins.haxe.lang.psi.impl.HaxeTypeParameterDeclaration;
import com.intellij.plugins.haxe.model.type.ResultHolder;
import com.intellij.plugins.haxe.model.type.resolver.ResolveSource;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

public class TypeParameterTable {

  private record typeAndSource(ResultHolder type, ResolveSource resolveSource) {}

  Map<HaxeTypeParameterDeclaration, typeAndSource> data = new HashMap<>();

  public void put(@NotNull HaxeTypeParameterDeclaration typeParameter, ResultHolder holder, ResolveSource scope) {
    data.put(typeParameter, new typeAndSource(holder, scope));
  }

  public boolean contains(HaxeTypeParameterDeclaration typeParameter) {
    return data.containsKey(typeParameter);
  }
  public boolean contains(@NotNull HaxeTypeParameterDeclaration name, @NotNull ResolveSource scope) {
    return data.entrySet().stream().anyMatch(entry -> entry.getKey().equals(name) && entry.getValue().resolveSource().equals(scope));
  }

  @Nullable
  public ResultHolder get(@NotNull HaxeTypeParameterDeclaration typeParameter) {
    if(!contains(typeParameter)) return null;
    return data.get(typeParameter).type();
  }
  @Nullable
  public ResultHolder get(@NotNull HaxeTypeParameterDeclaration typeParameter, ResolveSource scope) {
    return data.entrySet().stream().filter(entry -> entry.getKey().equals(typeParameter) && entry.getValue().resolveSource().equals(scope))
      .findFirst()
      .map(entry -> entry.getValue().type)
      .orElse(null);
  }
}
