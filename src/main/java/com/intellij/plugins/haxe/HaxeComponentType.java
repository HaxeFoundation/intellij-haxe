/*
 * Copyright 2000-2013 JetBrains s.r.o.
 * Copyright 2014-2014 AS3Boyan
 * Copyright 2014-2014 Elias Ku
 * Copyright 2017-2017 Ilya Malanin
 * Copyright 2019 Eric Bishton
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
import icons.HaxeIcons;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

/**
 * @author: Fedor.Korotkov
 */
public enum HaxeComponentType {
  CLASS(0) {
    @Override
    public Icon getIcon() {
      return icons.HaxeIcons.Class;
    }
    @Override
    public Icon getCompletionIcon() {
      return HaxeIcons.Class;
    }
  }, ENUM(1) {
    @Override
    public Icon getIcon() {
      return icons.HaxeIcons.Enum;
    }
    @Override
    public Icon getCompletionIcon() {
      return HaxeIcons.Enum;
    }
  }, INTERFACE(2) {
    @Override
    public Icon getIcon() {
      return icons.HaxeIcons.Interface;
    }
    @Override
    public Icon getCompletionIcon() {
      return HaxeIcons.Interface;
    }
  }, FUNCTION(3) {
    @Override
    public Icon getIcon() {
      return AllIcons.Nodes.Function;
    }
  }, METHOD(4) {
    @Override
    public Icon getIcon() {
      return HaxeIcons.Method;
    }
  }, VARIABLE(5) {
    @Override
    public Icon getIcon() {
      return HaxeIcons.Variable;
    }
  }, FIELD(6) {
    @Override
    public Icon getIcon() {
      return HaxeIcons.Field;
    }
  }, PARAMETER(7) {
    @Override
    public Icon getIcon() {
      return HaxeIcons.Parameter;
    }
  }, TYPEDEF(8) {
    @Override
    public Icon getIcon() {
      return HaxeIcons.Typedef;
    }
  }, CLASSVARIABLE(9) {
    @Override
    public Icon getIcon() {
      return  HaxeIcons.Field;
    }
  }, TYPE_PARAMETER(10) {
    public Icon getIcon() {
      return HaxeIcons.Class;
    }
  }, MODULE(11) {
    public Icon getIcon() {
      return HaxeIcons.Module;
    }
  }, ABSTRACT(12) {
    public Icon getIcon() {
      return HaxeIcons.Abstract;
    }
  }, ANONYMOUS_TYPE(13) {
    public Icon getIcon() {
      return HaxeIcons.Anonymous;
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

  public Icon getCompletionIcon() {
    return getIcon();
  }

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
      case 9:
        return CLASSVARIABLE;
      case 10:
        return TYPE_PARAMETER;
      case 11:
        return MODULE;
      case 12:
        return ABSTRACT;
      case 13:
        return ANONYMOUS_TYPE;
    }
    return null;
  }

  /*
    TODO MLO: we should try to avoid this one for now as instanceOf  used heavily and  causes slowdowns due to bug:
    https://bugs.openjdk.org/browse/JDK-8180450
     there is a pending backport to java 21 but already released versions of intellij might never be updated.
    https://github.com/openjdk/jdk21u-dev/pull/1090
   */
  @Nullable
  public static HaxeComponentType typeOf(PsiElement element) {
    if (element instanceof HaxeClassDeclaration ||
        element instanceof HaxeExternClassDeclaration) {
      return CLASS;
    }
    if(element instanceof HaxeObjectLiteral ||
       element instanceof HaxeAnonymousType) {
      return ANONYMOUS_TYPE;
    }
    if (element instanceof HaxeAbstractTypeDeclaration) {
      return ABSTRACT;
    }
    if (element instanceof HaxeEnumDeclaration) {
      return ENUM;
    }
    if (element instanceof HaxeInterfaceDeclaration ||
        element instanceof HaxeExternInterfaceDeclaration) {
      return INTERFACE;
    }
    if (element instanceof HaxeTypedefDeclaration) {
      return TYPEDEF;
    }
    if (element instanceof HaxeMethodDeclaration ||
        element instanceof HaxeEnumValueDeclarationConstructor) {
      return METHOD;
    }
    if (element instanceof HaxeLocalFunctionDeclaration ||
        element instanceof HaxeFunctionLiteral) {
      return FUNCTION;
    }
    if (element instanceof HaxeFieldDeclaration ||
        element instanceof HaxeEnumValueDeclarationField ||
        element instanceof HaxeAnonymousTypeField ||
        element instanceof HaxeObjectLiteralElement
    ) {
      return FIELD;
    }
    if (element instanceof HaxeLocalVarDeclaration ||
        element instanceof HaxeForStatement ||
        element instanceof HaxeEnumExtractedValue ||
        element instanceof HaxeValueIterator || // default iterator
        element instanceof HaxeIteratorkey || // keyValueIterator
        element instanceof HaxeIteratorValue // keyValueIterator
    ) {
      return VARIABLE;
    }
    if (element instanceof HaxeParameter) {
      return PARAMETER;
    }
    if (element instanceof HaxeGenericListPart ||
        element instanceof HaxeGenericConstraintPart
    ) {
      return TYPE_PARAMETER;
    }
    if (element instanceof HaxeModule ) {
      return MODULE;
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
      case CLASS:
            if (element instanceof HaxeGenericListPart) {
              return ((HaxeGenericListPart)element).getName();
            }
            return ((HaxeClass) element).getQualifiedName();
      case TYPEDEF:
      case ENUM:
      case INTERFACE:
            return ((HaxeClass) element).getQualifiedName();
      case FUNCTION:
      case METHOD:
      case FIELD:
      case VARIABLE:
      case PARAMETER:
            return ((HaxeNamedComponent) element).getName();
      default:
            return null;
    }
  }
}
