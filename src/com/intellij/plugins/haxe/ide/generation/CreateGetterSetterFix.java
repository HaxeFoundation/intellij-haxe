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
package com.intellij.plugins.haxe.ide.generation;

import com.intellij.plugins.haxe.lang.lexer.HaxeTokenTypes;
import com.intellij.plugins.haxe.lang.psi.HaxeClass;
import com.intellij.plugins.haxe.lang.psi.HaxeNamedComponent;
import com.intellij.plugins.haxe.lang.psi.HaxePropertyDeclaration;
import com.intellij.plugins.haxe.lang.psi.HaxeVarDeclarationPart;
import com.intellij.plugins.haxe.util.HaxeElementGenerator;
import com.intellij.plugins.haxe.util.HaxePresentableUtil;
import com.intellij.plugins.haxe.util.UsefulPsiTreeUtil;
import com.intellij.psi.PsiElement;
import com.intellij.psi.impl.source.tree.PsiWhiteSpaceImpl;

import java.util.Set;

/**
 * @author: Fedor.Korotkov
 */
public class CreateGetterSetterFix extends BaseCreateMethodsFix {
  public static enum Strategy {
    GETTER {
      @Override
      boolean accept(String name, Set<String> names) {
        return !names.contains(HaxePresentableUtil.getterName(name));
      }
    }, SETTER {
      @Override
      boolean accept(String name, Set<String> names) {
        return !names.contains(HaxePresentableUtil.setterName(name));
      }
    }, GETTERSETTER {
      @Override
      boolean accept(String name, Set<String> names) {
        return !names.contains(HaxePresentableUtil.getterName(name)) &&
               !names.contains(HaxePresentableUtil.setterName(name));
      }
    };

    abstract boolean accept(String name, Set<String> names);
  }

  private final Strategy myStratagy;

  public CreateGetterSetterFix(final HaxeClass haxeClass, Strategy strategy) {
    super(haxeClass);
    myStratagy = strategy;
  }

  @Override
  protected String buildFunctionsText(HaxeNamedComponent namedComponent) {
    if (!(namedComponent instanceof HaxeVarDeclarationPart)) {
      return "";
    }
    final String typeText = HaxePresentableUtil.buildTypeText(namedComponent, ((HaxeVarDeclarationPart)namedComponent).getTypeTag());
    final StringBuilder result = new StringBuilder();
    if (myStratagy == Strategy.GETTER || myStratagy == Strategy.GETTERSETTER) {
      buildGetter(result, namedComponent.getName(), typeText);
    }
    if (myStratagy == Strategy.SETTER || myStratagy == Strategy.GETTERSETTER) {
      buildSetter(result, namedComponent.getName(), typeText);
    }
    return result.toString();
  }

  @Override
  protected void modifyElement(HaxeNamedComponent namedComponent) {
    if (!(namedComponent instanceof HaxeVarDeclarationPart)) {
      return;
    }
    if (((HaxeVarDeclarationPart)namedComponent).getPropertyDeclaration() != null) {
      // todo: modify
      return;
    }

    final HaxeVarDeclarationPart declarationPart =
      HaxeElementGenerator.createVarDeclarationPart(namedComponent.getProject(), buildVarDeclaration(namedComponent.getName()));
    final HaxePropertyDeclaration propertyDeclaration = declarationPart.getPropertyDeclaration();
    if (propertyDeclaration != null) {
      namedComponent.addAfter(propertyDeclaration, namedComponent.getComponentName());
      if (myStratagy == Strategy.GETTERSETTER) {
        makeHaxeFieldPhysical(namedComponent);
      }
    }
  }

  /**
   * make field to physical
   * @see <a href="http://haxe.org/manual/class-field-property-rules.html</a>
   */
  private void makeHaxeFieldPhysical(HaxeNamedComponent namedComponent) {
    addPublicAccessor(namedComponent);
    addIsVarMetadata(namedComponent);
  }

  /**
   * set variable to have a 'public' AccessLevel.
   */
  private static void addPublicAccessor(HaxeNamedComponent namedComponent) {
    if (UsefulPsiTreeUtil.getChildOfType(namedComponent, HaxeTokenTypes.KPUBLIC) != null) {
      return;
    }

    // Adds "public"
    final PsiElement parentPsiElement = namedComponent.getParent();
    final PsiElement publicPsiElement = HaxeElementGenerator.createStatementFromText(namedComponent.getProject(), "public");
    parentPsiElement.addBefore(publicPsiElement, parentPsiElement.getFirstChild());

    // Add white space PsiElement
    final PsiElement whiteSpacePsiElement = new PsiWhiteSpaceImpl("");
    //parentPsiElement.addAfter(whiteSpacePsiElement, publicPsiElement); //throws error

    ////////////////////////////////////////////////////
    // Note: Alternative that seems better but isn't working
    ////////////////////////////////////////////////////

    // Add HaxeTokenTypes.KPUBLIC to DeclarationTypes
    //final HaxeDeclarationAttribute[] declarationAttributeList = PsiTreeUtil.getChildrenOfType(namedComponent, HaxeDeclarationAttribute.class);
    //HaxeResolveUtil.getDeclarationTypes(declarationAttributeList).add(HaxeTokenTypes.KPUBLIC);
  }

  /**
   * add '@:isVar' metadata before 'public' AccessLevel
   */
  private void addIsVarMetadata(HaxeNamedComponent namedComponent) {
    final PsiElement parentPsiElement = namedComponent.getParent();

    // Add @:isVar
    final PsiElement isVarPsiElement = HaxeElementGenerator.createStatementFromText(namedComponent.getProject(), "@:isVar");
    parentPsiElement.addBefore(isVarPsiElement, parentPsiElement.getFirstChild());

    // Add white space PsiElement
    final PsiElement whiteSpacePsiElement = new PsiWhiteSpaceImpl("");
    //parentPsiElement.addAfter(whiteSpacePsiElement, isVarPsiElement); //throws error
  }

  private String buildVarDeclaration(String name) {
    final StringBuilder result = new StringBuilder();
    result.append("var ");
    result.append(name);
    result.append("(");
    if (myStratagy == Strategy.GETTER || myStratagy == Strategy.GETTERSETTER) {
      result.append("get");
    }
    else {
      result.append("null");
    }
    result.append(",");
    if (myStratagy == Strategy.SETTER || myStratagy == Strategy.GETTERSETTER) {
      result.append("set");
    }
    else {
      result.append("null");
    }
    result.append(")");
    result.append(";");
    return result.toString();
  }

  private static void buildGetter(StringBuilder result, String name, String typeText) {
    build(result, name, typeText, true);
  }

  private static void buildSetter(StringBuilder result, String name, String typeText) {
    build(result, name, typeText, false);
  }

  private static void build(StringBuilder result, String name, String typeText, boolean isGetter) {
    result.append("public function ");
    result.append(isGetter ? HaxePresentableUtil.getterName(name) : HaxePresentableUtil.setterName(name));
    result.append("(");
    if (!isGetter) {
      result.append("value:");
      result.append(typeText);
    }
    result.append(")");
    if (isGetter) {
      result.append(":");
      result.append(typeText);
    }
    result.append("{\n");
    if (isGetter) {
      result.append("return ");
      result.append(name);
      result.append(";");
    }
    else {
      result.append("return ");
      result.append("this.");
      result.append(name);
      result.append("=value;");
    }
    result.append("\n}");
  }
}
