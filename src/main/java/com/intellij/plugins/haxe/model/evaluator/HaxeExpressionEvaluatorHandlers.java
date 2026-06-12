package com.intellij.plugins.haxe.model.evaluator;

import com.esotericsoftware.kryo.kryo5.util.Null;
import com.intellij.lang.ASTNode;
import com.intellij.lang.annotation.AnnotationBuilder;
import com.intellij.openapi.util.RecursionGuard;
import com.intellij.openapi.util.RecursionManager;
import com.intellij.plugins.haxe.HaxeBundle;
import com.intellij.plugins.haxe.ide.annotator.HaxeStandardAnnotation;
import com.intellij.plugins.haxe.lang.lexer.HaxeEmbeddedElementType;
import com.intellij.plugins.haxe.lang.lexer.HaxeTokenTypeSets;
import com.intellij.plugins.haxe.lang.lexer.HaxeTokenTypes;
import com.intellij.plugins.haxe.lang.psi.*;
import com.intellij.plugins.haxe.lang.psi.impl.*;
import com.intellij.plugins.haxe.model.*;
import com.intellij.plugins.haxe.model.evaluator.callexpression.HaxeCallExpressionContext;
import com.intellij.plugins.haxe.model.evaluator.callexpression.HaxeCallExpressionContextContainer;
import com.intellij.plugins.haxe.model.evaluator.callexpression.HaxeCallExpressionEvaluation;
import com.intellij.plugins.haxe.model.evaluator.callexpression.HaxeCallExpressionUtil;
import com.intellij.plugins.haxe.model.fixer.*;
import com.intellij.plugins.haxe.model.type.*;
import com.intellij.plugins.haxe.model.type.HaxeArgument;
import com.intellij.plugins.haxe.util.*;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiPackage;
import com.intellij.psi.PsiReference;
import com.intellij.psi.impl.source.tree.LazyParseablePsiElement;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.tree.TokenSet;
import com.intellij.psi.util.CachedValueProvider;
import com.intellij.psi.util.CachedValuesManager;
import com.intellij.psi.util.PsiTreeUtil;
import lombok.CustomLog;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.intellij.plugins.haxe.lang.lexer.HaxeTokenTypeSets.ONLY_COMMENTS;
import static com.intellij.plugins.haxe.lang.lexer.HaxeTokenTypes.KUNTYPED;
import static com.intellij.plugins.haxe.lang.psi.HaxeResolver.buildExtractVarPath;
import static com.intellij.plugins.haxe.lang.psi.impl.HaxeReferenceImpl.getLiteralClassName;
import static com.intellij.plugins.haxe.lang.psi.impl.HaxeReferenceImpl.tryToFindTypeFromCallExpression;
import static com.intellij.plugins.haxe.lang.psi.impl.HaxeReferenceUtil.isStaticExtension;
import static com.intellij.plugins.haxe.lang.psi.impl.HaxeReferenceUtil.wrapTypeInClassOrEnum;
import static com.intellij.plugins.haxe.model.evaluator.HaxeExpressionUsageUtil.searchReferencesForTypeParameters;
import static com.intellij.plugins.haxe.model.evaluator.HaxeExpressionUsageUtil.tryToFindTypeFromUsage;
import static com.intellij.plugins.haxe.model.evaluator.callexpression.HaxeCallExpressionUtil.isBindCall;
import static com.intellij.plugins.haxe.model.evaluator.callexpression.HaxeCallExpressionUtil.tryGetCallieType;
import static com.intellij.plugins.haxe.model.type.HaxeMacroUtil.resolveMacroTypesForFunction;
import static com.intellij.plugins.haxe.model.type.ResultHolder.nullOrUnknown;
import static com.intellij.plugins.haxe.model.type.SpecificTypeReference.*;
import static com.intellij.plugins.haxe.model.evaluator.HaxeExpressionEvaluator.*;
import static com.intellij.plugins.haxe.util.UsefulPsiTreeUtil.getExpectedTypeForReturn;

@CustomLog
public class HaxeExpressionEvaluatorHandlers {


  private static final RecursionGuard<PsiElement>
    evaluatorHandlersRecursionGuard = RecursionManager.createGuard("EvaluatorHandlersRecursionGuard");

  @Nullable
  static ResultHolder handleWithRecursionGuard(PsiElement element,
                                               HaxeExpressionEvaluatorContext context,
                                               HaxeGenericResolver resolver) {

    if (element == null ) return null;
    return evaluatorHandlersRecursionGuard.doPreventingRecursion(element, false, () -> handle(element, context, resolver));
  }


  static ResultHolder handleTernaryExpression(
    HaxeExpressionEvaluatorContext context,
    HaxeGenericResolver resolver,
    HaxeTernaryExpression ternaryExpression) {
    HaxeExpression[] list = ternaryExpression.getExpressionList().toArray(new HaxeExpression[0]);
    SpecificTypeReference type1 = handle(list[1], context, resolver).getType();
    SpecificTypeReference type2 = handle(list[2], context, resolver).getType();
    UnificationRules rules = context.getScope().unificationRules;
    SpecificTypeReference suggested = assignHintAsSuggestedType(resolver, rules);
    return HaxeTypeUnifier.unify(type1, type2, ternaryExpression, suggested, rules)
      .createHolder();
  }

  // Without a suggested type, HaxeTypeUnifier picks the first match in getCompatibleTypes() iteration,
  // which puts the parent class ahead of any implemented interface. Forwarding the assign hint lets
  // siblings unify to a shared interface when that is what the declaration site expects. We skip the
  // hint inside comprehension / function-literal scopes (IGNORE_VOID / PREFER_VOID) — there the hint
  // describes the outer container or return type, not the if/ternary value itself.
  private static SpecificTypeReference assignHintAsSuggestedType(HaxeGenericResolver resolver, UnificationRules rules) {
    if (resolver == null) return null;
    if (rules == UnificationRules.IGNORE_VOID || rules == UnificationRules.PREFER_VOID) return null;
    ResultHolder hint = resolver.getAssignHint();
    if (hint == null || hint.isUnknown()) return null;
    return hint.getType();
  }

  // Specifically for lambda return-type inference: extracts the return component of a function-type hint.
  // The generic assignHintAsSuggestedType helper deliberately skips PREFER_VOID scopes, but a function
  // literal *is* a PREFER_VOID scope by design, so it needs its own projection from `(args) -> R` to `R`.
  private static SpecificTypeReference functionReturnHintFor(HaxeGenericResolver resolver) {
    if (resolver == null) return null;
    ResultHolder hint = resolver.getAssignHint();
    if (hint == null || hint.isUnknown()) return null;
    if (hint.getFunctionType() instanceof SpecificFunctionReference functionRef) {
      ResultHolder ret = functionRef.getReturnType();
      if (ret != null && !ret.isUnknown()) return ret.getType();
    }
    return null;
  }

  static ResultHolder handleBinaryExpression(HaxeExpressionEvaluatorContext context, HaxeGenericResolver resolver,
                                                     HaxeBinaryExpression expression) {
    if (
      (expression instanceof HaxeAdditiveExpression) ||
      (expression instanceof HaxeModuloExpression) ||
      (expression instanceof HaxeBitwiseExpression) ||
      (expression instanceof HaxeShiftExpression) ||
      (expression instanceof HaxeLogicAndExpression) ||
      (expression instanceof HaxeLogicOrExpression) ||
      (expression instanceof HaxeCompareExpression) ||
      (expression instanceof HaxeCoalescingExpression) ||
      (expression instanceof HaxeInExpression) ||
      (expression instanceof HaxeMultiplicativeExpression)
    ) {
      PsiElement[] children = expression.getChildren();
      String operatorText;
      if (children.length == 3) {
        if(children[1] instanceof  HaxeOperator operator) {
          SpecificTypeReference left = handle(children[0], context, resolver).getType();
          SpecificTypeReference right = handle(children[2], context, resolver).getType();

          left = resolveAnyTypeDefsOrTypeParameterConstraint(left);
          right = resolveAnyTypeDefsOrTypeParameterConstraint(right);

          if(left != null && left.isNullType()) left = SpecificHaxeClassReference.tryUnwrapNullType(left);
          if(right != null && right.isNullType()) right =SpecificHaxeClassReference.tryUnwrapNullType(right);

          // we might have constraints that help up here
          if (left != null && left.isTypeParameter()) left = tryResolveTypeParameter(left, resolver);
          if (right != null && right.isTypeParameter()) right = tryResolveTypeParameter(right, resolver);

          if(left == null || right == null) return createUnknown(expression);

          return HaxeOperatorResolver.getBinaryOperatorResult(expression, left, right, operator, context).createHolder();
        }
      }
      else {
        operatorText = getOperator(expression, HaxeTokenTypeSets.OPERATORS);
        SpecificTypeReference left = handle(children[0], context, resolver).getType();
        SpecificTypeReference right = handle(children[1], context, resolver).getType();
        left = resolveAnyTypeDefsOrTypeParameterConstraint(left);
        right = resolveAnyTypeDefsOrTypeParameterConstraint(right);
        // we might have constraints that help up here
        if(left.isTypeParameter())  left = tryResolveTypeParameter(left, resolver);
        if(right.isTypeParameter())  right = tryResolveTypeParameter(right, resolver);
      //TODO
        throw  new RuntimeException("MLO: inspect");
        //return HaxeOperatorResolver.getBinaryOperatorResult(expression, left, right, operatorText, context).createHolder();
      }
    }
    return createUnknown(expression);
  }

  private static SpecificTypeReference tryResolveTypeParameter(SpecificTypeReference typeParam, HaxeGenericResolver resolver) {
    ResultHolder resolve = resolver.resolve(typeParam.createHolder());
    if( resolve != null && !resolve.isUnknown()) {
      return resolve.getType();
    }
    return typeParam;
  }

  static ResultHolder handleTypeCheckExpr(
    HaxeExpressionEvaluatorContext context,
    HaxeGenericResolver resolver,
    HaxeTypeCheckExpr typeCheckExpr) {
    PsiElement[] children = typeCheckExpr.getChildren();
    if (children.length == 2) {
      SpecificTypeReference statementType = handle(children[0], context, resolver).getType();
      SpecificTypeReference assertedType = SpecificTypeReference.getUnknown(children[1]);
      if (children[1] instanceof HaxeTypeOrAnonymous) {
        HaxeTypeOrAnonymous toa = typeCheckExpr.getTypeOrAnonymous();
        if (toa != null ) {
          assertedType = HaxeTypeResolver.getTypeFromTypeOrAnonymous(toa).getType();
        }
      }
      // When we have proper unification (not failing to dynamic), then we should be checking if the
      // values unify.
      //SpecificTypeReference unified = HaxeTypeUnifier.unify(statementType, assertedType, element);
      //if (!unified.canAssign(statementType)) {
      if (!assertedType.canAssign(statementType)) {
        context.addError(typeCheckExpr, "Statement of type '" + statementType.getElementContext().getText() + "' does not unify with asserted type '" + assertedType.getElementContext().getText() + ".'");
        // TODO: Develop some fixers.
        // annotation.registerFix(new HaxeCreateLocalVariableFixer(accessName, element));
      }

      return statementType.createHolder();
    }
    return createUnknown(typeCheckExpr);
  }

  static ResultHolder handleGuard(
    HaxeExpressionEvaluatorContext context,
    HaxeGenericResolver resolver,
    HaxeGuard haxeGuard) {
    HaxeExpression guardExpression = haxeGuard.getExpression();
    SpecificTypeReference expr = handle(guardExpression, context, resolver).getType();
    if (!SpecificTypeReference.getBool(haxeGuard).canAssign(expr)) {
      context.addError(
        guardExpression,
        "If expr " + expr + " should be bool",
        new HaxeCastFixer(guardExpression, expr, SpecificHaxeClassReference.getBool(haxeGuard))
      );
    }

    if (expr.isConstant()) {
      context.addWarning(guardExpression, "If expression constant");
    }
    return expr.createHolder();
  }

