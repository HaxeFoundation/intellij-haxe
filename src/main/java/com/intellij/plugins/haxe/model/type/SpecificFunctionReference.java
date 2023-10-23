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

public class SpecificFunctionReference extends SpecificTypeReference {
  private static final String DELIMITER = "->";

  public static class StdFunctionReference extends SpecificFunctionReference {
    public StdFunctionReference(@NotNull PsiElement context) {
      super(new ArrayList<Argument>(), SpecificTypeReference.getDynamic(context).createHolder(), (HaxeMethodModel)null, context);
    }
  }


  final public List<Argument> arguments;
  final public ResultHolder returnValue;

  @Nullable final public HaxeMethodModel method;

  @Nullable final public HaxeFunctionType functionType;
  @Nullable final public Object constantValue;

  public SpecificFunctionReference(List<Argument> arguments,
                                   ResultHolder returnValue,
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

  public SpecificFunctionReference(List<Argument> arguments,
                                   ResultHolder returnValue,
                                   @Nullable HaxeMethodModel method,
                                   @NotNull PsiElement context) {
    this(arguments, returnValue, method, context, null);
  }

  public SpecificFunctionReference(List<Argument> arguments,
                                   ResultHolder returnValue,
                                   @Nullable HaxeFunctionType functionType,
                                   @NotNull PsiElement context) {
    super(context);

    this.arguments = arguments;
    this.returnValue = returnValue;
    this.method = null;
    this.constantValue = null;
    this.functionType = functionType;
  }

  public static SpecificFunctionReference create(HaxeMethodModel model) {
    LinkedList<Argument> args = new LinkedList<>();
    List<HaxeParameterModel> parameters = model.getParameters();
    if (parameters.isEmpty()) {
      SpecificTypeReference voidArg = SpecificTypeReference.getVoid((model.getMethodPsi()));
      args.add(new Argument(0, false, voidArg.createHolder(), voidArg.toStringWithoutConstant()));
    } else {
      for (int i = 0; i < parameters.size(); i++) {
        HaxeParameterModel parameterModel = parameters.get(i);
        args.add(new Argument(i, parameterModel.isOptional(), parameterModel.getResultType(), parameterModel.getName()));
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

    LinkedList<Argument> args = new LinkedList<>();
    List<HaxeFunctionArgument> arguments = func.getFunctionArgumentList();
    if (arguments.size() == 0) {
      SpecificTypeReference voidArg = SpecificTypeReference.getVoid((func));
      args.add(new Argument(0, false, voidArg.createHolder(), voidArg.toStringWithoutConstant()));
    } else {
      for (int i = 0; i < arguments.size(); i++) {
        HaxeFunctionArgument arg = arguments.get(i);
        ResultHolder result = determineType(func, resolver, arg.getFunctionType(), arg.getTypeOrAnonymous());
        args.add(new Argument(i, null != arg.getOptionalMark(), result, arg.getName()));
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
        return SpecificHaxeClassReference.propagateGenericsToType(result.getClassType(), resolver).createHolder();
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
    return new SpecificFunctionReference(arguments, returnValue, method, context, constantValue);
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

  public List<Argument> getArguments() {
    return arguments;
  }

  public ResultHolder getReturnType() {
    return returnValue;
  }

  public static String toFunctionDescription(boolean presentable, List<Argument> arguments, ResultHolder returnValue) {
    StringBuilder out = new StringBuilder();

    final boolean notSingleArgument = arguments.size() > 1;
    if (notSingleArgument) out.append('(');
    for (int n = 0; n < arguments.size(); n++) {
      if (n > 0) out.append(", ");
      Argument argument = arguments.get(n);
      out.append(argument.toStringWithoutConstant());
    }
    if (0 == arguments.size() && presentable) {
      out.append("Void");
    }
    if (notSingleArgument) out.append(')');

    out.append(DELIMITER);
    out.append(null != returnValue ? returnValue.toStringWithoutConstant() : "unknown");

    return out.toString();
  }

  public String toPresentationString() {
    return toFunctionDescription(true, arguments, returnValue);
  }

  @Override
  public String toString() {
    return toFunctionDescription(false, arguments, returnValue);
  }

  @Override
  public String toStringWithoutConstant() {
    return toPresentationString();
  }

  @Override
  public String toStringWithConstant() {
    return toPresentationString(); // XXX: If there's an anonymous function, should we be adding it here?
  }

  public static class Argument {
    final private int index;
    final private boolean optional;
    final private String name;
    final private ResultHolder type;

    public Argument(int index, boolean optional, ResultHolder type, @Nullable String name) {
      this.index = index;
      this.optional = optional;
      this.name = name;
      this.type = type;
    }

    public boolean isOptional() {
      return optional;
    }

    public int getIndex() {
      return index;
    }

    public String getName() {
      return name;
    }

    public boolean hasName() {
      return name != null;
    }

    public ResultHolder getType() {
      return type;
    }

    public String toString() {
      return buildStringRepresentation(true);
    }

    public String toStringWithoutConstant() {
      return buildStringRepresentation(false);
    }

    @NotNull
    private String buildStringRepresentation(final boolean withConstantValue) {
      StringBuilder builder = new StringBuilder();
      if (isOptional()) builder.append('?');
      if (withConstantValue && hasName()) {
        builder.append(getName());
        builder.append(':');
      }

      if (withConstantValue) {
        builder.append(type.toString());
      } else {
        builder.append(type.toStringWithoutConstant());
      }

      return builder.toString();
    }

    public boolean isVoid() {
      return type.getType().isVoid();
    }

    public boolean isInvalid() {
      return type.getType().isInvalid();
    }

    public boolean canAssignToFrom(Argument argument) {
      // TO can accept optional but not the other way around.
      // if TO has optional argument and  FROM does not then the assignment should fail.
      return (argument.isOptional() || this.isOptional() == argument.isOptional()) && type.canAssign(argument.type);
    }

    public Argument changeType(ResultHolder newType) {
      return new Argument(this.index, this.optional, newType, this.name);
    }
  }
}

