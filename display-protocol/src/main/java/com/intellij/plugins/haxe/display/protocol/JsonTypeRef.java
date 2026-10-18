package com.intellij.plugins.haxe.display.protocol;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import tools.jackson.databind.JsonNode;

/**
 * A {@code haxe.display.JsonModuleTypes.JsonType}, kept as raw JSON. The
 * accessors cover the kinds the plugin renders and converts: named types with
 * their parameters, function types with their arguments, dot paths. The full
 * typedef family is large and recursive, so anything else is read from
 * {@link #args()} directly.
 */
public record JsonTypeRef(String kind, JsonNode args) {

  /** One parameter of a function type; {@code name} is null when the signature declares none. */
  public record Argument(String name, boolean optional, JsonTypeRef type) {
  }

  /** The kinds that name a declaration: class, enum, typedef, abstract. */
  private static final Set<String> NAMED_KINDS = Set.of("TInst", "TEnum", "TType", "TAbstract");

  public static JsonTypeRef of(JsonNode node) {
    if (node == null || node.isNull()) return null;
    return new JsonTypeRef(node.path("kind").asString(""), node.path("args"));
  }

  /** Whether the type names a declaration (TInst, TEnum, TType, TAbstract). */
  public boolean isNamedType() {
    return NAMED_KINDS.contains(kind);
  }

  public boolean isFunction() {
    return "TFun".equals(kind);
  }

  public boolean isDynamic() {
    return "TDynamic".equals(kind);
  }

  /** Whether the compiler has not bound the type (TMono). */
  public boolean isMonomorph() {
    return "TMono".equals(kind);
  }

  public boolean isAnonymous() {
    return "TAnonymous".equals(kind);
  }

  /** Whether this is the {@code Void} type. */
  public boolean isVoid() {
    return "Void".equals(dotPath());
  }

  /** The declared name of a named type without its package; null for other kinds and when the wire omits it. */
  public String typeName() {
    if (!isNamedType()) return null;
    String typeName = args.path("path").path("typeName").asString("");
    return typeName.isEmpty() ? null : typeName;
  }

  /** The type parameters of a named type in declaration order; empty for other kinds. */
  public List<JsonTypeRef> typeParameters() {
    return isNamedType() ? typesOf(args.path("params")) : List.of();
  }

  /** The parameters of a function type in declaration order; empty for other kinds. */
  public List<Argument> functionArguments() {
    if (!isFunction()) return List.of();
    List<Argument> arguments = new ArrayList<>();
    for (JsonNode argument : args.path("args")) {
      String name = argument.path("name").asString("");
      boolean optional = argument.path("opt").asBoolean(false);
      arguments.add(new Argument(name.isEmpty() ? null : name, optional, of(argument.path("t"))));
    }
    return arguments;
  }

  /** The return type of a function type; null for any other kind. */
  public JsonTypeRef returnType() {
    return isFunction() ? of(args.path("ret")) : null;
  }

  /** The type of a function type's parameter by name; null when the type is no function or has no such parameter. */
  public JsonTypeRef parameterType(String name) {
    for (Argument argument : functionArguments()) {
      if (name.equals(argument.name())) return argument.type();
    }
    return null;
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
   * {@code StdTypes.Void}. A module-private type is named like any other,
   * although the compiler files it under an extra package segment. Empty when
   * the type name is missing.
   */
  public static String qualifiedNameOf(JsonNode typePath) {
    String typeName = typePath.path("typeName").asString("");
    if (typeName.isEmpty()) return "";
    String moduleName = typePath.path("moduleName").asString(typeName);
    String moduleSegment = moduleName.equals(typeName) ? "" : moduleName + ".";
    return withoutPrivateSegment(packagePrefix(typePath), moduleName) + moduleSegment + typeName;
  }

  // the compiler files a module-private type under an extra last package segment, "_" plus the
  // module name (hxparse._ParserBuilderImpl.ParserCase); no real package looks like that, as a
  // package name must start lowercase after its underscores and a module name never does
  private static String withoutPrivateSegment(String packagePrefix, String moduleName) {
    String segment = "_" + moduleName + ".";
    boolean marked = packagePrefix.equals(segment) || packagePrefix.endsWith("." + segment);
    return marked ? packagePrefix.substring(0, packagePrefix.length() - segment.length()) : packagePrefix;
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
    if (isNamedType()) {
      String path = dotPath();
      return path != null ? path : "?";
    }
    if (isFunction()) return functionSignature();
    if (isDynamic()) return "Dynamic";
    if (isAnonymous()) return "{ ... }";
    return "?";
  }

  private String functionSignature() {
    String parameters = functionArguments().stream()
      .map(JsonTypeRef::argumentSignature)
      .collect(Collectors.joining(", "));
    JsonTypeRef ret = returnType();
    return "(" + parameters + ") -> " + (ret != null ? ret.presentable() : "?");
  }

  private static String argumentSignature(Argument argument) {
    String typeText = argument.type() != null ? argument.type().presentable() : "?";
    return argument.name() == null ? typeText : argument.name() + ":" + typeText;
  }

  private static List<JsonTypeRef> typesOf(JsonNode nodes) {
    List<JsonTypeRef> types = new ArrayList<>();
    for (JsonNode node : nodes) {
      types.add(of(node));
    }
    return types;
  }
}
