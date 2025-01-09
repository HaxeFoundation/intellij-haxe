package com.intellij.plugins.haxe.model.evaluator;

import com.intellij.openapi.util.RecursionGuard;
import com.intellij.openapi.util.RecursionManager;
import com.intellij.plugins.haxe.lang.psi.*;
import com.intellij.plugins.haxe.lang.psi.impl.HaxeTypeParameterDeclaration;
import com.intellij.plugins.haxe.model.HaxeBaseMemberModel;
import com.intellij.plugins.haxe.model.HaxeClassModel;
import com.intellij.plugins.haxe.model.HaxeMethodModel;
import com.intellij.plugins.haxe.model.HaxeParameterModel;
import com.intellij.plugins.haxe.model.evaluator.callexpression.HaxeCallExpressionContext;
import com.intellij.plugins.haxe.model.evaluator.callexpression.HaxeCallExpressionEvaluation;
import com.intellij.plugins.haxe.model.evaluator.callexpression.HaxeCallExpressionUtil;
import com.intellij.plugins.haxe.model.type.*;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.intellij.psi.search.PsiSearchHelper;
import com.intellij.psi.search.SearchScope;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

import static com.intellij.plugins.haxe.model.evaluator.HaxeExpressionEvaluator.*;
import static com.intellij.plugins.haxe.model.evaluator.HaxeExpressionEvaluator.searchReferencesForType;
import static com.intellij.plugins.haxe.model.evaluator.HaxeExpressionEvaluatorHandlers.*;

public class HaxeExpressionUsageUtil {

  private static final RecursionGuard<PsiElement>
    searchReferencesForTypeGuard = RecursionManager.createGuard("searchReferencesForTypeGuard");


  public static @Nullable ResultHolder tryToFindTypeFromUsage(HaxeComponentName element,
                                                              ResultHolder result,
                                                              ResultHolder hint,
                                                              HaxeExpressionEvaluatorContext context,
                                                              HaxeGenericResolver resolver,
                                                              @Nullable PsiElement scope
  ) {
    ResultHolder searchResult = searchReferencesForTypeGuard
      .computePreventingRecursion(element, true, () -> searchReferencesForType(element, context, resolver, scope, hint));
    if (searchResult != null && !searchResult.isUnknown()) {
      if (result == null) {
        result = searchResult;
      }else if(isDynamicBecauseOfNullValueInit(result)){
        result = HaxeTypeUnifier.unify(result, searchResult, UnificationRules.UNIFY_NULL);
      }else if (searchResult.getType().isSameType(result.getType())) {
        result = HaxeTypeUnifier.unify(result, searchResult);
      }
    }
    return result;
  }

  public static @Nullable ResultHolder findUsageAsParameterInFunctionCall(HaxeExpression referenceExpression,
                                                                          HaxeCallExpression callExpression,
                                                                          HaxeCallExpressionList list,
                                                                          PsiElement resolved) {
    int index = -1;
    if (list != null) index = list.getExpressionList().indexOf(referenceExpression);
    if (index == -1) return null;

    if (resolved instanceof HaxeMethod method) {
      HaxeCallExpressionContext context = HaxeCallExpressionUtil.createContextForMethodCall(callExpression, method);
      HaxeCallExpressionEvaluation evaluated = context.evaluate();
      if (context.isStaticExtension) index++;
     return evaluated.getParameterType(index);
    }
    return null;
  }

  public static @Nullable ResultHolder findUsageAsParameterInFunctionCall(HaxeExpression referenceExpression,
                                                                          HaxeCallExpression callExpression,
                                                                          HaxeCallExpressionList list,
                                                                          SpecificFunctionReference functionReference) {
    int index = -1;
    if (list != null) index = list.getExpressionList().indexOf(referenceExpression);
    if (index == -1) return null;
    HaxeCallExpressionContext context = HaxeCallExpressionUtil.createContextForFunctionCall(callExpression, functionReference);
    HaxeCallExpressionEvaluation evaluated = context.evaluate();
    if (context.isStaticExtension) index++;
    return evaluated.getParameterType(index);
  }


  @NotNull
  public static ResultHolder searchReferencesForTypeParameters(final HaxeComponentName componentName,
                                                               final HaxeExpressionEvaluatorContext context,
                                                               final HaxeGenericResolver resolver, ResultHolder resultHolder) {
   return searchReferencesForTypeParameters(componentName,context,resolver,resultHolder,0);
  }

  private static final RecursionGuard<PsiElement> searchReferencesForTypeParametersRecursionGuard = RecursionManager.createGuard("searchReferencesForTypeParametersRecursionGuard");

