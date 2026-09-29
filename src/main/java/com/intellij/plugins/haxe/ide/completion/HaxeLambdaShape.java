package com.intellij.plugins.haxe.ide.completion;

import com.intellij.plugins.haxe.display.protocol.JsonTypeRef;
import com.intellij.plugins.haxe.model.type.HaxeArgument;
import com.intellij.plugins.haxe.model.type.ResultHolder;
import com.intellij.plugins.haxe.model.type.SpecificFunctionReference;
import com.intellij.plugins.haxe.model.type.SpecificHaxeClassReference;
import com.intellij.plugins.haxe.model.type.SpecificTypeReference;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import tools.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.List;

/**
 * The shape of the function a completion position expects: its parameters,
 * and whether it returns Void, which decides whether a function literal's
 * body starts with {@code return}. It is built from the static type model
 * in the IDE completion modes and from the compiler's expected type in the
 * compiler-only mode; the lookups read only this record, so both sources
 * give the same suggestions. Parameter names are not chosen here; the name
 * suggester derives them from what each parameter records.
 */
public record HaxeLambdaShape(@NotNull List<Parameter> parameters, boolean returnsVoid) {

  /** One expected parameter: the name its signature declares, if any, the simple name of its type, and whether that type is a function. */
  public record Parameter(@Nullable String declaredName, @Nullable String typeName, boolean isFunction) {
  }

  /** The shape of the compiler's expected type, or null when it is not a function. */
  @Nullable
  public static HaxeLambdaShape fromCompiler(@Nullable JsonTypeRef expected) {
    if (expected == null || !expected.isFunction()) return null;
    List<Parameter> parameters = new ArrayList<>();
    for (JsonNode argument : expected.args().path("args")) {
      JsonTypeRef type = JsonTypeRef.of(argument.path("t"));
      parameters.add(new Parameter(argument.path("name").asString(""), simpleNameOf(type), type != null && type.isFunction()));
    }
    JsonTypeRef returnType = JsonTypeRef.of(expected.args().path("ret"));
    return new HaxeLambdaShape(List.copyOf(parameters), returnType != null && "Void".equals(returnType.dotPath()));
  }

  /** The shape of a statically resolved expected type, typedefs followed, or null when it is not a function. */
  @Nullable
  public static HaxeLambdaShape fromStatic(@Nullable ResultHolder expected) {
    SpecificFunctionReference function = expected == null ? null : functionTypeOf(expected);
    if (function == null) return null;
    List<Parameter> parameters = new ArrayList<>();
    for (HaxeArgument argument : function.getArguments()) {
      // a parameterless function type is modelled with one Void argument
      if (argument.isVoid()) continue;
      ResultHolder type = argument.getType();
      SpecificHaxeClassReference classType = type.getClassType();
      String typeName = classType == null ? null : classType.getClassName();
      parameters.add(new Parameter(argument.getName(), typeName, type.isFunctionType()));
    }
    return new HaxeLambdaShape(List.copyOf(parameters), function.getReturnType().isVoid());
  }

  @Nullable
  private static SpecificFunctionReference functionTypeOf(@NotNull ResultHolder expected) {
    SpecificFunctionReference direct = expected.getFunctionType();
    if (direct != null) return direct;
    SpecificHaxeClassReference classType = expected.getClassType();
    if (classType == null) return null;
    SpecificTypeReference unwrapped = classType.fullyResolveTypeDefAndUnwrapNullTypeReference();
    return unwrapped instanceof SpecificFunctionReference function ? function : null;
  }

  /** The last segment of a wire type's dot path, or null for a type without one. */
  @Nullable
  private static String simpleNameOf(@Nullable JsonTypeRef type) {
    String dotPath = type == null ? null : type.dotPath();
    return dotPath == null ? null : dotPath.substring(dotPath.lastIndexOf('.') + 1);
  }
}
