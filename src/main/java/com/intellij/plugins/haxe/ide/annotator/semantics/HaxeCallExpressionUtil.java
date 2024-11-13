package com.intellij.plugins.haxe.ide.annotator.semantics;

import com.intellij.openapi.util.TextRange;
import com.intellij.plugins.haxe.HaxeBundle;
import com.intellij.plugins.haxe.lang.psi.*;
import com.intellij.plugins.haxe.lang.psi.impl.HaxeMethodDeclarationImpl;
import com.intellij.plugins.haxe.lang.psi.impl.HaxeTypeParameterDeclaration;
import com.intellij.plugins.haxe.model.*;
import com.intellij.plugins.haxe.model.evaluator.HaxeExpressionEvaluator;
import com.intellij.plugins.haxe.model.evaluator.HaxeExpressionEvaluatorContext;
import com.intellij.plugins.haxe.model.type.*;
import com.intellij.plugins.haxe.model.type.HaxeArgument;
import com.intellij.plugins.haxe.model.type.resolver.ResolverEntry;
import com.intellij.plugins.haxe.util.UsefulPsiTreeUtil;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import lombok.Data;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

import static com.intellij.plugins.haxe.ide.annotator.semantics.TypeParameterUtil.*;
import static com.intellij.plugins.haxe.model.type.HaxeTypeCompatible.canAssignToFrom;

public class HaxeCallExpressionUtil {

