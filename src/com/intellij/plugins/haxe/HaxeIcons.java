package com.intellij.plugins.haxe;

import com.intellij.openapi.util.IconLoader;
import com.intellij.plugins.haxe.lang.psi.*;
import com.intellij.plugins.haxe.lang.psi.impl.HaxeClassDeclarationImpl;
import com.intellij.plugins.haxe.lang.psi.impl.HaxeEnumDeclarationImpl;
import com.intellij.plugins.haxe.lang.psi.impl.HaxeInterfaceDeclarationImpl;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class HaxeIcons {
  public final static Icon HAXE_ICON_16x16 = IconLoader.getIcon("/icons/haXe_16.png");
  public final static Icon HAXE_ICON_24x24 = IconLoader.getIcon("/icons/haXe_24.png");

  public final static Icon CLASS_ICON = IconLoader.getIcon("/icons/C_haXe.png");
  public final static Icon ENUM_ICON = IconLoader.getIcon("/icons/E_haXe.png");
  public final static Icon INTERFACE_ICON = IconLoader.getIcon("/icons/I_haXe.png");

  public final static Icon FUNCTION = IconLoader.getIcon("/nodes/function.png");
  public final static Icon METHOD = IconLoader.getIcon("/nodes/method.png");
  public final static Icon VARIABLE = IconLoader.getIcon("/nodes/variable.png");
  public final static Icon FIELD = IconLoader.getIcon("/nodes/field.png");
  public final static Icon PARAMETER = IconLoader.getIcon("/nodes/parameter.png");

  @Nullable
  public static Icon getIcon(PsiElement element, int flags) {
    if (element instanceof HaxeClassDeclarationImpl) {
      return CLASS_ICON;
    }
    if (element instanceof HaxeEnumDeclarationImpl) {
      return ENUM_ICON;
    }
    if (element instanceof HaxeInterfaceDeclarationImpl) {
      return INTERFACE_ICON;
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