  @Nullable
  static ResultHolder handleReferenceExpression( HaxeExpressionEvaluatorContext context, HaxeGenericResolver resolver,
                                                         HaxeReferenceExpression element) {
    PsiElement[] children = element.getChildren();
    ResultHolder typeHolder = null;
    if (children.length == 0) {
       typeHolder  = SpecificTypeReference.getUnknown(element).createHolder();
    }else {
      PsiElement firstChild = children[0];
      //TODO mlo: might be able to give a better type based on what macro typedef is used, but for now we stick with
      // expr to avoid assign errors
      if(firstChild instanceof HaxeMacroIdentifier) {
        typeHolder  = HaxeMacroTypeUtil.getExpr(element).createHolder();
      }else {
        // make sure  expression  is not something like  `var myVar = myVar.add(x)`, we cant resolve type from this
        if (firstChild != element) {
          typeHolder = handle(firstChild, context, resolver);
        } else {
          typeHolder = SpecificTypeReference.getUnknown(element).createHolder();
        }
      }
    }

    boolean resolved = !typeHolder.getType().isUnknown();
    for (int n = 1; n < children.length; n++) {
      PsiElement child = children[n];
      SpecificTypeReference typeReference = typeHolder.getType();
      if (typeReference.isString() && typeReference.isConstant() && child.textMatches("code")) {
        String str = (String)typeReference.getConstant();
        typeHolder = SpecificTypeReference.getInt(element, (str != null && !str.isEmpty()) ? str.charAt(0) : -1).createHolder();
        if (str == null || str.length() != 1) {
          context.addError(element, "String must be a single UTF8 char");
        }
      } else {

        if (typeReference.isUnknown()) continue;

        if (typeReference.isNullType()) {
          typeHolder = typeHolder.tryUnwrapNullType();
        }

        // TODO: Yo! Eric!!  This needs to get fixed.  The resolver is coming back as Dynamic, when it should be String

        // Grab the types out of the original resolver (so we don't modify it), and overwrite them
        // (by adding) with the class' resolver. That way, we get the combination of the two, and
        // any parameters provided/set in the class will override any from the calling context.
        HaxeGenericResolver localResolver = new HaxeGenericResolver();
        localResolver.addAll(resolver);

        SpecificHaxeClassReference classType = typeHolder.getClassType();

        // if typeParameter with constraint use constraints
        if (classType != null && classType.getHaxeClass() instanceof HaxeGenericListPart genericListPart) {
          HaxeGenericParamModel model = genericListPart.getModel();
          if(model.hasConstraint()) {
            ResultHolder constraint = model.getConstraint(resolver);
            if (constraint != null && !constraint.isUnknown()){
              typeHolder = constraint;
              SpecificHaxeClassReference constraintClass = typeHolder.getClassType();
              if(constraintClass!= null) localResolver.addAll(constraintClass.getGenericResolver());
            }
          }
        }
        if (null != classType) {
          localResolver.addAll(classType.getGenericResolver());
        }
        String accessName = child.getText();
        ResultHolder access = typeHolder.getType().access(accessName, context, localResolver);
        if (access == null) {
          resolved = false;

          if (children.length == 1) {
            context.addError(children[n], "Can't resolve '" + accessName + "' in " + typeHolder.getType());
          }
          else {
            context.addError(children[n], "Can't resolve '" + accessName + "' in " + typeHolder.getType());
          }

        }
        if (access != null){
          typeHolder = access;
        }else {
          typeHolder = createUnknown(child);
        }
      }
    }

    // If we aren't walking the body, then we might not have seen the reference.  In that
    // case, the type is still unknown.  Let's see if the resolver can figure it out.
    PsiElement subelement = null;
    if (!resolved) {
      PsiReference reference = element.getReference();
      if (reference != null) {
        subelement = reference.resolve();
        //TODO
        // currently we don't resolve switchCaseExpr in enum extraction to anything so the reference points to itself
        // we should probably rewrite the BNF so its not a reference but more of a field
        if (subelement == element) {
          if (element.getParent() instanceof  HaxeSwitchCaseExpr switchCaseExpr) {
            subelement = switchCaseExpr;
          }
        }
        //TODO make a cleaner solution for this:
        // hackish way to add GenericResolver values to  EnumArgumentExtractor expressions
        if (subelement instanceof HaxeParameter parameter) {
          if(parameter.getParent().getParent() instanceof  HaxeEnumValueDeclarationConstructor) {
            HaxeExtractorMatchExpression matchExpression = PsiTreeUtil.getParentOfType(element, HaxeExtractorMatchExpression.class);
            if(matchExpression != null) {
              HaxeModel model = ((HaxeEnumArgumentExtractorImpl) matchExpression.getParent().getParent()).getModel();
              if(model instanceof HaxeEnumExtractorModel extractorModel) {
                resolver.addAll(extractorModel.getGenericResolver());
              }
            }
          }
        }
        if (subelement != element) {
          if (subelement instanceof HaxeReferenceExpression referenceExpression) {
            PsiElement resolve = referenceExpression.resolve();
            if (resolve != element)
              typeHolder = handleWithRecursionGuard(resolve, context, resolver);
          }
          if (subelement instanceof HaxeImportAliasPsiMixinImpl importAlias) {
            //TODO mlo, add tests for method/function alias
            typeHolder = handleWithRecursionGuard(importAlias, context, resolver);
            if(typeHolder != null) {
              if (reference instanceof HaxeReferenceExpressionImpl expression) {
                if (expression.isPureClassReferenceOf(importAlias.getIdentifier().getText())) {
                  SpecificHaxeClassReference classType = typeHolder.getClassType();
                  if(classType != null) {
                    HaxeClass haxeClass = classType.getHaxeClass();
                    if(haxeClass != null) typeHolder = wrapTypeInClassOrEnum(element, haxeClass);
                  }
                }
              }
            }
          }
          if (subelement instanceof HaxeClass haxeClass) {

            HaxeClassModel model = haxeClass.getModel();
            HaxeClassReference classReference = new HaxeClassReference(model, element);

            if (haxeClass.isGeneric()) {
              @NotNull ResultHolder[] specifics = resolver.getSpecificsFor(classReference);
              SpecificHaxeClassReference specificReference = SpecificHaxeClassReference.withGenerics(classReference, specifics);
              // hackish way to ignore typeParameters for dynamic if not in expression
              if (specificReference.isDynamic() && element.textMatches("Dynamic")) {
                specificReference = SpecificHaxeClassReference.getDynamic(element);
              }
              typeHolder = specificReference.createHolder();
            }
            else {
              typeHolder = SpecificHaxeClassReference.withoutGenerics(classReference).createHolder();
            }
            // make sure we do not wrap type in class if reference is type in ObjectLiteral
            if (!(element.getParent() instanceof HaxeObjectLiteralElement)) {
              // check if pure Class Reference
              if (reference instanceof HaxeReferenceExpressionImpl expression) {
                if (expression.isPureClassReferenceOf(haxeClass)) {
                  // make sure its not an import statement
                  if (PsiTreeUtil.getParentOfType(expression, HaxeImportStatement.class) == null) {
                    typeHolder = wrapTypeInClassOrEnum(element,  haxeClass);
                  }
                }
              }
            }
          }
          else if (subelement instanceof HaxeFieldDeclaration fieldDeclaration) {

            // check if enum abstract field and override type if referenced from outside the enum
            HaxeAbstractTypeDeclaration abstractParentFromResolved = PsiTreeUtil.getStubOrPsiParentOfType(subelement, HaxeAbstractTypeDeclaration.class);
            if(abstractParentFromResolved != null) {
              HaxeAbstractTypeDeclaration abstractParentFromReference = PsiTreeUtil.getStubOrPsiParentOfType(subelement, HaxeAbstractTypeDeclaration.class);
              if (abstractParentFromReference != abstractParentFromResolved) {
                HaxeClassModel model = abstractParentFromResolved.getModel();
                if (model.isEnum()) {
                  return model.getInstanceReference().createHolder();
                }
              }
            }

            HaxeVarInit init = fieldDeclaration.getVarInit();
            if (init != null) {
              boolean immutable = false;
              if (fieldDeclaration.getModel() instanceof HaxeFieldModel model) {
                immutable = model.isFinal();
              }
              HaxeExpression initExpression = init.getExpression();
              HaxeGenericResolver initResolver = HaxeGenericResolverUtil.generateResolverFromScopeParents(initExpression);
              typeHolder = HaxeTypeResolver.getFieldOrMethodReturnType(fieldDeclaration, initResolver).setImmutable(immutable);
            }
            else {
              HaxeTypeTag tag = fieldDeclaration.getTypeTag();
              if (tag != null) {
                typeHolder = HaxeTypeResolver.getTypeFromTypeTag(tag, fieldDeclaration);
                HaxeClass  usedIn = PsiTreeUtil.getStubOrPsiParentOfType((PsiElement)reference, HaxeClass.class);
                HaxeClass containingClass = (HaxeClass)fieldDeclaration.getContainingClass();
                if (usedIn != null && containingClass != null && usedIn != containingClass && containingClass.isGeneric()) {
                  HaxeGenericResolver inheritedClassResolver = resolver.translateFromTo(usedIn, containingClass);
                  ResultHolder resolve = inheritedClassResolver.resolve(typeHolder);
                  if (resolve != null && !resolve.isUnknown()) typeHolder = resolve;
                }else if (typeHolder.isTypeParameter()) {
                  ResultHolder resolve = resolver.resolve(typeHolder);
                  if(resolve != null && !resolve.isUnknown()) {
                    typeHolder = resolve;
                  }
                }
              }
            }
          }
          else if (subelement instanceof HaxeMethod haxeMethod) {
            boolean isFromCallExpression = reference instanceof  HaxeCallExpression;

            HaxeMethodModel model = haxeMethod.getModel();
            HaxeGenericResolver localResolver = new HaxeGenericResolver();
            localResolver.addAll(resolver);

            if(model != null) {
              HaxeClass referenceClass = PsiTreeUtil.getStubOrPsiParentOfType(element, HaxeClass.class);
              HaxeClassModel classModel = model.getDeclaringClass();
              if(referenceClass != null && classModel != null && classModel.haxeClass != null) {
                localResolver = resolver.translateFromTo(referenceClass, classModel.haxeClass);
              }
            }

            SpecificFunctionReference type = haxeMethod.getModel().getFunctionType(isFromCallExpression ? localResolver : localResolver.withoutAssignHint());
            if (!isFromCallExpression) {
              if( reference instanceof HaxeReferenceExpression referenceExpression) {
                // if this is a reference to an extension method we need to bind the callie type
                if(isStaticExtension(referenceExpression)) {
                  List<HaxeArgument> argumentsToKeep  = type.getArguments();
                  HaxeGenericResolver bindResolver =  null;
                  // if this is from a method we might have typeParameters and need to "bind" these if they are present in the callie
                  if(type.method != null) {
                    List<SpecificTypeReference> params = argumentsToKeep.stream().map(HaxeArgument::getType).map(ResultHolder::getType).collect(Collectors.toList());
                    params.removeFirst();
                    SpecificTypeReference callieType = tryToFindPreviousTypeInChainForExtensionMethods(element.getFirstChild());
                    params.addFirst(callieType);
                    HaxeCallExpressionContext tmpContext = HaxeCallExpressionUtil.createContextForMethodCall(params, type.method, null);
                    HaxeCallExpressionEvaluation evaluate = tmpContext.evaluate();
                    bindResolver = evaluate.getCallExpressionResolver();
                  }
                  argumentsToKeep.removeFirst(); // remove first argument as this should be the callie
                  type = type.performMethodBind(argumentsToKeep, bindResolver);
                }
              }
              //  expression is referring to the method not calling it.
              //  assign hint should be used for substituting parameters instead of being used as return type
              type = resolver.substituteTypeParamsWithAssignHintTypes(type);
            }
            typeHolder = type.createHolder();
          }

          else if (subelement instanceof HaxeValueIterator valueIterator) {
            typeHolder = handleValueIterator(context, resolver, valueIterator);
          }

          else if (subelement instanceof HaxeIteratorkey || subelement instanceof HaxeIteratorValue) {
            typeHolder = findIteratorType(subelement);
          }
          // case MyEnum(ref)
          else if (subelement instanceof HaxeEnumExtractedValue extractedValue) {
            typeHolder = handle(extractedValue.getExpression(),context,resolver);
          }

          // case var x; / case x = ...;
          else if (subelement instanceof HaxeSwitchCaseCaptureVar || subelement instanceof  HaxeSwitchCaseCapture) {
            HaxeEnumArgumentExtractor argumentExtractor =  PsiTreeUtil.getParentOfType(subelement, HaxeEnumArgumentExtractor.class, true, HaxeSwitchStatement.class);
            // if reference is in an argument extractor, get type from enum constructor parameter list (typical "case MyEnumVal( x = {..}")
            if (argumentExtractor != null) {
              List<@NotNull PsiElement> argExtractChildren = Arrays.asList(argumentExtractor.getEnumExtractorArgumentList().getChildren());
              int index = argExtractChildren.indexOf(subelement);
              if (index > -1) {
                PsiElement enumConsPsi = argumentExtractor.getEnumValueReference().getReferenceExpression().resolve();
                if (enumConsPsi instanceof HaxeEnumValueDeclarationConstructor constructor) {
                  HaxeParameterList parameterList = constructor.getParameterList();
                  List<HaxeParameter> list = parameterList.getParameterList();
                  if (index < list.size()) {
                    HaxeParameter parameter = list.get(index);
                    return handle(parameter, context, resolver);
                  }
                }
              }
            }
            // if not in an arg extractor, then use type from switch (typical in  "case var x:" and "case x = ..." )
            HaxeSwitchStatement switchStatement = PsiTreeUtil.getParentOfType(subelement, HaxeSwitchStatement.class);
            if (switchStatement.getExpression() != null) {
              return handle(switchStatement.getExpression(), context, resolver);
          }
          }

          else if (subelement instanceof HaxeSwitchCaseExpr caseExpr) {
            if(caseExpr.getParent() instanceof  HaxeExtractorMatchExpression matchExpression) {
              HaxeModel model = ((HaxeEnumArgumentExtractorImpl) matchExpression.getParent().getParent()).getModel();
              if(model instanceof HaxeEnumExtractorModel extractorModel) {
                resolver.addAll(extractorModel.getGenericResolver());
              }
              typeHolder = handle(matchExpression.getExtractorExpression(),context, resolver);
            }else {
              HaxeSwitchStatement switchStatement = PsiTreeUtil.getParentOfType(caseExpr, HaxeSwitchStatement.class);
              if (switchStatement.getExpression() != null) {
                typeHolder = handle(switchStatement.getExpression(), context, resolver);
              }
            }
          }

          else if (typeHolder == null  || typeHolder.isUnknown()) {
            // attempt to resolve sub-element using default handle logic
            if (subelement != null && !(subelement instanceof PsiPackage)) {
              typeHolder = handle(subelement, context, resolver);
            }
          }
        }
      }
    }

    if (typeHolder != null) {
      if (isReificationReference(element)) {
        ResultHolder specifics = tryExtractTypeFormExprOf(element, typeHolder);
        if (specifics != null) return specifics;
      }

      if (isReificationExpression(element)) {
        return HaxeMacroTypeUtil.getExpr(element).createHolder();
      }

      if (subelement instanceof HaxeImportAlias) return typeHolder;
      // overriding  context  to avoid problems with canAssign thinking this is a "Pure" class reference
      if (!typeHolder.isFunctionType())  return typeHolder.withElementContext(element);
      return typeHolder;
    }

    return typeHolder;
    //return SpecificTypeReference.getDynamic(element).createHolder();
  }

  private static SpecificTypeReference tryToFindPreviousTypeInChainForExtensionMethods(PsiElement firstChild) {
    ResultHolder result = evaluateWithRecursionGuard(firstChild).result;
    if(result == null) return createUnknown(firstChild).getType();
    // check if extension method is on a pure references and if so wrap it in class/enum
    if(result.getType() instanceof SpecificHaxeClassReference classReference) {
      String className = classReference.getClassName();
      if (className != null &&firstChild.textMatches(className)) {
        HaxeClass haxeClass = classReference.getHaxeClass();
        if(haxeClass != null) return wrapTypeInClassOrEnum(firstChild, haxeClass).getType();
      }
    }
    return result.getType();
  }


  private static boolean isReificationExpression(HaxeReferenceExpression element) {
    return false;
  }

  private static @Nullable ResultHolder tryExtractTypeFormExprOf(HaxeReferenceExpression element, ResultHolder typeHolder) {
    SpecificHaxeClassReference type = typeHolder.getClassType();
    if (type != null && type.getHaxeClass() != null) {
      String qualifiedName = type.getHaxeClass().getQualifiedName();
      if (qualifiedName != null) {
        if (qualifiedName.equals(HaxeMacroTypeUtil.EXPR_OF)) {
          @NotNull ResultHolder[] specifics = type.getSpecifics();
          if (specifics.length == 1) return specifics[0];
        } else if (qualifiedName.equals(HaxeMacroTypeUtil.EXPR)) {
          SpecificTypeReference.getDynamic(element).createHolder();
        }
      }
    }
    return null;
  }

  private static boolean isReificationReference(HaxeReferenceExpression element) {
    @NotNull PsiElement[] children = element.getChildren();
    if (children.length == 1) {
      if (children[0] instanceof HaxeMacroIdentifier identifier) {
        return identifier.getMacroId() != null;
      }
    }
    return false;
  }

  static ResultHolder handleValueIterator(HaxeExpressionEvaluatorContext context,
                                        HaxeGenericResolver resolver,
                                        HaxeValueIterator valueIterator) {
    HaxeForStatement forStatement = PsiTreeUtil.getParentOfType(valueIterator, HaxeForStatement.class);
    if (forStatement != null) {
        final HaxeIterable iterable = forStatement.getIterable();
        if (iterable != null) {
          // NOTE do not forward resolver here, this is a different expression and might have its own typeParameters
          ResultHolder iterator = handle(iterable, context, null);
          SpecificHaxeClassReference classType = iterator.getClassType();
          if (classType == null) return createUnknown(valueIterator);

          SpecificTypeReference resolved = classType.fullyResolveTypeDefAndUnwrapNullTypeReference();
          if (resolved instanceof SpecificHaxeClassReference resolvedClassReference) {
            classType = resolvedClassReference;
          }


          //extract iterator type from "next" method
          HaxeGenericResolver iteratorResolver = classType.getGenericResolver();
          HaxeClassModel classModel = classType.getHaxeClassModel();
          if (classModel != null) {
            HaxeMethodModel next = classModel.getMethod("next", iteratorResolver);
            if (next != null) {
              return next.getReturnType(iteratorResolver);
            }
          }
        }
    }
    return createUnknown(valueIterator);
  }

  static ResultHolder handleRegularExpressionLiteral(HaxeRegularExpressionLiteral regexLiteral) {
    HaxeClass regexClass = HaxeResolveUtil.findClassByQName(getLiteralClassName(HaxeTokenTypes.REG_EXP), regexLiteral);
    if (regexClass != null) {
      return SpecificHaxeClassReference.withoutGenerics(new HaxeClassReference(regexClass.getModel(), regexLiteral)).createHolder();
    }
    return createUnknown(regexLiteral);
  }

  static ResultHolder handleStringLiteralExpression(PsiElement element) {
    // @TODO: check if it has string interpolation inside, in that case text is not constant
    String constant = HaxeStringUtil.unescapeString(element.getText());
    return SpecificHaxeClassReference.primitive("String", element, constant).createHolder();
  }

  static ResultHolder handleSwitchCaseCaptureVar(HaxeGenericResolver resolver, HaxeSwitchCaseCaptureVar captureVar) {
    HaxeSwitchStatement switchStatement = PsiTreeUtil.getParentOfType(captureVar, HaxeSwitchStatement.class);
    if(switchStatement != null && switchStatement.getExpression() != null){
      return HaxeTypeResolver.getPsiElementType(switchStatement.getExpression(), resolver);
    }
    return createUnknown(captureVar);
  }

