package com.intellij.plugins.haxe.lang.psi.impl;

import com.intellij.lang.ASTNode;
import com.intellij.plugins.haxe.lang.psi.*;
import com.intellij.plugins.haxe.util.HaxeResolveUtil;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * @author: Fedor.Korotkov
 */
public abstract class AbstractHaxeTypeDefImpl extends AbstractHaxePsiClass implements HaxeTypedefDeclaration {
  public AbstractHaxeTypeDefImpl(@NotNull ASTNode node) {
    super(node);
  }

  public HaxeClassResolveResult getTargetClass() {
    return getTargetClass(Collections.<String, HaxeClassResolveResult>emptyMap());
  }

  public HaxeClassResolveResult getTargetClass(Map<String, HaxeClassResolveResult> specialization) {
    if (getAnonymousType() != null) {
      return new HaxeClassResolveResult(getAnonymousType(), specialization);
    }
    return HaxeResolveUtil.getHaxeClass(getType(), specialization);
  }

  @NotNull
  @Override
  public List<HaxeType> getExtendsList() {
    final HaxeClass targetHaxeClass = getTargetClass().getHaxeClass();
    if (targetHaxeClass != null) {
      return targetHaxeClass.getExtendsList();
    }
    return super.getExtendsList();
  }

  @NotNull
  @Override
  public List<HaxeType> getImplementsList() {
    final HaxeClass targetHaxeClass = getTargetClass().getHaxeClass();
    if (targetHaxeClass != null) {
      return targetHaxeClass.getImplementsList();
    }
    return super.getImplementsList();
  }

  @Override
  public boolean isInterface() {
    final HaxeClass targetHaxeClass = getTargetClass().getHaxeClass();
    if (targetHaxeClass != null) {
      return targetHaxeClass.isInterface();
    }
    return super.isInterface();
  }

  @NotNull
  @Override
  public List<HaxeNamedComponent> getMethods() {
    final HaxeClass targetHaxeClass = getTargetClass().getHaxeClass();
    if (targetHaxeClass != null) {
      return targetHaxeClass.getMethods();
    }
    return super.getMethods();
  }

  @NotNull
  @Override
  public List<HaxeNamedComponent> getFields() {
    final HaxeClass targetHaxeClass = getTargetClass().getHaxeClass();
    if (targetHaxeClass != null) {
      return targetHaxeClass.getFields();
    }
    return super.getFields();
  }

  @Override
  public HaxeNamedComponent findFieldByName(@NotNull String name) {
    final HaxeClass targetHaxeClass = getTargetClass().getHaxeClass();
    if (targetHaxeClass != null) {
      return targetHaxeClass.findFieldByName(name);
    }
    return super.findFieldByName(name);
  }

  @Override
  public HaxeNamedComponent findMethodByName(@NotNull String name) {
    final HaxeClass targetHaxeClass = getTargetClass().getHaxeClass();
    if (targetHaxeClass != null) {
      return targetHaxeClass.findMethodByName(name);
    }
    return super.findMethodByName(name);
  }
}
