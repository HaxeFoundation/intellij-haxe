package com.intellij.plugins.haxe.model.type.resolver;

public enum ResolveSource {
  METHOD_TYPE_PARAMETER(1),
  CLASS_TYPE_PARAMETER(2),
  ARGUMENT_TYPE(0),
  ASSIGN_TYPE(3),

  TODO(10);

 public final int priority;

  ResolveSource(int priority) {
    this.priority = priority;
  }
}