  static ResultHolder handleFieldDeclaration(
    HaxeExpressionEvaluatorContext context,
    HaxeGenericResolver resolver,
    HaxeFieldDeclaration declaration) {
    HaxeTypeTag typeTag = declaration.getTypeTag();

    boolean immutable = false;
    HaxeVarInit init = declaration.getVarInit();

    if (declaration.getModel() instanceof HaxeFieldModel model) {
      immutable = model.isFinal() && init != null;
    }
    if (typeTag != null) {
      return HaxeTypeResolver.getTypeFromTypeTag(typeTag, declaration).setImmutable(immutable);
    } else if (init != null) {
      if (init.getExpression() != null) {
        ResultHolder result = handle(init.getExpression(), context, resolver);
        if (isDynamicBecauseOfNullValueInit(result)) {
          HaxeComponentName element = declaration.getComponentName();
          final ResultHolder hint = result;
          result = tryToFindTypeFromUsage(element, result, hint, context, resolver, null);
        }
        if (result != null) return result.setImmutable(immutable);
      }
    }
    return createUnknown(declaration);
  }

  static ResultHolder handleSpreadExpression(HaxeGenericResolver resolver, HaxeSpreadExpression spreadExpression) {
    HaxeExpression expression = spreadExpression.getExpression();
    // we treat restParameters as arrays, so we need to "unwrap" the array to get the correct type.
    // (currently restParameters and Arrays are the only types you can spread afaik. and only in method calls)
    if (expression instanceof HaxeReferenceExpression referenceExpression) {
      ResultHolder type = HaxeTypeResolver.getPsiElementType(referenceExpression, resolver);
      if (type.isClassType()) {
        ResultHolder[] specifics = type.getClassType().getSpecifics();
        if (specifics.length == 1) {
          return specifics[0];
        }
      }
    }
    else if (expression instanceof HaxeArrayLiteral arrayLiteral) { // TODO mlo: verify this works
      ResultHolder resultHolder = handleArrayLiteral(new HaxeExpressionEvaluatorContext(spreadExpression), resolver, arrayLiteral);
      SpecificHaxeClassReference classType = resultHolder.getClassType();
      if(classType != null) {
        @NotNull ResultHolder[] specifics = classType.getSpecifics();
        if (specifics.length == 1) {
          return specifics[0];
        }
      }
    }
    return createUnknown(spreadExpression);
  }


  static ResultHolder handleParameter(
    HaxeExpressionEvaluatorContext context,
    HaxeGenericResolver resolver,
    HaxeParameter parameter) {
    HaxeTypeTag typeTag = parameter.getTypeTag();
    if (typeTag != null) {
      ResultHolder typeFromTypeTag = HaxeTypeResolver.getTypeFromTypeTag(typeTag, parameter);
      ResultHolder resolve = resolver.withoutUnknowns().resolve(typeFromTypeTag);
      if (resolve != null && !resolve.isUnknown()) typeFromTypeTag = resolve;
      // if parameter is optional then its nullable and should be Null<T>
      if (parameter.getOptionalMark() != null && !typeFromTypeTag.isNullWrappedType()) {
        return typeFromTypeTag.wrapInNullType(parameter.getOptionalMark());
      }
      return typeFromTypeTag;
    }

    HaxeVarInit init = parameter.getVarInit();
    if (init != null) {
      ResultHolder holder = handle(init, context, resolver);
      if (!holder.isUnknown()) {
        if (parameter.getOptionalMark() != null && !holder.isNullWrappedType()) {
          // if parameter is optional then its nullable and should be Null<T>
          return holder.wrapInNullType(parameter.getOptionalMark());
        }
        return holder;
      }
    }else {
      if (parameter.getParent().getParent() instanceof HaxeFunctionLiteral functionLiteral) {
        ResultHolder holder = null;
          holder = tryToFindTypeFromCallExpression(functionLiteral, parameter);
          if (holder == null) {
            holder = tryGetTypeFromAssignToType(functionLiteral, parameter, resolver);
          }
          if (holder == null || holder.isOrContainsTypeParameters()) {
            HaxeComponentName name = parameter.getComponentName();
            final ResultHolder hint = holder;
            ResultHolder searchResult =  evaluatorHandlersRecursionGuard.computePreventingRecursion(name, true, () -> {
                return searchReferencesForType(name, context, resolver, functionLiteral, hint);
            });
            if (searchResult!= null && !searchResult.isUnknown()) holder = searchResult;
          }

        if (holder!= null && !holder.isUnknown()) {
          ResultHolder resolve = resolver.resolve(holder);
          return resolve != null && !resolve.isUnknown() ? resolve : holder;
        }else {

          return createUnknown(parameter);
        }
      }else {
        HaxeMethod method = PsiTreeUtil.getParentOfType(parameter, HaxeMethod.class);
        ResultHolder holder = searchReferencesForType(parameter.getComponentName(), context, resolver, method.getBody());
        if (holder!= null && !holder.isUnknown()) {
          return holder;
        }
      }
    }
    return createUnknown(parameter);
  }

  private static ResultHolder tryGetTypeFromAssignToType(HaxeFunctionLiteral functionLiteral, HaxeParameter parameter, HaxeGenericResolver resolver) {
    if(functionLiteral.getParent() instanceof  HaxeAssignExpression assignExpression) {
      HaxeExpression leftExpression = assignExpression.getLeftExpression();
      if (leftExpression != null) {
        ResultHolder handle = handle(leftExpression, new HaxeExpressionEvaluatorContext(parameter), resolver);
        if (handle.isFunctionType()) {
          SpecificFunctionReference functionType = handle.getFunctionType();
          HaxeParameterList parameterList = functionLiteral.getParameterList();
          if(parameterList != null && functionType != null) {
            int index = parameterList.getParameterList().indexOf(parameter);
            List<HaxeArgument> arguments = functionType.getArguments();
            if(index != -1 && index < arguments.size()) {
              return arguments.get(index).getType();
            }
          }
        }
      }
    }
    return null;
  }

  static ResultHolder handleNewExpression(
    HaxeExpressionEvaluatorContext context,
    HaxeGenericResolver resolver,
    HaxeNewExpression expression) {
    HaxeType type = expression.getType();
    if (type != null) {
      if (isMacroVariable(type.getReferenceExpression().getIdentifier())){
        return SpecificTypeReference.getDynamic(expression).createHolder();
      }
      ResultHolder hint = resolver.getAssignHint();
      // remove Null wrapping in hints, a new expression can not be Null<> and just adds another unnecessary layer to the generic resolvers
      if(hint != null && hint.isNullWrappedType()) {
        hint = hint.tryUnwrapNullType();
      }

      ResultHolder typeHolder = HaxeTypeResolver.getTypeFromType(type, resolver);


      if (!typeHolder.isUnknown() && typeHolder.getClassType() != null) {
        SpecificHaxeClassReference classReference = typeHolder.getClassType();
        HaxeClassModel classModel = classReference.getHaxeClassModel();
        HaxeGenericResolver classResolver = classReference.getGenericResolver();
        if (classModel != null) {
          HaxeMethodModel constructor = classModel.getConstructor(classResolver);
          if (constructor != null) {
            HaxeMethod method = constructor.getMethod();
            HaxeMethodModel methodModel = method.getModel();
            if (methodModel.getGenericParams().isEmpty()) {
              HaxeCallExpressionContextContainer contextContainer = HaxeCallExpressionUtil.createContextForConstructorCall(expression);
              HaxeCallExpressionEvaluation validation =contextContainer.evaluateContexts();
              if (validation != null) {
                ResultHolder returnType = validation.getReturnType();

                // NOTE:
                // in the case of typeDefs the constructor and class does not reflect the typeDefinition
                // ex. typedef  nyTypedef<V> = MyClass<String, V>
                // so we need to do an translate and extra resolve step

                if (returnType.getClassType() != null ) {
                  HaxeClassModel declaringClass = methodModel.getDeclaringClass();
                  if (declaringClass != null && declaringClass.haxeClass != null) {
                    HaxeClass constructorHaxeClass = declaringClass.haxeClass;
                    HaxeClass returnTypeHaxeClass = returnType.getClassType().getHaxeClass();
                    if (constructorHaxeClass != returnTypeHaxeClass) {
                      HaxeGenericResolver resolverFromCallExpression = validation.getCallExpressionResolver();
                      HaxeGenericResolver genericResolver = resolverFromCallExpression.translateFromTo(constructorHaxeClass, returnTypeHaxeClass);
                      returnType = genericResolver.resolve(returnType);
                    }
                  }
                }
                if (returnType!= null && !returnType.isUnknown()) typeHolder = returnType;
              }
            }
          }
        }
      }

      if (hint != null && hint.isClassType()) {
        HaxeGenericResolver localResolver = new HaxeGenericResolver();
        HaxeGenericResolver hintsResolver = hint.getClassType().getGenericResolver();
        localResolver.addAll(hintsResolver);
        ResultHolder resolvedWithHint = localResolver.resolve(typeHolder);
        // TODO mlo: make a resolveWithConstraintCheck or something like that
        //verify that this does not break constraints
        if (resolvedWithHint != null && !resolvedWithHint.isUnknown()) {
          if(typeHolder.canAssign(resolvedWithHint)) {
            typeHolder = resolvedWithHint;
          }
        }
      }

      if (typeHolder.getType() instanceof SpecificHaxeClassReference classReference) {
        final HaxeClassModel clazz = classReference.getHaxeClassModel();
        if (clazz != null) {
          HaxeMethodModel constructor = clazz.getConstructor(resolver);
          if (constructor == null) {
            context.addError(expression, "Class " + clazz.getName() + " doesn't have a constructor", new HaxeFixer("Create constructor") {
              @Override
              public void run() {
                // @TODO: Check arguments
                clazz.addMethod("new");
              }
            });
          } else {
            //checkParameters(element, constructor, expression.getExpressionList(), context, resolver);
          }
        }
      }
      return typeHolder.duplicate();
    }
    return createUnknown(expression);
  }


  static ResultHolder handleExtractedValue(@NotNull HaxeEnumExtractedValueReference extractedValueRef, @NotNull HaxeGenericResolver resolver) {
    HaxeExtractorMatchExpression matchExpression = PsiTreeUtil.getParentOfType(extractedValueRef, HaxeExtractorMatchExpression.class, true, HaxeEnumArgumentExtractor.class);
    if (matchExpression != null) {
      HaxeSwitchCaseExpr match = matchExpression.getMatch();
      if (PsiTreeUtil.isAncestor(match, extractedValueRef, false)) {
        return evaluate(matchExpression.getExtractorExpression(), resolver).result;
      }
    }

    HaxeEnumArgumentExtractor extractor = PsiTreeUtil.getParentOfType(extractedValueRef, HaxeEnumArgumentExtractor.class, true, HaxeEnumArgumentExtractor.class);
    if (extractor != null) {
      HaxeEnumExtractorModel extractorModel = (HaxeEnumExtractorModel)extractor.getModel();
      return extractorModel.resolveExtractedValueType(extractedValueRef);
    }

    PsiElement resolve = extractedValueRef.resolve();
    if(resolve != null) {

      ResultHolder result = evaluate(resolve).result;

        if (extractedValueRef.getParent() instanceof HaxeEnumExtractedValue extractedValue) {
          //TODO: mlo - We should probably try to  redo how switch extractors are parsed in the BNF
          // to get more consistency between ExprArray and ArrayLiteral (we might not need both)
          if (extractedValue.getParent() instanceof HaxeEnumExtractArrayLiteral
                  || extractedValue.getParent() instanceof HaxeSwitchCaseExprArray) {

            Stack<Object> objectPath = new Stack<>();

            HaxeSwitchStatement switchStatement = PsiTreeUtil.getParentOfType(extractedValue, HaxeSwitchStatement.class);
            buildExtractVarPath(extractedValue, true, objectPath, switchStatement);

            Collections.reverse(objectPath);
            // workaround for switch on literal array  (ex. switch (["mixed Types", 1, false]{...}))
            // as we are not going to look for generics in these  situations to determine type.
            boolean isLiteralInSwitchExpression = isLiteralInSwitchExpression(resolve, extractedValueRef);
            int offset = isLiteralInSwitchExpression ? 1 : 0;

            ResultHolder typePointer = result;
              for (int i = offset; i < objectPath.size(); i++) {
                  Object o = objectPath.get(i);
                  if (typePointer == null || typePointer.isUnknown()) break;

                  if (o instanceof Integer index) {
                      typePointer = typeFromArray(typePointer, index);
                    //TODO do we need support for objects here ?
//                  } else if (o instanceof String name) {
                    // typePointer = typeFromObjectName(typePointer,name);
                  }
              }
            return typePointer != null ? typePointer : createUnknown(extractedValueRef);
          }
        }
      return result;
    }
    return createUnknown(extractedValueRef);
  }

  private static boolean isLiteralInSwitchExpression(PsiElement resolve, @NotNull HaxeEnumExtractedValueReference extractedValueRef) {
    HaxeLiteralExpression isLiteral = PsiTreeUtil.getParentOfType(resolve, HaxeLiteralExpression.class, false, HaxeSwitchStatement.class);
    HaxeArrayLiteral isInsideArrayLiteral = PsiTreeUtil.getParentOfType(resolve, HaxeArrayLiteral.class, false, HaxeSwitchStatement.class);
    HaxeSwitchStatement resolvedSwitchParent = PsiTreeUtil.getParentOfType(resolve, HaxeSwitchStatement.class);
    if(resolvedSwitchParent == null) return false;
    boolean isInSameSwitch = PsiTreeUtil.isAncestor(resolvedSwitchParent.getSwitchBlock(), extractedValueRef, false);
    return isInSameSwitch && (isLiteral != null || isInsideArrayLiteral != null);
  }

  private static ResultHolder typeFromArray(ResultHolder typePointer, Integer index) {

      SpecificHaxeClassReference classType = typePointer.getClassType();
      if(classType != null){
        if(classType.fullyResolveTypeDefAndUnwrapNullTypeReference() instanceof SpecificHaxeClassReference classReference) {
          if(classReference.isArray()) {
            @NotNull ResultHolder[] specifics = classReference.getSpecifics();
            if(specifics.length == 1) return specifics[0];
//          }else {
            // TODO considder checking array access in other types for expected return value
            // - verify if its supported first
          }
        }
      }
      return null;
    }


  static ResultHolder handleForStatement(
    HaxeExpressionEvaluatorContext context,
    HaxeGenericResolver resolver,
    HaxeForStatement forStatement) {
    final HaxeExpression forStatementExpression = forStatement.getExpression();
    final HaxeKeyValueIterator keyValueIterator = forStatement.getKeyValueIterator();
    final HaxeValueIterator valueIterator = forStatement.getValueIterator();
    final HaxeIterable iterable = forStatement.getIterable();
    final PsiElement body = forStatement.getLastChild();
    context.beginScope();

    if(context.getScope().deepSearchForReturnValues) handle(body, context, resolver);

    try {
      final SpecificTypeReference iterableValue = handle(iterable, context, resolver).getType();
      ResultHolder iteratorResult = iterableValue.getIterableElementType(resolver);
      SpecificTypeReference type = iteratorResult != null ? iteratorResult.getType() : null;

      if (type != null) {
        if (forStatementExpression != null) {
          ResultHolder handle = handle(forStatementExpression, context, resolver);
          // check if Array comprehensions and use assign hint if possible
          if (forStatement.getParent() instanceof HaxeExpressionList expressionList) {
            if (expressionList.getParent() instanceof HaxeArrayLiteral) {
              ResultHolder assignHint = resolver.getAssignHint();
              if (assignHint != null && assignHint.getClassType() != null) {
                SpecificHaxeClassReference classType = assignHint.getClassType();
                if (classType.isArray()) {
                  @NotNull ResultHolder[] specifics = classType.getSpecifics();
                  ResultHolder specific = specifics[0];
                  if (specific.canAssign(handle)) return specific;
                }
              }
            }
          }
          return handle.getType().createHolder();
        }
      }
      if ( type != null) {
        if (iterableValue.isConstant()) {
          if (iterableValue.getConstant() instanceof HaxeRange constant) {
            type = type.withRangeConstraint(constant);
          }
        }
        if (valueIterator != null) {
          HaxeComponentName name = valueIterator.getComponentName();
            context.setLocal(name, new ResultHolder(type));
          } else if (keyValueIterator != null) {
            context.setLocal(keyValueIterator.getIteratorkey().getComponentName(), new ResultHolder(type));
            context.setLocal(keyValueIterator.getIteratorValue().getComponentName(), new ResultHolder(type));
          }
        return handle(body, context, resolver);
      }
      //attempt to get type from body (could be function call etc.)
      if (body != null) return handle(body, context, resolver);
    }
    finally {
      context.endScope();
    }
    return createUnknown(forStatement);
  }

