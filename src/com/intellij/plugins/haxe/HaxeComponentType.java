package com.intellij.plugins.haxe;

import com.intellij.plugins.haxe.lang.psi.*;
import com.intellij.plugins.haxe.lang.psi.impl.HaxeClassDeclarationImpl;
import com.intellij.plugins.haxe.lang.psi.impl.HaxeEnumDeclarationImpl;
import com.intellij.plugins.haxe.lang.psi.impl.HaxeInterfaceDeclarationImpl;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

/**
 * @author: Fedor.Korotkov
 */
public enum HaxeComponentType {
  CLASS(0) {
    @Override
    public Icon getIcon() {
      return HaxeIcons.CLASS_ICON;
    }
  }, ENUM(1) {
    @Override
    public Icon getIcon() {
      return HaxeIcons.ENUM_ICON;
    }
  }, INTERFACE(2) {
    @Override
    public Icon getIcon() {
      return HaxeIcons.INTERFACE_ICON;
    }
  }, FUNCTION(3) {
    @Override
    public Icon getIcon() {
      return HaxeIcons.FUNCTION;
    }
  }, METHOD(4) {
    @Override
    public Icon getIcon() {
      return HaxeIcons.METHOD;
    }
  }, VARIABLE(5) {
    @Override
    public Icon getIcon() {
      return HaxeIcons.VARIABLE;
    }
  }, FIELD(6) {
    @Override
    public Icon getIcon() {
      return HaxeIcons.FIELD;
    }
  }, PARAMETER(7) {
    @Override
    public Icon getIcon() {
      return HaxeIcons.PARAMETER;
    }
  };

  private final int myKey;

  HaxeComponentType(int key) {
    myKey = key;
  }

  public int getKey() {
    return myKey;
  }

  public abstract Icon getIcon();

  @Nullable
  public static HaxeComponentType valueOf(int key) {
    switch (key) {
      case 0:
        return CLASS;
      case 1:
        return ENUM;
      case 2:
        return INTERFACE;
      case 3:
        return FUNCTION;
      case 4:
        return METHOD;
      case 5:
        return VARIABLE;
      case 6:
        return FIELD;
      case 7:
        return PARAMETER;
    }
    return null;
  }

  @Nullable
  public static HaxeComponentType typeOf(PsiElement element) {
    if (element instanceof HaxeClassDeclarationImpl) {
      return CLASS;
    }
    if (element instanceof HaxeEnumDeclarationImpl) {
      return ENUM;
    }
    if (element instanceof HaxeInterfaceDeclarationImpl) {
      return INTERFACE;
    }
    if (element instanceof HaxeFunctionDeclarationWithAttributes ||
        element instanceof HaxeFunctionPrototypeDeclarationWithAttributes) {
      return METHOD;
    }
    if (element instanceof HaxeLocalFunctionDeclaration) {
      return FUNCTION;
    }
    if (element instanceof HaxeVarDeclarationPart || element instanceof HaxeEnumValueDeclaration) {
      return FIELD;
    }
    if (element instanceof HaxeLocalVarDeclarationPart ||
        element instanceof HaxeForStatement) {
      return VARIABLE;
    }
    if (element instanceof HaxeParameter) {
      return PARAMETER;
    }
    return null;
  }
}
