/*
 * Copyright 2000-2013 JetBrains s.r.o.
 * Copyright 2014-2015 AS3Boyan
 * Copyright 2014-2014 Elias Ku
 * Copyright 2018 Ilya Malanin
 * Copyright 2019 Eric Bishton
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

import com.intellij.plugins.haxe.lang.psi.*;
import com.intellij.plugins.haxe.model.HaxeMethodModel;
import com.intellij.plugins.haxe.model.HaxeParameterModel;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import static com.intellij.plugins.haxe.model.type.HaxeTypeResolver.getTypeFromTypeOrAnonymous;

//TODO mlo: consider spliting this class into 2, one for method references and one for functionType signatures.
//should probably also create a model for HaxeFunctionType psi.

public class SpecificFunctionReference extends SpecificTypeReference {
  private static final String DELIMITER = "->";

  @Nullable
  public HaxeResolveResult asResolveResult() {
    return functionType != null ? HaxeResolveResult.create(functionType) : null;
  }

  public boolean containsUnknownTypes() {
    for (HaxeArgument argument : getArguments()) {
      ResultHolder argumentType = argument.getType();
      if(argumentType.isUnknown() || argumentType.containsUnknownTypes()) return true;
    }

    ResultHolder type = getReturnType();
    return type.isUnknown() || type.containsUnknownTypes();
  }

  public SpecificFunctionReference performMethodBind(List<HaxeArgument> newArgumentList) {
    if(functionType != null) {
      return new SpecificFunctionReference(newArgumentList, returnValue,functionType,context);
    }else {
    return  new SpecificFunctionReference(newArgumentList, returnValue,method,context);
    }
  }


  public static class StdFunctionReference extends SpecificFunctionReference {
    public StdFunctionReference(@NotNull PsiElement context) {
      super(new ArrayList<HaxeArgument>(), SpecificTypeReference.getDynamic(context).createHolder(), (HaxeMethodModel)null, context);
    }
  }


  final public List<HaxeArgument> arguments;
  final public ResultHolder returnValue;

  @Nullable final public HaxeMethodModel method;

  @Nullable final public HaxeFunctionType functionType;
  @Nullable final public Object constantValue;

  public SpecificFunctionReference(List<HaxeArgument> arguments,
                                   @NotNull ResultHolder returnValue,
                                   @Nullable HaxeMethodModel method,
                                   @NotNull PsiElement context,
                                   @Nullable Object constantValue) {
    super(context);

    this.arguments = arguments;
    this.returnValue = returnValue;
    this.method = method;
    this.constantValue = constantValue;
    this.functionType = null;
  }

  public SpecificFunctionReference(List<HaxeArgument> arguments,
                                   @NotNull ResultHolder returnValue,
                                   @Nullable HaxeMethodModel method,
                                   @NotNull PsiElement context) {
    this(arguments, returnValue, method, context, null);
  }

  public SpecificFunctionReference(List<HaxeArgument> arguments,
                                   @NotNull ResultHolder returnValue,
                                   @Nullable HaxeFunctionType functionType,
                                   @NotNull PsiElement context) {
    super(context);

    this.arguments = arguments;
    this.returnValue = returnValue;
    this.method = null;
    this.constantValue = null;
    this.functionType = functionType;
  }

  @Override
  public List<ResultHolder> getTypeParameters() {
    List<ResultHolder> genericsTypes = new ArrayList<>();
      for (HaxeArgument argument : arguments) {
        ResultHolder holder = argument.getType();
        if (holder.isTypeParameter()) {
          genericsTypes.add(holder);
        }else {
          genericsTypes.addAll(holder.getType().getTypeParameters());
          }
        }
      if (returnValue.isTypeParameter()) {
        genericsTypes.add(returnValue);
      }else {
        genericsTypes.addAll(returnValue.getType().getTypeParameters());
      }
      return genericsTypes;
    }

  @Override
  public PsiElement getTypePsi() {
    return method == null ? functionType : method.getMethodPsi();
  }

  public static SpecificFunctionReference create(HaxeMethodModel model) {
    LinkedList<HaxeArgument> args = new LinkedList<>();
    List<HaxeParameterModel> parameters = model.getParameters();
    if (parameters.isEmpty()) {
      HaxeMethodPsiMixin methodPsi = model.getMethodPsi();
      SpecificTypeReference voidArg = SpecificTypeReference.getVoid(methodPsi);
      args.add(new HaxeArgument(methodPsi,0, false, false, voidArg.createHolder(), voidArg.toStringWithoutConstant()));
    } else {
      for (int i = 0; i < parameters.size(); i++) {
        HaxeParameterModel parameterModel = parameters.get(i);
        args.add(new HaxeArgument(parameterModel.getParameterPsi(), i, parameterModel.isOptional(), parameterModel.isRest(), parameterModel.getResultType(), parameterModel.getName()));
      }
    }
    return new SpecificFunctionReference(args, model.getReturnType(null), model, model.getMethodPsi());
  }

  // This is an adapter to deal with the function-type mismatch between the old resolver
  // and the models.
  // TODO: Technical debt: Need to unify the resolver and the models.
  public static SpecificFunctionReference create(HaxeSpecificFunction func) {
    if (null == func) return null;
    // this is a workaround for missing optional support (fn(arg = null))
    // this problem might go away when the todo on this method is solved?
    if (func.getMethod() != null && func.getMethod() instanceof  HaxeMethodDeclaration) {
      return create(func.getMethod().getModel());
    }

    HaxeGenericSpecialization specialization = func.getSpecialization();
    HaxeGenericResolver resolver = specialization.toGenericResolver(func);

    LinkedList<HaxeArgument> args = new LinkedList<>();
    List<HaxeFunctionArgument> arguments = func.getFunctionArgumentList();
    if (arguments.isEmpty()) {
      SpecificTypeReference voidArg = SpecificTypeReference.getVoid((func));
      args.add(new HaxeArgument(func,0, false, false, voidArg.createHolder(), voidArg.toStringWithoutConstant()));
    } else {
      for (int i = 0; i < arguments.size(); i++) {
        HaxeFunctionArgument arg = arguments.get(i);
        ResultHolder result = determineType(func, resolver, arg.getFunctionType(), arg.getTypeOrAnonymous());
        args.add(new HaxeArgument(arg, i, null != arg.getOptionalMark(), null != arg.getRestArgumentType(), result, arg.getName()));
      }
    }

    HaxeFunctionReturnType returnType = func.getFunctionReturnType();
    // TODO?: Infer the return type if there is no type tag?
    ResultHolder returnResult = returnType != null
                                ? determineType(func, resolver, returnType.getFunctionType(), returnType.getTypeOrAnonymous())
                                : determineType(func, resolver, null, null);

    return new SpecificFunctionReference(args, returnResult, func, func);
  }

  private static ResultHolder determineType(PsiElement context, HaxeGenericResolver resolver, HaxeFunctionType fnType, HaxeTypeOrAnonymous toa) {
    if (null != toa) {
      ResultHolder result = getTypeFromTypeOrAnonymous(toa);
      if (null != result.getClassType()) {
        return SpecificHaxeClassReference.propagateGenericsToType(result, resolver);
      }
      return result;
    }
    if (null != fnType) {
      return create(new HaxeSpecificFunction(fnType, resolver.getSpecialization(context))).createHolder();
    }
    return SpecificTypeReference.getUnknown(context).createHolder();
  }


  @Override
  public SpecificFunctionReference withConstantValue(Object constantValue) {
    if (method != null) {
      return new SpecificFunctionReference(arguments, returnValue, method, context, constantValue);
    }else {
      return new SpecificFunctionReference(arguments, returnValue, functionType, context);
    }
  }

  @Override
  public Object getConstant() {
    return constantValue;
  }

  public int getNonOptionalArgumentsCount() {
    if (arguments.isEmpty()) return 0;

    return (int)arguments.stream()
      .filter(argument -> !argument.isOptional())
      .count();
  }

  public List<HaxeArgument> getArguments() {
    return arguments;
  }

  public ResultHolder getReturnType() {
    return returnValue;
  }

  public static String toFunctionDescription(boolean presentable, List<HaxeArgument> arguments, ResultHolder returnValue) {
    StringBuilder out = new StringBuilder();

    final boolean notSingleArgument = arguments.size() > 1;
    if (notSingleArgument) out.append('(');
    for (int n = 0; n < arguments.size(); n++) {
      if (n > 0) out.append(", ");
      HaxeArgument argument = arguments.get(n);
      out.append(argument.toStringWithoutConstant());
    }
    if (arguments.isEmpty() && presentable) {
      out.append("Void");
    }
    if (notSingleArgument) out.append(')');

    out.append(DELIMITER);
    out.append(null != returnValue ? returnValue.toStringWithoutConstant() : "unknown");

    return out.toString();
  }

  public String toPresentationString(boolean showOnlyConstraintForTypeParam) {
    return toFunctionDescription(true, arguments, returnValue);
  }

  @Override
  public String toString() {
    return toFunctionDescription(false, arguments, returnValue);
  }

  @Override
  public String toStringWithoutConstant() {
    return toPresentationString(false);
  }

  @Override
  public String toStringWithConstant() {
    return toPresentationString(false); // XXX: If there's an anonymous function, should we be adding it here?
  }

  public SpecificFunctionReference withTypes(@NotNull List<HaxeArgument> newArgs, @NotNull ResultHolder newReturnType) {
    return new SpecificFunctionReference( newArgs, newReturnType, method, context, constantValue) ;
  }

  @Override
  public SpecificTypeReference withElementContext(PsiElement element) {
    return new SpecificFunctionReference( arguments, returnValue, method, element, constantValue) ;
  }
}