  static ResultHolder createUnknown(PsiElement element) {
    return createUnknown(element, true);
  }
  static ResultHolder createUnknown(PsiElement element, boolean cacheable) {
      ResultHolder holder = getUnknown(element).createHolder();
      holder.cacheable = cacheable;
      return holder;
  }

  static ResultHolder handlePrefixExpression(
    HaxeExpressionEvaluatorContext context,
    HaxeGenericResolver resolver,
    HaxePrefixExpression prefixExpression) {

    HaxeExpression expression = prefixExpression.getExpression();
    ResultHolder typeHolder = handle(expression, context, resolver);
    SpecificTypeReference type = typeHolder.getType();
    if(type instanceof SpecificHaxeClassReference classReference && classReference.isAbstractType()) {
      return handlePrefix(classReference, prefixExpression, resolver);
    }
    return createUnknown(prefixExpression);
  }
  static ResultHolder handlePostfixExpression(
    HaxeExpressionEvaluatorContext context,
    HaxeGenericResolver resolver,
    HaxePostfixExpression postfixExpression) {

    HaxeExpression expression = postfixExpression.getExpression();
    ResultHolder typeHolder = handle(expression, context, resolver);
    SpecificTypeReference type = typeHolder.getType();
    if(type instanceof SpecificHaxeClassReference classReference && classReference.isAbstractType()) {
      return handlePostfix(classReference, postfixExpression, resolver);
    }
    return createUnknown(postfixExpression);

  }

  private static ResultHolder handlePostfix(SpecificHaxeClassReference classReference, HaxePostfixExpression postfixExpression, HaxeGenericResolver resolver) {
    // core types (int float etc) and String(extern) does not contain definitions for operators (no need to try to find overload methods)
    if(classReference.isCoreType() || classReference.isString()) return classReference.createHolder();

    List<HaxeMethodModel> operatorOverloads = classReference.getOperatorOverloads(postfixExpression.getAssignableOperator());
      if (!operatorOverloads.isEmpty()) {
          return operatorOverloads.getFirst().getReturnType(resolver);
      }
      return createUnknown(postfixExpression);
  }

  private static ResultHolder handlePrefix(SpecificHaxeClassReference classReference, HaxeUnaryExpression unaryExpression, HaxeGenericResolver resolver) {
    // core types (int float etc) and String(extern) does not contain definitions for operators (no need to try to find overload methods)
    if (classReference.isCoreType() || classReference.isString()) return classReference.createHolder();
    HaxeOperator operator = unaryExpression.getOperator();
    List<HaxeMethodModel> operatorOverloads = classReference.getOperatorOverloads(operator);
    if (!operatorOverloads.isEmpty()) {
      return operatorOverloads.getFirst().getReturnType(resolver);
    }

    return createUnknown(unaryExpression);
  }

  static ResultHolder handleIfStatement(
    HaxeExpressionEvaluatorContext context,
    HaxeGenericResolver resolver,
    HaxeIfStatement ifStatement) {
    SpecificTypeReference guardExpr = handle(ifStatement.getGuard(), context, resolver).getType();
    HaxeGuardedStatement guardedStatement = ifStatement.getGuardedStatement();
    HaxeElseStatement elseStatement = ifStatement.getElseStatement();

    PsiElement eTrue = UsefulPsiTreeUtil.getFirstChildSkipWhiteSpacesAndComments(guardedStatement);
    PsiElement eFalse = UsefulPsiTreeUtil.getFirstChildSkipWhiteSpacesAndComments(elseStatement);

    SpecificTypeReference tTrue = null;
    SpecificTypeReference tFalse = null;
    if (eTrue != null) tTrue = handle(eTrue, context, resolver).getType();
    if (eFalse != null) tFalse = handle(eFalse, context, resolver).getType();
    if (guardExpr.isConstant()) {
      if (guardExpr.getConstantAsBool()) {
        if (tFalse != null) {
          context.addUnreachable(eFalse);
        }
      } else {
        if (tTrue != null) {
          context.addUnreachable(eTrue);
        }
      }
    }
    // we need to ignore missing false/elseStatement type when doing comprehension
    // unificationRules  should be IGNORE_VOID when in comprehension, hopefully this wont create any other side effects
    if(context.getScope().unificationRules != UnificationRules.IGNORE_VOID) {
      // No 'else' clause means the if results in a Void type.
      if (null == tFalse) tFalse = SpecificHaxeClassReference.getVoid(ifStatement);
    }
    // TODO create rule use first on unknown
    UnificationRules rules = context.getScope().unificationRules;
    SpecificTypeReference suggested = assignHintAsSuggestedType(resolver, rules);
    return HaxeTypeUnifier.unify(tTrue, tFalse, ifStatement, suggested, rules).createHolder();
  }

  static ResultHolder handleFunctionLiteral(
    HaxeExpressionEvaluatorContext context,
    HaxeGenericResolver resolver,
    HaxeFunctionLiteral function) {
    //HaxeParameterList params = function.getParameterList(); // TODO mlo: get expected type to use if signature/parameters are without types
    //if (params == null) {
    //  return SpecificHaxeClassReference.getInvalid(function).createHolder();
    //}
    LinkedList<HaxeArgument> arguments = new LinkedList<>();
    ResultHolder returnType = null;
    HaxeScope scope = context.beginScope();
    scope.unificationRules = UnificationRules.PREFER_VOID;
    try {
      HaxeOpenParameterList openParamList = function.getOpenParameterList();
      HaxeParameterList parameterList = function.getParameterList();
      if (openParamList != null) {
        // Arrow function with a single, unparenthesized, parameter.

        ResultHolder argumentType = handle(openParamList.getUntypedParameter(), new HaxeExpressionEvaluatorContext(openParamList), null);
        HaxeUntypedParameter untypedParameter = openParamList.getUntypedParameter();
        HaxeComponentName componentName = untypedParameter.getComponentName();
        String argumentName = componentName.getName();
        // if defined in a call expression (as an argument) we can match definitions with corresponding method  parameter
        if(function.getParent() instanceof  HaxeCallExpressionList callExpressionList
           && callExpressionList.getParent() instanceof  HaxeCallExpression callExpression
          && callExpression.getExpression() instanceof HaxeReferenceExpression referenceExpression) {

          PsiElement resolve = referenceExpression.resolve();
          if(resolve instanceof  HaxeMethod method) {
            int index = callExpressionList.getExpressionList().indexOf(function);
            HaxeCallExpressionEvaluation validation = HaxeCallExpressionEvaluatorCacheService.cachedHaxeCallExpressionEvaluation(method, callExpression);
            if (validation != null) {
              Map<Integer, Integer> indexMap = validation.getArgumentToParameterIndex();
              int parameterIndex = indexMap.getOrDefault(index, -1);
              ResultHolder holder = validation.getParameterType(parameterIndex);
              if (holder != null && !holder.isUnknown()) {
                SpecificTypeReference reference;
                if (holder.getClassType() != null) {
                  reference = holder.getClassType().fullyResolveTypeDefAndUnwrapNullTypeReference();
                } else {
                  reference = holder.getFunctionType();
                }
                if (reference instanceof SpecificFunctionReference functionReference) {
                  // if type found in param, override  argumentType (default is unknown)
                  if (argumentType.isUnknown() && !functionReference.getArguments().isEmpty()) {
                    HaxeArgument argument = functionReference.getArguments().get(0);
                    argumentType = argument.getType();
                  }
                }
              }
            }
          }

        }
        context.setLocal(componentName, argumentType);
        // TODO check if rest param?
        arguments.add(new HaxeArgument(untypedParameter, 0, false, false, argumentType, argumentName));
      } else if (parameterList != null) {
        List<HaxeParameter> list = parameterList.getParameterList();
        for (int i = 0; i < list.size(); i++) {
          HaxeParameter parameter = list.get(i);
          //ResultHolder argumentType = HaxeTypeResolver.getTypeFromTypeTag(parameter.getTypeTag(), function);
          ResultHolder argumentType = handleWithRecursionGuard(parameter, context, resolver);
          if (argumentType == null) argumentType = SpecificTypeReference.getUnknown(parameter).createHolder();
          context.setLocal(parameter, argumentType);
          // TODO check if rest param?
          boolean optional = parameter.getOptionalMark() != null || parameter.getVarInit() != null;
          arguments.add(new HaxeArgument(parameter, i, optional, false, argumentType, parameter.getName()));
        } // TODO: Add Void if list.size() == 0
      }
      context.addLambda(context.createChild(function.getLastChild()));
      HaxeTypeTag tag = (function.getTypeTag());
      if (null != tag) {
        returnType = HaxeTypeResolver.getTypeFromTypeTag(tag, function);
      } else {
        // If there was no type tag on the function, then we try to infer the value:
        // If there is a block to this method, then return the type of the block.
        //  - in order to avoid unnecessary overhead evaluating the entire block we search for
        //    return statements that belong to the method and evaluate those
        //  - if no return statement evaluate the last element  (lambda expressions)
        // If there is not a block, but there is an expression, then return the type of that expression.
        // If there is not a block, but there is a statement, then return the type of that statement.
        HaxeBlockStatement block = function.getBlockStatement();
        if (null != block) {
          // make sure we do not carry assign hint into a new block of code
          HaxeGenericResolver blockResolver = resolver.withoutAssignHint();
          List<HaxeReturnStatement> returnStatementList =
            CachedValuesManager.getCachedValue(block,  () -> HaxeTypeResolver.findReturnStatementsForMethod(block));
          List<ResultHolder> returnTypes = returnStatementList.stream().map(statement -> HaxeTypeResolver.getPsiElementType(statement, blockResolver)).toList();
          if (!returnTypes.isEmpty())  {
            // Project the function-type hint's return component down so sibling subclasses returned
            // from different branches unify to the declared return type instead of their parent class.
            SpecificTypeReference suggestedReturn = functionReturnHintFor(resolver);
            returnType = HaxeTypeUnifier.unifyHolders(returnTypes, block, suggestedReturn, UnificationRules.PREFER_VOID);
          } else {
            // TODO cache last element
            boolean filtered = false;
            HaxePsiCompositeElement lastExpression = getLastExpressionCached(block);
            if (lastExpression != null) {
              // TODO try to eliminate any expressions that are not valid  value expressions
              if (lastExpression instanceof HaxeIfStatement ifStatement) {
                if (ifStatement.getElseStatement() == null) {
                  // ignore if statements if there's no else as it cant be a value expression
                  filtered = true;
                  returnType = SpecificFunctionReference.getVoid(block).createHolder();
                }
              }
              if (!filtered) returnType = HaxeTypeResolver.getPsiElementType(lastExpression, blockResolver);
            }else {
              returnType = SpecificFunctionReference.getVoid(block).createHolder();
            }
          }

        } else if (null != function.getExpression()) {
          returnType = handle(function.getExpression(), context, resolver);
        } else {
          // Only one of these can be non-null at a time.
          PsiElement possibleStatements[] = {function.getDoWhileStatement(), function.getForStatement(), function.getIfStatement(),
            function.getReturnStatement(), function.getThrowStatement(), function.getWhileStatement()};
          for (PsiElement statement : possibleStatements) {
            if (null != statement) {
              returnType = handle(statement, context, resolver);
              break;
            }
          }
        }
      }
    }
    finally {
      context.endScope();
    }
    return new SpecificFunctionReference(arguments, returnType, null, function, function).createHolder();
  }

  static HaxePsiCompositeElement getLastExpressionCached(HaxeBlockStatement block) {
   return  CachedValuesManager.getCachedValue(block, () -> {
     HaxePsiCompositeElement element = null;
     for (PsiElement child : block.getChildren()) {
       if (child instanceof  HaxePsiCompositeElement nextElement) element = nextElement;
     }
     return new CachedValueProvider.Result<>(element, block);
    });
  }
  static ResultHolder handleArrayAccessExpression(
    HaxeExpressionEvaluatorContext context,
    HaxeGenericResolver resolver,
    HaxeArrayAccessExpression arrayAccessExpression) {
    final List<HaxeExpression> list = arrayAccessExpression.getExpressionList();
    if (list.size() >= 2) {
      SpecificTypeReference left = handle(list.get(0), context, resolver).getType();
      SpecificTypeReference right = handle(list.get(1), context, resolver).getType();
      // if left is typeParameter try to use typeParameter constraints and see if it have array accessor
      if(left.isTypeParameter()) {
        ResultHolder resolve = resolver.resolve(left.createHolder());
        if(resolve != null && !resolve.isUnknown())left = resolve.getType();
      }
      // make sure we fully resolve and unwrap any nulls and typedefs before searching for accessors or checking if array class
      if(left instanceof SpecificHaxeClassReference classReference) {
        left = classReference.fullyResolveTypeDefAndUnwrapNullTypeReference();
      }

      if (left.isArray()) {
        Object constant = null;
        if (left.isConstant()) {
          if (left.getConstant() instanceof List array) {
            //List array = (List)left.getConstant();
            // TODO got class cast exception here due to constant being "HaxeAbstractClassDeclarationImpl
            //  possible expression causing issue: ("this[x + 1]  in  abstractType(Array<Float>)" ?)

            final HaxeRange constraint = right.getRangeConstraint();
            HaxeRange arrayBounds = new HaxeRange(0, array.size());
            if (right.isConstant()) {
              final int index = HaxeTypeLiteralsUtils.getIntValue(right.getConstant());
              if (arrayBounds.contains(index)) {
                constant = array.get(index);
              }
              else {
                context.addWarning(arrayAccessExpression, "Out of bounds " + index + " not inside " + arrayBounds);
              }
            }
            else if (constraint != null) {
              if (!arrayBounds.contains(constraint)) {
                context.addWarning(arrayAccessExpression, "Out of bounds " + constraint + " not inside " + arrayBounds);
              }
            }
          }
        }
        ResultHolder arrayType = left.getArrayElementType().getType().withConstantValue(constant).createHolder();
        ResultHolder resolved = resolver.resolve(arrayType);
        return resolved == null || resolved.isUnknown() ? arrayType : resolved;
      }
      //if not native array, look up ArrayAccessGetter method and use result
      if(left instanceof SpecificHaxeClassReference classReference) {
        // Pick the @:op([]) getter overload that fits the index argument and infer the
        // getter's own type parameters from it (e.g. getTyped<T>(Id<T>):T  with  a[Id<Foo>]  ->  Foo).
        ResultHolder getterReturnType = resolveArrayAccessGetterReturnType(classReference, right.createHolder());
        if (getterReturnType != null) {
          return getterReturnType;
        }
        // no getter declared on the class itself: the legacy lookup also covers inherited __get
        // getters and the extern ArrayAccess marker interface
        ResultHolder legacyType = getArrayAccessTypeFromClass(classReference);
        if (legacyType != null) {
          return legacyType;
        }
      }
    }
    return createUnknown(arrayAccessExpression);
  }

