package com.intellij.plugins.haxe.v2.display;

import com.intellij.plugins.haxe.display.protocol.JsonTypeRef;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import tools.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.List;

/**
 * The inlay text of a compiler type, in parts: type names without their
 * package, type parameters in angle brackets, a function as {@code (A, B)->R}
 * like the IDE's own hints, an anonymous structure with its fields, and
 * {@code ?} where the compiler has not bound a type. A part that is a type
 * name carries the wire type, so the inlay can navigate to its declaration.
 * A bare unbound type gives no text, since there is nothing to show.
 */
final class HaxeInlayTypeText {

  /** One piece of the text; {@code namedType} is set on a type name, null on punctuation and unbound types. */
  record Part(@NotNull String text, @Nullable JsonTypeRef namedType) {
  }

  private final List<Part> parts;

  private HaxeInlayTypeText(@NotNull List<Part> parts) {
    this.parts = List.copyOf(parts);
  }

  @Nullable
  static HaxeInlayTypeText of(@Nullable JsonTypeRef type) {
    if (type == null || type.isMonomorph()) return null;
    List<Part> parts = new ArrayList<>();
    render(type, parts);
    return new HaxeInlayTypeText(parts);
  }

  /** A text with no navigation, as tests seed it. */
  @NotNull
  static HaxeInlayTypeText plain(@NotNull String text) {
    return new HaxeInlayTypeText(List.of(new Part(text, null)));
  }

  @NotNull
  List<Part> parts() {
    return parts;
  }

  @NotNull
  String text() {
    StringBuilder text = new StringBuilder();
    for (Part part : parts) {
      text.append(part.text());
    }
    return text.toString();
  }

  @Override
  public String toString() {
    return text();
  }

  private static void render(@Nullable JsonTypeRef type, @NotNull List<Part> parts) {
    if (type == null) {
      parts.add(plainPart("?"));
      return;
    }
    if (type.isNamedType()) named(type, parts);
    else if (type.isFunction()) function(type, parts);
    else if (type.isDynamic()) dynamic(type, parts);
    else if (type.isAnonymous()) anonymous(type, parts);
    else parts.add(plainPart("?"));
  }

  private static void named(@NotNull JsonTypeRef type, @NotNull List<Part> parts) {
    String name = type.typeName();
    parts.add(new Part(name != null ? name : "?", type));
    List<JsonTypeRef> parameters = type.typeParameters();
    if (parameters.isEmpty()) return;
    parts.add(plainPart("<"));
    renderList(parameters, parts);
    parts.add(plainPart(">"));
  }

  private static void function(@NotNull JsonTypeRef type, @NotNull List<Part> parts) {
    parts.add(plainPart("("));
    List<JsonTypeRef> arguments = type.functionArguments().stream().map(JsonTypeRef.Argument::type).toList();
    renderList(arguments, parts);
    parts.add(plainPart(")->"));
    render(type.returnType(), parts);
  }

  /** {@code Dynamic}, or {@code Dynamic<T>} when the compiler's args carry the parameter type. */
  private static void dynamic(@NotNull JsonTypeRef type, @NotNull List<Part> parts) {
    parts.add(plainPart("Dynamic"));
    JsonNode args = type.args();
    if (args == null || args.isMissingNode() || args.isNull()) return;
    parts.add(plainPart("<"));
    render(JsonTypeRef.of(args), parts);
    parts.add(plainPart(">"));
  }

  private static void anonymous(@NotNull JsonTypeRef type, @NotNull List<Part> parts) {
    parts.add(plainPart("{"));
    boolean first = true;
    for (JsonNode field : type.args().path("fields")) {
      if (!first) parts.add(plainPart(", "));
      first = false;
      parts.add(plainPart(field.path("name").asString("?") + ":"));
      render(JsonTypeRef.of(field.path("type")), parts);
    }
    parts.add(plainPart("}"));
  }

  /** The types, comma-separated. */
  private static void renderList(@NotNull List<JsonTypeRef> types, @NotNull List<Part> parts) {
    for (int i = 0; i < types.size(); i++) {
      if (i > 0) parts.add(plainPart(", "));
      render(types.get(i), parts);
    }
  }

  @NotNull
  private static Part plainPart(@NotNull String text) {
    return new Part(text, null);
  }
}