  //TODO find a way to combine check method, function, constructor and Enum constructor
  // (amongst the problem is mixed parameter classes and method needing reference for type resolve)
  @NotNull
  public static CallExpressionValidation checkMethodCall(@NotNull HaxeCallExpression callExpression, @NotNull HaxeMethod method) {
    return checkMethodCall(callExpression, method, false);
  }
  public static CallExpressionValidation checkMethodCall(@NotNull HaxeCallExpression callExpression, @NotNull HaxeMethod method, boolean isFirstRef) {
    return checkMethodCall(callExpression, method, null, false);
  }
  public static CallExpressionValidation checkMethodCall(@NotNull HaxeCallExpression callExpression, @NotNull HaxeMethod method, ResultHolder callieType, boolean isFirstRef) {
    CallExpressionValidation validation  = new CallExpressionValidation();
    validation.isMethod = true;

    HaxeMethodModel methodModel = method.getModel();
    if (methodModel.isOverload()) {
      //TODO implement support for overloaded methods (need to get correct model ?)
      return validation; //(stopping here to avoid marking arguments as type mismatch)
    }

    validation.memberMacroFunction = methodModel.isMacro() && !methodModel.isStatic();
    validation.isStaticExtension = callExpression.resolveIsStaticExtension();
    HaxeExpression methodExpression = callExpression.getExpression();


    HaxeCallExpressionList callExpressionList = callExpression.getExpressionList();
    List<HaxeParameterModel> parameterList = methodModel.getParameters();
    if (method instanceof  HaxeMethodDeclarationImpl methodDeclaration) {

      if (HaxeMacroUtil.isMacroMethod(methodDeclaration)) {
        parameterList = parameterList.stream().map(HaxeCallExpressionUtil::resolveMacroTypes).toList();
      }
    }
    List<HaxeExpression> argumentList = Optional.ofNullable(callExpressionList)
      .map(HaxeExpressionList::getExpressionList)
      .orElse(List.of());

    boolean hasVarArgs = hasVarArgs(parameterList);
    boolean hasThisReference = validation.isStaticExtension || validation.memberMacroFunction;
    long minArgRequired = countRequiredArguments(parameterList) - (hasThisReference ? 1 : 0);
    long maxArgAllowed = hasVarArgs ? Long.MAX_VALUE : parameterList.size() - (hasThisReference ? 1 : 0);

    // min arg check

    if (argumentList.size() < minArgRequired) {
      String message = HaxeBundle.message("haxe.semantic.method.parameter.missing", minArgRequired, argumentList.size());
      if (argumentList.isEmpty()) {
        PsiElement first = UsefulPsiTreeUtil.getNextSiblingSkipWhiteSpacesAndComments(methodExpression);
        PsiElement second = UsefulPsiTreeUtil.getNextSiblingSkipWhiteSpacesAndComments(first);
        TextRange range = TextRange.create(first.getTextOffset(), second.getTextOffset() + 1);
        validation.errors.add(new ErrorRecord(range, message));
      }
      else {
        validation.errors.add(new ErrorRecord(callExpressionList.getTextRange(), message));
      }
    }
    //max arg check
    if (argumentList.size() > maxArgAllowed) {
      String message = HaxeBundle.message("haxe.semantic.method.parameter.too.many", maxArgAllowed, argumentList.size());
      if (callExpressionList != null) {
        validation.errors.add(new ErrorRecord(callExpressionList.getTextRange(), message));
      }else {
        validation.errors.add(new ErrorRecord(callExpression.getTextRange(), message));
      }
    }

    // generics and type parameter
    HaxeGenericResolver classTypeResolver =  new HaxeGenericResolver();
     callieType = callieType != null ? callieType :  tryGetCallieType(callExpression, method, validation.isStaticExtension).tryUnwrapNullType();
    // if  this is not a static extension method we can inherit callie's class type parameter
    if(!validation.isStaticExtension) {
      if (callieType.isClassType() && !callieType.isUnknown()) {
        classTypeResolver = callieType.getClassType().getGenericResolver();
        HaxeClassModel methodClassModel = methodModel.getDeclaringClass();
        // map type Parameters to method declaring class resolver if necessary
        if (methodClassModel != null && methodClassModel.haxeClass != null) {
          HaxeClass callieClass = callieType.getClassType().getHaxeClass();
          if(callieClass != null) {
            classTypeResolver = classTypeResolver.translateFromTo(callieClass, methodClassModel.haxeClass);
          }
        }
      }
    }
    validation.setCallie(callieType);

    if(callieType.isUnknown() && !validation.isStaticExtension && !isFirstRef ) {
      //TODO hack?
      // if callie is unknown we prohibit class TypeParameters  unless first reference flag
      // reason: we need to prevent incorrect typeParameters when evaluating monomorphs and recursion guards can cause callie to be null
      validation.unknownCallie = true;
    }
    HaxeGenericResolver resolver = HaxeGenericResolverUtil.appendCallExpressionGenericResolver(callExpression, classTypeResolver);

    int parameterCounter = 0;
    int argumentCounter = 0;

    boolean isRestArg = false;
    HaxeParameterModel parameter = null;
    HaxeExpression argument;

    ResultHolder parameterType = null;
    ResultHolder argumentType = null;

    resolver.addAll(methodModel.getGenericResolver(resolver));

    HaxeGenericResolver argumentResolver = resolver.withoutUnknowns();
    // methods might have typeParameters with same name as a parent so we need to make sure we are not resolving parents type
    // when resolving parameters
    HaxeGenericResolver parameterResolver = resolver.withoutUnknowns();

    HashMap<HaxeTypeParameterDeclaration, ResultHolder> typeParamTable = createTypeParameterConstraintTable(method, resolver, validation.unknownCallie);

    if (validation.isStaticExtension) {
      // this might not work for literals, need to handle those in a different way
      if (methodExpression instanceof HaxeReferenceExpression) {
        HaxeParameterModel model = parameterList.get(parameterCounter++);
        ResultHolder type = model.getType(resolver.withoutUnknowns());
        if (!canAssignToFrom(type, callieType)) {
          // TODO better error message
          validation.errors.add(new ErrorRecord(callExpression.getTextRange(), "Can not use extension method, wrong type"));
          return validation;
        } else {
          SpecificHaxeClassReference paramClass = type.getClassType();
          SpecificHaxeClassReference callieClass = callieType.getClassType();
          HaxeGenericResolver remappedResolver = remapTypeParameters(paramClass, callieClass);
          argumentResolver.addAll(remappedResolver);
          resolver.addAll(remappedResolver);

          applyCallieConstraints(typeParamTable, remappedResolver);

        }
      }
    }else {
      if (callieType.getClassType() != null) {
        applyCallieConstraints(typeParamTable, callieType.getClassType().getGenericResolver());
      }

    }


    // checking arguments is a bit complicated, rest parameters allow "infinite" arguments and optional parameters can be "skipped"
    // so we only want to break the loop once we have either exhausted the arguments or parameter list.
    while (true) {
      if (argumentList.size() > argumentCounter) {
        argument = argumentList.get(argumentCounter++);
      }
      else {
        // out of arguments
        break;
      }

      if (!isRestArg) {
        if (parameterList.size() > parameterCounter) {
          parameter = parameterList.get(parameterCounter++);
          if (isVarArg(parameter)) isRestArg = true;
        }
        else {
          // out of parameters and last is not var arg, must mean that ve have skipped optionals and still had arguments left
          if (parameterType != null && argumentType != null) {
            validation.errors.addAll(annotateTypeMismatch(parameterType, argumentType, argument, null));
          }
          break;
        }
      }


      HaxeGenericResolver callieResolver = Optional.ofNullable(callieType.getClassType())
        .map(SpecificHaxeClassReference::getGenericResolver).orElse(new HaxeGenericResolver());
      argumentType = resolveArgumentType(argument, argumentResolver, callieResolver);
      // make sure we got any argument typeParameter overrides when resolving parameter
      parameterResolver.addArguments(argumentResolver);
      parameterType = resolveParameterType(parameter, parameterResolver);
      //TODO hack
      // unwrap Null<T> if arg is Null<> but not Param (and update resolver so we dont use T from Null<T>)
      if ((argumentType.getType() instanceof SpecificHaxeClassReference argRef && argRef.isNullType())
        && !(parameterType.getType() instanceof SpecificHaxeClassReference paramRef && paramRef.isNullType())) {
        SpecificTypeReference typeReference = argRef.unwrapNullType();
        if (typeReference instanceof SpecificHaxeClassReference classReference) {
          argumentType = new ResultHolder(argRef.unwrapNullType());
          //TODO  Hackish workaround, should really try to fix Null<T> logic so we dont have to unwrap

          argumentResolver.addAll(classReference.getGenericResolver());
          // updating paramsResolver as well becuase it inherits from CallExpressionGenericResolver
          //parameterResolver = parameterResolver.without("T");
          parameterResolver.addAll(classReference.getGenericResolver());
        }
      }

      // when methods has type-parameters we can inherit the type from arguments (note that they may contain constraints)
      ResultHolder unresolvedParameterType = parameter.getType();
      if (containsTypeParameter(unresolvedParameterType, typeParamTable)) {
        inheritTypeParametersFromArgument(unresolvedParameterType, argumentType, argumentResolver, resolver, typeParamTable);
      }

      // hack if functionType has untyped open parameterlist, if so inherit type, (lambdas can also be missing type)
      if (parameterType.isFunctionType() &&  argumentType.isFunctionType()  &&  argument instanceof HaxeFunctionLiteral literal) {
        SpecificFunctionReference paramFn = parameterType.getFunctionType();
        SpecificFunctionReference argFn = argumentType.getFunctionType();
        if(literal.getOpenParameterList() != null) {
          argumentType =  new SpecificFunctionReference(paramFn.getArguments(), argFn.getReturnType(), null, literal, literal).createHolder();
        }else if (literal.getParameterList() != null) {
          List<HaxeParameter> list = literal.getParameterList().getParameterList();
          List<HaxeArgument> argumentsArgs = argFn.getArguments();
          List<HaxeArgument> paramsArgs = paramFn.getArguments();
          int min = Math.min(Math.min(list.size(), argumentsArgs.size()),paramsArgs.size());

          for (int i = 0; i < min; i++) {
            HaxeParameter haxeParameter = list.get(i);
            if (haxeParameter.getTypeTag() == null && haxeParameter.getVarInit() == null) {
              HaxeArgument argArg = argumentsArgs.get(i);
              HaxeArgument paramArg = paramsArgs.get(i);
              ResultHolder argArgType = argArg.getType();
              ResultHolder paramArgType = paramArg.getType();
              if(argArgType.isUnknown() && !paramArgType.isUnknown()) {
                argumentsArgs.set(i, argArg.withType(paramArgType));
              }
            }
          }
          argumentType =  new SpecificFunctionReference(argumentsArgs, argFn.getReturnType(), null, literal, literal).createHolder();
        }
      }

      //TODO properly resolve typedefs
      SpecificHaxeClassReference argumentClass = argumentType.getClassType();
      if (argumentClass != null && argumentClass.isFunction() && parameterType.isTypeDef()) {
        // make sure that if  parameter type is typedef  try to convert to function so we can compare with argument
        parameterType = resolveParameterType(parameterType, argumentClass);
      }


      Optional<ResultHolder> optionalTypeParameterConstraint = findConstraintForTypeParameter(parameter, parameterType, typeParamTable);

      // check if  argument matches Type Parameter constraint
      if (optionalTypeParameterConstraint.isPresent()) {
        HaxeAssignContext  assignContext = new HaxeAssignContext(parameter.getBasePsi(), argument);
        // Note that we use parameter type without resolved typeParameters here to check against method and class constraints.
        assignContext.setConstraintCheck(containsTypeParameter(unresolvedParameterType, typeParamTable));
        ResultHolder constraint = optionalTypeParameterConstraint.get();
        if (canAssignToFrom(constraint, argumentType, assignContext)) {
          updateValidationData(validation, argumentCounter, parameterCounter, argumentType, parameterType, parameter);
        } else {
          if (parameter.isOptional()) {
            argumentCounter--; //retry argument with next parameter
          }
          else {

            if (constraint.isClassType() && constraint.isMissingClassModel()) {
              validation.warnings.add(annotateUnableToCompare(constraint, argument));
            }else if (argumentType.isClassType() && argumentType.isMissingClassModel()){
              validation.warnings.add(annotateUnableToCompare( argumentType, argument));
            }else {
              validation.errors.addAll(annotateTypeMismatch(constraint, argumentType, argument, assignContext));
            }
            addToIndexMap(validation, argumentCounter, parameterCounter);
          }
        }
      }
      else {
        ResultHolder resolvedParameterType = HaxeTypeResolver.resolveParameterizedType(parameterType, resolver.withoutUnknowns());

        HaxeAssignContext  assignContext = new HaxeAssignContext(parameter.getBasePsi(), argument);
        if (canAssignToFrom(resolvedParameterType, argumentType, assignContext)) {
          updateValidationData(validation, argumentCounter, parameterCounter, argumentType, parameterType, parameter);
        } else {
          if (parameter.isOptional()) {
            argumentCounter--; //retry argument with next parameter
          }
          else {
            if (resolvedParameterType.isClassType() && resolvedParameterType.isMissingClassModel()) {
              validation.warnings.add(annotateUnableToCompare(resolvedParameterType, argument));
            }else if (argumentType.isClassType() && argumentType.isMissingClassModel()){
              validation.warnings.add(annotateUnableToCompare( argumentType, argument));
            }else {
              validation.errors.addAll(annotateTypeMismatch(resolvedParameterType, argumentType, argument, assignContext));
            }
            addToIndexMap(validation, argumentCounter, parameterCounter);
          }
        }
      }
    }
    validation.completed = true;
    validation.resolver = resolver;
    validation.reResolveParameters();
    return validation;
  }

