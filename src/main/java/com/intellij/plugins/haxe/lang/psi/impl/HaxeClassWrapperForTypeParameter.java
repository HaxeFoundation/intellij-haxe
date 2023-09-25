package com.intellij.plugins.haxe.lang.psi.impl;

import com.intellij.lang.ASTNode;
import com.intellij.plugins.haxe.lang.psi.HaxeAnonymousTypeBody;
import com.intellij.plugins.haxe.lang.psi.HaxeType;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Experimental hack to  allow HaxeGenericSpecialization to keep typeParameters that can not be resolved to any type.
 */
public class HaxeClassWrapperForTypeParameter extends AnonymousHaxeTypeImpl {

  private final List<HaxeType> typeList;
  private final HaxeAnonymousTypeBodyImpl body;

  public HaxeClassWrapperForTypeParameter(@NotNull ASTNode node, @NotNull List<HaxeType> typeList) {
    super(node);
    this.body = new HaxeAnonymousTypeBodyImpl(node);
    this.typeList = typeList;
  }

  public List<HaxeType> getHaxeExtendsList() {
    return typeList;
  }

  @NotNull
  @Override
  public HaxeAnonymousTypeBody getAnonymousTypeBody() {
    return body;
  }

}