  /**
   * Resolves the return type of an abstract / class array-access getter (@:op([]) or old-style __get).
   *
   * <p>An abstract may declare several overloaded array-access getters, for example a typed one and a
   * plain String one:
   * <pre>
   *   @:op([]) function getTyped&lt;T&gt;(id:Id&lt;T&gt;):T;
   *   @:op([]) function get(id:String):Value;
   * </pre>
   * Given the type of the actual index argument this picks the matching overload (preferring a direct
   * match over one that only works through an abstract implicit cast) and infers the getter's own type
   * parameters from the argument, so {@code container[Id<Foo>]} resolves to {@code Foo} rather than to
   * an unresolved {@code T} or to the wrong overload.
   *
   * <p>When {@code indexType} is null (callers that have no concrete index expression) this falls back
   * to the first declared getter and its declared return type, matching the previous behaviour.
   *
   * @return the getter's return type, or null if the class declares no array-access getter method.
   */
  @Nullable
  private static ResultHolder resolveArrayAccessGetterReturnType(@NotNull SpecificHaxeClassReference classReference,
                                                                 @Nullable ResultHolder indexType) {
    HaxeClass haxeClass = classReference.getHaxeClass();
    if (haxeClass == null) return null;

    HaxeGenericResolver classResolver = classReference.getGenericResolver();

    List<HaxeMethodModel> getters = new ArrayList<>();
    List<HaxeMethodModel> legacyGetters = new ArrayList<>();
    for (HaxeMethod method : haxeClass.getHaxeMethodsSelf(classResolver)) {
      HaxeMethodModel model = method.getModel();
      if (model == null || model.getParameterCount() != 1) continue;
      if (model.isArrayAccessor()) {
        getters.add(model);
      }
      else if ("__get".equals(model.getName())) {
        legacyGetters.add(model);
      }
    }
    // old-style __get getters count only when no @:arrayAccess / @:op([]) getter is declared
    if (getters.isEmpty()) getters = legacyGetters;
    if (getters.isEmpty()) return null;

    HaxeMethodModel chosen = getters.getFirst();
    HaxeCallExpressionEvaluation chosenEvaluation = null;
    if (indexType != null && !indexType.isUnknown()) {
      int bestScore = Integer.MIN_VALUE;
      for (HaxeMethodModel getter : getters) {
        HaxeCallExpressionEvaluation evaluation =
          HaxeCallExpressionUtil.createContextForMethodCall(List.of(indexType.getType()), getter, classResolver).evaluate();
        // an invalid evaluation ranks below every real score (0..2), so one is kept only as a last resort
        int score = evaluation.isValid() ? scoreArrayAccessGetterMatch(getter, indexType, classResolver) : -1;
        if (score > bestScore) {
          bestScore = score;
          chosen = getter;
          chosenEvaluation = evaluation;
        }
      }
    }

    if (chosenEvaluation != null) {
      ResultHolder returnType = chosenEvaluation.getReturnType();
      if (returnType != null && !returnType.isUnknown()) {
        return returnType;
      }
    }

    // fall back to the declared return type resolved with the class + method type parameters
    return resolveDeclaredReturnType(chosen, classResolver);
  }

  /**
   * Scores how well an array-access getter parameter matches the index argument, so that a direct match
   * (same underlying type, or a type parameter) is preferred over one that only works via an abstract
   * implicit cast. Higher is better.
   */
  private static int scoreArrayAccessGetterMatch(@NotNull HaxeMethodModel getter,
                                                 @NotNull ResultHolder indexType,
                                                 @Nullable HaxeGenericResolver classResolver) {
    List<HaxeParameterModel> parameters = getter.getParameters();
    if (parameters.isEmpty()) return 0;
    ResultHolder paramType = parameters.getFirst().getType(classResolver);
    SpecificTypeReference paramRef = paramType == null ? null : paramType.getType();
    SpecificTypeReference argRef = indexType.getType();
    if (paramRef == null) return 0;
    // a type parameter accepts the argument directly (its own constraints are checked elsewhere)
    if (paramRef.isTypeParameter()) return 1;
    HaxeClass paramClass = paramRef instanceof SpecificHaxeClassReference p ? p.getHaxeClass() : null;
    HaxeClass argClass = argRef instanceof SpecificHaxeClassReference a ? a.getHaxeClass() : null;
    // same underlying type means no implicit cast was needed (e.g. Id<T> vs Id<Foo>)
    if (paramClass != null && paramClass == argClass) return 2;
    // matched only through an abstract to/from conversion
    return 0;
  }

  /**
   * Declared return type of an array-access getter, resolved with the class type parameters plus the
   * getter's own constraints. Mutates the passed resolver.
   */
  private static ResultHolder resolveDeclaredReturnType(@NotNull HaxeMethodModel method, @NotNull HaxeGenericResolver classResolver) {
    classResolver.addAll(method.getGenericResolver(classResolver));// apply constraints from methodSignature (if any)
    return method.getReturnType(classResolver);
  }

  public static ResultHolder getArrayAccessTypeFromClass(SpecificHaxeClassReference classReference) {
    SpecificTypeReference reference = classReference.fullyResolveTypeDefAndUnwrapNullTypeReference();
    if (reference instanceof SpecificHaxeClassReference fullyResolved){
      classReference = fullyResolved;
    }
    if(classReference.isArray()) {
      // hack (Array does not have any "get" method for array access)
      @NotNull ResultHolder[] specifics = classReference.getSpecifics();
      if(specifics.length == 1) {
        return specifics[0];
      }
    }
    HaxeClass haxeClass = classReference.getHaxeClass();
    if (haxeClass != null) {
      HaxeNamedComponent getter = haxeClass.findArrayAccessGetter(classReference.getGenericResolver());
      if (getter instanceof HaxeMethodDeclaration methodDeclaration) {
        return resolveDeclaredReturnType(methodDeclaration.getModel(), classReference.getGenericResolver());
      }
      // TODO make better solution
      // hack to work around external ArrayAccess interface, interface that has no methods but tells compiler that implementing class has array access
      else if (getter instanceof HaxeExternInterfaceDeclaration interfaceDeclaration) {
        HaxeGenericResolver classResolver = classReference.getGenericResolver();
        HaxeGenericResolver interfaceResolver = classResolver.translateFromTo(classReference.getHaxeClass(), interfaceDeclaration);
        ResultHolder interfaceType = interfaceResolver.resolve(interfaceDeclaration.getModel().getInstanceType());
        if(interfaceType != null) {
          @NotNull ResultHolder[] specifics = interfaceType.getClassType().getSpecifics();
          if (specifics.length == 1) return specifics[0];
        }
      }
    }
    return null;
  }

  static ResultHolder handleIteratorExpression(
    HaxeExpressionEvaluatorContext context,
    HaxeGenericResolver resolver,
    HaxeIteratorExpression iteratorExpression) {
    final List<HaxeExpression> list = iteratorExpression.getExpressionList();
    if (list.size() >= 2) {
      final SpecificTypeReference left = handle(list.get(0), context, resolver).getType();
      final SpecificTypeReference right = handle(list.get(1), context, resolver).getType();
      Object constant = null;
      if (left.isConstant() && right.isConstant()) {
        constant = new HaxeRange(
          HaxeTypeLiteralsUtils.getIntValue(left.getConstant()),
          HaxeTypeLiteralsUtils.getIntValue(right.getConstant())
        );
      }
      return SpecificHaxeClassReference.getIntIterator(iteratorExpression).withConstantValue(constant)
        .createHolder();
    }
    return createUnknown(iteratorExpression);
  }

  static ResultHolder handleSuperExpression(HaxeExpressionEvaluatorContext context, HaxeGenericResolver resolver,
                                                    HaxeSuperExpression superExpression) {

    final HaxeMethodModel method = HaxeJavaUtil.cast(HaxeBaseMemberModel.fromPsi(superExpression), HaxeMethodModel.class);
    if (superExpression.getParent() instanceof HaxeCallExpression) {
      final HaxeMethodModel parentMethod = (method != null) ? method.getParentMethod(resolver) : null;
      if (parentMethod != null) {
        return parentMethod.getFunctionType(resolver).createHolder();
      }
      context.addError(superExpression, "Calling super without parent constructor");
    } else {
      HaxeClass parentOfType = PsiTreeUtil.getStubOrPsiParentOfType(superExpression, HaxeClass.class);
      if (parentOfType != null){
        HaxeClassModel model = parentOfType.getModel();
        // abstracts do not support the super keyword
        if(!model.isAbstractType()) {
          List<HaxeClassReferenceModel> extendingTypes = model.getExtendingTypes();
          if(!extendingTypes.isEmpty()) {
            HaxeClassModel haxeClassModel = extendingTypes.getFirst().getHaxeClassModel();
            if(haxeClassModel != null) {
              return haxeClassModel.getInstanceReference().createHolder();
            }
          }
        }
      }
      // called outside class ?
    }
    return createUnknown(superExpression);
  }

  static ResultHolder handleValueExpression(HaxeExpressionEvaluatorContext context,
                                                    HaxeGenericResolver resolver,
                                                    HaxeValueExpression valueExpression) {
    if (valueExpression.getSwitchStatement() != null){
      return handle(valueExpression.getSwitchStatement(), context, resolver);
    }
    if (valueExpression.getIfStatement() != null){
      return handle(valueExpression.getIfStatement(), context, resolver);
    }
    if (valueExpression.getTryStatement() != null){
      return handle(valueExpression.getTryStatement(), context, resolver);
    }
    if (valueExpression.getVarInit() != null){
      return handle(valueExpression.getVarInit(), context, resolver);
    }
    if (valueExpression.getExpression() != null){
      return handle(valueExpression.getExpression(), context, resolver);
    }
    if (valueExpression.getMacroTypeReification() != null){
       return HaxeMacroTypeUtil.getComplexType(valueExpression.getMacroTypeReification()).createHolder();
    }

    if(valueExpression.getMacroExpressionReification() != null) {
      HaxeMacroExpressionReification reification = valueExpression.getMacroExpressionReification();

      HaxeMacroValueReification valueReification = reification.getMacroValueReification();
      if (valueReification != null) {
        return handle(valueReification, context, resolver);
      }
      HaxeMacroExpReification expReification = reification.getMacroExpReification();
      if(expReification != null) {
        return SpecificHaxeClassReference.getDynamic(valueExpression).createHolder();
      }

      HaxeMacroArrayReification arrayReification = reification.getMacroArrayReification();
      if (arrayReification != null) {
        return handle(arrayReification.getExpression(), context, resolver);
      }
    }
    return createUnknown(valueExpression);
  }

  static ResultHolder handlePrimitives(PsiElement element, HaxePsiToken psiToken) {
    IElementType type = psiToken.getTokenType();

    String text = element.getText().replaceAll("_","");// removing separators for numeric values (ex "1_000" -> "1000")
    if (type == HaxeTokenTypes.LITINT
        || type == HaxeTokenTypes.LITHEX
        || type == HaxeTokenTypes.LITOCT
    ) {
      return SpecificHaxeClassReference.primitive("Int", element, Long.decode(text)).createHolder();
    } else if (type == HaxeTokenTypes.LITBIN) {
      long value = Long.parseLong(text.substring(2), 2);
      return SpecificHaxeClassReference.primitive("Int", element, value).createHolder();
    } else if (type == HaxeTokenTypes.LITFLOAT) {
      Float value = Float.valueOf(text);
      return SpecificHaxeClassReference.primitive("Float", element, Double.parseDouble(text))
        .withConstantValue(value)
        .createHolder();
    } else if (type == HaxeTokenTypes.KFALSE || type == HaxeTokenTypes.KTRUE) {
      Boolean value = type == HaxeTokenTypes.KTRUE;
      return SpecificHaxeClassReference.primitive("Bool", element, type == HaxeTokenTypes.KTRUE)
        .withConstantValue(value)
        .createHolder();
    } else if (type == HaxeTokenTypes.KNULL) {
      return SpecificHaxeClassReference.primitive("Dynamic", element, HaxeNull.instance).createHolder();
    } else {
      if(log.isDebugEnabled())log.debug("Unhandled token type: " + type);
      return SpecificHaxeClassReference.getDynamic(element).createHolder();
    }
  }

  static ResultHolder handleArrayLiteral(
    HaxeExpressionEvaluatorContext context,
    HaxeGenericResolver resolver,
    HaxeArrayLiteral arrayLiteral) {

    HaxeExpressionList list = arrayLiteral.getExpressionList();

    // Check if it's a comprehension.
    if (list != null) {
      final List<HaxeExpression> expressionList = list.getExpressionList();
      if (expressionList.isEmpty()) {
        final PsiElement child = list.getFirstChild();
        if ((child instanceof HaxeForStatement) || (child instanceof HaxeWhileStatement)) {
          HaxeScope scope = context.beginScope();
          scope.unificationRules = UnificationRules.IGNORE_VOID;
          ResultHolder handle = handle(child, context, resolver);
          context.endScope();
          return SpecificTypeReference.createArray(handle, arrayLiteral).createHolder();
        }
      }
    }

    ArrayList<SpecificTypeReference> references = new ArrayList<SpecificTypeReference>();
    ArrayList<Object> constants = new ArrayList<Object>();
    boolean allConstants = true;

    if (list != null) {
      for (HaxeExpression expression : list.getExpressionList()) {
        // dropping AssignHint as we are in an array so field type will include the array part.
        SpecificTypeReference type = handle(expression, context, resolver.withoutAssignHint()).getType();
        if (!type.isConstant()) {
          allConstants = false;
        } else {
          constants.add(type.getConstant());
        }
        // Convert enum Value types to Enum class  (you cant have an Array of EnumValue types)
        if (type instanceof  SpecificEnumValueReference enumValueReference) {
          type = enumValueReference.getEnumClass();
        }
        // if value is null we skip it and hope the rest contains the type or that the type can be resolved from usage later
        if(type.isDynamic() && type.getConstant() instanceof HaxeNull) continue;;
        references.add(type);
      }
    }
    // an attempt at suggesting what to unify  types into (useful for when typeTag is an anonymous structure as those would never be used in normal unify)
    SpecificTypeReference suggestedType = null;
    ResultHolder  typeTagType = findExpectedTypeForUnify(arrayLiteral);
    if (typeTagType!= null) {
      // make sure we do not pass Null<> as expected type
      if(typeTagType.getType().isNullType()) {
        typeTagType = typeTagType.tryUnwrapNullType();
      }
      // we expect Array<T> or collection type with type parameter (might not work properly if type is implicit cast)
      if (typeTagType.getClassType() != null) {
        @NotNull ResultHolder[] specifics = typeTagType.getClassType().getSpecifics();
        if (specifics.length == 1) {
          suggestedType = specifics[0].getType();
        }else if (specifics.length == 2) {
          ResultHolder unknown = createUnknown(arrayLiteral);
          ResultHolder holder = createMap(unknown, unknown, arrayLiteral).createHolder();
          if(typeTagType.canAssign(holder)) {
            return  typeTagType;
          }
        }
      }
    }
    // empty expression with type tag (var x:Array<T> = []), no need to look for usage, use typetag
    if (references.isEmpty() && suggestedType != null && !suggestedType.isUnknown()) {
      return typeTagType;
    } else {
      ResultHolder elementTypeHolder = references.isEmpty()
                                       ? SpecificTypeReference.getUnknown(arrayLiteral).createHolder()
                                       : HaxeTypeUnifier.unify(references, arrayLiteral, suggestedType, UnificationRules.IGNORE_VOID).withoutConstantValue().createHolder();

      SpecificTypeReference result = SpecificHaxeClassReference.createArray(elementTypeHolder, arrayLiteral);
      if (allConstants) result = result.withConstantValue(constants);
      ResultHolder holder = result.createHolder();

      // try to resolve typeParameter when we got empty literal array with declaration without typeTag
      if (elementTypeHolder.isUnknown()) {
        // note to avoid recursive loop we only  do this check if its part of a varInit and not part of any expression,
        // it would not make sense trying to look it up in a callExpression etc because then its type should be defined in the parameter list.
        if (arrayLiteral.getParent() instanceof HaxeVarInit) {
          HaxePsiField declaringField =
            UsefulPsiTreeUtil.findParentOfTypeButStopIfTypeIs(arrayLiteral, HaxePsiField.class, HaxeCallExpression.class);
          if (declaringField != null) {
            HaxeComponentName componentName = declaringField.getComponentName();
            if(componentName != null) {
              ResultHolder searchResult = searchReferencesForTypeParameters(componentName, context, resolver, holder);
              if (!searchResult.isUnknown()) holder = searchResult;
            }
          }
        }
      }
      return holder;
    }
  }