  private static void addOriginalParameterTypeToIndex(CallExpressionValidation validation, int index, ResultHolder type) {
    validation.originalParameterIndexToType.put(index - 1, type);
  }


  private static void addArgumentTypeToIndex(CallExpressionValidation validation, int index, ResultHolder type) {
    validation.argumentIndexToType.put(index - 1, type);
  }
  private static void addParameterTypeToIndex(CallExpressionValidation validation, int index, ResultHolder type) {
    validation.parameterIndexToType.put(index - 1, type);
  }

  @NotNull
  private static HaxeGenericResolver remapTypeParameters(SpecificHaxeClassReference paramClass, SpecificHaxeClassReference callieClass) {
    HaxeGenericResolver remappedResolver = new HaxeGenericResolver();
    if (paramClass != null && !paramClass.isUnknown()
        && callieClass != null && !callieClass.isUnknown()) {
      // just going to do exact match remapping for now, unifying parameter type and callie type and then their typeParameters is probably quite complicated.
      // anonymous structures are also OK  as long as the types can be assigned to each other, it means they have the same structures and
      // it should be safe to remapTypeParameters.
      SpecificTypeReference paramFullyResolved = paramClass.fullyResolveTypeDefAndUnwrapNullTypeReference();
      SpecificTypeReference callieFullyResolved = callieClass.fullyResolveTypeDefAndUnwrapNullTypeReference();

      if (paramFullyResolved instanceof SpecificHaxeClassReference  param
          && callieFullyResolved instanceof SpecificHaxeClassReference callie) {
        if (param.getHaxeClassReference().refersToSameClass(callie.getHaxeClassReference())
            || param.isAnonymousType() || callie.isAnonymousType())
        {
          remap(param, callie, remappedResolver);
        }
      }
    }
    return remappedResolver;
  }

  private static void remap(SpecificHaxeClassReference param, SpecificHaxeClassReference callie, HaxeGenericResolver remappedResolver) {
    @NotNull ResultHolder[] specificsFromMethodArg = param.getSpecifics();
    @NotNull ResultHolder[] specificsFromCallie = callie.getSpecifics();
    int maxSpecifics = Math.min(specificsFromMethodArg.length, specificsFromCallie.length);
    for (int i = 0; i < maxSpecifics; i++) {
      ResultHolder specArg = specificsFromMethodArg[i];
      ResultHolder specCallie = specificsFromCallie[i];
      if (specArg.isTypeParameter()) {
        SpecificHaxeClassReference classType = specArg.getClassType();

        if (classType != null && classType.isTypeParameter()) {
          if (classType.getHaxeClassModel() instanceof HaxeGenericParamModel classModel) {
            remappedResolver.addArgument(classModel.getTypeParameter(), specCallie);
          }
        }
      }
    }
  }

