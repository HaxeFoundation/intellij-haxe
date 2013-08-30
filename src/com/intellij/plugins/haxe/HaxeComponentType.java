/*
 * Copyright 2000-2013 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.intellij.plugins.haxe;

import com.intellij.icons.AllIcons;
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
      return icons.HaxeIcons.C_Haxe;
    }
  }, ENUM(1) {
    @Override
    public Icon getIcon() {
      return icons.HaxeIcons.E_Haxe;
    }
  }, INTERFACE(2) {
    @Override
    public Icon getIcon() {
      return icons.HaxeIcons.I_Haxe;
    }
  }, FUNCTION(3) {
    @Override
    public Icon getIcon() {
      return AllIcons.Nodes.Function;
    }
  }, METHOD(4) {
    @Override
    public Icon getIcon() {
      return AllIcons.Nodes.Method;
    }
  }, VARIABLE(5) {
    @Override
    public Icon getIcon() {
      return AllIcons.Nodes.Variable;
    }
  }, FIELD(6) {
    @Override
    public Icon getIcon() {
      return AllIcons.Nodes.Field;
    }
  }, PARAMETER(7) {
    @Override
    public Icon getIcon() {
      return AllIcons.Nodes.Parameter;
    }
  }, TYPEDEF(8) {
    @Override
    public Icon getIcon() {
      return AllIcons.Nodes.Annotationtype;
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
        element instanceof HaxeGenericListPart ||
        element instanceof HaxeAbstractClassDeclaration) {
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