  static ResultHolder handleMapLiteral(
    HaxeExpressionEvaluatorContext context,
    HaxeGenericResolver resolver,
    HaxeMapLiteral mapLiteral) {
    Collection<HaxeMapInitializerExpression> initializers = PsiTreeUtil.findChildrenOfType(mapLiteral, HaxeMapInitializerExpression.class);

    var enumValuePreferredKey = false;
    var enumValuePreferredValue = false;

    ResultHolder assignHint = resolver.getAssignHint();
    SpecificTypeReference suggestedKeyType = null;
    SpecificTypeReference suggestedValueType = null;
    if (assignHint != null) {
      SpecificHaxeClassReference hintClassType = assignHint.getClassType();
      if (hintClassType != null) {
        ResultHolder unknown = createUnknown(mapLiteral);
        SpecificHaxeClassReference map = hintClassType.tryCastTo(createMap(unknown, unknown, mapLiteral));
        if (map != null) {
          @NotNull ResultHolder[] specifics = hintClassType.getSpecifics();
          if (specifics.length == 2) {
              if (specifics[0].getType().isEnumValueClass()) enumValuePreferredKey = true;
              if (specifics[1].getType().isEnumValueClass()) enumValuePreferredValue = true;
              // Mirrors handleArrayLiteral: forwarding the projected K/V as suggestedType keeps
              // sibling classes that share an interface from collapsing to their parent class.
              if (!specifics[0].isUnknown()) suggestedKeyType = specifics[0].getType();
              if (!specifics[1].isUnknown()) suggestedValueType = specifics[1].getType();
          }
        }
      }
    }



    ArrayList<SpecificTypeReference> keyReferences = new ArrayList<>(initializers.size());
    ArrayList<SpecificTypeReference> valueReferences = new ArrayList<>(initializers.size());
    HaxeGenericResolver resolverWithoutHint = resolver.withoutAssignHint();
    for (HaxeMapInitializerExpression initializerExpression : initializers) {

      SpecificTypeReference keyType = handle(initializerExpression.getLeftHand(), context, resolverWithoutHint).getType();
      if (keyType instanceof SpecificEnumValueReference enumValueReference) {
        keyType = enumValuePreferredKey
                ?  SpecificHaxeClassReference.getEnumValue(enumValueReference.context)
                : enumValueReference.getEnumClass();
      }
      keyReferences.add(keyType);
      SpecificTypeReference valueType = handle(initializerExpression.getRightHand(), context, resolverWithoutHint).getType();
      if (valueType instanceof SpecificEnumValueReference enumValueReference) {
        valueType = enumValuePreferredValue
                ?  SpecificHaxeClassReference.getEnumValue(enumValueReference.context)
                : enumValueReference.getEnumClass();
      }

      valueReferences.add(valueType);
    }

    // XXX: Maybe track and add constants to the type references, like arrays do??
    //      That has implications on how they're displayed (e.g. not as key=>value,
    //      but as separate arrays).
    ResultHolder keyTypeHolder = HaxeTypeUnifier.unify(keyReferences, mapLiteral, suggestedKeyType, UnificationRules.IGNORE_VOID).withoutConstantValue().createHolder();
    ResultHolder valueTypeHolder = HaxeTypeUnifier.unify(valueReferences, mapLiteral, suggestedValueType, UnificationRules.IGNORE_VOID).withoutConstantValue().createHolder();

    SpecificHaxeClassReference result = SpecificHaxeClassReference.createMap(keyTypeHolder, valueTypeHolder, mapLiteral);
    if (mapLiteral.getParent() instanceof HaxeVarInit ) {
      if(assignHint != null && assignHint.isClassType()) {
        // try to use assignHint to figure out expected map type (literal maps should work with any class with interface IMap )
        SpecificHaxeClassReference hintClassType = assignHint.getClassType();
          if (hintClassType != null) {
            SpecificHaxeClassReference hintAsSameType = hintClassType.tryCastToClass(result);
            if (hintAsSameType != null) {
              if (hintClassType.canAssign(result)) {
                return hintAsSameType.createHolder().noCache();
              }
            }
          }
      }
    }
    return result.createHolder();
  }

  @NotNull
  static ResultHolder handleExpressionList(
    HaxeExpressionEvaluatorContext context,
    HaxeGenericResolver resolver,
    HaxeExpressionList expressionList) {
    ArrayList<ResultHolder> references = new ArrayList<ResultHolder>();
    for (HaxeExpression expression : expressionList.getExpressionList()) {
      references.add(handle(expression, context, resolver));
    }
    return HaxeTypeUnifier.unifyHolders(references, expressionList, UnificationRules.DEFAULT);
  }

  static ResultHolder handleCallExpression(
    HaxeExpressionEvaluatorContext context,
    HaxeGenericResolver resolver,
    HaxeCallExpression callExpression) {

      boolean allowCaching = true;

    HaxeExpression callExpressionRef = callExpression.getExpression();
    // generateResolverFromScopeParents -  making sure we got typeParameters from arguments/parameters
    // The outer resolver's assign hint is forwarded here so the type-parameter pre-pinning loop
    // can prefer the declared assignment target when unifying sibling subclass arguments.
    HaxeGenericResolver localResolver = HaxeGenericResolverUtil.generateResolverFromScopeParents(callExpression, resolver.getAssignHint());
    localResolver.addAll(resolver);
    if(resolver.getAssignHint() != null) {
      localResolver.setAssignHint(resolver.getAssignHint());
    }

    SpecificTypeReference functionType;
    if (callExpressionRef != null) {   // can be null if the entire expression is a macro  of callExpression
      // map type Parameters to methods declaring class resolver if necessary


      HaxeMethodModel methodModel = tryGetMethodModel(callExpression);
      if(methodModel != null) {
        if(isBindCall(callExpression)) {
          return tryHandleFunctionBind(methodModel.getFunctionType(resolver), callExpression);
        }

        ResultHolder assignHint = resolver.getAssignHint();
        SpecificTypeReference assignHintType = assignHint == null ? null : assignHint.getType();
        HaxeCallExpressionContextContainer contextContainer = HaxeCallExpressionUtil.createContextForMethodCall(callExpression, assignHintType, methodModel.getMethod());
        if(!contextContainer.canCache()) {
            allowCaching = false;
        }
        HaxeCallExpressionEvaluation evaluate = contextContainer.evaluateContexts();
        if(evaluate != null && evaluate.isValid()) {
          functionType = evaluate.getFunctionType(methodModel);
        }else {
          functionType = createUnknown(callExpression, false).getType();
        }
      }else {
        SpecificTypeReference callieRef = tryGetCallieType(callExpression);
        if(callieRef instanceof SpecificHaxeClassReference  callieClassReference) {
          if(callieClassReference.isNullType() || callieClassReference.isTypeDef()) {
            callieRef = callieClassReference.fullyResolveTypeDefAndUnwrapNullTypeReference();
          }
        }
        if (callieRef instanceof SpecificHaxeClassReference classReference &&  !classReference.isUnknown()) {
          HaxeGenericResolver callieResolver = classReference.getGenericResolver();
          HaxeClass callieType = classReference.getHaxeClass();
          HaxeClass methodTypeClassType = tryGetMethodDeclaringClass(callExpression);
          if (callieType != null && methodTypeClassType != null) {

            localResolver.addAll(callieResolver);
            localResolver = localResolver.translateFromTo(callieType, methodTypeClassType);
          }
        }else if (callieRef instanceof SpecificFunctionReference functionReference) {
          if(isBindCall(callExpression)) {
            return tryHandleFunctionBind(functionReference, callExpression);
          }
        }
        functionType = handle(callExpressionRef, context, localResolver).getType();
        if(functionType instanceof SpecificHaxeClassReference  functionTypeRef) {
          if(functionTypeRef.isNullType() || functionTypeRef.isTypeDef()) {
            functionType = functionTypeRef.fullyResolveTypeDefAndUnwrapNullTypeReference();
          }
          if(functionType instanceof SpecificHaxeClassReference  classReference) {
            HaxeClassModel haxeClassModel = classReference.getHaxeClassModel();
            if (haxeClassModel instanceof HaxeAbstractClassModel abstractClassModel) {
              if(abstractClassModel.isCallable()) {
                functionType = abstractClassModel.getUnderlyingType(resolver);
              }
            }
          }
        }
      }
        boolean varIsMacroFunction = isCallExpressionToMacroMethod(callExpressionRef);
        boolean callIsFromMacroContext = isInMacroFunction(callExpressionRef);
        if (varIsMacroFunction && !callIsFromMacroContext) {
          ResultHolder holder = resolveMacroTypesForFunction(functionType.createHolder());
          functionType = holder.getFunctionType();
        }
    }else  if (callExpression.getMacroExpressionReification() != null) {
      functionType = SpecificTypeReference.getUnknown(callExpression.getMacroExpressionReification());
    }else {
      // should not happen
      functionType = SpecificTypeReference.getUnknown(callExpression);
    }

    // @TODO: this should be innecessary when code is working right!
    if ( functionType == null  || functionType.isUnknown()) {
      if (callExpressionRef instanceof HaxeReference) {
        PsiReference reference = callExpressionRef.getReference();
        if (reference != null) {
          PsiElement subelement = reference.resolve();
          if (subelement instanceof HaxeMethod haxeMethod) {
            functionType = haxeMethod.getModel().getFunctionType(resolver);
          }
        }
      }
    }

    if (functionType == null || functionType.isUnknown()) {
      if(log.isDebugEnabled()) log.debug("Couldn't resolve " + callExpressionRef);
    }

    List<HaxeExpression> parameterExpressions = null;
    if (callExpression.getExpressionList() != null) {
      parameterExpressions = callExpression.getExpressionList().getExpressionList();
    } else {
      parameterExpressions = Collections.emptyList();
    }

    if (functionType instanceof  SpecificHaxeClassReference classReference && classReference.isTypeDef() ) {
      functionType = classReference.fullyResolveTypeDefReference();
    }
    if (functionType instanceof SpecificEnumValueReference enumValueConstructor) {
      // TODO, this probably should not be handled here, but its detected as a call expression


      SpecificHaxeClassReference enumClass = enumValueConstructor.enumClass;
      HaxeGenericResolver enumResolver = enumClass.getGenericResolver();
      SpecificFunctionReference constructor = enumValueConstructor.getConstructor();

      List<ResultHolder> list = parameterExpressions.stream()
        .map(expression -> HaxeExpressionEvaluator.evaluate(expression, new HaxeExpressionEvaluatorContext(expression), enumResolver).result)
        .toList();


      ResultHolder holder = enumClass.createHolder();
      SpecificHaxeClassReference type = holder.getClassType();
      @NotNull ResultHolder[] specifics = type.getSpecifics();
      // convert any parameter that matches argument of type TypeParameter into specifics for enum type
      HaxeGenericParam param = enumClass.getHaxeClass().getGenericParam();
      List<HaxeGenericParamModel> params = enumClass.getHaxeClassModel().getGenericParams();

      Map<HaxeTypeParameterDeclaration, List<ResultHolder>> genericsMap = new HashMap<>();
      params.forEach(g -> genericsMap.put(g.getTypeParameter(), new ArrayList<>()));

      for (HaxeGenericParamModel model : params) {
        HaxeTypeParameterDeclaration typeParameter = model.getTypeParameter();

        int parameterIndex = 0;
        List<HaxeArgument> arguments = constructor.getArguments();
        for (int argumentIndex = 0; argumentIndex < arguments.size(); argumentIndex++) {
          HaxeArgument argument = arguments.get(argumentIndex);
          if (parameterIndex < list.size()) {
            ResultHolder parameter = list.get(parameterIndex++);
            if (argument.getType().canAssign(parameter)) {
              if (argument.getType().getType() instanceof SpecificHaxeClassReference classReference ){
                if (classReference.isTypeParameter() && typeParameter == classReference.getHaxeClass()) {
                  genericsMap.get(typeParameter).add(parameter);
                } else {
                  if (argument.getType().isClassType()) {
                    SpecificHaxeClassReference classType = parameter.getClassType();
                    HaxeGenericResolver parameterResolver = classType != null ? classType.getGenericResolver() : new HaxeGenericResolver();
                    ResultHolder test = parameterResolver.resolveTypeParameter(typeParameter);
                    if (test != null && !test.isUnknown()) {
                      genericsMap.get(typeParameter).add(parameter);
                    }
                  }
                }
              }
            }
          }
        }
      }
      // unify all usage of generics
      for (int i = 0; i < params.size(); i++) {
        HaxeGenericParamModel paramModel = params.get(i);
        HaxeTypeParameterDeclaration typeParameter = paramModel.getTypeParameter();
        List<ResultHolder> holders = genericsMap.get(typeParameter);
        ResultHolder unified = HaxeTypeUnifier.unifyHolders(holders, callExpression, UnificationRules.DEFAULT);
        enumResolver.add(paramModel.getTypeParameter(), unified);
        specifics[i] = unified;
      }
      return holder;

    }
    if (functionType instanceof SpecificFunctionReference ftype) {

      ResultHolder returnType = ftype.getReturnType();
      boolean nullWrapped = returnType.isNullWrappedType();

      HaxeGenericResolver functionResolver = new HaxeGenericResolver();
      functionResolver.addAll(resolver.withoutArgumentType());

      // if reference to "real" method, try to use any argument to type parameter mapping
      if (ftype.method != null && returnType.isOrContainsTypeParameters()) {
        HaxeCallExpressionEvaluation validation = HaxeCallExpressionEvaluatorCacheService.cachedHaxeCallExpressionEvaluation(ftype.method.getMethod(), callExpression);
        if(validation != null) {
        functionResolver.addAll(validation.getCallExpressionResolver());
        }
      }

      //ResultHolder resolved = functionResolver.resolveReturnType(returnType.tryUnwrapNullType());
      ResultHolder resolved = functionResolver.resolve(returnType.tryUnwrapNullType());
      if (!nullOrUnknown(resolved)) {
        if(nullWrapped) resolved = resolved.wrapInNullType(returnType.getContext());
        returnType = resolved;
      }
      if(returnType.isUnknown() || returnType.isDynamic() || returnType.isVoid()) {
        return returnType.duplicate();
      }

      if(returnType.getFunctionType() != null){
          ResultHolder holder = returnType.getFunctionType().createHolder();
          holder.cacheable = allowCaching;
          return holder;
      }

      if(returnType.isClassType() || returnType.isEnumValueType()) {
          ResultHolder result = returnType.copy();
          result.cacheable = allowCaching;
          return result;
      }
    }

    if (functionType!= null && functionType.isDynamic()) {
        ResultHolder holder = functionType.withoutConstantValue().createHolder();
        holder.cacheable = allowCaching;
        return holder;
    }

    // @TODO: resolve the function type return type
    return createUnknown(callExpression, false);
  }

