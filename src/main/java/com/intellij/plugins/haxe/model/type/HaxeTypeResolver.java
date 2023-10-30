/*
 * Copyright 2000-2013 JetBrains s.r.o.
 * Copyright 2014-2015 AS3Boyan
 * Copyright 2014-2014 Elias Ku
 * Copyright 2017-2018 Ilya Malanin
 * Copyright 2018-2020 Eric Bishton
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
import com.intellij.plugins.haxe.model.*;
import com.intellij.plugins.haxe.model.type.SpecificFunctionReference.Argument;
import com.intellij.plugins.haxe.util.HaxeAbstractEnumUtil;
import com.intellij.plugins.haxe.util.HaxeResolveUtil;
import com.intellij.plugins.haxe.util.UsefulPsiTreeUtil;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
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
  static public ResultHolder getFieldOrMethodReturnType(@NotNull HaxeNamedComponent comp, @Nullable HaxeGenericResolver resolver) {
    // @TODO: cache should check if any related type has changed, which return depends
    if (comp.getContainingFile() == null) {
      return SpecificHaxeClassReference.getUnknown(comp).createHolder();
    }

    // EMB - Skip the cache while debugging.  There may be a recursive issue.  There are definitely multi-threading issues.
    //long stamp = comp.getContainingFile().getModificationStamp();
    //if (comp._cachedType == null || comp._cachedTypeStamp != stamp) {
    //  comp._cachedType = _getFieldOrMethodReturnType(comp, resolver);
    //  comp._cachedTypeStamp = stamp;
    //}
    //
    //return comp._cachedType;
    return _getFieldOrMethodReturnType(comp, resolver);
  }

  @NotNull
  static public ResultHolder getMethodFunctionType(PsiElement comp, @Nullable HaxeGenericResolver resolver) {
    if (comp instanceof HaxeMethod method) {
      resolver = resolver == null ? null : resolver.withoutUnknowns();
      return method.getModel().getFunctionType(resolver).createHolder();
    }
    // @TODO: error
    return SpecificTypeReference.getInvalid(comp).createHolder();
  }

  @NotNull
  static private ResultHolder _getFieldOrMethodReturnType(HaxeNamedComponent comp, @Nullable HaxeGenericResolver resolver) {
    try {
      if (comp instanceof PsiMethod) {
        return getFunctionReturnType(comp, resolver);
      } else if (comp instanceof HaxeFunctionLiteral) {
        return getFunctionReturnType(comp, resolver);
      } else if (comp instanceof HaxeEnumValueDeclaration enumValueDeclaration) {
        return getEnumReturnType(enumValueDeclaration, resolver);
      } else {
        return getFieldType(comp, resolver);
      }
    }
    catch (Throwable e) {
      e.printStackTrace();
      return SpecificTypeReference.getUnknown(comp).createHolder();
    }
  }

  @NotNull
  public static ResultHolder getEnumReturnType(HaxeEnumValueDeclaration comp, HaxeGenericResolver resolver) {
    ResultHolder result = getTypeFromTypeTag(comp.getReturnType(), comp.getParent());
    if (result.isUnknown()) {
      result = new SpecificEnumValueReference(comp, comp.getParent(), resolver).createHolder();
    }
    return result;
  }

  @NotNull
  static private ResultHolder getFieldType(HaxeNamedComponent comp, HaxeGenericResolver resolver) {
    //ResultHolder type = getTypeFromTypeTag(comp);
    // Here detect assignment
    final ResultHolder abstractEnumType = HaxeAbstractEnumUtil.getFieldType(comp, resolver);
    if (abstractEnumType != null) {
      return abstractEnumType;
    }

    if (comp instanceof HaxePsiField psiField) {
      ResultHolder result = null;

      HaxeVarInit init = psiField.getVarInit();
      if (init != null) {
        PsiElement child = init.getExpression();
        final ResultHolder initType = HaxeTypeResolver.getPsiElementType(child, resolver);
        boolean isConstant = psiField.hasModifierProperty(HaxePsiModifier.INLINE) && psiField.isStatic();
        result = isConstant ? initType : initType.withConstantValue(null);
      }

      HaxeTypeTag typeTag = psiField.getTypeTag();
      if (typeTag != null) {
        ResultHolder typeFromTag = getTypeFromTypeTag(typeTag, comp);
        if(resolver != null) {
          ResultHolder resolved = resolver.resolve(typeFromTag);
          if (resolved != null) typeFromTag = resolved;
        }
        final Object initConstant = result != null ? result.getType().getConstant() : null;
        if(typeFromTag != null) {
          result = typeFromTag.withConstantValue(initConstant);
        }
      }
      // look for usage (only relevant for  localVars)
      if (init == null && typeTag == null) {
        HaxeTypeResolver.getPsiElementType(psiField, null);
      }
      if (result != null) {
        return result;
      }
    }

    return SpecificTypeReference.getUnknown(comp).createHolder();
  }

  /**
   * Resolves declaration (NOT reference) parameters to types.
   *
   * @param comp Declaration with parameters to resolve
   * @param resolver Resolver from a *reference* with instance parameters to apply to the declaration.
   * @return A list of specific resolved types matching the parameters of the declaration.
   */
  @NotNull
  static public ResultHolder[] resolveDeclarationParametersToTypes(@NotNull HaxeNamedComponent comp, HaxeGenericResolver resolver) {
    return resolveDeclarationParametersToTypes(comp, resolver, true);
  }
  @NotNull
  static public ResultHolder[] resolveDeclarationParametersToTypes(@NotNull HaxeNamedComponent comp,
                                                                   HaxeGenericResolver resolver,
                                                                   boolean resolveElementTypes) {

    List<HaxeGenericParamModel> genericParams = null;

    // Note that this clause must come before 'comp instanceof HaxeClass'.
    if (comp instanceof HaxeAnonymousType) {
      // For typedefs of anonymous functions, the generic parameters from the typedef are used.
      // Switch the context.
      HaxeNamedComponent typeParameterContributor = HaxeResolveUtil.findTypeParameterContributor(comp);
      comp = null != typeParameterContributor ? typeParameterContributor : comp;
    }

    if (comp instanceof HaxeTypedefDeclaration) {
      // TODO: Make a HaxeTypedefModel and use it here.
      HaxeGenericParam param = ((HaxeTypedefDeclaration)comp).getGenericParam();
      genericParams = translateGenericParamsToModelList(param);
    }
    else if (comp instanceof HaxeClass) {
      HaxeClassModel model = ((HaxeClass)comp).getModel();
      genericParams = model.getGenericParams();
    }
    else if (comp instanceof HaxeMethodDeclaration) {
      HaxeMethodModel model = ((HaxeMethod)comp).getModel();
      genericParams = model.getGenericParams();
    }
    else if (comp instanceof HaxeEnumValueDeclaration) {
      // TODO: HaxeEnumModel inheritance is screwed up. (It doesn't inherit from HaxeModel, among other things.) Fix it and use the model here.
      HaxeGenericParam param = ((HaxeEnumValueDeclaration)comp).getGenericParam();
      genericParams = translateGenericParamsToModelList(param);
    }

    if (null != genericParams && !genericParams.isEmpty()) {
      ResultHolder[] specifics = new ResultHolder[genericParams.size()];
      int i = 0;
      for (HaxeGenericParamModel param : genericParams) {
        ResultHolder resolved = null;
        if (null != resolver) {
          resolved = resolver.resolve(param.getName());  // Null if no name match.
        }
        if (null == resolved && resolveElementTypes) {
            resolved = getPsiElementType(param.getPsi(), comp, resolver);
        }
        ResultHolder result = resolved != null ? resolved
                                               : new ResultHolder(SpecificTypeReference.getUnknown(param.getPsi()));
        specifics[i++] = result;
      }
      return specifics;
    }
    return ResultHolder.EMPTY;
  }

  private static List<HaxeGenericParamModel> translateGenericParamsToModelList(HaxeGenericParam param) {
    List<HaxeGenericParamModel> genericParams = null;
    if (null != param) {
      List<HaxeGenericListPart> list = param.getGenericListPartList();
      genericParams = new ArrayList<>(list.size());
      int index = 0;
      for (HaxeGenericListPart listPart : list) {
        genericParams.add(new HaxeGenericParamModel(listPart, index));
        index++;
      }
    }
    return genericParams;
  }

  /**
   * Resolves the given type, if it's generic, against the resolver and then
   * resolves its type parameters, if any, against the same resolver.
   *
   * @param result A type result
   * @param resolver
   * @return
   */
  @NotNull
  static public ResultHolder resolveParameterizedType(@NotNull ResultHolder result, HaxeGenericResolver resolver) {
    return resolveParameterizedType(result, resolver, false);
  }
  @NotNull
  static public ResultHolder resolveParameterizedType(@NotNull ResultHolder result, HaxeGenericResolver resolver, boolean returnType) {
    SpecificTypeReference typeReference = result.getType();
    if (resolver != null) {
      if (typeReference instanceof SpecificHaxeClassReference haxeClassReference && typeReference.isFromTypeParameter()) {
        String className = haxeClassReference.getClassName();
        ResultHolder resolved = returnType ? resolver.resolveReturnType(haxeClassReference) : resolver.resolve(className);
        if (null != resolved) {
          result = resolved;
        }
      }
    }

    // Resolve any generics on the resolved type as well.
    typeReference = result.getType();
    if (typeReference instanceof SpecificHaxeClassReference classReference) {
      SpecificHaxeClassReference.propagateGenericsToType(classReference, resolver, returnType);
    }

    return result;
  }

  @NotNull
  static private ResultHolder getFunctionReturnType(HaxeNamedComponent comp, HaxeGenericResolver resolver) {
    if (comp instanceof HaxeMethodImpl method) {
      HaxeTypeTag typeTag = method.getTypeTag();
      if (typeTag != null) {
        if (resolver != null) {
          HaxeTypeOrAnonymous typeOrAnonymous = typeTag.getTypeOrAnonymous();
          if (typeOrAnonymous != null) {
            //TODO resolve for anonymous types ?
            if (typeOrAnonymous.getType() != null) {
              ResultHolder type = HaxeTypeResolver.getTypeFromType(typeOrAnonymous.getType());
              ResultHolder resolve = resolver.resolveReturnType(type);
              if (resolve != null && !resolve.isUnknown()) {
                return resolve;
              }
            }
          }
        }
        return resolveParameterizedType(getTypeFromTypeTag(typeTag, comp), resolver, true);
      }
    }
    if (comp instanceof HaxeConstructor constructor) {
      // TODO constrcutors should return their declaringClass type
      HaxeClassModel declaringClass = constructor.getModel().getDeclaringClass();
      ResultHolder type = declaringClass.getInstanceType();
      if (resolver!= null) {
        type = resolver.withoutUnknowns().resolve(type);
      }
      return type;
    } else if (comp instanceof HaxeMethod method) {
      HaxeMethodModel methodModel = method.getModel();
      PsiElement psi = methodModel.getBodyPsi();
      if (psi == null) psi = methodModel.getBasePsi();

      //Performance tweak avoiding  full body evaluation if it does not contain any return statements (no returns = void)
      HaxeReturnStatement returnStatement = findReturnStatement(psi);
      if (returnStatement == null ||  isVoidReturn(returnStatement)) {
        return SpecificHaxeClassReference.getVoid(psi).createHolder();
      }

      final HaxeExpressionEvaluatorContext context = getPsiElementType(psi, (AnnotationHolder)null, resolver);
      return resolveParameterizedType(context.getReturnType(), resolver);
    } else if (comp instanceof HaxeFunctionLiteral) {
      final HaxeExpressionEvaluatorContext context = getPsiElementType(comp.getLastChild(), (AnnotationHolder)null, resolver);
      return resolveParameterizedType(context.getReturnType(), resolver);
    } else {
      throw new RuntimeException("Can't determine function type if the element isn't a method or function literal.");
    }
  }

  @Nullable
  private static HaxeReturnStatement findReturnStatement(PsiElement psi) {
    if (psi instanceof  HaxeReturnStatement statement){ return statement;
    } else {
       return PsiTreeUtil.findChildOfType(psi, HaxeReturnStatement.class);
    }
  }

  private static boolean isVoidReturn(HaxeReturnStatement statement) {
    //instead of checking all possible types that a return statement might have
    // we just check if its only child is ";" to determine if its a void return
    PsiElement child = statement.getFirstChild();
    if (child == null || child.getNextSibling() == null || child.getNextSibling().textMatches(";"))
      return true;
    return false;
  }

  @NotNull
  static public ResultHolder getTypeFromTypeTag(@Nullable final HaxeTypeTag typeTag, @NotNull PsiElement context) {
    if (typeTag != null) {

      final HaxeTypeOrAnonymous typeOrAnonymous = typeTag.getTypeOrAnonymous();
      if (typeOrAnonymous != null) {
        return getTypeFromTypeOrAnonymous(typeOrAnonymous);
      }

      final HaxeFunctionType functionType = typeTag.getFunctionType();
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
      HaxeFunctionType functionType = returnType.getFunctionType();
      if (functionType != null) {
        returnValue = getTypeFromFunctionType(functionType);
      } else {
        HaxeTypeOrAnonymous typeOrAnonymous = returnType.getTypeOrAnonymous();
        if (typeOrAnonymous != null) {
          returnValue = getTypeFromTypeOrAnonymous(typeOrAnonymous);
        }
      }
    }

    if (returnValue == null) {
      returnValue = SpecificTypeReference.getInvalid(type).createHolder();
    }

    return new SpecificFunctionReference(args, returnValue, (HaxeMethodModel)null, type).createHolder();
  }

  static String getArgumentName(HaxeFunctionArgument argument) {
    HaxeComponentName componentName = argument.getComponentName();
    String argumentName = null;
    if (componentName != null) {
      argumentName = componentName.getIdentifier().getText();
    }

    return argumentName;
  }

  public static ResultHolder getTypeFromFunctionArgument(HaxeFunctionArgument argument) {
    HaxeFunctionType functionType = argument.getFunctionType();
    if (functionType != null) return getTypeFromFunctionType(functionType);

     HaxeTypeOrAnonymous typeOrAnonymous = argument.getTypeOrAnonymous();
    if (typeOrAnonymous != null) return getTypeFromTypeOrAnonymous(typeOrAnonymous);

    return SpecificTypeReference.getUnknown(argument).createHolder();
  }

  /**
   * Resolves the type reference in HaxeType, including type parameters,
   * WITHOUT generic parameters being fully resolved.
   * See {@link SpecificHaxeClassReference#propagateGenericsToType(SpecificHaxeClassReference, HaxeGenericResolver)}
   * to fully resolve generic parameters.
   *
   * NOTE: If types were constrained in scope, (e.g. {@code subClass<T:Constraint> extends superClass<T>})the type
   *       parameter resolves to the constraint type because that's what {@link HaxeResolver#resolve} returns.
   *
   * @param type - Type reference.
   * @return - resolved type with non-generic parameters resolved.
   *           (e.g. &lt;T&gt; will remain an unresolved reference to T.)
   */
  @NotNull
  static public ResultHolder getTypeFromType(@NotNull HaxeType type) {
    return getTypeFromType(type, null);
  }

  static public  boolean isTypeParameter(HaxeReferenceExpression expression) {
    if(PsiTreeUtil.getParentOfType(expression, HaxeTypeParam.class) != null) {
      return true;
    }
    if (expression.resolve() instanceof  HaxeGenericListPart) {
      return true;
    }
    return false;
  }

  /**
   * Resolves the type reference in HaxeType, including type parameters,
   * and fully resolving type parameters if they are fully specified types or
   * appear in the HaxeGenericResolver.
   * See {@link SpecificHaxeClassReference#propagateGenericsToType(SpecificHaxeClassReference, HaxeGenericResolver)}
   * to fully resolve generic parameters.
   *
   * @param type - Type reference.
   * @param resolver - Resolver containing a type->parameter map.
   * @return - resolved type with non-generic parameters resolved.
   *           (e.g. &lt;T&gt; will remain an unresolved reference to T.)
   */
  @NotNull
  static public ResultHolder getTypeFromType(@NotNull HaxeType type, @Nullable HaxeGenericResolver resolver) {
    //System.out.println("Type:" + type);
    //System.out.println("Type:" + type.getText());
    if (resolver != null && !resolver.isEmpty()) {
        ResultHolder resolve = resolver.resolve(type);
        if (resolve != null && !resolve.isUnknown()) {
          return resolve;
        }
      }

    HaxeReferenceExpression expression = type.getReferenceExpression();
    HaxeClassReference reference;
    final HaxeClass resolvedHaxeClass = expression.resolveHaxeClass().getHaxeClass();
    if (resolvedHaxeClass == null) {
      boolean isTypeParameter = isTypeParameter(expression);
      reference = new HaxeClassReference(expression.getText(), type, isTypeParameter);
    } else {
      reference = new HaxeClassReference(resolvedHaxeClass.getModel(), type);
    }

    HaxeTypeParam param = type.getTypeParam();
    ArrayList<ResultHolder> references = new ArrayList<>();
    if (param != null) {
      for (HaxeTypeListPart part : param.getTypeList().getTypeListPartList()) {
        ResultHolder partResult = null;
        if (resolver != null && !resolver.isEmpty()) {
          partResult = resolver.resolve(part);
        }
        if (null == partResult) {
          HaxeFunctionType fnType = part.getFunctionType();
          if (fnType != null) {
            partResult = getTypeFromFunctionType(fnType);
          } else {
            HaxeTypeOrAnonymous toa = part.getTypeOrAnonymous();
            if (toa != null) {
              partResult = getTypeFromTypeOrAnonymous(toa);
            }
          }
        }
        if (null == partResult) {
          partResult = SpecificTypeReference.getUnknown(type).createHolder();
        }
        references.add(partResult);
      }
    } else if (null != resolvedHaxeClass) {

        ResultHolder[] specifics = expression.resolveHaxeClass().getGenericResolver().getSpecificsFor(resolvedHaxeClass);
        Collections.addAll(references, specifics);

    }
    return SpecificHaxeClassReference.withGenerics(reference, references.toArray(ResultHolder.EMPTY)).createHolder();
  }

  @NotNull
  static public ResultHolder getTypeFromTypeOrAnonymous(@NotNull HaxeTypeOrAnonymous typeOrAnonymous) {
    return getTypeFromTypeOrAnonymous(typeOrAnonymous, null);
  }
  static public ResultHolder getTypeFromTypeOrAnonymous(@NotNull HaxeTypeOrAnonymous typeOrAnonymous, @Nullable HaxeGenericResolver parentResolver) {
    // @TODO: Do a proper type resolving
    HaxeType type = typeOrAnonymous.getType();
    if (type != null) {
      return getTypeFromType(type, parentResolver);
    }
    final HaxeAnonymousType anonymousType = typeOrAnonymous.getAnonymousType();
    if (anonymousType != null) {
      HaxeNamedComponent contributor = HaxeResolveUtil.findTypeParameterContributor(anonymousType);
      if (null != contributor) {
        HaxeClassModel contributorModel = HaxeClassModel.fromElement(contributor);
        if (contributorModel.hasGenericParams()) {
          HaxeGenericResolver resolver = contributorModel.getGenericResolver(parentResolver);
          return SpecificHaxeClassReference.withGenerics(new HaxeClassReference(anonymousType.getModel(), typeOrAnonymous),
                                                         resolver.getSpecificsFor(anonymousType)).createHolder();
        }
      }
      return SpecificHaxeClassReference.withoutGenerics(new HaxeClassReference(anonymousType.getModel(), typeOrAnonymous)).createHolder();
    }
    return SpecificTypeReference.getDynamic(typeOrAnonymous).createHolder();
  }

  @NotNull
  static public ResultHolder getPsiElementType(@NotNull PsiElement element, HaxeGenericResolver resolver) {
    return getPsiElementType(element, (PsiElement)null, resolver);
  }

  @NotNull
  static public ResultHolder getPsiElementType(@NotNull PsiElement element, @Nullable PsiElement resolveContext, HaxeGenericResolver resolver) {
    if (element == resolveContext) return SpecificTypeReference.getInvalid(element).createHolder();
    if (element instanceof HaxeReferenceExpression expression) {
      // First, try to resolve it to a class -- this deals with field-level specializations.
      // This calls the old resolver which doesn't deal with expressions.
      ResultHolder resultHolder = null;
      HaxeResolveResult result = expression.resolveHaxeClass();
      HaxeClass haxeClass = result.getHaxeClass();
      if (haxeClass instanceof HaxeSpecificFunction specificFunction) {
        resultHolder = new ResultHolder(SpecificFunctionReference.create(specificFunction));
      } else if (null != haxeClass) {
        resultHolder = new ResultHolder(result.getSpecificClassReference(element, result.getGenericResolver()));
        // if class reference (not chain) wrap as Class<T>
        // ex: var myTypeVar:Class<MyClass> = MyClass;
        String className = haxeClass.getName();
        if(element.getParent() instanceof  HaxeVarInit // is init expression
           && className != null // must have name (filtering out anonymous types)
           && element.textMatches(className)  // identical classname and elementText
           && element.getNextSibling() == null) { // not part of a expression chain (MyClass.someMember)
          resultHolder = HaxeTypeCompatible.wrapType(resultHolder, element, haxeClass.isEnum()).createHolder();
        }
      }
      // If it doesn't resolve to a class, fall back to whatever *does* resolve to. This calls
      // the newer resolve code (this class), which does deal with expressions properly.
      PsiElement targetElement = expression.resolve();
      if (null == resultHolder && targetElement instanceof HaxePsiField psiField) {
        resultHolder = getTypeFromFieldDeclaration(psiField, element, resolver);
      }

      if (null != resultHolder) {
        // If it's a constant, grab the constant value.
        if (targetElement instanceof HaxePsiField field) {
          HaxeFieldModel model = (HaxeFieldModel)field.getModel();
          if (model.isConstant()) {
            resultHolder = resultHolder.withConstantValue(model.isEnumValue() ? model.getBasePsi() : model.getInitializerExpression());
          }
        }
        if (targetElement instanceof HaxeLocalVarDeclaration varDeclaration) {
          HaxeLocalVarModel model = new HaxeLocalVarModel(varDeclaration);
          if (model.isFinal()) {
            resultHolder.disableMutating();
          }
        }

       if (resultHolder!= null) return resultHolder;
      }
    }
    return getPsiElementType(element, (AnnotationHolder)null, resolver).result;
  }

  @NotNull
  private static ResultHolder getTypeFromFieldDeclaration(HaxePsiField element, PsiElement resolveContext, HaxeGenericResolver resolver) {
    HaxeTypeTag typeTag = element.getTypeTag();
    if (typeTag != null) {
      ResultHolder result = getTypeFromTypeTag(typeTag, resolveContext);
      if (null != resolver) {
        ResultHolder resolved = resolveParameterizedType(result, resolver);
        if (null != resolved) {
          result = resolved;
        }
      }
      return result;
    }
    HaxeVarInit varInit = element.getVarInit();
    if (varInit != null) {
      return getPsiElementType(varInit.getExpression(), resolveContext, resolver);
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
      if (context.holder != null) {
        if (expectedType.canAssign(retinfo.type)) continue;
        context.addError(
          retinfo.element,
          "Can't return " + retinfo.type + ", expected " + expectedType.toStringWithoutConstant()
        );
      }
    }
  }

  @NotNull
  static public HaxeExpressionEvaluatorContext getPsiElementType(@NotNull PsiElement element, @Nullable AnnotationHolder holder,
                                                                 HaxeGenericResolver resolver) {
    return evaluateFunction(new HaxeExpressionEvaluatorContext(element, holder), resolver);
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
  static public HaxeExpressionEvaluatorContext evaluateFunction(@NotNull HaxeExpressionEvaluatorContext context,
                                                                HaxeGenericResolver resolver) {
    PsiElement element = context.root;
    if (processedElements.get().contains(element)) {
      context.result = SpecificHaxeClassReference.getDynamic(element).createHolder();
      return context;
    }

    processedElements.get().add(element);
    try {
      HaxeExpressionEvaluator.evaluate(element, context, resolver);
      checkMethod(element.getParent(), context);

      for (HaxeExpressionEvaluatorContext lambda : context.lambdas) {
        evaluateFunction(lambda, resolver);
      }

      return context;
    }
    finally {
      processedElements.get().remove(element);
    }
  }
}