  @NotNull
  public static ResultHolder searchReferencesForTypeParameters(final HaxeComponentName componentName,
                                                               final HaxeExpressionEvaluatorContext context,
                                                               final HaxeGenericResolver resolver, ResultHolder resultHolder, int continueFrom) {

    // This recursive guard serves a double purpose, it should prevent recursion
    // AND stop any other logic picking up typeParameters from later reference when current reference is skipped by the recursion guard.
    // This is a common problem when you got a variable that gets its typeParameters from method calls on that instance,
    // and our code will try to find callie type
    var newValues = searchReferencesForTypeParametersRecursionGuard.computePreventingRecursion(componentName, true, () -> {
      ResultHolder originalType = resultHolder.duplicate();
      SpecificHaxeClassReference classType = originalType.getClassType();
      // TODO mlo: should we add some kind of support for functions here ?
      if (classType == null) return originalType;

      HaxeGenericResolver classResolver = classType.getGenericResolver();
      PsiSearchHelper searchHelper = PsiSearchHelper.getInstance(componentName.getProject());
      final SearchScope useScope = searchHelper.getCodeUsageScope(componentName);

      List<PsiReference> references = referenceSearch(componentName, useScope);
      // search until all typeParams are found or we dont have any more references

      for (int i = continueFrom, size = references.size(); i < size; i++) {

        PsiReference reference = references.get(i);

        if (reference instanceof HaxeExpression expression) {
          PsiElement parent = expression.getParent();

          if (reference instanceof HaxeReferenceExpression referenceExpression) {
            ResultHolder result = tryFindTypeWhenUsedAsParameterInCallExpression(originalType, referenceExpression, parent);
            if (result != null) originalType = mapTypeParameterIfAssignable(originalType, result);
            if (!originalType.containsUnknownTypes()) return originalType;
          }

          if (parent instanceof HaxeAssignExpression assignExpression) {
            ResultHolder assignType = tryTypeFromAssignExpression(context, resolver, originalType, assignExpression, componentName);


            if (assignType != null) {
              // we want to ignore assign to null value (flag to not change isFirst)
              if (!(assignType.getConstant() instanceof HaxeNull)) {
                boolean isRightExpresion = false;

                HaxeExpression rightExpression = assignExpression.getRightExpression();
                if (rightExpression instanceof HaxeReferenceExpression referenceExpression) {
                  PsiElement resolve = referenceExpression.resolve();
                  if (resolve instanceof HaxeNamedComponent namedComponent) {
                    if (namedComponent.getComponentName() == componentName) {
                      isRightExpresion = true;

                    }
                  }
                }
                if (isRightExpresion) {
                  ResultHolder instanceType = getInstanceTypeWithDefaultTypeParameters(originalType);
                  if (instanceType.canAssign(assignType)) {
                    originalType = mapTypeParameter(originalType, assignType);
                  }
                } else {
                  if (assignType.canAssign(originalType)) {
                    originalType = mapTypeParameter(originalType, assignType);
                  }
                }

              }
            }
            if (!originalType.containsUnknownTypes()) return originalType;
          }

          if (parent instanceof HaxeReferenceExpression referenceExpression) {
            ResultHolder result = tryFindTypeFromMethodCallOnReference(originalType, referenceExpression);
            if (result != null) originalType = mapTypeParameterIfAssignable(originalType, result);
            if (!originalType.containsUnknownTypes()) return originalType;
          }

          if (parent instanceof HaxeObjectLiteralElement literalElement) {
            ResultHolder result = tryTypeFromObjectLiteral(context, resolver, literalElement);
            if (result != null) originalType = mapTypeParameterIfAssignable(originalType, result);
            if (!originalType.containsUnknownTypes()) return originalType;
          }

          if (parent instanceof HaxeArrayAccessExpression arrayAccessExpression) {
            ResultHolder result = tryUpdateTypeParamFromArrayAccess(context, resolver, arrayAccessExpression, classType, classResolver, classType);
            if (result != null) originalType = mapTypeParameterIfAssignable(originalType, result);
            if (!originalType.containsUnknownTypes()) return originalType;
          }

          if (parent instanceof HaxeObjectLiteralElement literalElement) {
            ResultHolder result = tryUpdateTypeParamFromObjectLiteral(context, resolver, literalElement, classType);
            if (result != null) originalType = mapTypeParameterIfAssignable(originalType, result);
            if (!originalType.containsUnknownTypes()) return originalType;
          }
        }
      }
      return originalType;
    });
    return newValues != null ? newValues.noCache() : resultHolder.noCache();
  }

