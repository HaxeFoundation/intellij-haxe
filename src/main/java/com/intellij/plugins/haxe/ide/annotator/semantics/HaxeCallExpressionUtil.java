package com.intellij.plugins.haxe.ide.annotator.semantics;

import com.intellij.openapi.util.TextRange;
import com.intellij.plugins.haxe.HaxeBundle;
import com.intellij.plugins.haxe.lang.psi.*;
import com.intellij.plugins.haxe.lang.psi.impl.HaxeMethodDeclarationImpl;
import com.intellij.plugins.haxe.model.*;
import com.intellij.plugins.haxe.model.type.*;
import com.intellij.plugins.haxe.model.type.resolver.ResolveSource;
import com.intellij.plugins.haxe.util.HaxeResolveUtil;
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

  public static CallExpressionValidation checkMethodCall(@NotNull HaxeCallExpression callExpression, HaxeMethod method) {
    CallExpressionValidation validation  = new CallExpressionValidation();
    validation.isMethod = true;

    if (method.getModel().isOverload()) {
      //TODO implement support for overloaded methods (need to get correct model ?)
      return validation; //(stopping here to avoid marking arguments as type mismatch)
    }


    validation.isStaticExtension = callExpression.resolveIsStaticExtension();
    HaxeExpression methodExpression = callExpression.getExpression();


    HaxeCallExpressionList callExpressionList = callExpression.getExpressionList();
    List<HaxeParameterModel> parameterList = method.getModel().getParameters();
    if (method instanceof  HaxeMethodDeclarationImpl methodDeclaration) {

      if (HaxeMacroUtil.isMacroMethod(methodDeclaration)) {
        parameterList = parameterList.stream().map(HaxeCallExpressionUtil::resolveMacroTypes).toList();
      }
    }
    List<HaxeExpression> argumentList = Optional.ofNullable(callExpressionList)
      .map(HaxeExpressionList::getExpressionList)
      .orElse(List.of());

    boolean hasVarArgs = hasVarArgs(parameterList);
    long minArgRequired = countRequiredArguments(parameterList) - (validation.isStaticExtension ? 1 : 0);
    long maxArgAllowed = hasVarArgs ? Long.MAX_VALUE : parameterList.size() - (validation.isStaticExtension ? 1 : 0);

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
      return validation;
    }
    //max arg check
    if (argumentList.size() > maxArgAllowed) {
      String message = HaxeBundle.message("haxe.semantic.method.parameter.too.many", maxArgAllowed, argumentList.size());
      validation.errors.add(new ErrorRecord(callExpressionList.getTextRange(), message));
      return validation;
    }

    // generics and type parameter
    HaxeGenericResolver classTypeResolver =  tryGetCallieResolveResult(callExpression);
    HaxeGenericResolver resolver = HaxeGenericResolverUtil.appendCallExpressionGenericResolver(callExpression, classTypeResolver);

    Map<String, ResultHolder> typeParamMap = createTypeParameterConstraintMap(method, resolver);


    int parameterCounter = 0;
    int argumentCounter = 0;

    if (validation.isStaticExtension) {
      // this might not work for literals, need to handle those in a different way
      if (methodExpression instanceof HaxeReferenceExpression referenceChain) {
        HaxeReference leftReference = HaxeResolveUtil.getLeftReference(referenceChain);
        ResultHolder leftType = HaxeTypeResolver.getPsiElementType(leftReference, resolver);

        HaxeParameterModel model = parameterList.get(parameterCounter++);
        ResultHolder type = model.getType(resolver.withoutUnknowns());
        if (!canAssignToFrom(type, leftType)) {
          // TODO better error message
          validation.errors.add(new ErrorRecord(callExpression.getTextRange(), "Can not use extension method, wrong type"));
          return validation;
        }
      }
      else {
        // TODO check if literals, like "myString".SomeExtension()
      }
    }

    boolean isRestArg = false;
    HaxeParameterModel parameter = null;
    HaxeExpression argument;

    ResultHolder parameterType = null;
    ResultHolder argumentType = null;

    HaxeGenericResolver argumentResolver = resolver.withoutUnknowns();
    // methods might have typeParameters with same name as a parent so we need to make sure we are not resolving parents type
    // when resolving parameters
    HaxeGenericResolver parameterResolver = resolver.withoutUnknowns();
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
          // out of parameters and last is not var arg, must mean that ve have skipped optionals and still had arguments left
          if (parameterType != null && argumentType != null) {
            validation.errors.add(annotateTypeMismatch(parameterType, argumentType, argument));
          }
          break;
        }
      }


      argumentType = resolveArgumentType(argument, argumentResolver);
      // parameters might have type parameters with same name as a parent so we need to make sure we are not resolving parents type
      //HaxeGenericResolver parameterResolver = ((HaxeMethodModel)parameter.getMemberModel()).getGenericResolver(localResolver);
      parameterType = resolveParameterType(parameter, parameterResolver);

      // when methods has type-parameters we can inherit the type from arguments (note that they may contain constraints)
      if (containsTypeParameter(parameterType, typeParamMap)) {
        inheritTypeParametersFromArgument(parameterType, argumentType, argumentResolver, typeParamMap);
        // attempt re-resolve after adding inherited type parameters
        parameterType = resolveParameterType(parameter, argumentResolver);
      }

      //TODO properly resolve typedefs
      SpecificHaxeClassReference argumentClass = argumentType.getClassType();
      if (argumentClass != null && argumentClass.isFunction() && parameterType.isTypeDef()) {
        // make sure that if  parameter type is typedef  try to convert to function so we can compare with argument
        parameterType = resolveParameterType(parameterType, argumentClass);
      }


      //TODO mlo: note to self , when argument function, can assign to "Function"

      Optional<ResultHolder> optionalTypeParameterConstraint = findConstraintForTypeParameter(parameterType, typeParamMap);

      // check if  argument matches Type Parameter constraint
      if (optionalTypeParameterConstraint.isPresent()) {
        ResultHolder constraint = optionalTypeParameterConstraint.get();
        if (canAssignToFrom(constraint, argumentType)) {
          addToIndexMap(validation, argumentCounter, parameterCounter);
        } else {
          if (parameter.isOptional()) {
            argumentCounter--; //retry argument with next parameter
          }
          else {
            validation.errors.add(annotateTypeMismatch(constraint, argumentType, argument));
            addToIndexMap(validation, argumentCounter, parameterCounter);
          }
        }
      }
      else {
        ResultHolder resolvedParameterType = HaxeTypeResolver.resolveParameterizedType(parameterType, resolver.withoutUnknowns());


        if (canAssignToFrom(resolvedParameterType, argumentType)) {
          addToIndexMap(validation, argumentCounter, parameterCounter);
        } else {
          if (parameter.isOptional()) {
            argumentCounter--; //retry argument with next parameter
          }
          else {
            validation.errors.add(annotateTypeMismatch(resolvedParameterType, argumentType, argument));
            addToIndexMap(validation, argumentCounter, parameterCounter);
          }
        }
      }
    }
    validation.completed = true;
    return validation;
  }

  public static CallExpressionValidation checkFunctionCall(HaxeCallExpression callExpression, @Nullable HaxeFunctionType functionType) {
    CallExpressionValidation validation  = new CallExpressionValidation();
    validation.isFunction = true;
    if (functionType != null) {

      HaxeFunctionTypeModel model = new HaxeFunctionTypeModel(functionType);
      HaxeExpression methodExpression = callExpression.getExpression();

      HaxeCallExpressionList callExpressionList = callExpression.getExpressionList();
      List<HaxeFunctionTypeParameterModel> parameterList = model.getParameters();
      List<HaxeExpression> argumentList = Optional.ofNullable(callExpressionList)
        .map(HaxeExpressionList::getExpressionList)
        .orElse(List.of());

      boolean hasVarArgs = parameterList.stream().anyMatch(HaxeFunctionTypeParameterModel::isRestArgument);
      long minArgRequired = countRequiredFunctionTypeArguments(parameterList);
      long maxArgAllowed = hasVarArgs ? Long.MAX_VALUE : parameterList.size();

      // min arg check

      if (argumentList.size() < minArgRequired) {
        String message = HaxeBundle.message("haxe.semantic.method.parameter.missing", minArgRequired, argumentList.size());
        if (argumentList.isEmpty()) {
          PsiElement first = UsefulPsiTreeUtil.getNextSiblingSkipWhiteSpacesAndComments(methodExpression);
          PsiElement second = UsefulPsiTreeUtil.getNextSiblingSkipWhiteSpacesAndComments(first);
          TextRange range = TextRange.create(first.getTextOffset(), second.getTextOffset() + 1);
          validation.errors.add(new ErrorRecord(range, message));
          //holder.newAnnotation(HighlightSeverity.ERROR, message).range(range).create();
        }
        else {
          validation.errors.add(new ErrorRecord(callExpressionList.getTextRange(), message));
          //holder.newAnnotation(HighlightSeverity.ERROR, message).range(callExpressionList).create();
        }
        return validation;
      }
      //max arg check
      if (argumentList.size() > maxArgAllowed) {
        String message = HaxeBundle.message("haxe.semantic.method.parameter.too.many", maxArgAllowed, argumentList.size());
        validation.errors.add(new ErrorRecord(callExpressionList.getTextRange(), message));
        //holder.newAnnotation(HighlightSeverity.ERROR, message).range(callExpressionList).create();
        return validation;
      }

      // generics and type parameter
      //HaxeGenericSpecialization specialization = specificFunction.getSpecialization();
      HaxeGenericResolver resolver = null;


      //if (resolver == null && specialization != null) {
      //  resolver = specialization.toGenericResolver(callExpression);
      //}
      if (resolver == null) resolver = new HaxeGenericResolver();

      resolver = HaxeGenericResolverUtil.appendCallExpressionGenericResolver(callExpression, resolver);

      int parameterCounter = 0;
      int argumentCounter = 0;

      boolean isRestArg = false;
      HaxeFunctionTypeParameterModel parameter = null;
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
          if (parameterList.size() > parameterCounter) {
            parameter = parameterList.get(parameterCounter++);
            if (parameter.isRestArgument()) isRestArg = true;
          }
          else {
            // out of parameters and last is not var arg, must mean that ve have skipped optionals and still had arguments left
            if (parameterType != null && argumentType != null) {
              validation.errors.add(annotateTypeMismatch(parameterType, argumentType, argument));
            }
            break;
          }
        }

        argumentType = resolveArgumentType(argument, localResolver);
        parameterType = parameter.getArgumentType();


        //TODO properly resolve typedefs
        SpecificHaxeClassReference argumentClass = argumentType.getClassType();
        if (argumentClass != null && argumentClass.isFunction() && parameterType.isTypeDef()) {
          // make sure that if  parameter type is typedef  try to convert to function so we can compare with argument
          parameterType = resolveParameterType(parameterType, argumentClass);
        }


        // check if  argument matches Type Parameter constraint
        ResultHolder resolvedParameterType = HaxeTypeResolver.resolveParameterizedType(parameterType, resolver);


        if (canAssignToFrom(resolvedParameterType, argumentType)) {
          addToIndexMap(validation, argumentCounter, parameterCounter);
        }else {
          if (parameter.isOptional()) {
            argumentCounter--; //retry argument with next parameter
          } else {
            validation.errors.add(annotateTypeMismatch(parameterType, argumentType, argument));
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
    if (type.missingClassModel()) {
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
            TextRange range = TextRange.create(first.getTextOffset(), second.getTextOffset() + 1);
            //holder.newAnnotation(HighlightSeverity.ERROR, message).range(range).create();
            validation.errors.add(new ErrorRecord(range, message));
            return validation;
          }
        }
      }
      //holder.newAnnotation(HighlightSeverity.ERROR, message).range(resolvedNewExpression).create();
      validation.errors.add(new ErrorRecord(newExpression.getTextRange(), message));
      return validation;
    }
    //max arg check
    if (argumentList.size() > maxArgAllowed) {
      String message = HaxeBundle.message("haxe.semantic.method.parameter.too.many", maxArgAllowed, argumentList.size());
      //holder.newAnnotation(HighlightSeverity.ERROR, message).range(resolvedNewExpression).create();
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

    Map<String, ResultHolder> typeParamMap = createTypeParameterConstraintMap(constructor.getMethod(), resolver);


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

      argumentType = resolveArgumentType(argument, argumentResolver);
      parameterType = resolveParameterType(parameter, parameterResolver);

      // when methods has type-parameters we can inherit the type from arguments (note that they may contain constraints)
      if (containsTypeParameter(parameterType, typeParamMap)) {
        inheritTypeParametersFromArgument(parameterType, argumentType, resolver, typeParamMap);
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

      Optional<ResultHolder> optionalTypeParameterConstraint = findConstraintForTypeParameter(parameterType, typeParamMap);

      // check if  argument matches Type Parameter constraint
      if (optionalTypeParameterConstraint.isPresent()) {
        ResultHolder constraint = optionalTypeParameterConstraint.get();
        if (canAssignToFrom(constraint, argumentType)) {
          addToIndexMap(validation, argumentCounter, parameterCounter);
        } else {
          if (parameter.isOptional()) {
            argumentCounter--; //retry argument with next parameter
          }
          else {
            validation.errors.add(annotateTypeMismatch(constraint, argumentType, argument));
            addToIndexMap(validation, argumentCounter, parameterCounter);
          }
        }
      }
      else {
        ResultHolder resolvedParameterType = HaxeTypeResolver.resolveParameterizedType(parameterType, resolver.withoutUnknowns());


        if (canAssignToFrom(resolvedParameterType, argumentType)) {
          addToIndexMap(validation, argumentCounter, parameterCounter);
        }
        else {
          if (parameter.isOptional()) {
            argumentCounter--; //retry argument with next parameter
          }else {
            validation.errors.add(annotateTypeMismatch(parameterType, argumentType, argument));
            addToIndexMap(validation, argumentCounter, parameterCounter);
          }
        }
      }
    }
    validation.completed = true;
    return validation;
  }

  public static CallExpressionValidation checkEnumConstructor(HaxeCallExpression expression, HaxeEnumValueModel model) {
    CallExpressionValidation validation  = new CallExpressionValidation();
    validation.isConstructor = true;


    if (model == null || model.getEnumValuePsi() == null) return validation; // incomplete new expression

    ResultHolder type = HaxeTypeResolver.getEnumReturnType(model.getEnumValuePsi(), new HaxeGenericResolver());
    SpecificEnumValueReference enumValueType = type.getEnumValueType();
    SpecificHaxeClassReference declaringClass = enumValueType.getEnumClass();

    // if we cant find a constructor  ignore
    // TODO (might add a missing constructor  annotation here)
    if (!model.hasConstructor()) {
      return validation;
    }



    List<HaxeParameterModel> parameterList = mapParametersToModel(model.getConstructorParameters());
    List<HaxeExpression> argumentList = expression.getExpressionList().getExpressionList();


    boolean hasVarArgs = hasVarArgs(parameterList);
    long minArgRequired = countRequiredArguments(parameterList);
    long maxArgAllowed = hasVarArgs ? Long.MAX_VALUE : parameterList.size();

    // min arg check

    if (argumentList.size() < minArgRequired) {
      String message = HaxeBundle.message("haxe.semantic.method.parameter.missing", minArgRequired, argumentList.size());
      if (argumentList.isEmpty()) {
        @NotNull PsiElement[] children = expression.getChildren();
        if (children.length > 0) {
          PsiElement first = UsefulPsiTreeUtil.getNextSiblingSkipWhiteSpacesAndComments(children[0]);
          if (first != null) {
            PsiElement second = UsefulPsiTreeUtil.getNextSiblingSkipWhiteSpacesAndComments(first);
            TextRange range = TextRange.create(first.getTextOffset(), second.getTextOffset() + 1);
            //holder.newAnnotation(HighlightSeverity.ERROR, message).range(range).create();
            validation.errors.add(new ErrorRecord(range, message));
            return validation;
          }
        }
      }
      //holder.newAnnotation(HighlightSeverity.ERROR, message).range(expression).create();
      validation.errors.add(new ErrorRecord(expression.getTextRange(), message));
      return validation;
    }
    //max arg check
    if (argumentList.size() > maxArgAllowed) {
      String message = HaxeBundle.message("haxe.semantic.method.parameter.too.many", maxArgAllowed, argumentList.size());
      //holder.newAnnotation(HighlightSeverity.ERROR, message).range(expression).create();
      validation.errors.add(new ErrorRecord(expression.getTextRange(), message));
      return validation;
    }

    // generics and type parameter
    HaxeGenericSpecialization specialization = expression.getSpecialization();
    HaxeGenericResolver resolver = null;


    if (resolver == null && specialization != null) {
      resolver = specialization.toGenericResolver(expression);
    }
    if (resolver == null) resolver = new HaxeGenericResolver();

    resolver = HaxeGenericResolverUtil.appendCallExpressionGenericResolver(expression, resolver);

    Map<String, ResultHolder> typeParamMap = createTypeParameterConstraintMap(declaringClass.getHaxeClassModel().getGenericParams(), resolver);


    int parameterCounter = 0;
    int argumentCounter = 0;


    boolean isRestArg = false;
    HaxeParameterModel parameter = null;
    HaxeExpression argument;

    ResultHolder parameterType;
    ResultHolder argumentType;

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
        if (parameterList.size() > parameterCounter) {
          parameter = parameterList.get(parameterCounter++);
          if (isVarArg(parameter)) isRestArg = true;
        }
        else {
          // out of parameters and last is not var arg
          break;
        }
      }

      localResolver.addAll(declaringClass.getGenericResolver().withoutUnknowns());

      argumentType = resolveArgumentType(argument, localResolver);
      parameterType = resolveParameterType(parameter, localResolver);

      // when methods has type-parameters we can inherit the type from arguments (note that they may contain constraints)
      if (containsTypeParameter(parameterType, typeParamMap)) {
        inheritTypeParametersFromArgument(parameterType, argumentType, resolver, typeParamMap);
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

      Optional<ResultHolder> optionalTypeParameterConstraint = findConstraintForTypeParameter(parameterType, typeParamMap);

      // check if  argument matches Type Parameter constraint
      if (optionalTypeParameterConstraint.isPresent()) {
        ResultHolder constraint = optionalTypeParameterConstraint.get();
        if (canAssignToFrom(constraint, argumentType)) {
          addToIndexMap(validation, argumentCounter, parameterCounter);
        } else {
          if (parameter.isOptional()) {
            argumentCounter--; //retry argument with next parameter
          }
          else {
            validation.errors.add(annotateTypeMismatch(constraint, argumentType, argument));
            addToIndexMap(validation, argumentCounter, parameterCounter);
          }
        }
      }
      else {
        ResultHolder resolvedParameterType = HaxeTypeResolver.resolveParameterizedType(parameterType, resolver.withoutUnknowns());


        if (canAssignToFrom(resolvedParameterType, argumentType)) {
          addToIndexMap(validation, argumentCounter, parameterCounter);
        }
        else {
          if (parameter.isOptional()) {
            argumentCounter--; //retry argument with next parameter
          }else {
            validation.errors.add(annotateTypeMismatch(parameterType, argumentType, argument));
            addToIndexMap(validation, argumentCounter, parameterCounter);
          }
        }
      }
    }
    validation.completed = true;
    return validation;
  }


  @NotNull
  private static List<HaxeParameterModel> mapParametersToModel(HaxeParameterList parameterList) {
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
                                                        HaxeGenericResolver resolver,
                                                        Map<String, ResultHolder> typeParamMap) {
    if (argumentType == null) return; // this should not happen, we should have an argument
    HaxeGenericResolver inherit = findTypeParametersToInherit(parameterType.getType(), argumentType.getType(), resolver, typeParamMap);
    resolver.addAll(inherit);
    // parameter is a typeParameter type, we can just add it to resolver
    if (parameterType.getClassType().isFromTypeParameter()) {
      String className = parameterType.getClassType().getClassName();
      resolver.add(className, argumentType, ResolveSource.ARGUMENT_TYPE);
      typeParamMap.put(className, argumentType);
    }
  }

  private static ResultHolder resolveArgumentType(HaxeExpression argument, HaxeGenericResolver resolver) {
    ResultHolder expressionType = null;
    // try to resolve methods/function types
    if (argument instanceof HaxeReferenceExpression referenceExpression) {
      PsiElement resolvedExpression = referenceExpression.resolve();
      if (resolvedExpression instanceof HaxeLocalFunctionDeclaration functionDeclaration) {
        SpecificFunctionReference type = functionDeclaration.getModel().getFunctionType(null);
        expressionType = type.createHolder();
      }
      else if (resolvedExpression instanceof HaxeMethodDeclaration methodDeclaration) {
        SpecificFunctionReference type = methodDeclaration.getModel().getFunctionType(null);
        expressionType = type.createHolder();
      }else if (resolvedExpression instanceof HaxeEnumValueDeclaration valueDeclaration) {
        return  HaxeTypeResolver.getEnumReturnType(valueDeclaration, referenceExpression.resolveHaxeClass().getGenericResolver());
      }
    }
    // anything else is resolved here (literals etc.)
    if (expressionType == null) {
      HaxeExpressionEvaluatorContext context = new HaxeExpressionEvaluatorContext(argument);
      HaxeGenericResolver genericResolver = HaxeGenericResolverUtil.generateResolverFromScopeParents(argument);
      expressionType = HaxeExpressionEvaluator.evaluate(argument, context, genericResolver.withoutUnknowns()).result;
    }

    // if expression is enumValue we need to resolve the underlying enumType type to test assignment
    if (expressionType != null && expressionType.getType() instanceof SpecificEnumValueReference type) {
      SpecificHaxeClassReference enumType = type.getEnumClass();
      expressionType = enumType.createHolder();
    }

    return expressionType;
  }

  private static ResultHolder resolveParameterType(HaxeParameterModel parameter, HaxeGenericResolver localResolver) {
    ResultHolder type = parameter.getType(localResolver.withoutUnknowns());
    // custom handling for macro based rest parameters (commonly used before we got native support for rest parameters)
    if (type.getClassType() != null) {
      SpecificHaxeClassReference classType = type.getClassType();
      @NotNull ResultHolder[] specifics = classType.getSpecifics();
      if (isExternRestClass(classType)) {
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


  private static HaxeGenericResolver findTypeParametersToInherit(SpecificTypeReference parameter,
                                                                 SpecificTypeReference argument,
                                                                 HaxeGenericResolver resolver, Map<String, ResultHolder> map) {

    HaxeGenericResolver inheritResolver = new HaxeGenericResolver();
    if (parameter instanceof SpecificHaxeClassReference parameterReference &&
        argument instanceof SpecificHaxeClassReference argumentReference) {
      HaxeGenericResolver paramResolver = parameterReference.getGenericResolver().addAll(resolver.withoutUnknowns());
      HaxeGenericResolver argResolver = argumentReference.getGenericResolver().addAll(resolver.withoutUnknowns());
      for (String name : paramResolver.names()) {
        ResultHolder resolve = paramResolver.resolve(name);
        if (resolve != null && resolve.isClassType()) {
          String className = resolve.getClassType().getClassName();

          if (className != null && map.containsKey(className)) {
            ResultHolder argResolved = argResolver.resolve(className);
            if (argResolved != null) {
              resolver.add(className, argResolved);
            }
          }
        }
      }
    }


    return inheritResolver;
  }


  private static ErrorRecord annotateTypeMismatch( ResultHolder expected, ResultHolder got, HaxeExpression expression) {
    String message = HaxeBundle.message("haxe.semantic.method.parameter.mismatch",
                                        expected.toPresentationString(),
                                        got.toPresentationString());
    return new ErrorRecord(expression.getTextRange(), message);
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

  private static long countRequiredFunctionTypeArguments(List<HaxeFunctionTypeParameterModel> parametersList) {
    return parametersList.stream()
      .filter(p -> !p.isOptional() && !p.getArgumentType().isVoid() && !p.isRestArgument())
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
              // haxe.extern.Rest<>
              return isExternRestClass(classType);
            }
          }
        }
      }
    }
    return false;
  }

  private static boolean isMacroExpr(SpecificHaxeClassReference classReference) {
    if (classReference.getHaxeClass() == null) return false;
    return classReference.getHaxeClass().getQualifiedName().equals("haxe.macro.Expr");
  }

  private static boolean isExternRestClass(SpecificHaxeClassReference classReference) {
    if (classReference.getHaxeClass() == null) return false;
    return classReference.getHaxeClass().getQualifiedName().equals("haxe.extern.Rest");
  }



  @Data
  public static class CallExpressionValidation {
    Map<Integer, Integer> argumentToParameterIndex = new HashMap<>();

    List<ErrorRecord> errors = new ArrayList<>();
    boolean completed = false;

    boolean isStaticExtension = false;
    boolean isConstructor = false;
    boolean isFunction = false;
    boolean isMethod = false;

  }
  public record ErrorRecord (TextRange range, String message){};


//TODO mlo move this to some kind of util (see haxeReferenceImpl)
  @NotNull
  private static HaxeGenericResolver tryGetCallieResolveResult(HaxeCallExpression callExpression) {
    final HaxeReference leftReference = PsiTreeUtil.getChildOfType(callExpression.getExpression(), HaxeReference.class);

    HaxeGenericResolver resolver = new HaxeGenericResolver();
    PsiElement resolve = leftReference == null ? null :leftReference.resolve();
    if (resolve != null) {
      ResultHolder evaluateResult = HaxeExpressionEvaluator.evaluate(resolve, new HaxeExpressionEvaluatorContext(resolve), null).result;
      if (evaluateResult.isClassType()) {
        resolver.addAll(evaluateResult.getClassType().getGenericResolver());
      }
    }
    return resolver;


  }

}
