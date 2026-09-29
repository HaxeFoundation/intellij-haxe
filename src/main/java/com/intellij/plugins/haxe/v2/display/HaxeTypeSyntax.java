package com.intellij.plugins.haxe.v2.display;

import com.intellij.plugins.haxe.display.protocol.JsonTypeRef;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import tools.jackson.databind.JsonNode;

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

  private HaxeTypeSyntax() {
  }

  @NotNull
  static String safeTypeText(@Nullable JsonTypeRef type) {
    if (type == null) return "Dynamic";
    return switch (type.kind()) {
      case "TInst", "TEnum", "TType", "TAbstract" -> classTypeText(type);
      case "TFun" -> functionTypeText(type);
      default -> "Dynamic";
    };
  }

  /** The parameter list of a TFun as a declaration writes it: {@code ?name:Type, ...}. */
  @NotNull
  static String parameterListText(@NotNull JsonTypeRef functionType) {
    List<String> parameters = new ArrayList<>();
    int index = 0;
    for (JsonNode argument : functionType.args().path("args")) {
      String name = argument.path("name").asString("");
      // parameter names must be identifiers - the compiler can emit odd ones for closures
      if (!name.matches("[A-Za-z_]\\w*")) {
        name = "arg" + index;
      }
      boolean optional = argument.path("opt").asBoolean(false);
      parameters.add((optional ? "?" : "") + name + ":" + safeTypeText(JsonTypeRef.of(argument.path("t"))));
      index++;
    }
    return String.join(", ", parameters);
  }

  /** The return type of a TFun; {@code Dynamic} for anything else. */
  @NotNull
  static String returnTypeText(@Nullable JsonTypeRef functionType) {
    if (functionType == null || !functionType.isFunction()) return "Dynamic";
    return safeTypeText(JsonTypeRef.of(functionType.args().path("ret")));
  }

  @NotNull
  private static String classTypeText(@NotNull JsonTypeRef type) {
    String dotPath = type.dotPath();
    // a plain dot path of identifiers - privates/natives can carry other shapes
    if (dotPath == null || !dotPath.matches("[A-Za-z_][\\w.]*")) return "Dynamic";
    List<String> parameters = new ArrayList<>();
    for (JsonNode param : type.args().path("params")) {
      parameters.add(safeTypeText(JsonTypeRef.of(param)));
    }
    return parameters.isEmpty() ? dotPath : dotPath + "<" + String.join(", ", parameters) + ">";
  }

  @NotNull
  private static String functionTypeText(@NotNull JsonTypeRef type) {
    String arguments = type.args().path("args").valueStream()
      .map(argument -> safeTypeText(JsonTypeRef.of(argument.path("t"))))
      .collect(Collectors.joining(", "));
    return "(" + arguments + ") -> " + returnTypeText(type);
  }
}
