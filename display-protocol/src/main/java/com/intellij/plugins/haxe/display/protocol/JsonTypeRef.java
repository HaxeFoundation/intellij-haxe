package com.intellij.plugins.haxe.display.protocol;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import java.util.List;
import java.util.stream.Collectors;

/**
 * A {@code haxe.display.JsonModuleTypes.JsonType}: {@code {kind, args}},
 * where {@code args} has the shape of the {@code kind}. Each kind the plugin
 * renders and converts is a record; any other kind reads as an
 * {@link OtherType} that keeps only its kind.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "kind", visible = true, defaultImpl = JsonTypeRef.OtherType.class)
@JsonSubTypes({
  @JsonSubTypes.Type(value = JsonTypeRef.NamedType.class, names = {"TInst", "TEnum", "TType", "TAbstract"}),
  @JsonSubTypes.Type(value = JsonTypeRef.FunctionType.class, name = "TFun"),
  @JsonSubTypes.Type(value = JsonTypeRef.AnonymousType.class, name = "TAnonymous"),
  @JsonSubTypes.Type(value = JsonTypeRef.DynamicType.class, name = "TDynamic"),
  @JsonSubTypes.Type(value = JsonTypeRef.Monomorph.class, name = "TMono")})
public sealed interface JsonTypeRef {

  /** One parameter of a function type, as the wire names its parts; {@code name} is null when the signature declares none. */
  record Argument(String name, @JsonProperty("opt") boolean optional, @JsonProperty("t") JsonTypeRef type) {
    public Argument {
      name = name == null || name.isEmpty() ? null : name;
    }
  }

  /** The wire kind: TInst, TEnum, TType, TAbstract, TFun, TAnonymous, TDynamic, TMono, ... */
  String kind();

  /** A human-readable rendering: dot path, function signature, monomorph/dynamic placeholders. */
  String presentable();

  /** Whether the type names a declaration (TInst, TEnum, TType, TAbstract). */
  default boolean isNamedType() {
    return this instanceof NamedType;
  }

  /** Whether this is a function type (TFun). */
  default boolean isFunction() {
    return this instanceof FunctionType;
  }

  /** Whether this is {@code Dynamic} (TDynamic). */
  default boolean isDynamic() {
    return this instanceof DynamicType;
  }

  /** Whether the compiler has not bound the type (TMono). */
  default boolean isMonomorph() {
    return this instanceof Monomorph;
  }

  /** Whether this is an anonymous structure (TAnonymous). */
  default boolean isAnonymous() {
    return this instanceof AnonymousType;
  }

  /** Whether this is the {@code Void} type. */
  default boolean isVoid() {
    return "Void".equals(dotPath());
  }

  /** The declared name of a named type without its package; null for other kinds and when the wire omits it. */
  default String typeName() {
    return null;
  }

  /** The type parameters of a named type in declaration order; empty for other kinds. */
  default List<JsonTypeRef> typeParameters() {
    return List.of();
  }

  /** The parameters of a function type in declaration order; empty for other kinds. */
  default List<Argument> functionArguments() {
    return List.of();
  }

  /** The return type of a function type; null for any other kind. */
  default JsonTypeRef returnType() {
    return null;
  }

  /** The type of a function type's parameter by name; null when the type is no function or has no such parameter. */
  default JsonTypeRef parameterType(String name) {
    for (Argument argument : functionArguments()) {
      if (name.equals(argument.name())) return argument.type();
    }
    return null;
  }

  /** The dot path of a named type as Haxe prints it ({@code haxe.ds.StringMap}, {@code Void}); null for other kinds. */
  default String dotPath() {
    return null;
  }

  /** The {@link JsonTypePath#qualifiedName() qualified name} of a named type; empty for other kinds. */
  default String qualifiedName() {
    return "";
  }

  /** A class, enum, typedef or abstract, with its type parameters. */
  record NamedType(String kind, PathWithParams args) implements JsonTypeRef {

    /** The {@code args} of a named type: where it is declared and its parameters. */
    public record PathWithParams(JsonTypePath path, List<JsonTypeRef> params) {
    }

    @Override
    public String typeName() {
      String typeName = path().typeName();
      return typeName.isEmpty() ? null : typeName;
    }

    @Override
    public List<JsonTypeRef> typeParameters() {
      return args == null ? List.of() : args.params();
    }

    @Override
    public String dotPath() {
      return path().dotPath();
    }

    @Override
    public String qualifiedName() {
      return path().qualifiedName();
    }

    @Override
    public String presentable() {
      String dotPath = dotPath();
      return dotPath != null ? dotPath : "?";
    }

    /** Where the type is declared; {@link JsonTypePath#NONE} when the wire names nothing. */
    private JsonTypePath path() {
      return args == null || args.path() == null ? JsonTypePath.NONE : args.path();
    }
  }

  /** A function type (TFun): its parameters and return type. */
  record FunctionType(Signature args) implements JsonTypeRef {

    /** The {@code args} of a function type: its arguments and return type. */
    public record Signature(List<Argument> args, JsonTypeRef ret) {
    }

    @Override
    public String kind() {
      return "TFun";
    }

    @Override
    public List<Argument> functionArguments() {
      return args == null ? List.of() : args.args();
    }

    @Override
    public JsonTypeRef returnType() {
      return args == null ? null : args.ret();
    }

    @Override
    public String presentable() {
      String parameters = functionArguments().stream()
        .map(FunctionType::argumentSignature)
        .collect(Collectors.joining(", "));
      JsonTypeRef ret = returnType();
      return "(" + parameters + ") -> " + (ret != null ? ret.presentable() : "?");
    }

    private static String argumentSignature(Argument argument) {
      String typeText = argument.type() != null ? argument.type().presentable() : "?";
      return argument.name() == null ? typeText : argument.name() + ":" + typeText;
    }
  }

  /** An anonymous structure (TAnonymous): its fields and their types. */
  record AnonymousType(Fields args) implements JsonTypeRef {

    /** The {@code args} of an anonymous structure: its fields. */
    public record Fields(List<Field> fields) {
    }

    /** One field of an anonymous structure. */
    public record Field(String name, JsonTypeRef type) {
    }

    @Override
    public String kind() {
      return "TAnonymous";
    }

    /** The structure's fields in declaration order. */
    public List<Field> fields() {
      return args == null ? List.of() : args.fields();
    }

    @Override
    public String presentable() {
      return "{ ... }";
    }
  }

  /** {@code Dynamic}; {@code args} is the {@code T} of {@code Dynamic<T>}, null for a plain {@code Dynamic}. */
  record DynamicType(JsonTypeRef args) implements JsonTypeRef {

    @Override
    public String kind() {
      return "TDynamic";
    }

    @Override
    public String presentable() {
      return "Dynamic";
    }
  }

  /** A type the compiler has not bound yet. */
  record Monomorph() implements JsonTypeRef {

    @Override
    public String kind() {
      return "TMono";
    }

    @Override
    public String presentable() {
      return "?";
    }
  }

  /** A kind the plugin does not read, such as TLazy. */
  record OtherType(String kind) implements JsonTypeRef {

    @Override
    public String presentable() {
      return "?";
    }
  }
}
