package com.intellij.plugins.haxe.lang.psi.impl;

import com.intellij.lang.ASTNode;
import com.intellij.plugins.haxe.lang.psi.*;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;

/**
 * @author: Fedor.Korotkov
 */
public abstract class AnonymousHaxeTypeImpl extends AbstractHaxePsiClass implements HaxeAnonymousType {
  public AnonymousHaxeTypeImpl(@NotNull ASTNode node) {
    super(node);
  }

  @NotNull
  @Override
  public List<HaxeType> getExtendsList() {
    final HaxeTypeExtends typeExtends = getAnonymousTypeBody().getTypeExtends();
    if (typeExtends != null) {
      return Arrays.asList(typeExtends.getType());
    }
    return super.getExtendsList();
  }

  @Override
  public HaxeComponentName getComponentName() {
    return null;
  }

  @Override
  public HaxeGenericParam getGenericParam() {
    return null;
  }
}
