package com.intellij.plugins.haxe.model.type.resolver;

import com.intellij.plugins.haxe.model.type.ResultHolder;

public record ResolverEntry(String name, ResultHolder type, ResolveSource resolveSource) {
  public ResolverEntry copy() {
    return new ResolverEntry(name, type, resolveSource);
  }
}
