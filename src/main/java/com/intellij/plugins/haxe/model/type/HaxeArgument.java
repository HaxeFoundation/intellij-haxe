package com.intellij.plugins.haxe.model.type;

import com.intellij.plugins.haxe.model.evaluator.assign.HaxeTypeCompatible;
import com.intellij.psi.PsiElement;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class HaxeArgument {
  @Getter final private PsiElement element;
  @Getter final private int index;
  @Getter final private boolean optional;
  @Getter final private boolean isRest;
  @Getter final private String name;
  @Getter final private ResultHolder type;

  public HaxeArgument(PsiElement element, int index, boolean optional, boolean rest, @NotNull ResultHolder type, @Nullable String name) {
    this.element = element;
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

  /*
  When we check function type compatibility we have to evaluate things in the opposite direction for arguments
  if we got class A and B, and B extends A this would be legal for normal types

   var x:A = B; // B extends A so this is ok

   however for functions this works in inverse

   function acceptA(p:A):void{}
   function acceptB(p:B):void{}

   // this would fail as it would allow you to call acceptB with an A argument when the function expects minimum a B.
   var x:A->Void = acceptB;

   // the inverse is however allowed:

   // this works because  the minimum requirement is type B and B extends A  so you can call a function accepting A.
   var x:B->Void = acceptA;

    same type is of course accepted
    var x:A->Void = acceptA;
    var x:B->Void = acceptB;

    return type follow "normal" rules so this only apply to arguments

    // allowed
    var x:Void->A =  function ():B {return null;}
    // not allowed
    var x:Void->B =  function ():A {return null;}
 */
  public boolean canAssignToFrom(HaxeArgument from) {
    // TO can accept optional but not the other way around.
    // if TO has optional from and  FROM does not then the assignment should fail.
    if (!from.isOptional() && this.isOptional()) return false;

    // on purpose inverse order from-to instead of to-from, read explanation above method.
    return HaxeTypeCompatible.canAssignToFromReference(from.getType(), this.getType(), true, false);
  }

  public HaxeArgument withType(ResultHolder newType) {
    return new HaxeArgument(this.element, this.index, this.optional, this.isRest, newType, this.name);
  }
  public HaxeArgument copy() {
    return new HaxeArgument(this.element,this.index, this.optional, this.isRest, this.type, this.name);
  }
}
