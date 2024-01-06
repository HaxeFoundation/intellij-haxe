package com.intellij.plugins.haxe.lang.psi;

import org.jetbrains.annotations.NotNull;

public interface HaxeClassResolvable extends  HaxeExpression  {
  @NotNull // TODO mlo: rename to ResolveType or something like that
  HaxeResolveResult resolveHaxeClass();


}
