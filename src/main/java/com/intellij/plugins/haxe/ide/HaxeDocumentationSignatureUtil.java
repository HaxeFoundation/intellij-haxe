/*
 * Copyright 2000-2013 JetBrains s.r.o.
 * Copyright 2014-2014 AS3Boyan
 * Copyright 2014-2014 Elias Ku
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
package com.intellij.plugins.haxe.ide;

import com.intellij.plugins.haxe.lang.psi.*;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;

/**
 * @author: Fedor.Korotkov
 */
public class HaxeDocumentationSignatureUtil {

  @NotNull
  public static String getInterfaceSignature(HaxeInterfaceDeclaration declaration) {
    return getPsiText(declaration.getPrivateKeyWord())
           + "interface " +
           getPsiText(declaration.getComponentName()) +
           getPsiText(declaration.getGenericParam()) +
           separateToLines(declaration.getInheritList());
  }

  @NotNull
  public static String getExternInterfaceSignature(HaxeExternInterfaceDeclaration declaration) {
    return getPsiText(declaration.getExternKeyWord()) +
           getPsiText(declaration.getPrivateKeyWord())
           + "interface " +
           getPsiText(declaration.getComponentName()) +
           getPsiText(declaration.getGenericParam()) +
           separateToLines(declaration.getInheritList());
  }

  private static String separateToLines(HaxeInheritList list) {
    if (list == null) return "";
    StringBuilder stringBuilder = new StringBuilder();
    list.getExtendsDeclarationList().forEach(declaration -> stringBuilder.append("\n").append(declaration.getText()));
    list.getImplementsDeclarationList().forEach(declaration -> stringBuilder.append("\n").append(declaration.getText()));
    return stringBuilder.toString();
  }

  @NotNull
  public static String getEnumSignature(HaxeEnumDeclaration declaration) {
    return getPsiText(declaration.getExternKeyWord())
           + getPsiText(declaration.getPrivateKeyWord())
           + "enum "
           + getPsiText(declaration.getComponentName())
           + getPsiText(declaration.getGenericParam());
  }


  @NotNull
  public static String getAbstractSignature(HaxeAbstractClassDeclaration declaration) {
    return getPsiText(declaration.getPrivateKeyWord()) +
           getPsiText(declaration.getAbstractClassType()) +
           getPsiText(declaration.getComponentName()) +
           getPsiText(declaration.getGenericParam()) +
           getPsiText(declaration.getUnderlyingType());
    //TODO  TO & FROM list
  }

  @NotNull
  public static String getClassSignature(HaxeClassDeclaration declaration) {
    return (getPsiText(declaration.getClassModifierList())) +
           "class " +
           getPsiText(declaration.getComponentName()) +
           getPsiText(declaration.getGenericParam()) +
           separateToLines(declaration.getInheritList());
  }

  @NotNull
  public static String getExternClassSignature(HaxeExternClassDeclaration declaration) {
    return (getPsiText(declaration.getExternClassModifierList())) +
           "class " +
           getPsiText(declaration.getComponentName()) +
           getPsiText(declaration.getGenericParam()) +
           separateToLines(declaration.getInheritList());
  }

  @NotNull
  public static String getTypeDefSignature(HaxeTypedefDeclaration declaration) {
    return getPsiText(declaration.getExternKeyWord())
           + getPsiText(declaration.getPrivateKeyWord())
           + "typedef "
           + getPsiText(declaration.getComponentName())
           + getPsiText(declaration.getGenericParam());
  }

  private static String getPsiText(PsiElement o) {
    return o == null ? "" : o.getText() + " ";
  }
}
