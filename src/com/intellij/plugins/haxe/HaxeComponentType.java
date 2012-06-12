package com.intellij.plugins.haxe;

import com.intellij.plugins.haxe.lang.psi.*;
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
  }, TYPEDEF(8) {
    @Override
    public Icon getIcon() {
      return HaxeIcons.TYPEDEF_ICON;
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

  public static boolean isVariable(@Nullable HaxeComponentType type) {
    return type == VARIABLE || type == PARAMETER || type == FIELD;
  }


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
      case 8:
        return TYPEDEF;
    }
    return null;
  }

  @Nullable
  public static HaxeComponentType typeOf(PsiElement element) {
    if (element instanceof HaxeClassDeclaration ||
        element instanceof HaxeExternClassDeclaration ||
        element instanceof HaxeGenericListPart) {
      return CLASS;
    }
    if (element instanceof HaxeEnumDeclaration) {
      return ENUM;
    }
    if (element instanceof HaxeInterfaceDeclaration) {
      return INTERFACE;
    }
    if (element instanceof HaxeTypedefDeclaration) {
      return TYPEDEF;
    }
    if (element instanceof HaxeFunctionDeclarationWithAttributes ||
        element instanceof HaxeExternFunctionDeclaration ||
        element instanceof HaxeFunctionPrototypeDeclarationWithAttributes) {
      return METHOD;
    }
    if (element instanceof HaxeLocalFunctionDeclaration ||
        element instanceof HaxeFunctionLiteral) {
      return FUNCTION;
    }
    if (element instanceof HaxeVarDeclarationPart ||
        element instanceof HaxeEnumValueDeclaration ||
        element instanceof HaxeAnonymousTypeField) {
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

  @Nullable
  public static String getName(PsiElement element) {
    final HaxeComponentType type = typeOf(element);
    if (type == null) {
      return null;
    }
    return type.toString().toLowerCase();
  }

  @Nullable
  public static String getPresentableName(PsiElement element) {
    final HaxeComponentType type = typeOf(element);
    if (type == null) {
      return null;
    }
    switch (type) {
      case TYPEDEF:
      case CLASS:
      case ENUM:
      case INTERFACE:
        return ((HaxeClass)element).getQualifiedName();
      case FUNCTION:
      case METHOD:
      case FIELD:
      case VARIABLE:
      case PARAMETER:
        return ((HaxeNamedComponent)element).getName();
      default:
        return null;
    }
  }
}
