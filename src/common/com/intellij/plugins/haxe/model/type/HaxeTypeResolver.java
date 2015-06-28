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
import com.intellij.plugins.haxe.lang.psi.impl.HaxeMethodImpl;
import com.intellij.plugins.haxe.model.HaxeMethodModel;
import com.intellij.plugins.haxe.model.fixer.HaxeCastFixer;
import com.intellij.plugins.haxe.model.resolver.HaxeResolver2;
import com.intellij.plugins.haxe.model.resolver.HaxeResolver2Dummy;
import com.intellij.plugins.haxe.util.UsefulPsiTreeUtil;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class HaxeTypeResolver {
  @NotNull
  static public ResultHolder getFieldOrMethodReturnType(@NotNull AbstractHaxeNamedComponent comp) {
    return getFieldOrMethodReturnType(comp, null);
  }

  // @TODO: Check if cache works
  @NotNull
  static public ResultHolder getFieldOrMethodReturnType(@NotNull AbstractHaxeNamedComponent comp, @Nullable HaxeGenericResolver resolver) {
    // @TODO: cache should check if any related type has changed, which return depends
    if (comp.getContainingFile() == null) {
      return SpecificHaxeClassReference.getUnknown(comp).createHolder();
    }
    long stamp = comp.getContainingFile().getModificationStamp();
    if (comp._cachedType == null || comp._cachedTypeStamp != stamp) {
      comp._cachedType = _getFieldOrMethodReturnType(comp, resolver);
      comp._cachedTypeStamp = stamp;
    }

    return comp._cachedType;
  }

  @NotNull
  static public ResultHolder getMethodFunctionType(PsiElement comp, @Nullable HaxeGenericResolver resolver) {
    if (comp instanceof HaxeMethod) {
      return ((HaxeMethod)comp).getModel().getFunctionType(resolver).createHolder();
    }
    // @TODO: error
    return SpecificTypeReference.getInvalid(comp).createHolder();
  }

  @NotNull
  static private ResultHolder _getFieldOrMethodReturnType(AbstractHaxeNamedComponent comp, @Nullable HaxeGenericResolver resolver) {
    try {
      if (comp instanceof PsiMethod) {
        return getFunctionReturnType(comp);
      } else if (comp instanceof HaxeFunctionLiteral) {
        return getFunctionReturnType(comp);
      } else {
        return getFieldType(comp);
      }
    } catch (Throwable e) {
      e.printStackTrace();
      return SpecificTypeReference.getUnknown(comp).createHolder();
    }
  }

  @NotNull
  static private ResultHolder getFieldType(AbstractHaxeNamedComponent comp) {
    //ResultHolder type = getTypeFromTypeTag(comp);
    // Here detect assignment
    if (comp instanceof HaxeVarDeclarationPart) {
      HaxeTypeTag typeTag = ((HaxeVarDeclarationPart)comp).getTypeTag();
      HaxeVarInit init = ((HaxeVarDeclarationPart)comp).getVarInit();
      if (init != null) {
        PsiElement child = init.getExpression();
        ResultHolder type1 = HaxeTypeResolver.getPsiElementType(child, new HaxeResolver2Dummy());
        // @TODO: resolver should be calculated (if var has static modifier)
        HaxeVarDeclaration decl = ((HaxeVarDeclaration)comp.getParent());
        boolean isConstant = false;
        if (decl != null) {
          isConstant = decl.hasModifierProperty(HaxePsiModifier.INLINE);
          PsiModifierList modifierList = decl.getModifierList();
          //System.out.println(decl.getText());
        }
        return isConstant ? type1 : type1.withConstantValue(null);
      }
      if (typeTag != null) {
        return getTypeFromTypeTag(typeTag, comp);
      }
    }

    return SpecificTypeReference.getUnknown(comp).createHolder();
  }

  @NotNull
  static private ResultHolder getFunctionReturnType(AbstractHaxeNamedComponent comp) {
    if (comp instanceof HaxeMethodImpl) {
      HaxeTypeTag typeTag = ((HaxeMethodImpl)comp).getTypeTag();
      if (typeTag != null) {
        return getTypeFromTypeTag(typeTag, comp);
      }
    }
    if (comp instanceof HaxeMethod) {
      HaxeMethodModel methodModel = ((HaxeMethod)comp).getModel();
      final HaxeExpressionEvaluatorContext context = getPsiElementType(methodModel.getBodyPsi(), null, methodModel.getResolver());
      return context.getReturnType();
    } else if (comp instanceof HaxeFunctionLiteral) {
      final HaxeExpressionEvaluatorContext context = getPsiElementType(comp.getLastChild(), null, ((HaxeFunctionLiteral)comp).getModel().getResolver());
      return context.getReturnType();
    } else {
      throw new RuntimeException("Can't get the body of a no PsiMethod");
    }
  }

  @NotNull
  static public ResultHolder getTypeFromTypeTag(@Nullable final HaxeTypeTag typeTag, @NotNull PsiElement context) {
    if (typeTag != null) {
      final HaxeTypeOrAnonymous typeOrAnonymous = typeTag.getTypeOrAnonymous();
      final HaxeFunctionType functionType = typeTag.getFunctionType();

      if (typeOrAnonymous != null) {
        return getTypeFromTypeOrAnonymous(typeOrAnonymous);
      }

      //comp.getContainingFile().getNode().putUserData();

      if (functionType != null) {
        return getTypeFromFunctionType(functionType);
      }
    }

    return SpecificTypeReference.getUnknown(context).createHolder();
  }

  @NotNull
  static public ResultHolder getTypeFromTypeTag(AbstractHaxeNamedComponent comp, @NotNull PsiElement context) {
    return getTypeFromTypeTag(PsiTreeUtil.getChildOfType(comp, HaxeTypeTag.class), context);
  }

  @NotNull
  static public ResultHolder getTypeFromFunctionType(HaxeFunctionType type) {
    // @TODO: This is messy, should be refactored
    ArrayList<ResultHolder> args = new ArrayList<ResultHolder>();
    for (HaxeTypeOrAnonymous anonymous : type.getTypeOrAnonymousList()) {
      args.add(getTypeFromTypeOrAnonymous(anonymous));
    }
    ResultHolder retval = args.get(args.size() - 1);
    args.remove(args.size() - 1);
    if (args.size() == 1 && args.get(0).getType().toStringWithoutConstant().equals("Void")) {
      args.clear();
    }
    return new SpecificFunctionReference(args, retval, null, type).createHolder();
  }

  @NotNull
  static public ResultHolder getTypeFromType(@NotNull HaxeType type) {
    //System.out.println("Type:" + type);
    //System.out.println("Type:" + type.getText());
    HaxeReferenceExpression expression = type.getReferenceExpression();
    HaxeClassReference reference = new HaxeClassReference(expression.getText(), expression);
    HaxeTypeParam param = type.getTypeParam();
    ArrayList<ResultHolder> references = new ArrayList<ResultHolder>();
    if (param != null) {
      for (HaxeTypeListPart part : param.getTypeList().getTypeListPartList()) {
        for (HaxeTypeOrAnonymous anonymous : part.getTypeOrAnonymousList()) {
          references.add(getTypeFromTypeOrAnonymous(anonymous));
        }
      }
    }
    //type.getTypeParam();
    return SpecificHaxeClassReference.withGenerics(reference, references.toArray(ResultHolder.EMPTY)).createHolder();
  }

  @NotNull
  static public ResultHolder getTypeFromTypeOrAnonymous(@NotNull HaxeTypeOrAnonymous typeOrAnonymous) {
    // @TODO: Do a proper type resolving
    HaxeType type = typeOrAnonymous.getType();
    if (type != null) {
      return getTypeFromType(type);
    }
    return SpecificTypeReference.getDynamic(typeOrAnonymous).createHolder();
  }

  @NotNull
  static public ResultHolder getPsiElementType(PsiElement element, HaxeResolver2 resolver) {
    return getPsiElementType(element, null, resolver).result;
  }

  // @TODO: hack to avoid stack overflow, until a proper non-static fix is done
  static private Set<PsiElement> processedElements = new HashSet<PsiElement>();

  static private void checkMethod(PsiElement element, HaxeExpressionEvaluatorContext context) {
    //final ResultHolder retval = context.getReturnType();

    if (!(element instanceof HaxeMethod)) return;
    final HaxeTypeTag typeTag = UsefulPsiTreeUtil.getChild(element, HaxeTypeTag.class);
    ResultHolder expectedType = SpecificTypeReference.getDynamic(element).createHolder();
    if (typeTag == null) {
      final List<ResultHolder> infos = context.getReturnValues();
      if (!infos.isEmpty()) {
        expectedType = infos.get(0);
      }
    } else {
      expectedType = getTypeFromTypeTag(typeTag, element);
    }

    if (expectedType == null) return;
    for (ResultHolder retinfo : context.getReturnValues()) {
      if (expectedType.canAssign(retinfo)) continue;
      HaxeReturnStatement returnStatment = (HaxeReturnStatement)retinfo.element;
      if (returnStatment != null) {
        context.addError(
          returnStatment,
          "Can't return " + retinfo + ", expected " + expectedType.toStringWithoutConstant(),
          new HaxeCastFixer(returnStatment.getExpression(), retinfo.getType(), expectedType.getType())
        );
      }
    }
  }

  @NotNull
  static public HaxeExpressionEvaluatorContext getPsiElementType(PsiElement element, @Nullable AnnotationHolder holder, HaxeResolver2 resolver) {
    return evaluateFunction(new HaxeExpressionEvaluatorContext(element, resolver, holder));
  }

  @NotNull
  static public HaxeExpressionEvaluatorContext evaluateFunction(@NotNull HaxeExpressionEvaluatorContext context) {
    PsiElement element = context.root;
    if (processedElements.contains(element)) {
      context.result = SpecificHaxeClassReference.primitive("Dynamic", element).createHolder();
      return context;
    }

    processedElements.add(element);
    try {
      HaxeExpressionEvaluator.evaluate(element, context);
      checkMethod(element.getParent(), context);

      for (HaxeExpressionEvaluatorContext lambdaContext : context.lambdas) {
        evaluateFunction(lambdaContext);
        lambdaContext.functionType.canAssign(lambdaContext.getReturnType());
      }

      return context;
    } finally {
      processedElements.remove(element);
    }
  }

  static private SpecificHaxeClassReference createPrimitiveType(String type, PsiElement element, Object constant) {
    return SpecificHaxeClassReference.withoutGenerics(new HaxeClassReference(type, element), constant);
  }
}
