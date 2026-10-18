package com.intellij.plugins.haxe.v2.display;

import com.intellij.plugins.haxe.display.protocol.JsonTypeRef;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Haxe type syntax rendered from the compiler's JSON types, for source the
 * plugin generates from them (blueprint declarations, missing-member stubs).
 * Anything not safely expressible degrades to {@code Dynamic} rather than
 * producing a declaration that fails to parse.
 */
final class HaxeTypeSyntax {

  // a Haxe identifier - the compiler can emit odd parameter names for closures
  private static final String IDENTIFIER = "[A-Za-z_]\\w*";
  // a plain dot path of identifiers - privates/natives can carry other shapes
  private static final String DOT_PATH = "[A-Za-z_][\\w.]*";

  private HaxeTypeSyntax() {
  }

  @NotNull
  static String safeTypeText(@Nullable JsonTypeRef type) {
    if (type == null) return "Dynamic";
    if (type.isNamedType()) return classTypeText(type);
    if (type.isFunction()) return functionTypeText(type);
    return "Dynamic";
  }

  /** The parameter list of a TFun as a declaration writes it: {@code ?name:Type, ...}. */
  @NotNull
  static String parameterListText(@NotNull JsonTypeRef functionType) {
    List<String> parameters = new ArrayList<>();
    int index = 0;
    for (JsonTypeRef.Argument argument : functionType.functionArguments()) {
      String name = argument.name();
      if (name == null || !name.matches(IDENTIFIER)) {
        name = "arg" + index;
      }
      parameters.add((argument.optional() ? "?" : "") + name + ":" + safeTypeText(argument.type()));
      index++;
    }
    return String.join(", ", parameters);
  }

  /** The return type of a TFun; {@code Dynamic} for anything else. */
  @NotNull
  static String returnTypeText(@Nullable JsonTypeRef functionType) {
    return safeTypeText(functionType == null ? null : functionType.returnType());
  }

  @NotNull
  private static String classTypeText(@NotNull JsonTypeRef type) {
    String dotPath = type.dotPath();
    if (dotPath == null || !dotPath.matches(DOT_PATH)) return "Dynamic";
    List<String> parameters = new ArrayList<>();
    for (JsonTypeRef parameter : type.typeParameters()) {
      parameters.add(safeTypeText(parameter));
    }
    return parameters.isEmpty() ? dotPath : dotPath + "<" + String.join(", ", parameters) + ">";
  }

  @NotNull
  private static String functionTypeText(@NotNull JsonTypeRef type) {
    String arguments = type.functionArguments().stream()
      .map(argument -> safeTypeText(argument.type()))
      .collect(Collectors.joining(", "));
    return "(" + arguments + ") -> " + returnTypeText(type);
  }
}
