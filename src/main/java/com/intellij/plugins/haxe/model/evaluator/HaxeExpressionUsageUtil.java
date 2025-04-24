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

  public static @Nullable ResultHolder findUsageAsParameterInConstructorCall(HaxeExpression referenceExpression,
                                                                             HaxeNewExpression newExpression,
                                                                             List<HaxeExpression> list) {
    int index = -1;
    if (list != null) index = list.indexOf(referenceExpression);
    if (index == -1) return null;
    ResultHolder  assignHint=  lookForAssignHints(newExpression);
      HaxeCallExpressionContext context = HaxeCallExpressionUtil.createContextForConstructorCall(newExpression, assignHint);
      if(context != null) {
        HaxeCallExpressionEvaluation evaluated = context.evaluate();
        if (context.isStaticExtension) index++;
        return evaluated.getParameterType(index);
      }
      return null;
  }

  private static ResultHolder lookForAssignHints(HaxeNewExpression newExpression) {
    HaxePsiField field = null;
    if(newExpression.getParent() instanceof HaxeAssignExpression assignExpression) {
      HaxeExpression assignedTo = assignExpression.getLeftExpression();
      if(assignedTo instanceof  HaxeReferenceExpression assignedToReference) {
        PsiElement resolve = assignedToReference.resolve();
        if(resolve instanceof  HaxePsiField haxePsiField) {
          field = haxePsiField;
        }
      }
    }
    else if (newExpression.getParent() instanceof  HaxeVarInit  init) {
      if( init.getParent() instanceof  HaxePsiField haxePsiField) {
        field = haxePsiField;
      }
    }

      // avoiding evaluate as it will probably stop in a recursion guard
      if(field != null) {
        HaxeTypeTag typeTag = field.getTypeTag();
        if(typeTag != null) {
          ResultHolder typeFromTypeTag = HaxeTypeResolver.getTypeFromTypeTag(typeTag, field);
          if(!typeFromTypeTag.isUnknown()) return typeFromTypeTag;
        }
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
    var newValues = searchReferencesForTypeParametersRecursionGuard.computePreventingRecursion(componentName, false, () -> { //TODO mlo: figure out if we can optimize
      ResultHolder updatedType = resultHolder.duplicate();
      SpecificHaxeClassReference classType = updatedType.getClassType();
      // TODO mlo: should we add some kind of support for functions here ?
      if (classType == null) return updatedType;

      HaxeGenericResolver classResolver = classType.getGenericResolver();
      final SearchScope useScope = HaxeExpressionEvaluatorSearchUtil.getSmallestPossibleSearchScope(componentName, null);

      List<PsiReference> references = referenceSearch(componentName, useScope);
      // search until all typeParams are found or we dont have any more references or we stop due to recursion guard.
      //
      // a few notes on this loop and the method it calls
      // - if information found they return a real type
      // - if no information is found they return unknown
      // - if stopped by recursion guard og result point it itself we return null
      //
      // when we get a null we break the loop as any reference occurring later can not guarantee that it wont
      // provide other type parameters that are not compatible with our current reference that we are still
      // trying to evaluate.
      for (int i = continueFrom, size = references.size(); i < size; i++) {

        PsiReference reference = references.get(i);

        // if we are trying to resolve type for objectLiteral reference and have reach ourselves, we stop to prevent later references.
        if (context.root.getParent() instanceof HaxeObjectLiteralElement) {
          if (context.root == references.getFirst()) return null;
        }

        if (reference instanceof HaxeExpression expression) {
          PsiElement parent = expression.getParent();

          if (reference instanceof HaxeReferenceExpression referenceExpression) {
            ResultHolder result = tryFindTypeWhenUsedAsParameterInCallOrNewExpression(updatedType, referenceExpression, parent);
            if (result == null) return null;
            if (result.isDynamic()) return result;
            if (!result.isUnknown()) updatedType = mapTypeParameterIfAssignable(updatedType, result);
            if (!updatedType.containsUnknownTypes() || updatedType.isDynamic()) return updatedType;
          }

          if (parent instanceof HaxeAssignExpression assignExpression) {
            ResultHolder assignType = tryTypeFromAssignExpression(context, resolver, updatedType, assignExpression, componentName);
            if (assignType == null) return null;
            if (!assignType.isUnknown()) {
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
                  ResultHolder instanceType = getInstanceTypeWithDefaultTypeParameters(updatedType);
                  if (instanceType.canAssign(assignType)) {
                    updatedType = mapTypeParameter(updatedType, assignType);
                  }
                } else {
                  if (assignType.canAssign(updatedType)) {
                    updatedType = mapTypeParameter(updatedType, assignType);
                  }
                }
              }
            }
            if (!updatedType.containsUnknownTypes()) return updatedType;
          }

          if (parent.getParent() instanceof HaxeAssignExpression assignExpression) {
            ResultHolder result = tryFindTypeParametersFromMemberAssign(updatedType, assignExpression);
           if (result == null) continue;
           if (result.isDynamic()) return result;
           if (!result.isUnknown()) updatedType = mapTypeParameterIfAssignable(updatedType, result);
           if (!updatedType.containsUnknownTypes() || updatedType.isDynamic()) return updatedType;
          }

          if (parent instanceof HaxeReferenceExpression referenceExpression) {
            ResultHolder result = tryFindTypeFromMethodCallOnReference(updatedType, referenceExpression);
            if (result == null) continue;
            if (result.isDynamic()) return result;
            if (!result.isUnknown()) updatedType = mapTypeParameterIfAssignable(updatedType, result);
            if (!updatedType.containsUnknownTypes()) return updatedType;
          }

          if (parent instanceof HaxeObjectLiteralElement literalElement) {
            ResultHolder result = tryTypeFromObjectLiteral(context, resolver, literalElement);
            if (result == null) return null;
            if (result.isDynamic()) return result;
            if (!result.isUnknown()) updatedType = mapTypeParameterIfAssignable(updatedType, result);
            if (!updatedType.containsUnknownTypes()) return updatedType;
          }

          if (parent instanceof HaxeArrayAccessExpression arrayAccessExpression) {
            ResultHolder result = tryUpdateTypeParamFromArrayAccess(context, resolver, arrayAccessExpression, classType, classResolver, classType);
            if (result == null) return null;
            if (result.isDynamic()) return result;
            if (!result.isUnknown()) updatedType = mapTypeParameterIfAssignable(updatedType, result);
            if (!updatedType.containsUnknownTypes()) return updatedType;
          }

          if (parent instanceof HaxeObjectLiteralElement literalElement) {
            ResultHolder result = tryUpdateTypeParamFromObjectLiteral(context, resolver, literalElement, classType);
            if (result == null) return null;
            if (result.isDynamic()) return result;
            if (!result.isUnknown()) updatedType = mapTypeParameterIfAssignable(updatedType, result);
            if (!updatedType.containsUnknownTypes()) return updatedType;
          }
        }
      }
      return updatedType;
    });
    return newValues != null ? newValues.noCache() : resultHolder.noCache();
  }

  private static HaxeGenericResolver findAndSetResolverValues(SpecificFunctionReference functionLeft, SpecificFunctionReference functionRight, HaxeGenericResolver genericResolver) {
    HaxeGenericResolver resolver = genericResolver.copy();

    List<HaxeArgument> leftArgs = functionLeft.getArguments();
    List<HaxeArgument> rightArgs = functionRight.getArguments();

    int min = Math.min(leftArgs.size(), rightArgs.size());

      for (int i = 0; i < min; i++) {
        ResultHolder leftArgType = leftArgs.get(i).getType();
        ResultHolder rightArgType = rightArgs.get(i).getType();
        if (leftArgType.getClassType() != null) {
          if (leftArgType.getClassType().getHaxeClass() instanceof HaxeTypeParameterDeclaration tp) {
            resolver.add(tp, rightArgType.getType().createHolder());
          } else if (leftArgType.isOrContainsTypeParameters()) {
            resolver.addAll(findAndSetResolverValues(leftArgType, rightArgType, resolver));
          }
        }
      }

    return resolver;
  }

  private static @Nullable HaxeGenericResolver findAndSetResolverValues(ResultHolder leftArgType, ResultHolder rightArgType, HaxeGenericResolver genericResolver) {
    HaxeGenericResolver resolver = genericResolver.copy();
    SpecificTypeReference leftType = leftArgType.getType();
    SpecificTypeReference rightType = rightArgType.getType();
    if (leftType instanceof SpecificFunctionReference leftFunction && rightType instanceof SpecificFunctionReference rightFunction) {
      resolver.addAll(findAndSetResolverValues(leftFunction, rightFunction, resolver));
    }

    if (leftType instanceof SpecificHaxeClassReference leftClass && rightType instanceof SpecificHaxeClassReference rightClass) {
      if (leftClass.getHaxeClass() != rightClass.getHaxeClass()) {
        SpecificHaxeClassReference casted = rightClass.tryCastToClass(leftClass);
        if (casted != null)rightClass = casted;
      }
      @NotNull ResultHolder[] leftSpecifics = leftClass.getSpecifics();
      @NotNull ResultHolder[] rightSpecifics = rightClass.getSpecifics();
      int min = Math.min(leftSpecifics.length, rightSpecifics.length);
        for (int i = 0; i < min; i++) {
            ResultHolder leftSpecific = leftSpecifics[i];
            ResultHolder rightSpecific = rightSpecifics[i];
            if(leftSpecific.isTypeParameter()) {
              if (leftSpecific.getClassType().getHaxeClass() instanceof HaxeTypeParameterDeclaration tp) {
                resolver.add(tp, rightArgType.getType().createHolder());
              }
            }else {
              resolver.addAll(findAndSetResolverValues(leftSpecific, rightSpecific, resolver));
            }
        }
    }
    return resolver;
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
    SpecificHaxeClassReference casted = foundType.tryCastTo(current.getClassType());
    if (casted == null || casted.getTypePsi() != current.getType().getTypePsi()) return current;


    @NotNull ResultHolder[] currentSpecifics = current.getClassType().getSpecifics();
    @NotNull ResultHolder[] foundSpecifics = casted.getSpecifics();
    @NotNull ResultHolder[] newSpecifics = new ResultHolder[currentSpecifics.length];
      for (int i = 0; i < newSpecifics.length; i++) {
          ResultHolder currentSpecific = currentSpecifics[i];
          ResultHolder foundSpecific = foundSpecifics[i];
          // important, make sure we are not updating already found values
          if (currentSpecific.canMorph()
              // might not be the best solution but an attempt to avoid disableMutating when typeParameter was not used.
              // Our methods return a complete type with all typeParameters so here we guess that unchanged means not used.
              &&  (currentSpecific.getType() !=  foundSpecific.getType())
              && (currentSpecific.isUnknown() ||  (currentSpecific.isTypeParameter()  && currentSpecific.canAssign(foundSpecific))))
          {
            newSpecifics[i] = foundSpecific.duplicate();
            newSpecifics[i].disableMorphing();
          }else {
            if(currentSpecific.containsUnknownTypeParameters()) {
              newSpecifics[i] = mapTypeParameter(currentSpecific, foundSpecific);
            }else {
              newSpecifics[i] = currentSpecific;
            }
          }
      }

    return SpecificHaxeClassReference.withGenerics(current.getClassType().getHaxeClassReference(), newSpecifics).createHolder();
  }

  private static ResultHolder tryUpdateTypeParamFromObjectLiteral(HaxeExpressionEvaluatorContext context,
                                                                  HaxeGenericResolver resolver,
                                                                  HaxeObjectLiteralElement literalElement,
                                                                  SpecificHaxeClassReference type) {
    HaxeObjectLiteral objectLiteral = PsiTreeUtil.getParentOfType(literalElement, HaxeObjectLiteral.class);
    if (objectLiteral == null) return createUnknown(literalElement);

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
            ResultHolder resolve = genericResolver.resolve(type.createHolder());
            if (resolve != null) return resolve;
          }
        }
      }
    }
    return createUnknown(literalElement);
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
        ResultHolder resolve = resolverForClass.resolve(classType.createHolder());
        if (resolve != null)return resolve;

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
          ResultHolder resolve = resolverForClass.resolve(classType.createHolder());
          if (resolve != null) return resolve;
        }
      }
    }
    return createUnknown(arrayAccessExpression);
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

  // if returns null if stopped because of recursion, else real type or unknown
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
                if (resultType != null) {
                  return resultType;
                }
              }
            }
          }
        }
        return createUnknown(context.root);
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
          if (resolve != null) {
            return resolve;
          }
        }
      }
    }
    return createUnknown(referenceExpression);
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

    return createUnknown(assignExpression);
  }


  private static @Nullable ResultHolder tryFindTypeWhenUsedAsParameterInCallOrNewExpression(ResultHolder resultHolder,
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
            return HaxeTypeUnifier.unify(resultHolder, paramType);
        }
      }
    }

    if (parent instanceof HaxeNewExpression newExpression) {
        ResultHolder paramType = findUsageAsParameterInConstructorCall(referenceExpression, newExpression, newExpression.getExpressionList());
        if (paramType != null) {
          // probably not the best solution, but the goal is to keep the original type and only update typeParameters
            return HaxeTypeUnifier.unify(resultHolder, paramType);
        }
    }
    return createUnknown(referenceExpression);
  }


  private static @Nullable ResultHolder tryFindTypeParametersFromMemberAssign(ResultHolder resultHolder, HaxeAssignExpression assignExpression) {
    if(assignExpression.getAssignOperation().textContains('=')) {
      if(assignExpression.getLeftExpression() instanceof  HaxeReferenceExpression left){
        if (left.resolve() instanceof  HaxePsiField field) {
          if(field.getTypeTag() != null) {
            ResultHolder memberType = HaxeTypeResolver.getTypeFromTypeTag(field.getTypeTag(), field);
            if(memberType.isOrContainsTypeParameters()) {
              HaxeExpressionEvaluatorContext evaluate = evaluate(assignExpression.getRightExpression());
              ResultHolder rightType = evaluate.result;
              if(memberType.canAssign(rightType)) {
                SpecificHaxeClassReference leftClass = memberType.getClassType();
                SpecificHaxeClassReference rightClass = rightType.getClassType();
                if (leftClass != null && rightClass != null) {
                  SpecificHaxeClassReference casted = rightClass.tryCastToClass(leftClass);
                  if(casted != null) {
                    HaxeGenericResolver genericResolver = casted.getGenericResolver();
                    return genericResolver.resolve(resultHolder);
                  }
                } else {
                  SpecificFunctionReference functionLeft = memberType.getFunctionType();
                  SpecificFunctionReference functionRight = rightType.getFunctionType();
                  if (functionLeft != null && functionRight != null) {
                    if(functionLeft.canAssign(functionRight)) {
                      HaxeGenericResolver genericResolver = resultHolder.getClassType().getGenericResolver();
                      genericResolver = findAndSetResolverValues(functionLeft,functionRight, genericResolver );
                      return  genericResolver.resolve(resultHolder);
                    }
                  }
                }
              }
            }
          }
        }
      }
    }
    return null;
  }

}