  private static @NotNull ResultHolder getInstanceTypeWithDefaultTypeParameters(ResultHolder resultHolder) {
    SpecificHaxeClassReference classType = resultHolder.getClassType();
    if(classType != null && classType.getHaxeClassModel() != null) {
      return classType.getHaxeClassModel().getInstanceType();
    }
    return resultHolder;// todo enums ?
  }

  private static @NotNull ResultHolder mapTypeParameterIfAssignable(ResultHolder current, ResultHolder found) {
    if(current.canAssign(found)) {
     return mapTypeParameter(current, found);
    }
    return current;
  }

  private static @NotNull ResultHolder mapTypeParameter(ResultHolder current, ResultHolder found) {
    SpecificHaxeClassReference foundType = found.getClassType();
    if (foundType == null) return current;

    // if class try to cast before attempting to  extract generics (Dynamic, Any  etc will get passed canAssign checks)
    foundType = foundType.tryCastTo(current.getClassType());
    if (foundType == null) return current;

    HaxeGenericResolver foundResolver = foundType.getGenericResolver();
    HaxeGenericResolver mappedResolver = foundResolver.translateFromTo(foundType.getHaxeClass(), current.getClassType().getHaxeClass());

    @NotNull ResultHolder[] currentSpecifics = current.getClassType().getSpecifics();
    @NotNull ResultHolder[] foundSpecifics = mappedResolver.getSpecifics();
    @NotNull ResultHolder[] newSpecifics = new ResultHolder[currentSpecifics.length];
      for (int i = 0; i < foundSpecifics.length; i++) {
          ResultHolder currentSpecific = currentSpecifics[i];
          ResultHolder foundSpecific = foundSpecifics[i];
          if (currentSpecific.isUnknown() ||  (currentSpecific.isTypeParameter()  && currentSpecific.canAssign(foundSpecific))) {
            newSpecifics[i] = foundSpecific;
          }else {
            newSpecifics[i] = currentSpecific;
          }
      }

    return SpecificHaxeClassReference.withGenerics(found.getClassType().getHaxeClassReference(), newSpecifics).createHolder();
  }

  private static ResultHolder tryUpdateTypeParamFromObjectLiteral(HaxeExpressionEvaluatorContext context,
                                                                  HaxeGenericResolver resolver,
                                                                  HaxeObjectLiteralElement literalElement,
                                                                  SpecificHaxeClassReference type) {
    HaxeObjectLiteral objectLiteral = PsiTreeUtil.getParentOfType(literalElement, HaxeObjectLiteral.class);
    if (objectLiteral == null) return null;

    ResultHolder objectLiteralType = findObjectLiteralType(context, resolver, objectLiteral);

    if (objectLiteralType != null && !objectLiteralType.isUnknown()) {
      SpecificHaxeClassReference typeFromUsage = objectLiteralType.getClassType();
      if (typeFromUsage != null && typeFromUsage.getHaxeClassModel() != null) {
        HaxeBaseMemberModel objectLiteralElementAsMember = typeFromUsage.getHaxeClassModel()
          .getMember(literalElement.getName(), typeFromUsage.getGenericResolver());

        if (objectLiteralElementAsMember != null) {
          ResultHolder objectLiteralElementType = objectLiteralElementAsMember.getResultType(resolver);
          if (objectLiteralElementType.getClassType() != null) {
            HaxeGenericResolver genericResolver = objectLiteralElementType.getClassType().getGenericResolver();
            return genericResolver.resolve(type.createHolder());
          }
        }
      }
    }
    return null;
  }

