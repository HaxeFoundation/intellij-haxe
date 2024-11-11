package com.intellij.plugins.haxe.model.type;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class HaxeArgument {
  @Getter final private int index;
  @Getter final private boolean optional;
  @Getter final private boolean isRest;
  @Getter final private String name;
  @Getter final private ResultHolder type;

  public HaxeArgument(int index, boolean optional, boolean rest, @NotNull ResultHolder type, @Nullable String name) {
    this.index = index;
    this.optional = optional;
    this.isRest = rest;
    this.name = name;
    this.type = type;
  }

  public boolean isTypeParameter() {
    return type.isTypeParameter();
  }

  public boolean hasName() {
    return name != null;
  }

  public String toString() {
    return buildStringRepresentation(true);
  }

  public String toStringWithoutConstant() {
    return buildStringRepresentation(false);
  }

  @NotNull
  private String buildStringRepresentation(final boolean withConstantValue) {
    StringBuilder builder = new StringBuilder();
    if (isOptional()) builder.append('?');
    if (withConstantValue && hasName()) {
      builder.append(getName());
      builder.append(':');
    }

    if (withConstantValue) {
      builder.append(type);
    }
    else {
      builder.append(type.toStringWithoutConstant());
    }

    return builder.toString();
  }

  public boolean isVoid() {
    return type.getType().isVoid();
  }

  public boolean isInvalid() {
    return type.getType().isInvalid();
  }

  public boolean canAssignToFrom(HaxeArgument argument) {
    // TO can accept optional but not the other way around.
    // if TO has optional argument and  FROM does not then the assignment should fail.
    return (argument.isOptional() || !this.isOptional()) && type.canAssign(argument.type);
  }

  public HaxeArgument withType(ResultHolder newType) {
    return new HaxeArgument(this.index, this.optional, this.isRest, newType, this.name);
  }
  public HaxeArgument copy() {
    return new HaxeArgument(this.index, this.optional, this.isRest, this.type, this.name);
  }
}