  public static CallExpressionValidation checkFunctionCall(HaxeCallExpression callExpression, SpecificFunctionReference functionType) {
    CallExpressionValidation validation  = new CallExpressionValidation();
    validation.isFunction = true;
    if (functionType != null) {

      //HaxeFunctionTypeModel model = new HaxeFunctionTypeModel(functionType);
      HaxeExpression methodExpression = callExpression.getExpression();

      HaxeCallExpressionList callExpressionList = callExpression.getExpressionList();
      //List<HaxeFunctionTypeParameterModel> parameterList = model.getParameters();
      List<HaxeArgument> arguments = functionType.getArguments();

      List<HaxeExpression> argumentList = Optional.ofNullable(callExpressionList)
        .map(HaxeExpressionList::getExpressionList)
        .orElse(List.of());



      boolean hasVarArgs = arguments.stream().anyMatch(HaxeArgument::isRest);
      long minArgRequired = countRequiredFunctionTypeArguments(arguments);
      long maxArgAllowed = hasVarArgs ? Long.MAX_VALUE : arguments.size();

      // min arg check

      if (argumentList.size() < minArgRequired) {
        String message = HaxeBundle.message("haxe.semantic.method.parameter.missing", minArgRequired, argumentList.size());
        if (argumentList.isEmpty()) {
          PsiElement first = UsefulPsiTreeUtil.getNextSiblingSkipWhiteSpacesAndComments(methodExpression);
          PsiElement second = UsefulPsiTreeUtil.getNextSiblingSkipWhiteSpacesAndComments(first);
          TextRange range = TextRange.create(first.getTextOffset(), second.getTextOffset() + 1);
          validation.errors.add(new ErrorRecord(range, message));
        }
        else {
          validation.errors.add(new ErrorRecord(callExpressionList.getTextRange(), message));
        }
      }
      //max arg check
      if (argumentList.size() > maxArgAllowed) {
        String message = HaxeBundle.message("haxe.semantic.method.parameter.too.many", maxArgAllowed, argumentList.size());
        validation.errors.add(new ErrorRecord(callExpressionList.getTextRange(), message));
        return validation;
      }

      HaxeGenericResolver resolver  = new HaxeGenericResolver();

      resolver = HaxeGenericResolverUtil.appendCallExpressionGenericResolver(callExpression, resolver);

      int parameterCounter = 0;
      int argumentCounter = 0;

      boolean isRestArg = false;
      HaxeArgument parameter = null;
      HaxeExpression argument;

      ResultHolder parameterType = null;
      ResultHolder argumentType = null;

      // checking arguments is a bit complicated, rest parameters allow "infinite" arguments and optional parameters can be "skipped"
      // so we only want to break the loop once we have either exhausted the arguments or parameter list.
      while (true) {
        HaxeGenericResolver localResolver = new HaxeGenericResolver();
        localResolver.addAll(resolver);

        if (argumentList.size() > argumentCounter) {
          argument = argumentList.get(argumentCounter++);
        }
        else {
          // out of arguments
          break;
        }

        if (!isRestArg) {
          if (arguments.size() > parameterCounter) {
            parameter = arguments.get(parameterCounter++);
            if (parameter.isRest()) isRestArg = true;
          }
          else {
            // out of parameters and last is not var arg, must mean that ve have skipped optionals and still had arguments left
            if (parameterType != null && argumentType != null) {
              validation.errors.addAll(annotateTypeMismatch(parameterType, argumentType, argument, null));
            }
            break;
          }
        }

        argumentType = resolveArgumentType(argument, localResolver, null);
        parameterType = parameter.getType();


        //TODO properly resolve typedefs
        SpecificHaxeClassReference argumentClass = argumentType.getClassType();
        if (argumentClass != null && argumentClass.isFunction() && parameterType.isTypeDef()) {
          // make sure that if  parameter type is typedef  try to convert to function so we can compare with argument
          parameterType = resolveParameterType(parameterType, argumentClass);
        }


        // check if  argument matches Type Parameter constraint
        ResultHolder resolvedParameterType = HaxeTypeResolver.resolveParameterizedType(parameterType, resolver);

        HaxeAssignContext  assignContext = new HaxeAssignContext(parameter.getType().getElementContext(), argument);
        if (canAssignToFrom(resolvedParameterType, argumentType, assignContext)) {
          addToIndexMap(validation, argumentCounter, parameterCounter);
          addArgumentTypeToIndex(validation, argumentCounter, argumentType);
          addParameterTypeToIndex(validation, parameterCounter, parameterType);
          addOriginalParameterTypeToIndex(validation, parameterCounter, parameter.getType());
          validation.ParameterNames.add(parameter.getName());
        }else {
          if (parameter.isOptional()) {
            argumentCounter--; //retry argument with next parameter
          } else {
            validation.errors.addAll(annotateTypeMismatch(parameterType, argumentType, argument, assignContext));
            addToIndexMap(validation, argumentCounter, parameterCounter);
          }
        }
      }
    }
    validation.completed = true;
    return validation;
  }


