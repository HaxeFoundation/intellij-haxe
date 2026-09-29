package com.intellij.plugins.haxe.display.protocol;

import java.util.stream.Collectors;
import tools.jackson.databind.JsonNode;

/**
 * A {@code haxe.display.JsonModuleTypes.JsonType}, kept as raw JSON. The
 * accessors cover what the plugin reads: dot paths and readable signatures.
 * The full typedef family is large and recursive, so anything else is read
 * from {@link #args()} directly.
 */
public record JsonTypeRef(String kind, JsonNode args) {

  public static JsonTypeRef of(JsonNode node) {
    if (node == null || node.isNull()) return null;
    return new JsonTypeRef(node.path("kind").asString(""), node.path("args"));
  }

  public boolean isFunction() {
    return "TFun".equals(kind);
  }

  /**
   * The dot path of a TInst, TEnum, TType or TAbstract as Haxe prints it: the
   * package plus the type name ({@code haxe.ds.StringMap}, {@code Void}).
   * Null for other kinds.
   */
  public String dotPath() {
    JsonNode path = args.path("path");
    if (path.isMissingNode()) return null;
    String typeName = path.path("typeName").asString("");
    if (typeName.isEmpty()) return null;
    return packagePrefix(path) + typeName;
  }

  /**
   * The qualified name of a wire {@code JsonTypePath} ({@code pack},
   * {@code moduleName}, {@code typeName}), in the form the plugin's
   * class-name indexes use. A module's main type is {@code pack.Module}, and
   * any other type the module declares is {@code pack.Module.SubType}. For
   * example, {@code Void} is declared in {@code StdTypes}, so its name is
   * {@code StdTypes.Void}. Empty when the type name is missing.
   */
  public static String qualifiedNameOf(JsonNode typePath) {
    String typeName = typePath.path("typeName").asString("");
    if (typeName.isEmpty()) return "";
    String moduleName = typePath.path("moduleName").asString(typeName);
    String moduleSegment = moduleName.equals(typeName) ? "" : moduleName + ".";
    return packagePrefix(typePath) + moduleSegment + typeName;
  }

  /** The {@code pack} segments of a type path, each followed by a dot. */
  private static String packagePrefix(JsonNode typePath) {
    StringBuilder prefix = new StringBuilder();
    for (JsonNode pack : typePath.path("pack")) {
      prefix.append(pack.asString("")).append('.');
    }
    return prefix.toString();
  }

  /** A human-readable rendering: dot path, function signature, monomorph/dynamic placeholders. */
  public String presentable() {
    return switch (kind) {
      case "TInst", "TEnum", "TType", "TAbstract" -> {
        String path = dotPath();
        yield path != null ? path : "?";
      }
      case "TFun" -> functionSignature();
      case "TDynamic" -> "Dynamic";
      case "TMono" -> "?";
      case "TAnonymous" -> "{ ... }";
      default -> "?";
    };
  }

  private String functionSignature() {
    String parameters = args.path("args").valueStream()
      .map(JsonTypeRef::argumentSignature)
      .collect(Collectors.joining(", "));
    JsonTypeRef ret = of(args.path("ret"));
    return "(" + parameters + ") -> " + (ret != null ? ret.presentable() : "?");
  }

  private static String argumentSignature(JsonNode argument) {
    String name = argument.path("name").asString("");
    JsonTypeRef type = of(argument.path("t"));
    String typeText = type != null ? type.presentable() : "?";
    return name.isEmpty() ? typeText : name + ":" + typeText;
  }
}