  private static ResultHolder tryHandleFunctionBind(SpecificFunctionReference functionReference, HaxeCallExpression callExpression) {
    List<HaxeArgument> arguments = functionReference.getArguments();
    List<HaxeArgument> argumentsToKeep = new ArrayList<>();
    List<SpecificTypeReference>  tmParamList = new ArrayList<>();
    boolean canHaveTypeParameters = functionReference.method != null;

    HaxeCallExpressionList expressionList = callExpression.getExpressionList();
    if (expressionList == null) {
      // no binds, drop all optionals
      for (HaxeArgument argument : arguments) {
        if (!argument.isOptional()) {
          argumentsToKeep.add(argument);
        }
      }
      return functionReference.performMethodBind(argumentsToKeep, null).createHolder();
    } else {
      List<HaxeExpression> expressions = expressionList.getExpressionList();

      for (int i = 0; i < arguments.size(); i++) {
        HaxeExpression expr = expressions.size() > i ? expressions.get(i) : null;
        HaxeArgument argument = arguments.get(i);
        boolean optional = argument.isOptional();

        // The underscore _ can be skipped for trailing arguments
        // By default, trailing optional arguments are bound to their default values and do not become arguments of the result function
        if (expr == null) {
          if (!optional) {
            argumentsToKeep.add(argument);
            if (canHaveTypeParameters)   tmParamList.add(argument.getType().getType());
          }
        } else if (expr.textMatches("_")) {
          argumentsToKeep.add(argument);
          if (canHaveTypeParameters)   tmParamList.add(argument.getType().getType());
        }else {
          if (canHaveTypeParameters) {
            ResultHolder expressionType = evaluateWithRecursionGuard(expr).result;
            tmParamList.add(expressionType.getType());
          }
        }
      }
      if (canHaveTypeParameters) {
        HaxeCallExpressionContext context = HaxeCallExpressionUtil.createContextForMethodCall(tmParamList, functionReference.method, null);
        HaxeCallExpressionEvaluation evaluate = context.evaluate();
        HaxeGenericResolver callExpressionResolver = evaluate.getCallExpressionResolver();
        return functionReference.performMethodBind(argumentsToKeep, callExpressionResolver).createHolder();
      }
      return functionReference.performMethodBind(argumentsToKeep, null).createHolder();
    }
  }

  @Nullable
  private static HaxeMethodModel tryGetMethodModel(HaxeCallExpression expression) {
    if (expression.getExpression() instanceof HaxeReference reference) {
      final PsiElement resolved = reference.resolve();
      if (resolved instanceof HaxeMethod method) {
        return method.getModel();
      }
    }
    return null;
  }

  @Null
  private static HaxeClass tryGetMethodDeclaringClass(HaxeCallExpression expression) {
    HaxeMethodModel model = tryGetMethodModel(expression);
    if (model != null) {
      HaxeClassModel classModel = model.getDeclaringClass();
      if (classModel != null) return classModel.haxeClass;
    }
    return null;
  }

  private static boolean isInMacroFunction(HaxeExpression ref) {
    HaxeMethodDeclaration type = PsiTreeUtil.getParentOfType(ref, HaxeMethodDeclaration.class);
    if (type != null && type.getModel() != null) {
      return type.getModel().isMacro();
    }
    return false;
  }

  private static boolean isCallExpressionToMacroMethod(HaxeExpression callExpressionRef) {
    if (callExpressionRef instanceof HaxeReference) {
      PsiReference reference = callExpressionRef.getReference();
      if (reference != null) {
        PsiElement subelement = reference.resolve();
        if (subelement instanceof HaxeMethod haxeMethod) {
          return haxeMethod.getModel().isMacro();
        }
      }
    }
    return false;
  }


  @Nullable
  static ResultHolder handleVarInit(
    HaxeExpressionEvaluatorContext context,
    HaxeGenericResolver resolver,
    HaxeVarInit varInit) {
    final HaxeExpression expression = varInit.getExpression();
    if (expression == null) {
      return SpecificTypeReference.getInvalid(varInit).createHolder();
    }
    return handleWithRecursionGuard(expression, context, resolver);

  }

  @Nullable
  static ResultHolder handleLocalVarDeclaration(
    HaxeExpressionEvaluatorContext context,
    HaxeGenericResolver resolver,
    HaxeLocalVarDeclaration varDeclaration) {
    final HaxeComponentName name = varDeclaration.getComponentName();
    final HaxeVarInit init = varDeclaration.getVarInit();
    final HaxeTypeTag typeTag = varDeclaration.getTypeTag();

    var immutable = false;
    if(varDeclaration.getModel() instanceof HaxeLocalVarModel model) {
      immutable = model.isFinal() && init != null;
    }
    ResultHolder result = null;
    HaxeGenericResolver localResolver = new HaxeGenericResolver();
    localResolver.addAll(resolver);

    if(init != null) {
      // find any type parameters used in init expression as the return type might be of that type
      HaxeGenericResolver initResolver = HaxeGenericResolverUtil.generateResolverFromScopeParents(init.getExpression());
      localResolver.addAll(initResolver.withoutUnknowns());
    }

    if (typeTag != null) {
      result = HaxeTypeResolver.getTypeFromTypeTag(typeTag, varDeclaration);
      ResultHolder resolve = resolver.resolve(result);
      if (!nullOrUnknown(resolve)) result = resolve;
    }

    if (result == null && init != null) {
      result = _handle(init, context, localResolver);
      // if result is null here, we have most likely hit a recursion guard (we should at least expect an "unknown" type returned)
      // continuing after this point will probably cause incorrect results("find from usage" etc. can end up with completely different types)
      if(result == null) return null;
    }

    // search for usage to determine type
    HaxeComponentName element = varDeclaration.getComponentName();
    final ResultHolder hint = result;

    if (result == null || isDynamicBecauseOfNullValueInit(result)) {
      result = tryToFindTypeFromUsage(element, result, hint, context, resolver, null);
    }

    if (isUnknownLiteralArray(result) && result.containsUnknownOrUnresolvedTypeParameters()) {
      result = searchReferencesForTypeParameters(name, context, resolver, result);
    }
    if (result != null && result.containsUnknownOrUnresolvedTypeParameters()) {
      result = searchReferencesForTypeParameters(name, context, resolver, result);
    }

    result = tryGetEnumValuesDeclaringClass(result);
    context.setLocal(name, result);
    // disable/enable mutation (disable if final with init expression)
      if (result != null) result.setImmutable(immutable);
    return result;
  }


  private static @Nullable ResultHolder tryGetEnumValuesDeclaringClass(ResultHolder result) {
    if (result != null && result.isEnumValueType()) {
      //TODO check if typeParams need to be copied over
      result = result.getEnumValueType().getEnumClass().createHolder();
    }
    return result;
  }

  private static boolean isUnknownLiteralArray(ResultHolder result) {
    if (result == null) return false;
    SpecificHaxeClassReference classType = result.getClassType();
    if (classType != null && result.getClassType().isArray()) {
      @NotNull ResultHolder[] specifics = classType.getSpecifics();
      if (specifics.length == 1) {
        ResultHolder specific = specifics[0];
        if (specific.isUnknown() || isUnknownLiteralArray(specific)) return true;
      }
      if (classType.context instanceof HaxeArrayLiteral arrayLiteral) {
        return arrayLiteral.getExpressionList() == null;
      }
    }
    return false;
  }

  public static boolean isDynamicBecauseOfNullValueInit(ResultHolder result) {
    if (result == null) return false;
    return  result.getType().isDynamic() && result.getType().getConstant() instanceof HaxeNull;
  }

  static ResultHolder handleAssignExpression(HaxeExpressionEvaluatorContext context, HaxeGenericResolver resolver, PsiElement element) {
    final PsiElement left = element.getFirstChild();
    final PsiElement right = element.getLastChild();
    if (left != null && right != null) {
      final ResultHolder leftResult = handle(left, context, resolver);
      final ResultHolder rightResult = handle(right, context, resolver);

      if (leftResult.isUnknown()) {
        leftResult.setType(rightResult.getType());
        context.setLocalWhereDefined(left, leftResult);
      }
      leftResult.removeConstant();

      final SpecificTypeReference leftValue = leftResult.getType();
      final SpecificTypeReference rightValue = rightResult.getType();

      //leftValue.mutateConstantValue(null);

      // skipping `canAssign` check if we dont have a holder to add annotations to
      // this is probably just waste of time when resolving in files we dont have open.
      // TODO try to  see if we need this or can move it so its not executed unnessesary
      if (context.holder != null) {
        if (!leftResult.canAssign(rightResult)) {

          List<HaxeExpressionConversionFixer> fixers = HaxeExpressionConversionFixer.createStdTypeFixers(right, rightValue, leftValue);
          AnnotationBuilder builder = HaxeStandardAnnotation
            .typeMismatch(context.holder, right, rightValue.toStringWithoutConstant(), leftValue.toStringWithoutConstant())
            .withFix(new HaxeCastFixer(right, rightValue, leftValue));

          fixers.forEach(builder::withFix);
          builder.create();
        }
      }

      if (leftResult.isImmutable()) {
        context.addError(element, HaxeBundle.message("haxe.semantic.trying.to.change.an.immutable.value"));
      }

      return rightResult;
    }
    return createUnknown(element);
  }

  static ResultHolder handleLocalVarDeclarationList(
    HaxeExpressionEvaluatorContext context,
    HaxeGenericResolver resolver,
    HaxeLocalVarDeclarationList varDeclarationList) {
    // Var declaration list is a statement that returns a Void type, not the type of the local vars it creates.
    // We still evaluate its sub-parts so that we can set the known value types of variables in the scope.
    for (HaxeLocalVarDeclaration part : varDeclarationList.getLocalVarDeclarationList()) {
      handle(part, context, resolver);
    }
    return SpecificHaxeClassReference.getVoid(varDeclarationList).createHolder();
  }

  static ResultHolder handleWhileStatement(
    HaxeExpressionEvaluatorContext context,
    HaxeGenericResolver resolver,
    HaxeWhileStatement whileStatement) {
    HaxeDoWhileBody whileBody = whileStatement.getBody();
    if (whileBody != null) {
      @NotNull PsiElement[] children = whileBody.getChildren();
      PsiElement lastChild = children[children.length - 1];
      return handle(lastChild, context, resolver);
    }
    return createUnknown(whileStatement);
  }

  static ResultHolder handleUnsafeCastExpression(HaxeUnsafeCastExpression castExpression) {
      return createUnknown(castExpression);
  }
  static ResultHolder handleSafeCastExpression(HaxeSafeCastExpression castExpression) {
    HaxeTypeOrAnonymous anonymous = castExpression.getTypeOrAnonymous();
    if (anonymous != null) {
      return HaxeTypeResolver.getTypeFromTypeOrAnonymous(anonymous);
    } else {
      return createUnknown(castExpression);
    }
  }
//  static ResultHolder handleCastExpression(HaxeCastExpression castExpression) {
//    HaxeTypeOrAnonymous anonymous = castExpression.getTypeOrAnonymous();
//    if (anonymous != null) {
//      return HaxeTypeResolver.getTypeFromTypeOrAnonymous(anonymous);
//    } else {
//      return createUnknown(castExpression);
//    }
//  }

  @NotNull
  static ResultHolder handleRestParameter(HaxeRestParameter restParameter) {
    HaxeTypeTag tag = restParameter.getTypeTag();
    ResultHolder type = HaxeTypeResolver.getTypeFromTypeTag(tag, restParameter);
    return new ResultHolder(SpecificTypeReference.getStdClass(ARRAY, restParameter, new ResultHolder[]{type}));
  }

  static ResultHolder handleIdentifier(HaxeExpressionEvaluatorContext context, HaxeIdentifier identifier) {
    // If it has already been seen, then use whatever type is already known.
    ResultHolder holder = context.get(identifier);
    if (holder == null) {
      // context.addError(element, "Unknown variable", new HaxeCreateLocalVariableFixer(element.getText(), element));

      return SpecificTypeReference.getUnknown(identifier).createHolder();
    }

    return holder;
  }

  static ResultHolder handleThisExpression(HaxeGenericResolver resolver, HaxeThisExpression thisExpression) {
    HaxeClass ancestor = UsefulPsiTreeUtil.getAncestor(thisExpression, HaxeClass.class);
    if (ancestor == null) return SpecificTypeReference.getDynamic(thisExpression).createHolder();
    HaxeClassModel model = ancestor.getModel();
    if (model.isAbstractType()) {
      SpecificTypeReference reference = model.getUnderlyingType(resolver);
      if (null != reference) {
        return reference.createHolder();
      }
    }
    ResultHolder[] specifics =  HaxeTypeResolver.resolveDeclarationParametersToTypes(model.haxeClass, resolver);
    return SpecificHaxeClassReference.withGenerics(new HaxeClassReference(model, thisExpression), specifics).createHolder();
  }

  static ResultHolder handleAbstractExpression(HaxeGenericResolver resolver, HaxeAbstractExpression abstractExpression) {
    HaxeClass ancestor = UsefulPsiTreeUtil.getAncestor(abstractExpression, HaxeClass.class);
    if (ancestor == null) return SpecificTypeReference.getDynamic(abstractExpression).createHolder();
    HaxeClassModel model = ancestor.getModel();
    ResultHolder[] specifics =  HaxeTypeResolver.resolveDeclarationParametersToTypes(model.haxeClass, resolver);
    return SpecificHaxeClassReference.withGenerics(new HaxeClassReference(model, abstractExpression), specifics).createHolder();
  }

  @NotNull
  static ResultHolder handleSwitchCaseBlock(
    HaxeExpressionEvaluatorContext context,
    HaxeGenericResolver resolver,
    HaxeSwitchCaseBlock caseBlock) {
    List<HaxeReturnStatement> returnStatements = caseBlock.getReturnStatementList();
    for (HaxeReturnStatement  statement : returnStatements) {
      ResultHolder returnType = handle(statement, context, resolver);
      context.addReturnType(returnType, statement);
    }
    List<HaxeExpression> expressions = caseBlock.getExpressionList();
    if (!expressions.isEmpty()) {
      HaxeExpression lastExpression = expressions.get(expressions.size() - 1);
      return handle(lastExpression, context, resolver);
    }
    // if block only has one expression (for some reason getExpressionList returns empty list)
    if(returnStatements.isEmpty() && expressions.isEmpty()) {
      @NotNull PsiElement[] children = caseBlock.getChildren();
      if(children.length == 1) {
        return handle(children[0], context, resolver);
      }
    }
    return new ResultHolder(SpecificHaxeClassReference.getVoid(caseBlock));
  }

  @NotNull
  static ResultHolder handleSwitchStatement(
    HaxeExpressionEvaluatorContext context,
    HaxeGenericResolver resolver,
    HaxeSwitchStatement switchStatement) {
    // TODO: Evaluating result of switch statement should properly implemented
    List<SpecificTypeReference> typeList = new LinkedList<>();
    SpecificTypeReference bestGuess = null;

    if(switchStatement.getSwitchBlock() != null) {
      List<HaxeSwitchCase> caseList = switchStatement.getSwitchBlock().getSwitchCaseList();

      for (HaxeSwitchCase switchCase : caseList) {
        HaxeSwitchCaseBlock block = switchCase.getSwitchCaseBlock();
        if (block != null) {
          ResultHolder handle = handle(block, context, resolver);
          if (!handle.isUnknown())typeList.add(handle.getType());
        }
      }

      for (SpecificTypeReference typeReference : typeList) {
        if (typeReference.isVoid()) continue;
        if (bestGuess == null) {
          ResultHolder assignHint = resolver.getAssignHint();
          if(assignHint != null &&  assignHint.canAssign(typeReference.createHolder())) {
            bestGuess = assignHint.getType();
          }else {
            bestGuess = typeReference;
          }
          continue;
        }
        bestGuess = HaxeTypeUnifier.unify(bestGuess, typeReference, switchStatement);
      }
    }

    if (bestGuess != null) {
      return new ResultHolder(bestGuess);
    }else {
      return new ResultHolder(SpecificHaxeClassReference.getUnknown(switchStatement));
    }
  }

  static ResultHolder handleCodeBlock(HaxeExpressionEvaluatorContext context, HaxeGenericResolver resolver, PsiElement element) {
    context.beginScope();
    context.getScope().deepSearchForReturnValues = true;

    ResultHolder type = createUnknown(element);
    boolean deadCode = false;
    for (PsiElement childElement : element.getChildren()) {
      // not sure why comments are available here but to avoid overhead we filter them out
      if (ONLY_COMMENTS.contains( childElement.getNode().getElementType())) continue;
      type = handle(childElement, context, resolver);
      if (deadCode) {
        //context.addWarning(childElement, "Unreachable statement");
        context.addUnreachable(childElement);
      }
      if (childElement instanceof HaxeReturnStatement) {
        deadCode = true;
      }
    }
    context.endScope();
    return type;
  }