  public static CallExpressionValidation checkConstructor(HaxeNewExpression newExpression) {
    CallExpressionValidation validation  = new CallExpressionValidation();
    validation.isConstructor = true;

    HaxeType expressionType = newExpression.getType();
    if (expressionType == null) return validation; // incomplete new expression

    ResultHolder type = HaxeTypeResolver.getTypeFromType(expressionType);
    // ignore anything where we dont have class model
    if (type.isMissingClassModel()) {
      return validation;
    }
    HaxeMethodModel constructor = type.getClassType().getHaxeClass().getModel().getConstructor(null);

    // if we cant find a constructor  ignore
    // TODO (might add a missing constructor  annotation here)
    if (constructor == null) {
      return validation;
    }


    if (constructor.isOverload()) {
      //TODO implement support for overloaded methods (need to get correct model ?)
      return validation; //(stopping here to avoid marking arguments as type mismatch)
    }


    List<HaxeParameterModel> parameterList = constructor.getParameters();
    List<HaxeExpression> argumentList = newExpression.getExpressionList();


    boolean hasVarArgs = hasVarArgs(parameterList);
    long minArgRequired = countRequiredArguments(parameterList);
    long maxArgAllowed = hasVarArgs ? Long.MAX_VALUE : parameterList.size();

    // min arg check

    if (argumentList.size() < minArgRequired) {
      String message = HaxeBundle.message("haxe.semantic.method.parameter.missing", minArgRequired, argumentList.size());
      if (argumentList.isEmpty()) {
        @NotNull PsiElement[] children = newExpression.getChildren();
        if (children.length > 0) {
          PsiElement first = UsefulPsiTreeUtil.getNextSiblingSkipWhiteSpacesAndComments(children[0]);
          if (first != null) {
            PsiElement second = UsefulPsiTreeUtil.getNextSiblingSkipWhiteSpacesAndComments(first);
            if (second != null) {
              TextRange range = TextRange.create(first.getTextOffset(), second.getTextOffset() + 1);
              validation.errors.add(new ErrorRecord(range, message));
              return validation;
            }
          }
        }
      }
      validation.errors.add(new ErrorRecord(newExpression.getTextRange(), message));
      return validation;
    }
    //max arg check
    if (argumentList.size() > maxArgAllowed) {
      String message = HaxeBundle.message("haxe.semantic.method.parameter.too.many", maxArgAllowed, argumentList.size());
      validation.errors.add(new ErrorRecord(newExpression.getTextRange(), message));
      return validation;
    }

    // generics and type parameter
    HaxeGenericSpecialization specialization = newExpression.getSpecialization();
    HaxeGenericResolver resolver = null;


    if (specialization != null) {
      resolver = specialization.toGenericResolver(newExpression);
    }
    if (resolver == null) resolver = new HaxeGenericResolver();

    resolver = HaxeGenericResolverUtil.appendCallExpressionGenericResolver(newExpression, resolver);

    HashMap<HaxeTypeParameterDeclaration, ResultHolder> typeParamTable = createTypeParameterConstraintTable(constructor.getMethod(), resolver, validation.unknownCallie);


    int parameterCounter = 0;
    int argumentCounter = 0;


    boolean isRestArg = false;
    HaxeParameterModel parameter = null;
    HaxeExpression argument;

    ResultHolder parameterType;
    ResultHolder argumentType;

    HaxeGenericResolver argumentResolver = resolver.withoutUnknowns();
    // methods might have typeParameters with same name as a parent so we need to make sure we are not resolving parents type
    // when resolving parameters
    HaxeGenericResolver parameterResolver = resolver.withoutUnknowns();
    HaxeMethod method = type.getClassType().getHaxeClassModel().getConstructor(resolver).getMethod();
    resolver.addAll(method.getModel().getGenericResolver(resolver));


    // checking arguments is a bit complicated, rest parameters allow "infinite" arguments and optional parameters can be "skipped"
    // so we only want to break the loop once we have either exhausted the arguments or parameter list.
    while (true) {
      if (argumentList.size() > argumentCounter) {
        argument = argumentList.get(argumentCounter++);
      }
      else {
        // out of arguments
        break;
      }

      if (!isRestArg) {
        if (parameterList.size() > parameterCounter) {
          parameter = parameterList.get(parameterCounter++);
          if (isVarArg(parameter)) isRestArg = true;
        }
        else {
          // out of parameters and last is not var arg
          break;
        }
      }

      argumentType = resolveArgumentType(argument, argumentResolver, null);
      parameterType = resolveParameterType(parameter, parameterResolver);

      // when methods has type-parameters we can inherit the type from arguments (note that they may contain constraints)
      if (containsTypeParameter(parameterType, typeParamTable)) {
        inheritTypeParametersFromArgument(parameterType, argumentType, argumentResolver, resolver, typeParamTable);
        // attempt re-resolve after adding inherited type parameters
        parameterType = resolveParameterType(parameter, resolver);
      }

      //TODO properly resolve typedefs
      SpecificHaxeClassReference argumentClass = argumentType.getClassType();
      if (argumentClass != null && argumentClass.isFunction() && parameterType.isTypeDef()) {
        // make sure that if  parameter type is typedef  try to convert to function so we can compare with argument
        parameterType = resolveParameterType(parameterType, argumentClass);
      }


      //TODO mlo: note to self , when argument function, can assign to "Function"

      Optional<ResultHolder> optionalTypeParameterConstraint = findConstraintForTypeParameter(parameter, parameterType, typeParamTable);

      // check if  argument matches Type Parameter constraint
      if (optionalTypeParameterConstraint.isPresent()) {
        ResultHolder constraint = optionalTypeParameterConstraint.get();
        HaxeAssignContext  assignContext = new HaxeAssignContext(parameter.getType().getElementContext(), argument);
        if (canAssignToFrom(constraint, argumentType, assignContext)) {
          updateValidationData(validation, argumentCounter, parameterCounter, argumentType, parameterType, parameter);
        } else {
          if (parameter.isOptional()) {
            argumentCounter--; //retry argument with next parameter
          }
          else {
            validation.errors.addAll(annotateTypeMismatch(constraint, argumentType, argument, assignContext));
            addToIndexMap(validation, argumentCounter, parameterCounter);
          }
        }
      }
      else {
        ResultHolder resolvedParameterType = HaxeTypeResolver.resolveParameterizedType(parameterType, resolver.withoutUnknowns());

        HaxeAssignContext  assignContext = new HaxeAssignContext(parameter.getType().getElementContext(), argument);
        if (canAssignToFrom(resolvedParameterType, argumentType, assignContext)) {
          updateValidationData(validation, argumentCounter, parameterCounter, argumentType, parameterType, parameter);
        }
        else {
          if (parameter.isOptional()) {
            argumentCounter--; //retry argument with next parameter
          }else {
            validation.errors.addAll(annotateTypeMismatch(parameterType, argumentType, argument, assignContext));
            addToIndexMap(validation, argumentCounter, parameterCounter);
          }
        }
      }
    }
    validation.completed = true;
    validation.resolver = resolver;
    return validation;
  }

