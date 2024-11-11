package com.intellij.plugins.haxe.model.type.resolver;

public enum ResolveSource {
  ARGUMENT_TYPE(0),
  METHOD_TYPE_PARAMETER(1),
  CLASS_TYPE_PARAMETER(2);

 public final int priority;

  ResolveSource(int priority) {
    this.priority = priority;
  }
}
