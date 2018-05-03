/*
 * Copyright 2000-2013 JetBrains s.r.o.
 * Copyright 2014-2015 AS3Boyan
 * Copyright 2014-2014 Elias Ku
 * Copyright 2017-2018 Ilya Malanin
 * Copyright 2018 Eric Bishton
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
import com.intellij.plugins.haxe.model.type.SpecificFunctionReference.Argument;
import com.intellij.plugins.haxe.util.HaxeAbstractEnumUtil;
import com.intellij.plugins.haxe.util.UsefulPsiTreeUtil;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
      } else if (comp instanceof HaxeEnumValueDeclaration) {
        return getEnumReturnType((HaxeEnumValueDeclaration)comp);
      } else {
        return getFieldType(comp);
      }
    }
    catch (Throwable e) {
      e.printStackTrace();
      return SpecificTypeReference.getUnknown(comp).createHolder();
    }
  }

  private static ResultHolder getEnumReturnType(HaxeEnumValueDeclaration comp) {
    return getTypeFromTypeTag(comp.getReturnType(), comp.getParent());
  }

  @NotNull
  static private ResultHolder getFieldType(AbstractHaxeNamedComponent comp) {
    //ResultHolder type = getTypeFromTypeTag(comp);
    // Here detect assignment
    final ResultHolder abstractEnumType = HaxeAbstractEnumUtil.getFieldType(comp);
    if (abstractEnumType != null) {
      return abstractEnumType;
    }

    if (comp instanceof HaxePsiField) {
      ResultHolder result = null;

      HaxePsiField decl = (HaxePsiField)comp;
      HaxeVarInit init = decl.getVarInit();
      if (init != null) {
        PsiElement child = init.getExpression();
        final ResultHolder initType = HaxeTypeResolver.getPsiElementType(child);
        boolean isConstant = decl.hasModifierProperty(HaxePsiModifier.INLINE) && decl.isStatic();
        result = isConstant ? initType : initType.withConstantValue(null);
      }

      HaxeTypeTag typeTag = decl.getTypeTag();
      if (typeTag != null) {
        final ResultHolder typeFromTag = getTypeFromTypeTag(typeTag, comp);
        final Object initConstant = result != null ? result.getType().getConstant() : null;
        result = typeFromTag.withConstantValue(initConstant);
      }

      if (result != null) {
        return result;
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
      final HaxeExpressionEvaluatorContext context = getPsiElementType(((HaxeMethod)comp).getModel().getBodyPsi(), (AnnotationHolder)null);
      return context.getReturnType();
    } else if (comp instanceof HaxeFunctionLiteral) {
      final HaxeExpressionEvaluatorContext context = getPsiElementType(comp.getLastChild(), (AnnotationHolder)null);
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
    ArrayList<Argument> args = new ArrayList<>();

    List<HaxeFunctionArgument> list = type.getFunctionArgumentList();
    for (int i = 0; i < list.size(); i++) {
      HaxeFunctionArgument argument = list.get(i);
      ResultHolder argumentType = getTypeFromFunctionArgument(argument);
      args.add(new Argument(i, argument.getOptionalMark() != null, argumentType, getArgumentName(argument)));
    }

    if (args.size() == 1 && args.get(0).isVoid()) {
      args.clear();
    }

    ResultHolder returnValue = null;
    HaxeFunctionReturnType returnType = type.getFunctionReturnType();
    if (returnType != null) {
      if (returnType.getFunctionType() != null) {
        returnValue = getTypeFromFunctionType(returnType.getFunctionType());
      } else if (returnType.getTypeOrAnonymous() != null) {
        returnValue = getTypeFromTypeOrAnonymous(returnType.getTypeOrAnonymous());
      }
    }

    if (returnValue == null) {
      returnValue = SpecificTypeReference.getInvalid(type).createHolder();
    }

    return new SpecificFunctionReference(args, returnValue, null, type).createHolder();
  }

  static String getArgumentName(HaxeFunctionArgument argument) {
    HaxeComponentName componentName = argument.getComponentName();
    String argumentName = null;
    if (componentName != null) {
      argumentName = componentName.getIdentifier().getText();
    }

    return argumentName;
  }

  private static ResultHolder getTypeFromFunctionArgument(HaxeFunctionArgument argument) {
    if (argument.getFunctionType() != null) {
      return getTypeFromFunctionType(argument.getFunctionType());
    } else if (argument.getTypeOrAnonymous() != null) {
      return getTypeFromTypeOrAnonymous(argument.getTypeOrAnonymous());
    }
    return SpecificTypeReference.getUnknown(argument).createHolder();
  }

  @NotNull
  static public ResultHolder getTypeFromType(@NotNull HaxeType type) {
    //System.out.println("Type:" + type);
    //System.out.println("Type:" + type.getText());
    HaxeReferenceExpression expression = type.getReferenceExpression();
    HaxeClassReference reference;
    final HaxeClass resolvedHaxeClass = expression.resolveHaxeClass().getHaxeClass();
    if (resolvedHaxeClass == null) {
      reference = new HaxeClassReference(expression.getText(), type);
    } else {
      reference = new HaxeClassReference(resolvedHaxeClass.getModel(), resolvedHaxeClass);
    }

    HaxeTypeParam param = type.getTypeParam();
    ArrayList<ResultHolder> references = new ArrayList<>();
    if (param != null) {
      for (HaxeTypeListPart part : param.getTypeList().getTypeListPartList()) {
        if (part.getFunctionType() != null) {
          references.add(getTypeFromFunctionType(part.getFunctionType()));
        }
        if (part.getTypeOrAnonymous() != null) {
          references.add(getTypeFromTypeOrAnonymous(part.getTypeOrAnonymous()));
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
    final HaxeAnonymousType anonymousType = typeOrAnonymous.getAnonymousType();
    if(anonymousType != null) {
      return SpecificHaxeClassReference.withoutGenerics(new HaxeClassReference(anonymousType.getModel(), typeOrAnonymous)).createHolder();
    }
    return SpecificTypeReference.getDynamic(typeOrAnonymous).createHolder();
  }

  @NotNull
  static public ResultHolder getPsiElementType(PsiElement element) {
    return getPsiElementType(element, (PsiElement)null);
  }

  @NotNull
  static public ResultHolder getPsiElementType(PsiElement element, @Nullable PsiElement resolveContext) {
    if (element == resolveContext) return SpecificTypeReference.getInvalid(element).createHolder();
    if (element instanceof HaxeReferenceExpression) {
      PsiElement targetElement = ((HaxeReferenceExpression)element).resolve();
      if (targetElement instanceof HaxePsiField) {
        return getTypeFromFieldDeclaration((HaxePsiField)targetElement, element);
      }
    } else if (element instanceof HaxeArrayLiteral) {
      HaxeArrayLiteral arrayLiteral = (HaxeArrayLiteral)element;
      if (arrayLiteral.getExpressionList() != null) {
        final List<HaxeExpression> expressions = arrayLiteral.getExpressionList().getExpressionList();
        if (!expressions.isEmpty()) {
          ResultHolder arrayElementType = getPsiElementType(expressions.get(0), arrayLiteral.getContext());
          return SpecificTypeReference.createArray(arrayElementType).createHolder();
        }
      }
      PsiElement context = element.getContext();
      if (context instanceof HaxeVarInit) {
        HaxePsiField field = PsiTreeUtil.getParentOfType(context, HaxePsiField.class);
        if (field != null) {
          SpecificHaxeClassReference fieldType = getTypeFromTypeTag(field.getTypeTag(), element).getClassType();
          if (fieldType != null && fieldType.isArray()) {
            return SpecificTypeReference.createArray(fieldType.getArrayElementType()).createHolder();
          }
        }
      }
    }
    return getPsiElementType(element, (AnnotationHolder)null).result;
  }

  private static ResultHolder getTypeFromFieldDeclaration(HaxePsiField element, PsiElement resolveContext) {
    HaxeTypeTag typeTag = element.getTypeTag();
    if (typeTag != null) {
      return getTypeFromTypeTag(typeTag, resolveContext);
    }
    HaxeVarInit varInit = element.getVarInit();
    if (varInit != null) {
      return getPsiElementType(varInit.getExpression(), resolveContext);
    }
    return SpecificTypeReference.getUnknown(element).createHolder();
  }

  static private void checkMethod(PsiElement element, HaxeExpressionEvaluatorContext context) {
    if (!(element instanceof HaxeMethod)) return;
    final HaxeTypeTag typeTag = UsefulPsiTreeUtil.getChild(element, HaxeTypeTag.class);
    ResultHolder expectedType = SpecificTypeReference.getDynamic(element).createHolder();
    if (typeTag == null) {
      final List<ReturnInfo> infos = context.getReturnInfos();
      if (!infos.isEmpty()) {
        expectedType = infos.get(0).type;
      }
    } else {
      expectedType = getTypeFromTypeTag(typeTag, element);
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
    return evaluateFunction(new HaxeExpressionEvaluatorContext(element, holder));
  }

  // @TODO: hack to avoid stack overflow, until a proper non-static fix is done
  //        At least, we've made it thread local, so the threads aren't stomping on each other any more.
  static private ThreadLocal<? extends Set<PsiElement>> processedElements = new ThreadLocal<HashSet<PsiElement>>() {
    @Override
    protected HashSet<PsiElement> initialValue() {
      return new HashSet<PsiElement>();
    }
  };

  @NotNull
  static public HaxeExpressionEvaluatorContext evaluateFunction(@NotNull HaxeExpressionEvaluatorContext context) {
    PsiElement element = context.root;
    if (processedElements.get().contains(element)) {
      context.result = SpecificHaxeClassReference.primitive("Dynamic", element).createHolder();
      return context;
    }

    processedElements.get().add(element);
    try {
      HaxeExpressionEvaluator.evaluate(element, context);
      checkMethod(element.getParent(), context);

      for (HaxeExpressionEvaluatorContext lambda : context.lambdas) {
        evaluateFunction(lambda);
      }

      return context;
    }
    finally {
      processedElements.get().remove(element);
    }
  }
}