  private static void updateValidationData(CallExpressionValidation validation,
                                           int argumentCounter,
                                           int parameterCounter,
                                           ResultHolder argumentType,
                                           ResultHolder parameterType,
                                           HaxeParameterModel parameter) {

    addToIndexMap(validation, argumentCounter, parameterCounter);
    addArgumentTypeToIndex(validation, argumentCounter, argumentType);
    addParameterTypeToIndex(validation, parameterCounter, parameterType);
    addOriginalParameterTypeToIndex(validation, parameterCounter, parameter.getType());

    validation.ParameterNames.add(parameter.getName());
  }


  @NotNull
  private static List<HaxeParameterModel> mapParametersToModel(HaxeParameterList parameterList) {
    if (parameterList == null) return List.of();
    return parameterList.getParameterList().stream().map(HaxeParameterModel::new).toList();
  }

  private static void addToIndexMap(CallExpressionValidation validation, int argumentCounter, int parameterCounter) {
    validation.argumentToParameterIndex.put(argumentCounter - 1, parameterCounter - 1);
  }


  private static HaxeParameterModel resolveMacroTypes(HaxeParameterModel parameterModel) {
    ResultHolder type = parameterModel.getType(null);
    ResultHolder resolved = HaxeMacroUtil.resolveMacroType(type);
    return parameterModel.replaceType(resolved);
  }

  private static void inheritTypeParametersFromArgument(ResultHolder parameterType,
                                                        ResultHolder argumentType,
                                                        HaxeGenericResolver argumentResolver,
                                                        HaxeGenericResolver parentResolver,
                                                        HashMap<HaxeTypeParameterDeclaration, ResultHolder> typeParamTable) {
    if (argumentType == null) return; // this should not happen, we should have an argument
    HaxeGenericResolver inherit = findTypeParametersToInherit(parameterType.getType(), argumentType.getType().withoutConstantValue(), new HaxeGenericResolver(), typeParamTable);
    for (ResolverEntry entry : inherit.entries()) {
      HaxeTypeParameterDeclaration typeParameter = entry.typeParameter();

      // TODO needs clean up
        if (typeParamTable.containsKey(typeParameter)) {
          ResultHolder constraint = typeParamTable.get(typeParameter);
          ResultHolder type = inherit.resolve(typeParameter);
          if (type == null) continue;
          // if TypeParameter without constraint
          if (constraint == null) {
            typeParamTable.put(typeParameter, type);
          }
          else if (type.isTypeParameter() && !constraint.isTypeParameter()) {
            continue;// skipping as we dont want to replace a real type with typeParameter
          }
          else if (constraint.canAssign(type)) {
            typeParamTable.put(typeParameter, type);
          }
          argumentResolver.add(typeParameter, type);
          parentResolver.add(typeParameter, type);
          //break;
        }
    }
  }

  private static ResultHolder resolveArgumentType(HaxeExpression argument, HaxeGenericResolver argumentResolver, HaxeGenericResolver callieResolver) {
    ResultHolder expressionType = null;
    // try to resolve methods/function types
    if (argument instanceof HaxeReferenceExpression referenceExpression) {
      PsiElement resolvedExpression = referenceExpression.resolve();
      if (resolvedExpression instanceof HaxeLocalFunctionDeclaration functionDeclaration) {
        SpecificFunctionReference type = functionDeclaration.getModel().getFunctionType(null);
        expressionType = type.createHolder();
      }
      // this one  also cover Enum constructors
      else if (resolvedExpression instanceof HaxeMethod method) {
        SpecificFunctionReference type = method.getModel().getFunctionType(null);
        expressionType = type.createHolder();
      }else if (resolvedExpression instanceof HaxeEnumValueDeclarationField enumValueField) {
        ResultHolder type = HaxeTypeResolver.getEnumReturnType(enumValueField, referenceExpression.resolveHaxeClass().getGenericResolver());
        // convert any EnumValue to its parent enumClass so we can do proper assignCheck
        if (type.isEnumValueType()) {
          type = type.getEnumValueType().getEnumClass().createHolder();
        }
        return type;
      }
    }

    // if expression is enumValue we need to resolve the underlying enumType type to test assignment
    if (expressionType != null && expressionType.getType() instanceof SpecificEnumValueReference type) {
      SpecificHaxeClassReference enumType = type.getEnumClass();
      expressionType = enumType.createHolder();
    }

    // anything else is resolved here (literals etc.)
    if (expressionType == null) {
      HaxeExpressionEvaluatorContext context = new HaxeExpressionEvaluatorContext(argument);
      HaxeGenericResolver genericResolver = HaxeGenericResolverUtil.generateResolverFromScopeParents(argument);
      expressionType = HaxeExpressionEvaluator.evaluateWithRecursionGuard(argument, context, genericResolver.withoutUnknowns()).result;
      //TODO
      //function literals can inherit from method parameters, and if parameter list contains type parameters then
      // it should be ok to use callies resolver values here, but currently it might add a bit much
      //if (argument instanceof HaxeFunctionLiteral && expressionType.containsTypeParameters()){
        //if (callieResolver != null) expressionType = callieResolver.resolve(expressionType);
      //}
    }



    return expressionType;
  }

  private static ResultHolder resolveParameterType(HaxeParameterModel parameter, HaxeGenericResolver localResolver) {
    ResultHolder type = parameter.getType(localResolver.withoutUnknowns());
    // custom handling for macro based rest parameters (commonly used before we got native support for rest parameters)
    if (type.getClassType() != null) {
      SpecificHaxeClassReference classType = type.getClassType();
      @NotNull ResultHolder[] specifics = classType.getSpecifics();
      if (isExternRestClass(classType) || isRestClass(classType)) {
        if (specifics.length == 1) {
          return specifics[0];
        }
      }
      if (specifics.length == 1) {
        SpecificTypeReference specificType = specifics[0].getType();
        if (classType.isArray() && specificType instanceof SpecificHaxeClassReference classReference && isMacroExpr(classReference)) {
          type = SpecificTypeReference.getDynamic(parameter.getParameterPsi()).createHolder();
        }
      }
    }
    // convert any EnumValue to its parent enumClass so we can do proper assignCheck
    if (type.isEnumValueType()) {
      type = type.getEnumValueType().getEnumClass().createHolder();
    }
    return type;
  }