  static ResultHolder handleIterable(
    HaxeExpressionEvaluatorContext context,
    HaxeGenericResolver resolver,
    HaxeIterable iterable) {
    ResultHolder iteratorParent = handle(iterable.getExpression(), context, resolver);
    SpecificTypeReference type = iteratorParent.getType();
    if (!type.isNumeric()) {
      if (iteratorParent.isClassType()) {
        SpecificHaxeClassReference haxeClassReference = iteratorParent.getClassType();
        HaxeGenericResolver localResolver =  new HaxeGenericResolver();
        localResolver.addAll(resolver);
        localResolver.addAll(haxeClassReference.getGenericResolver());// replace parent/old resolver values with newer from class reference
        if (haxeClassReference != null && haxeClassReference.getHaxeClassModel() != null) {
          HaxeForStatement parentForLoop = PsiTreeUtil.getParentOfType(iterable, HaxeForStatement.class);

          if(haxeClassReference.isTypeDefOfClass()) {
            SpecificTypeReference typeReference = haxeClassReference.fullyResolveTypeDefReference();
            if (typeReference instanceof  SpecificHaxeClassReference classReference) {
              HaxeGenericResolver typeDefResolved = classReference.getGenericResolver();
              localResolver.addAll(typeDefResolved);
            }
          }

            if (parentForLoop.getKeyValueIterator() != null) {
              ResultHolder iteratorType = searchForIteratorType(haxeClassReference, "keyValueIterator",  parentForLoop);
              if (iteratorType != null) return  iteratorType;
            } else {
              ResultHolder iteratorType = searchForIteratorType(haxeClassReference, "iterator",  parentForLoop);
              if (iteratorType != null) return  iteratorType;


              // if we can not find anny iterator methods, then we check for ArrayAccess<T> interface
              // it looks like types implementing this interface also gets some kind of iterator support
              // probably the ArrayIterator

              //TODO mlo: cache these and make string constants
              HaxeClass arrayAccess = HaxeResolveUtil.findClassByQName("ArrayAccess", iterable);
              HaxeClass arrayIterator = HaxeResolveUtil.findClassByQName("haxe.iterators.ArrayIterator", iterable);

              boolean hasArrayAccess = haxeClassReference.getHaxeClassModel().getImplementingInterfaces().stream()
                      .anyMatch(i -> i.getSpecificHaxeClassReference().getHaxeClass() == arrayAccess);

              if (!hasArrayAccess && haxeClassReference.isAbstractType()) {
                SpecificTypeReference underlyingType = haxeClassReference.getHaxeClassModel().getUnderlyingType();
                if (underlyingType instanceof SpecificHaxeClassReference underlyingClassReference) {
                  SpecificTypeReference resolvedUnderlyingClass = localResolver.resolve(underlyingClassReference);
                  if (resolvedUnderlyingClass instanceof SpecificHaxeClassReference fullyResolvedClass) {
                    hasArrayAccess = fullyResolvedClass.getHaxeClassModel().getImplementingInterfaces().stream()
                            .anyMatch(i -> i.getSpecificHaxeClassReference().getHaxeClass() == arrayAccess);

                    if (!hasArrayAccess) {
                      // mlo: this feels a bit wrong, but to get Vector class iteration to work we need to check  underlying types for both ArrayAccess and iterator methods
                      HaxeGenericResolver translated = localResolver.translateFromTo(haxeClassReference.getHaxeClass(), fullyResolvedClass.getHaxeClass());
                      SpecificTypeReference underlyingTypeResolved = translated.resolve(fullyResolvedClass);
                      if (underlyingTypeResolved instanceof SpecificHaxeClassReference underlyingClassResolved) {
                        ResultHolder iteratorTypeFromUnderlying = searchForIteratorType(underlyingClassResolved, "iterator", parentForLoop);
                        if (iteratorTypeFromUnderlying != null) return iteratorTypeFromUnderlying;
                      }
                    }
                  }
                }
              }

              if (hasArrayAccess) {
                HaxeGenericResolver translated = haxeClassReference.getGenericResolver().translateFromTo(haxeClassReference.getHaxeClass(), arrayAccess);
                return SpecificHaxeClassReference.withGenerics(new HaxeClassReference(arrayIterator.getModel(), arrayIterator), translated.getSpecifics()).createHolder();
              }
            }
        }
      }
    }

    return handle(iterable.getExpression(), context, resolver);
  }

  public static @Nullable ResultHolder searchForIteratorType(SpecificHaxeClassReference haxeClassReference, String iteratorName, PsiElement parent) {
    // ignore "Dynamic" as it can assign to anything and will in most cases incorrectly be matched with iterators for other types
    if (haxeClassReference.isDynamic()) return null;
    // do not attempt to find iterator if  type is unknown
    if(haxeClassReference.isUnknown()) return haxeClassReference.createHolder();

    SpecificTypeReference typeReference = haxeClassReference.fullyResolveTypeDefAndUnwrapNullTypeReference();
    if (typeReference instanceof SpecificHaxeClassReference resolvedClassReference) {
      HaxeGenericResolver referenceGenericResolver = resolvedClassReference.getGenericResolver();
      HaxeBaseMemberModel iterator = resolvedClassReference.getHaxeClassModel().getMember(iteratorName, referenceGenericResolver);

      if (iterator == null) {
        // look for extension method iterator
        List<HaxeUsingModel> usingModels = HaxeFileModel.fromElement(parent).getUsingModels();
        for (HaxeUsingModel usingModel : usingModels) {
          HaxeMethodModel extensionMethod = usingModel.findExtensionMethod(iteratorName, resolvedClassReference);
          if (extensionMethod != null){
            iterator = extensionMethod;
            break;
          }
        }
      }


      if (iterator instanceof HaxeMethodModel methodModel) {
        HaxeClassModel declaringClass = methodModel.getDeclaringClass();
        if (declaringClass != null) {
          HaxeGenericResolver translatedResolver = referenceGenericResolver.translateFromTo(resolvedClassReference.getHaxeClass(), declaringClass.haxeClass);
          return methodModel.getReturnType(translatedResolver);
        }
        return methodModel.getReturnType(referenceGenericResolver);
      }
    }
    return null;
  }

  @NotNull
  static ResultHolder handleTryStatement(HaxeExpressionEvaluatorContext context,
                                                 HaxeGenericResolver resolver,
                                                 HaxeTryStatement tryStatement) {
    //  try-catch can be used as a value expression all blocks must be evaluated and unified
    //  we should also iterate trough so we can pick up any return statements
    @NotNull PsiElement[] children = tryStatement.getChildren();
    List<ResultHolder> blockResults = new ArrayList<>();
    for (PsiElement child : children) {
      blockResults.add(handle(child, context, resolver));
    }
    UnificationRules rules = context.getScope().unificationRules;
    SpecificTypeReference suggested = assignHintAsSuggestedType(resolver, rules);
    return HaxeTypeUnifier.unifyHolders(blockResults, tryStatement, suggested, rules);
  }
  @NotNull
  static ResultHolder handleCatchStatement(HaxeExpressionEvaluatorContext context,
                                           HaxeGenericResolver resolver,
                                           HaxeCatchStatement catchStatement) {
    //  try-catch can be used as a value expression all blocks must be evaluated and unified
    //  we should also iterate trough so we can pick up any return statements
    @NotNull PsiElement[] children = catchStatement.getChildren();
    HaxeParameter parameter = catchStatement.getParameter();
    List<ResultHolder> blockResults = new ArrayList<>();
    for (PsiElement child : children) {
      if (child == parameter) continue;
      blockResults.add(handle(child, context, resolver));
    }
    UnificationRules rules = context.getScope().unificationRules;
    SpecificTypeReference suggested = assignHintAsSuggestedType(resolver, rules);
    return HaxeTypeUnifier.unifyHolders(blockResults, catchStatement, suggested, rules);
  }



  static ResultHolder handleReturnStatement(HaxeExpressionEvaluatorContext context,
                                                    HaxeGenericResolver resolver,
                                                    HaxeReturnStatement returnStatement) {

    ResultHolder result = SpecificHaxeClassReference.getVoid(returnStatement).createHolder();
    if (isUntypedReturn(returnStatement)) return result;
    List<PsiElement> children = withoutMetadata(returnStatement.getChildren());
    if (!children.isEmpty()) {
      PsiElement child = children.getFirst();
      result = handle(child, context, resolver);
    }
    context.addReturnType(result, returnStatement);
    return result;
  }

  private static List<PsiElement> withoutMetadata(PsiElement[] children) {
    return Stream.of(children).filter(child-> {
      if (child instanceof LazyParseablePsiElement element) {
        if (element.getElementType() instanceof HaxeEmbeddedElementType) {
          return false;
        }
      }
      return true;
    }).toList();
  }

  static ResultHolder handleImportAlias(HaxeExpressionEvaluatorContext context, HaxeGenericResolver resolver, HaxeImportAlias alias) {
    if (alias.getParent() instanceof HaxeImportStatement importStatement) {
      HaxeReferenceExpression expression = importStatement.getReferenceExpression();
      if (expression != null) {
        ResultHolder evaluationResult = HaxeExpressionEvaluator.evaluate(expression, resolver).result;
        if(evaluationResult != null) {
          // anything else, functions etc
          return evaluationResult;
        }
      }
    }
    return createUnknown(alias);
  }

  static SpecificTypeReference resolveAnyTypeDefsOrTypeParameterConstraint(SpecificTypeReference reference) {
      if (reference instanceof SpecificHaxeClassReference classReference) {
        if (classReference.isTypeDef()) {
          if (classReference.isTypeDefOfFunction()) {
            return classReference.resolveTypeDefFunction();
          } else {
            SpecificTypeReference resolvedClass = classReference.resolveTypeDefOfClassOrTypeParam();
            return resolveAnyTypeDefsOrTypeParameterConstraint(resolvedClass);
          }
        }
        if (classReference.isTypeParameterWithConstraints()) {
          if(classReference.getHaxeClassModel() instanceof HaxeGenericParamModel genericModel) {
            ResultHolder constraint = genericModel.getConstraint(classReference.getGenericResolver());
            if(constraint != null && !constraint.isUnknown())return constraint.getType();
          }
        }
      }
    return reference;
  }

  static void checkParameters(
    final PsiElement callelement,
    final HaxeMethodModel method,
    final List<HaxeExpression> arguments,
    final HaxeExpressionEvaluatorContext context,
    final HaxeGenericResolver resolver
  ) {
    checkParameters(callelement, method.getFunctionType(resolver), arguments, context, resolver);
  }

  static void checkParameters(
    PsiElement callelement,
    SpecificFunctionReference ftype,
    List<HaxeExpression> parameterExpressions,
    HaxeExpressionEvaluatorContext context,
    HaxeGenericResolver resolver
  ) {
    if (!context.isReportingErrors()) return;

    List<HaxeArgument> parameterTypes = ftype.getArguments();

    int parameterTypesSize = parameterTypes.size();
    int parameterExpressionsSize = parameterExpressions.size();
    int len = Math.min(parameterTypesSize, parameterExpressionsSize);

    for (int n = 0; n < len; n++) {
      ResultHolder type = HaxeTypeResolver.resolveParameterizedType(parameterTypes.get(n).getType(), resolver);
      HaxeExpression expression = parameterExpressions.get(n);
      ResultHolder value = handle(expression, context, resolver);

      if (context.holder != null) {
        if (!type.canAssign(value)) {
          context.addError(
            expression,
            "Can't assign " + value + " to " + type,
            new HaxeCastFixer(expression, value.getType(), type.getType())
          );
        }
      }
    }

    //log.debug(ftype.getDebugString());
    // More parameters than expected
    if (parameterExpressionsSize > parameterTypesSize) {
      for (int n = parameterTypesSize; n < parameterExpressionsSize; n++) {
        context.addError(parameterExpressions.get(n), "Unexpected argument");
      }
    }
    // Less parameters than expected
    else if (parameterExpressionsSize < ftype.getNonOptionalArgumentsCount()) {
      context.addError(callelement, "Less arguments than expected");
    }
  }

  static private String getOperator(PsiElement field, TokenSet set) {
    ASTNode operatorNode = field.getNode().findChildByType(set);
    if (operatorNode == null) return "";
    return operatorNode.getText();
  }

  static int getDistance(PsiReference reference, int offset) {
    return reference.getAbsoluteRange().getStartOffset() - offset;
  }

  static boolean isMacroVariable(HaxeIdentifier identifier) {
    if (identifier instanceof  HaxeMacroIdentifier macroIdentifier) return macroIdentifier.getMacroId() != null;
    return false;
  }

  @Nullable
  static ResultHolder findExpectedTypeForUnify(@NotNull PsiElement element) {

    PsiElement parent = element.getParent();
    if (parent instanceof HaxeVarInit varInit) {
      HaxePsiField type = PsiTreeUtil.getParentOfType(varInit, HaxePsiField.class);
      if (type!= null) {
        HaxeTypeTag tag = type.getTypeTag();
       if (tag != null) {
          ResultHolder typeTag = HaxeTypeResolver.getTypeFromTypeTag(tag, element);
          if (!typeTag.isUnknown()) {
            return typeTag;
          }
        }
      }
    }
    if(parent instanceof  HaxeAssignExpression assignExpression) {
      HaxeExpression leftExpression = assignExpression.getLeftExpression();
      if(leftExpression instanceof HaxeReferenceExpression referenceExpression) {
        PsiElement resolve = referenceExpression.resolve();
        if(resolve instanceof  HaxePsiField field) {
          HaxeTypeTag tag = field.getTypeTag();
          if (tag != null) {
            ResultHolder typeTag = HaxeTypeResolver.getTypeFromTypeTag(tag, element);
            if (!typeTag.isUnknown()) {
              return typeTag;
            }
          }
        }
      }
    }
    if (parent instanceof HaxeReturnStatement returnStatement) {
      ResultHolder type = getExpectedTypeForReturn(returnStatement);
      if ( type != null && !type.isUnknown()) {
        return type;
      }
    }

    if (parent instanceof HaxeCallExpressionList callExpressionList
        && callExpressionList.getParent() instanceof HaxeCallExpression callExpression) {

      int index = callExpressionList.getExpressionList().indexOf(element);
      if (callExpression.getExpression() instanceof HaxeReferenceExpression referenceExpression) {
        PsiElement resolve = referenceExpression.resolve();
        if (resolve instanceof HaxeMethod method) {
          HaxeCallExpressionEvaluation evaluate = HaxeCallExpressionEvaluatorCacheService.cachedHaxeCallExpressionEvaluation(method, callExpression);
          if (evaluate != null) {
            ResultHolder parameterType = evaluate.getParameterType(index);
            if (parameterType != null && !parameterType.isUnknown()) {
              SpecificHaxeClassReference classType = parameterType.getClassType();
              if (classType != null) {
                SpecificHaxeClassReference tmpArray = createArray(createUnknown(element), element);
                SpecificHaxeClassReference cast = classType.tryCastToClass(tmpArray);
                if (cast != null && !cast.getSpecifics()[0].isTypeParameter()) return cast.createHolder();
              }
            }
          }
        }
      }
    }
    return null;
  }

  //private static boolean containsTypeParameters(ResultHolder holder) {
  //  if (holder.isUnknown()) return  false;
  //  if (holder.isTypeParameter()) return true;
  //  SpecificTypeReference type = holder.getType();
  //  if (type instanceof  SpecificHaxeClassReference classReference) {
  //    for (ResultHolder specific : classReference.getSpecifics()) {
  //      if (containsTypeParameters(specific)) return  true;
  //    }
  //  }
  //  if (type instanceof SpecificFunctionReference  function) {
  //    if(!function.getTypeParameters().isEmpty()) return true;
  //  }
  //  return false;
  //}

  static boolean isUntypedReturn(HaxeReturnStatement statement) {
    PsiElement child = statement.getFirstChild();
    while(child != null) {
      if (child instanceof HaxePsiToken psiToken) {
        if (psiToken.getTokenType() == KUNTYPED) return true;
      }
      child = child.getNextSibling();
    }
    return false;
  }




}