  private static ResultHolder tryUpdateTypeParamFromArrayAccess(HaxeExpressionEvaluatorContext context,
                                                        HaxeGenericResolver resolver,
                                                        HaxeArrayAccessExpression arrayAccessExpression,
                                                        SpecificHaxeClassReference classType,
                                                        HaxeGenericResolver classResolver,
                                                        SpecificHaxeClassReference type) {
    // try to find setter first if that fails try getter
    if (classType.getHaxeClass() != null) { // need to check if Haxe class exists as it will be null when SDK is missing
      HaxeNamedComponent arrayAccessSetter = classType.getHaxeClass().findArrayAccessSetter(resolver);

      if (arrayAccessSetter instanceof HaxeMethodDeclaration methodDeclaration) {
        HaxeMethodModel methodModel = methodDeclaration.getModel();
        // make sure we are using class level typeParameters (and not method level)
        HaxeClass target = methodModel.getDeclaringClass().haxeClass;
        HaxeGenericResolver localResolver = classResolver.translateFromTo(classType.getHaxeClass(), target);
        if (methodModel.getGenericParams().isEmpty()) {
          List<HaxeParameterModel> parameters = methodModel.getParameters();

          HaxeTypeTag keyParamPsi = parameters.get(0).getTypeTagPsi();
          HaxeTypeTag valueParamPsi = parameters.get(1).getTypeTagPsi();

          // key
          HaxeTypeParameterDeclaration keyTypeParameter = tryGetTypeParameterFromTypeTag(keyParamPsi);
          if (localResolver.contains(keyTypeParameter)) {
            HaxeExpression keyExpression = arrayAccessExpression.getExpressionList().get(1);
            ResultHolder handle = handle(keyExpression, context, resolver);
            localResolver.update(keyTypeParameter, handle);
          }
            // value
          if (arrayAccessExpression.getParent() instanceof HaxeBinaryExpression binaryExpression) {
            HaxeTypeParameterDeclaration valueTypeParameter = tryGetTypeParameterFromTypeTag(valueParamPsi);
            if (localResolver.contains(valueTypeParameter)) {
              HaxeExpression valueExpression = binaryExpression.getExpressionList().get(1);
              ResultHolder handle = handle(valueExpression, context, resolver);
              localResolver.update(valueTypeParameter, handle);
            }
          }
        }
        HaxeGenericResolver resolverForClass = localResolver.translateFromTo(target, classType.getHaxeClass());
        return resolverForClass.resolve(classType.createHolder());

      } else {
        HaxeNamedComponent arrayAccessGetter = classType.getHaxeClass().findArrayAccessGetter(resolver);
        if (arrayAccessGetter instanceof HaxeMethodDeclaration methodDeclaration) {
          HaxeMethodModel methodModel = methodDeclaration.getModel();
          // make sure we are using class level typeParameters (and not method level)
          HaxeClass target = methodModel.getDeclaringClass().haxeClass;
          HaxeGenericResolver localResolver = classResolver.translateFromTo(classType.getHaxeClass(), target);
          if (methodModel.getGenericParams().isEmpty()) {
            List<HaxeParameterModel> parameters = methodModel.getParameters();
            HaxeParameterModel keyParameter = parameters.get(0);
            HaxeTypeTag keyParamPsi = keyParameter.getTypeTagPsi();

            HaxeTypeParameterDeclaration keyTypeParameter = tryGetTypeParameterFromTypeTag(keyParamPsi);
            if (localResolver.contains(keyTypeParameter)) {
              HaxeExpression KeyExpression = arrayAccessExpression.getExpressionList().get(1);
              ResultHolder handle = handle(KeyExpression, context, resolver);
              localResolver.update(keyTypeParameter, handle);
            }
          }
          HaxeGenericResolver resolverForClass = localResolver.translateFromTo(target, classType.getHaxeClass());
          return resolverForClass.resolve(classType.createHolder());
        }
      }
    }
    return null;
  }

  private static @Nullable HaxeTypeParameterDeclaration tryGetTypeParameterFromTypeTag(HaxeTypeTag keyParamPsi) {
    HaxeTypeOrAnonymous anonymous = keyParamPsi.getTypeOrAnonymous();
    if(anonymous == null) return null;
    HaxeType type = anonymous.getType();
    if(type == null) return null;
    PsiElement resolved = type.getReferenceExpression().resolve();
    if(resolved instanceof HaxeTypeParameterDeclaration declaration) return  declaration;
    return null;
  }

  private static @Nullable ResultHolder tryTypeFromObjectLiteral(HaxeExpressionEvaluatorContext context,
                                                  HaxeGenericResolver resolver,
                                                  HaxeObjectLiteralElement literalElement) {
    HaxeObjectLiteral objectLiteral = PsiTreeUtil.getParentOfType(literalElement, HaxeObjectLiteral.class);
    if (objectLiteral != null) {
      ResultHolder result = searchReferencesForTypeGuard.computePreventingRecursion(objectLiteral, false, () -> {
        ResultHolder objectLiteralType = findObjectLiteralType(context, resolver, objectLiteral);
        if (objectLiteralType != null && !objectLiteralType.isUnknown()) {
          SpecificHaxeClassReference literlClassType = objectLiteralType.getClassType();
          if (literlClassType != null) {
            HaxeClassModel classModel = literlClassType.getHaxeClassModel();
            if (classModel != null) {
              HaxeGenericResolver genericResolver = literlClassType.getGenericResolver();
              HaxeBaseMemberModel member = classModel.getMember(literalElement.getName(), genericResolver);
              if (member != null) {
                ResultHolder resultType = member.getResultType(genericResolver);
                if (resultType != null && !resultType.isUnknown()) {
                  return resultType;
                }
              }
            }
          }
        }
        return null;
      });
      if (result != null) return result;
    }
    return null;
  }