  private static ResultHolder resolveParameterType(ResultHolder parameterType, SpecificHaxeClassReference parameterClassType) {
    if (parameterClassType != null) {
      HaxeClass aClass = parameterClassType.getHaxeClass();
      if (aClass != null && aClass.getModel().isTypedef()) {
        SpecificFunctionReference functionReference = parameterClassType.resolveTypeDefFunction();
        if (functionReference != null) {
          parameterType = functionReference.createHolder();
        }
      }
    }
    return parameterType;
  }


  public static HaxeGenericResolver findTypeParametersToInherit(SpecificTypeReference parameter,
                                                                 SpecificTypeReference argument,
                                                                 HaxeGenericResolver resolver,
                                                                HashMap<HaxeTypeParameterDeclaration, ResultHolder> typeParamTable) {

    //fully resolving to make sure we dont have issues with Null<T> vs just T etc.
    if (parameter instanceof SpecificHaxeClassReference parameterReference) {
      parameter = parameterReference.fullyResolveTypeDefAndUnwrapNullTypeReference();
    }
    if (argument instanceof SpecificHaxeClassReference argumentReference) {
      argument = argumentReference.fullyResolveTypeDefAndUnwrapNullTypeReference();
    }

    if (parameter instanceof SpecificHaxeClassReference parameterReference &&
        argument instanceof SpecificHaxeClassReference argumentReference) {

      HaxeGenericResolver paramResolver = parameterReference.getGenericResolver().addAll(resolver.withoutUnknowns());
      HaxeGenericResolver argResolver = argumentReference.getGenericResolver().addAll(resolver.withoutUnknowns());
      if (parameterReference.isTypeParameter() && !argument.isTypeParameter() && !argument.isUnknown()) {
        HaxeGenericParamModel classModel = (HaxeGenericParamModel)parameterReference.getHaxeClassModel();
        resolver.addArgument(classModel.getTypeParameter(), argumentReference.createHolder());
      }else {
        for (ResolverEntry entry : paramResolver.entries()) {
          HaxeTypeParameterDeclaration tp = entry.typeParameter();
          ResultHolder resolve = paramResolver.resolve(tp);
          if (resolve != null && resolve.isClassType()) {
            SpecificHaxeClassReference classType = resolve.getClassType();
            if(classType.isTypeParameter()) {
              HaxeGenericParamModel classModel = (HaxeGenericParamModel)classType.getHaxeClassModel();

              HaxeTypeParameterDeclaration typeParameter = classModel.getTypeParameter();

              if (typeParameter != null && typeParamTable.containsKey(typeParameter)) {
                ResultHolder argResolved = argResolver.resolve(typeParameter);
                if (argResolved != null) {
                  resolver.add(typeParameter, argResolved);// TODO mlo: was Argument scope
                }
              }
            }
          }
        }
      }
    }
    if (parameter instanceof SpecificFunctionReference parameterReference &&
        argument instanceof SpecificFunctionReference argumentReference) {
      List<HaxeArgument> parameterFnArguments = parameterReference.getArguments();
      List<HaxeArgument> argumentFnArguments = argumentReference.getArguments();
      if (parameterFnArguments.size() == argumentFnArguments.size()) {
        for (int i = 0; i < parameterFnArguments.size(); i++) {
          SpecificTypeReference functionArgument = argumentFnArguments.get(i).getType().getType();
          SpecificTypeReference parameterArgument = parameterFnArguments.get(i).getType().getType();
          findTypeParametersToInherit(parameterArgument, functionArgument, resolver, typeParamTable);
        }
      }

      ResultHolder parameterReturnType = parameterReference.getReturnType();
      ResultHolder argumentReturnType = argumentReference.getReturnType();

      findTypeParametersToInherit(parameterReturnType.getType(), argumentReturnType.getType(), resolver, typeParamTable);
    }
    if (parameter.isTypeParameter() && !argument.isTypeParameter() && !argument.isUnknown()) {
      if (parameter instanceof  SpecificHaxeClassReference classReference && classReference.isTypeParameter()) {
        if (classReference.getHaxeClassModel() instanceof HaxeGenericParamModel classModel) {
          resolver.add(classModel.getTypeParameter(), argument.createHolder());// TODO mlo: was Argument scope
        }
      }
    }


    return resolver;
  }


  private static List<ErrorRecord> annotateTypeMismatch(ResultHolder expected, ResultHolder got, HaxeExpression expression,
                                                  HaxeAssignContext context) {
    if (context != null) {
      if (context.hasMissingMembers()) {
        String message = HaxeBundle.message("haxe.semantic.method.parameter.mismatch.missing.members",
                                            context.getMissingMembersString());
        return List.of(new ErrorRecord(expression.getTextRange(), message));

      } else if (context.hasWrongTypeMembers()) {
        TextRange expectedRange = expression.getTextRange();
        // we are not allowed to annotate outside the expression so we check if all are inside before attempting to make them
        // if they are not inside, we fall back to just annotating the entire expression
        Map<PsiElement, String> wrongTypeMap = context.getWrongTypeMap();
        boolean allInRange = wrongTypeMap.keySet().stream().allMatch(psi -> expectedRange.contains(psi.getTextRange()));
        if(allInRange) {
          return wrongTypeMap.entrySet().stream().map(e -> new ErrorRecord(e.getKey().getTextRange(), e.getValue())).toList();
        }else {
          String message = HaxeBundle.message("haxe.semantic.method.parameter.mismatch.wrong.type.members",
                                              context.geWrongTypeMembersString());
          return  List.of(new ErrorRecord(expression.getTextRange(), message));
        }
      }
    }

    String message = HaxeBundle.message("haxe.semantic.method.parameter.mismatch",
                                        expected.toPresentationString(),
                                        got.toPresentationString());
    return List.of(new ErrorRecord(expression.getTextRange(), message));
  }

  private static WarningRecord annotateUnableToCompare( ResultHolder problemType, HaxeExpression expression) {
    String message = HaxeBundle.message("haxe.semantic.method.parameter.unable.to.compare", problemType.toPresentationString());
    return new WarningRecord(expression.getTextRange(), message);
  }

