package com.intellij.plugins.haxe.lang.psi.impl;

import com.intellij.lang.ASTNode;
import com.intellij.plugins.haxe.lang.psi.HaxeAnonymousTypeBody;
import com.intellij.plugins.haxe.lang.psi.HaxeType;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;

/**
 * Experimental hack to  allow HaxeGenericSpecialization to keep typeParameters that can not be resolved to any type.
 */
public class HaxeClassWrapperForTypeParameter extends AnonymousHaxeTypeImpl {

  private final List<HaxeType> typeList;

  public HaxeClassWrapperForTypeParameter(@NotNull ASTNode node, @NotNull List<HaxeType> typeList) {
    super(node);
    this.typeList = typeList;
  }

  public List<HaxeType> getHaxeExtendsList() {
    return typeList;
  }


  @Override
  public @NotNull List<HaxeAnonymousTypeBody> getAnonymousTypeBodyList() {
    return List.of();
  }

  @Override
  public @NotNull List<HaxeType> getTypeList() {
    return List.of();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    HaxeClassWrapperForTypeParameter that = (HaxeClassWrapperForTypeParameter)o;
    return Objects.equals(typeList, that.typeList);
  }

  @Override
  public int hashCode() {
    return Objects.hash(typeList);
  }
}
