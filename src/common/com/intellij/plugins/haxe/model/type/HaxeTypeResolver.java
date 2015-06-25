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

import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.plugins.haxe.lang.psi.*;
import com.intellij.plugins.haxe.lang.psi.impl.AbstractHaxeNamedComponent;
import com.intellij.plugins.haxe.util.UsefulPsiTreeUtil;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class HaxeTypeResolver {
  // @TODO: Check if cache works
  static public SpecificTypeReference getFieldOrMethodReturnType(AbstractHaxeNamedComponent comp) {
    // @TODO: cache should check if any related type has changed, which return depends
    if (comp == null || comp.getContainingFile() == null) {
      return SpecificHaxeClassReference.getUnknown(comp);
    }
    long stamp = comp.getContainingFile().getModificationStamp();
    if (comp._cachedType == null || comp._cachedTypeStamp != stamp) {
      comp._cachedType = _getFieldOrMethodReturnType(comp);
      comp._cachedTypeStamp = stamp;
    }

    return comp._cachedType;
  }

  static public SpecificFunctionReference getMethodFunctionType(PsiElement comp) {
    if (comp instanceof HaxeMethod) {
      return ((HaxeMethod)comp).getModel().getFunctionType();
    }
    return null;
  }

  static private SpecificTypeReference _getFieldOrMethodReturnType(AbstractHaxeNamedComponent comp) {
    try {
      if (comp instanceof PsiMethod) {
        return SpecificHaxeClassReference.ensure(getFunctionReturnType(comp), comp);
      } else {
        return SpecificHaxeClassReference.ensure(getFieldType(comp), comp);
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
      final HaxeExpressionEvaluatorContext context = getPsiElementType(((HaxeMethod)comp).getModel().getBodyPsi(), null);
      return context.getReturnType();
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

  static public SpecificTypeReference getTypeFromFunctionType(HaxeFunctionType type) {
    ArrayList<SpecificTypeReference> args = new ArrayList<SpecificTypeReference>();
    for (HaxeTypeOrAnonymous anonymous : type.getTypeOrAnonymousList()) {
      args.add(getTypeFromTypeOrAnonymous(anonymous));
    }
    SpecificTypeReference retval = args.get(args.size() - 1);
    args.remove(args.size() - 1);
    return new SpecificFunctionReference(args, retval, null, type);
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

  static public SpecificTypeReference getTypeFromTypeOrAnonymous(@NotNull HaxeTypeOrAnonymous typeOrAnonymous) {
    // @TODO: Do a proper type resolving
    HaxeType type = typeOrAnonymous.getType();
    if (type != null) {
      return getTypeFromType(type);
    }
    return SpecificTypeReference.getDynamic(typeOrAnonymous);
  }

  @NotNull
  static public SpecificTypeReference getPsiElementType(PsiElement element) {
    return getPsiElementType(element, null).result.getType();
  }

  static private SpecificTypeReference getUnknown(PsiElement element) {
    return createPrimitiveType("Unknown", element, null);
  }

  static private SpecificTypeReference getVoid(PsiElement element) {
    return createPrimitiveType("Void", element, null);
  }

  // @TODO: hack to avoid stack overflow, until a proper non-static fix is done
  static private Set<PsiElement> processedElements = new HashSet<PsiElement>();

  static private void checkMethod(PsiElement element, HaxeExpressionEvaluatorContext context) {
    final SpecificTypeReference retval = context.getReturnType();

    if (!(element instanceof HaxeMethod)) return;
    final HaxeTypeTag typeTag = UsefulPsiTreeUtil.getChild(element, HaxeTypeTag.class);
    SpecificTypeReference expectedType = null;
    if (typeTag == null) {
      final List<ReturnInfo> infos = context.getReturnInfos();
      if (!infos.isEmpty()) {
        expectedType = infos.get(0).type.getType();
      }
    } else {
      expectedType = getTypeFromTypeTag(typeTag);
    }

    if (expectedType == null) return;
    for (ReturnInfo retinfo : context.getReturnInfos()) {
      if (expectedType.canAssign(retinfo.type)) continue;
      context.addError(
        retinfo.element,
        "Can't return " + retinfo.type + ", expected " + expectedType.toStringWithoutConstant()
      );
    }
  }

  @NotNull
  static public HaxeExpressionEvaluatorContext getPsiElementType(PsiElement element, @Nullable AnnotationHolder holder) {
    HaxeExpressionEvaluatorContext context = new HaxeExpressionEvaluatorContext();
    if (processedElements.contains(element)) {
      context.result = SpecificHaxeClassReference.primitive("Dynamic", element).createHolder();
      return context;
    }

    processedElements.add(element);
    try {
      context.root = element;
      context.holder = holder;
      HaxeExpressionEvaluator.evaluate(element, context);
      checkMethod(element.getParent(), context);
      return context;
    } finally {
      processedElements.remove(element);
    }
  }

  static private SpecificHaxeClassReference createPrimitiveType(String type, PsiElement element, Object constant) {
    return SpecificHaxeClassReference.withoutGenerics(new HaxeClassReference(type, element), constant);
  }

}
