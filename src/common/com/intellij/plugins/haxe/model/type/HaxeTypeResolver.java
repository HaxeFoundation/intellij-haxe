/*
 * Copyright 2000-2013 JetBrains s.r.o.
 * Copyright 2014-2015 AS3Boyan
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
package com.intellij.plugins.haxe.model.type;

import com.intellij.lang.ASTNode;
import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.plugins.haxe.lang.lexer.HaxeTokenTypeSets;
import com.intellij.plugins.haxe.lang.lexer.HaxeTokenTypes;
import com.intellij.plugins.haxe.lang.psi.*;
import com.intellij.plugins.haxe.lang.psi.impl.AbstractHaxeNamedComponent;
import com.intellij.plugins.haxe.util.HaxePsiUtils;
import com.intellij.plugins.haxe.util.HaxeResolveUtil;
import com.intellij.psi.*;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.tree.TokenSet;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;

public class HaxeTypeResolver {
  // @TODO: Check if cache works
  static public SpecificTypeReference getFieldOrMethodReturnType(AbstractHaxeNamedComponent comp) {
    // @TODO: cache should check if any related type has changed, which return depends
    if (comp == null) return createPrimitiveType("Unknown", comp, null);
    long stamp = comp.getContainingFile().getModificationStamp();
    if (comp._cachedType == null || comp._cachedTypeStamp != stamp) {
      comp._cachedType = _getFieldOrMethodReturnType(comp);
      comp._cachedTypeStamp = stamp;
    }

    return comp._cachedType;
  }

  static public SpecificFunctionReference getMethodFunctionType(PsiElement comp) {
    if (comp instanceof HaxeMethod) {
      HaxeParameterList parameterList = HaxePsiUtils.getChild(comp, HaxeParameterList.class);
      ArrayList<SpecificTypeReference> arguments = new ArrayList<SpecificTypeReference>();
      if (parameterList.getParameterList().size() == 0) {
        arguments.add(createPrimitiveType("Void", comp, null));
      } else {
        for (HaxeParameter parameter : parameterList.getParameterList()) {
          arguments.add(SpecificHaxeClassReference.ensure(getTypeFromTypeTag((AbstractHaxeNamedComponent)parameter)));
        }
      }
      arguments.add(getFieldOrMethodReturnType((AbstractHaxeNamedComponent)comp));

      return new SpecificFunctionReference(arguments.toArray(new SpecificTypeReference[0]));
    }
    return null;
  }

  static private SpecificTypeReference _getFieldOrMethodReturnType(AbstractHaxeNamedComponent comp) {
    try {
      if (comp instanceof PsiMethod) {
        return SpecificHaxeClassReference.ensure(getFunctionReturnType(comp));
      } else {
        return SpecificHaxeClassReference.ensure(getFieldType(comp));
      }
    } catch (Throwable e) {
      e.printStackTrace();
      return createPrimitiveType("Unknown", comp, null);
    }
  }

  static private SpecificTypeReference getFieldType(AbstractHaxeNamedComponent comp) {
    SpecificTypeReference type = getTypeFromTypeTag(comp);
    if (type != null) return type;
    // Here detect assignment
    if (comp instanceof HaxeVarDeclarationPart) {
      HaxeVarInit init = ((HaxeVarDeclarationPart)comp).getVarInit();
      if (init != null) {
        PsiElement child = init.getExpression();
        SpecificTypeReference type1 = HaxeTypeResolver.getPsiElementType(child);
        HaxeVarDeclaration decl = ((HaxeVarDeclaration)comp.getParent());
        boolean isConstant = false;
        if (decl != null) {
          isConstant = decl.hasModifierProperty(HaxePsiModifier.INLINE);
          PsiModifierList modifierList = decl.getModifierList();
          //System.out.println(decl.getText());
        }
        return isConstant ? type1 : type1.withConstantValue(null);
      }
    }

    return null;
  }

  static private SpecificTypeReference getFunctionReturnType(AbstractHaxeNamedComponent comp) {
    SpecificTypeReference type = getTypeFromTypeTag(comp);
    if (type != null) return type;

    if (comp instanceof HaxeMethod) {
      return getPsiElementType(comp.getLastChild());
    } else {
      throw new RuntimeException("Can't get the body of a no PsiMethod");
    }
  }

  static public SpecificTypeReference getTypeFromTypeTag(final HaxeTypeTag typeTag) {
    if (typeTag == null) return null;
    final HaxeTypeOrAnonymous typeOrAnonymous = typeTag.getTypeOrAnonymous();
    final HaxeFunctionType functionType = typeTag.getFunctionType();

    if (typeOrAnonymous != null) {
      return getTypeFromTypeOrAnonymous(typeOrAnonymous);
    }

    //comp.getContainingFile().getNode().putUserData();

    if (functionType != null) {
      return getTypeFromFunctionType(functionType);
    }

    return null;

  }

  static public SpecificTypeReference getTypeFromTypeTag(AbstractHaxeNamedComponent comp) {
    return getTypeFromTypeTag(PsiTreeUtil.getChildOfType(comp, HaxeTypeTag.class));
  }

  private static SpecificTypeReference getTypeFromFunctionType(HaxeFunctionType type) {
    ArrayList<SpecificTypeReference> references = new ArrayList<SpecificTypeReference>();
    for (HaxeTypeOrAnonymous anonymous : type.getTypeOrAnonymousList()) {
      references.add(getTypeFromTypeOrAnonymous(anonymous));
    }
    return new SpecificFunctionReference(references.toArray(new SpecificTypeReference[0]));
  }

  static public SpecificTypeReference getTypeFromType(@NotNull HaxeType type) {
    //System.out.println("Type:" + type);
    //System.out.println("Type:" + type.getText());
    HaxeReferenceExpression expression = type.getReferenceExpression();
    HaxeClassReference reference = new HaxeClassReference(expression.getText(), expression);
    HaxeTypeParam param = type.getTypeParam();
    ArrayList<SpecificTypeReference> references = new ArrayList<SpecificTypeReference>();
    if (param != null) {
      for (HaxeTypeListPart part : param.getTypeList().getTypeListPartList()) {
        for (HaxeTypeOrAnonymous anonymous : part.getTypeOrAnonymousList()) {
          references.add(getTypeFromTypeOrAnonymous(anonymous));
        }
      }
    }
    //type.getTypeParam();
    return SpecificHaxeClassReference.withGenerics(reference, references.toArray(SpecificHaxeClassReference.EMPTY));
  }

  static private SpecificTypeReference getTypeFromTypeOrAnonymous(@NotNull HaxeTypeOrAnonymous typeOrAnonymous) {
    // @TODO: Do a proper type resolving
    HaxeType type = typeOrAnonymous.getType();
    if (type != null) {
      return getTypeFromType(type);
    }
    return null;
  }

  @NotNull
  static public SpecificTypeReference getPsiElementType(PsiElement element) {
    return getPsiElementType(element, null).result;
  }

  static private SpecificTypeReference getUnknown(PsiElement element) {
    return createPrimitiveType("Unknown", element, null);
  }

  static private SpecificTypeReference getVoid(PsiElement element) {
    return createPrimitiveType("Void", element, null);
  }

  @NotNull
  static public HaxeExpressionEvaluatorContext getPsiElementType(PsiElement element, @Nullable AnnotationHolder holder) {
    HaxeExpressionEvaluatorContext context = new HaxeExpressionEvaluatorContext();
    context.holder = holder;
    return HaxeExpressionEvaluator.evaluate(element, context);
  }

  static private SpecificHaxeClassReference createPrimitiveType(String type, PsiElement element, Object constant) {
    return SpecificHaxeClassReference.withoutGenerics(new HaxeClassReference(type, element), constant);
  }

}