  private static @Nullable ResultHolder tryFindTypeFromMethodCallOnReference(ResultHolder resultHolder, HaxeReferenceExpression referenceExpression) {


    PsiElement resolved = referenceExpression.resolve();
    if (resolved instanceof HaxeMethodDeclaration methodDeclaration
        && referenceExpression.getParent() instanceof HaxeCallExpression callExpression) {

      HaxeMethodModel methodModel = methodDeclaration.getModel();
      HaxeCallExpressionContext context = HaxeCallExpressionUtil.createContextForMethodCall(callExpression, methodModel.getMethod());
      HaxeCallExpressionEvaluation validation = context.evaluate();
      HaxeGenericResolver resolverFromCallExpression = validation.getCallExpressionResolver();
      if (resolverFromCallExpression != null) {
        SpecificHaxeClassReference classType = resultHolder.getClassType();
        if(classType != null && methodModel.getDeclaringClass() != null) {
          HaxeClass methodDeclaringClass = methodModel.getDeclaringClass().haxeClass;
          HaxeGenericResolver translatedResolver = resolverFromCallExpression.translateFromTo(methodDeclaringClass, classType.getHaxeClass());
          ResultHolder resolve = translatedResolver.resolve(classType.replaceUnknownsWithTypeParameter());
          if (resolve != null && !resolve.isUnknown()) {
            return resolve;
          }
        }
      }
    }
    return null;
  }

  private static @Nullable ResultHolder tryTypeFromAssignExpression(HaxeExpressionEvaluatorContext context,
                                                                    HaxeGenericResolver resolver,
                                                                    ResultHolder resultHolder,
                                                                    HaxeAssignExpression assignExpression, HaxeComponentName componentName) {
    boolean isRight = false;
    boolean isLeft = false;

    HaxeExpression rightExpression = assignExpression.getRightExpression();
    HaxeExpression leftExpression = assignExpression.getLeftExpression();


    if(rightExpression instanceof  HaxeReferenceExpression referenceExpression) {
      PsiElement resolve = referenceExpression.resolve();
      if(resolve instanceof HaxeNamedComponent namedComponent) {
        if(namedComponent.getComponentName() == componentName) {
          isRight = true;
        }
      }
    }
    if(!isRight) {
      if (leftExpression instanceof HaxeReferenceExpression referenceExpression) {
        PsiElement resolve = referenceExpression.resolve();
        if (resolve instanceof HaxeNamedComponent namedComponent) {
          if (namedComponent.getComponentName() == componentName) {
            isLeft = true;
          }
        }
      }
    }

  if(isLeft || isRight) {
    ResultHolder result = handleWithRecursionGuard(isLeft ? rightExpression : leftExpression, context, resolver);
    if (result != null && !result.isUnknown() && result.getType().isSameType(resultHolder.getType())) {
      HaxeGenericResolver resultResolver = result.getClassType().getGenericResolver();
      HaxeGenericResolver resultResolverWithoutUnknowns = resultResolver.withoutUnknowns();
      // check that assigned value does not contain any unknown typeParameters (ex. someArrVar = [])
      if (resultResolver.entries().length == resultResolverWithoutUnknowns.entries().length) {
        return result;
      }
    }else if (result != null && result.isDynamic() && result.getConstant() instanceof HaxeNull) {
      return result;
    }
  }

    return null;
  }

  private static @Nullable ResultHolder tryFindTypeWhenUsedAsParameterInCallExpression(ResultHolder resultHolder,
                                                                                       HaxeReferenceExpression referenceExpression,
                                                                                       PsiElement parent) {
    if (parent != null && parent.getParent() instanceof HaxeCallExpression callExpression) {
      if (callExpression.getExpression() instanceof HaxeReference callExpressionReference) {
        final PsiElement resolved = callExpressionReference.resolve();
        HaxeCallExpressionList list = callExpression.getExpressionList();
        // check if reference used as parameter
        ResultHolder paramType = findUsageAsParameterInFunctionCall(referenceExpression, callExpression, list, resolved);
        if (paramType != null) {
          // probably not the best solution, but the goal is to keep the original type and only update typeParameters
          ResultHolder unified = HaxeTypeUnifier.unify(resultHolder, paramType);
          if (!unified.isUnknown()) return unified;
        }
      }
    }
    return null;
  }




}