  private static int findMinArgsCounts(List<HaxeFunctionArgument> argumentList) {
    int count = 0;
    for (HaxeFunctionArgument argument : argumentList) {
      if (argument.getOptionalMark() == null) {
        if (!isVoidArgument(argument)) count++;
      }
    }
    return count;
  }

  private static boolean isVoidArgument(HaxeFunctionArgument argument) {
    HaxeTypeOrAnonymous toa = argument.getTypeOrAnonymous();
    HaxeType t = null != toa ? toa.getType() : null;
    String name = t != null ? t.getText() : null;
    return SpecificHaxeClassReference.VOID.equals(name);
  }


  private static long countRequiredArguments(List<HaxeParameterModel> parametersList) {
    return parametersList.stream()
      .filter(p -> !p.isOptional() && !p.hasInit() && !isVarArg(p))
      .count();
  }

  private static long countRequiredFunctionTypeArguments(List<HaxeArgument> parametersList) {
    return parametersList.stream()
      .filter(p -> !p.isOptional() && !p.getType().isVoid() && !p.isRest())
      .count();
  }

  private static boolean hasVarArgs(List<HaxeParameterModel> parametersList) {
    for (HaxeParameterModel model : parametersList) {
      if (isVarArg(model)) {
        return true;
      }
    }
    return false;
  }

  private static boolean isVarArg(HaxeParameterModel model) {
    if (model.isRest()) {
      return true;
    }
    //Legacy solutions for rest arguments
    // Array<haxe.macro.Expr>
    // haxe.extern.Rest<Float>
    // TODO : this is a bit of a hack to avoid having to resolve Array<Expr> and Rest<> Class, should probably resolve and compare these properly
    if (model.getType().getType() instanceof SpecificHaxeClassReference classType) {
      if (classType.getHaxeClass() != null) {
        ResultHolder[] specifics = classType.getSpecifics();
        if (specifics.length == 1) {
          SpecificTypeReference type = specifics[0].getType();
          if (type instanceof SpecificHaxeClassReference specificType) {
            if (specificType.getHaxeClass() != null) {
              // Array<haxe.macro.Expr>
              if (classType.isArray() && isMacroExpr(specificType)) {
                return true;
              }
              // haxe.extern.Rest<> / haxe.Rest<>
              return isExternRestClass(classType) || isRestClass(classType);
            }
          }
        }
      }
    }
    return false;
  }

  private static boolean isMacroExpr(SpecificHaxeClassReference classReference) {
    if (classReference.getHaxeClass() == null) return false;
    return classReference.getHaxeClass().getQualifiedName().equals(HaxeMacroTypeUtil.EXPR);
  }

  private static boolean isRestClass(SpecificHaxeClassReference classReference) {
    if (classReference.getHaxeClass() == null) return false;
    return classReference.getHaxeClass().getQualifiedName().equals("haxe.Rest");
  }
  private static boolean isExternRestClass(SpecificHaxeClassReference classReference) {
    if (classReference.getHaxeClass() == null) return false;
    return classReference.getHaxeClass().getQualifiedName().equals("haxe.extern.Rest");
  }



  @Data
  public static class CallExpressionValidation {
    public boolean unknownCallie = false;
    public ResultHolder Callie = null;
    Map<Integer, Integer> argumentToParameterIndex = new HashMap<>();
    Map<Integer, ResultHolder> argumentIndexToType = new HashMap<>();
    Map<Integer, ResultHolder> parameterIndexToType = new HashMap<>();
    Map<Integer, ResultHolder> originalParameterIndexToType = new HashMap<>();
    List<String> ParameterNames = new ArrayList<>();
    ResultHolder returnType;

    List<ErrorRecord> errors = new ArrayList<>();
    List<WarningRecord> warnings = new ArrayList<>();

    HaxeGenericResolver resolver = new HaxeGenericResolver();

    boolean completed = false;
    boolean memberMacroFunction = false;
    boolean isStaticExtension = false;
    boolean isConstructor = false;
    boolean isFunction = false;
    boolean isMethod = false;

    public void reResolveParameters() {
      HaxeGenericResolver genericResolver = resolver.withoutUnknowns();
      for (Map.Entry<Integer, ResultHolder> entry : originalParameterIndexToType.entrySet()) {
        ResultHolder resolve = genericResolver.resolve(entry.getValue());
        if ( resolve != null && !resolve.isUnknown()) {
          ResultHolder firstType = parameterIndexToType.get(entry.getKey());
          if(firstType.canAssign(resolve)){
            parameterIndexToType.put(entry.getKey(), resolve);
          }
        }
      }
    }
  }
  public record ErrorRecord (TextRange range, String message){};
  public record WarningRecord (TextRange range, String message){};


  public static ResultHolder tryGetCallieType(@NotNull HaxeCallExpression callExpression) {
    return tryGetCallieType(callExpression, null, false);
  }
  public static ResultHolder tryGetCallieType(@NotNull HaxeCallExpression callExpression,  @Nullable HaxeMethod method, boolean extensionMethod) {

    HaxeExpression expression = callExpression.getExpression();
    if (expression != null) {
      @NotNull PsiElement[] children = expression.getChildren();
      // if we got more then one child we are a chain and need to resolve the chain to know correct class
      if (children.length > 1) {
        PsiElement child = children[children.length - 2];
        HaxeExpressionEvaluatorContext evaluatorContext = new HaxeExpressionEvaluatorContext(child);
        ResultHolder result = HaxeExpressionEvaluator.evaluateWithRecursionGuard(child, evaluatorContext, null).result;
        if (!result.isUnknown()) return result;

      }else {
        // if only 1 child then we are calling on default "this" class reference
        HaxeClass type = PsiTreeUtil.getParentOfType(callExpression.getExpression(), HaxeClass.class);
        if(type != null) {
          HaxeClassModel model = type.getModel();
          return model.getInstanceType();
        }
      }
    }
    // fallback: if we know the method we know its class (but need to check if used as extension method)
    if (! extensionMethod && method !=  null) {
      HaxeClassModel classModel = method.getModel().getDeclaringClass();
      if(classModel != null) return classModel.getInstanceType();
    }

    return SpecificTypeReference.getUnknown(callExpression).createHolder();
  }

}
