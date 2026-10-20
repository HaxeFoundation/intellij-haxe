package com.intellij.plugins.haxe.display.haxe3;

import com.intellij.plugins.haxe.display.protocol.JsonTypePath;
import com.intellij.plugins.haxe.display.protocol.JsonTypeRef;
import com.intellij.plugins.haxe.display.protocol.JsonTypeRef.AnonymousType;
import com.intellij.plugins.haxe.display.protocol.JsonTypeRef.Argument;
import com.intellij.plugins.haxe.display.protocol.JsonTypeRef.DynamicType;
import com.intellij.plugins.haxe.display.protocol.JsonTypeRef.FunctionType;
import com.intellij.plugins.haxe.display.protocol.JsonTypeRef.Monomorph;
import com.intellij.plugins.haxe.display.protocol.JsonTypeRef.NamedType;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Turns a type as haxe 3 prints it into the {@link JsonTypeRef} haxe 4
 * sends, so its consumers need no second form. Haxe 3 prints a function as
 * its arguments and return type joined by {@code ->}, an argument as
 * {@code [?]name : Type} (named or not, a function argument in parentheses),
 * a function without arguments as {@code Void -> T}, an unbound type as
 * {@code Unknown<n>} and an anonymous structure as {@code { name : Type, ... }}.
 *
 * A printed named type does not say whether it is a class, an enum, a
 * typedef or an abstract; it reads as a class ({@code TInst}), which every
 * consumer treats like the others. Its module is its own name, except for the
 * types the std {@code StdTypes} module declares.
 */
final class Haxe3Types {

  /** The types the std StdTypes module declares, which haxe 4 names with that module. */
  private static final Set<String> STD_TYPES_MODULE_TYPES =
    Set.of("Void", "Float", "Int", "Null", "Bool", "Dynamic", "Iterator", "Iterable", "ArrayAccess");
  private static final String STD_TYPES_MODULE = "StdTypes";
  private static final String DYNAMIC = "Dynamic";
  private static final String UNKNOWN = "Unknown";
  private static final String VOID = "Void";

  private final String text;
  private int index;

  private Haxe3Types(String text) {
    this.text = text;
  }

  /** The type the text prints; null when it is empty or not a type. */
  static JsonTypeRef parse(String printed) {
    if (printed == null || printed.isBlank()) return null;
    Haxe3Types parser = new Haxe3Types(printed.strip());
    JsonTypeRef type = parser.type();
    parser.skipWhitespace();
    return parser.index < parser.text.length() ? null : type;
  }

  /** A function type, or the single type it would be without arrows. */
  private JsonTypeRef type() {
    List<Argument> parts = new ArrayList<>();
    Argument part = argument();
    while (part != null) {
      parts.add(part);
      skipWhitespace();
      if (!text.startsWith("->", index)) break;
      index += 2;
      part = argument();
    }
    if (part == null) return null;
    if (parts.size() == 1) return parts.getFirst().type();
    return functionType(parts);
  }

  /** One {@code [?][name :] Type} part of a function type. */
  private Argument argument() {
    skipWhitespace();
    boolean optional = consume('?');
    String name = argumentName();
    JsonTypeRef type = primary();
    return type == null ? null : new Argument(name, optional, type);
  }

  /** The name before an argument's {@code :}; empty for an unnamed argument, whose type is then still ahead. */
  private String argumentName() {
    int start = index;
    String name = identifier();
    skipWhitespace();
    if (!name.isEmpty() && consume(':')) return name;
    index = start;
    return "";
  }

  private JsonTypeRef primary() {
    skipWhitespace();
    if (consume('(')) {
      JsonTypeRef inner = type();
      skipWhitespace();
      return consume(')') ? inner : null;
    }
    if (consume('{')) return anonymousType();
    return namedType();
  }

  private JsonTypeRef namedType() {
    List<String> pack = new ArrayList<>();
    String name = identifier();
    if (name.isEmpty()) return null;
    while (consume('.')) {
      pack.add(name);
      name = identifier();
      if (name.isEmpty()) return null;
    }
    List<JsonTypeRef> parameters = typeParameters();
    if (parameters == null) return null;
    if (pack.isEmpty() && name.equals(UNKNOWN)) return new Monomorph();
    if (pack.isEmpty() && name.equals(DYNAMIC)) return new DynamicType(null);
    return named(pack, name, parameters);
  }

  /** The {@code <A, B>} after a type name; empty without one, null when it does not close. */
  private List<JsonTypeRef> typeParameters() {
    List<JsonTypeRef> parameters = new ArrayList<>();
    skipWhitespace();
    if (!consume('<')) return parameters;
    do {
      skipWhitespace();
      // an unbound type prints its number: Unknown<0>
      if (Character.isDigit(peek())) {
        identifier();
        continue;
      }
      JsonTypeRef parameter = type();
      if (parameter == null) return null;
      parameters.add(parameter);
      skipWhitespace();
    } while (consume(','));
    skipWhitespace();
    return consume('>') ? parameters : null;
  }

  /** The fields after an anonymous structure's opening brace, up to its closing one. */
  private JsonTypeRef anonymousType() {
    List<AnonymousType.Field> fields = new ArrayList<>();
    skipWhitespace();
    if (!consume('}')) {
      do {
        skipWhitespace();
        consume('?');
        String name = identifier();
        skipWhitespace();
        if (name.isEmpty() || !consume(':')) return null;
        JsonTypeRef type = type();
        if (type == null) return null;
        fields.add(new AnonymousType.Field(name, type));
        skipWhitespace();
      } while (consume(','));
      skipWhitespace();
      if (!consume('}')) return null;
    }
    return new AnonymousType(new AnonymousType.Fields(List.copyOf(fields)));
  }

  /** The function type of the parts: every part but the last is an argument, the last is the return type. */
  private static JsonTypeRef functionType(List<Argument> parts) {
    List<Argument> arguments = parts.subList(0, parts.size() - 1);
    JsonTypeRef returned = parts.getLast().type();
    List<Argument> declared = isNoArguments(arguments) ? List.of() : List.copyOf(arguments);
    return new FunctionType(new FunctionType.Signature(declared, returned));
  }

  /** Whether the arguments are haxe 3's {@code Void} placeholder of a function without arguments. */
  private static boolean isNoArguments(List<Argument> arguments) {
    if (arguments.size() != 1) return false;
    Argument argument = arguments.getFirst();
    return argument.name() == null && VOID.equals(argument.type().dotPath());
  }

  private static JsonTypeRef named(List<String> pack, String name, List<JsonTypeRef> parameters) {
    boolean stdTypes = pack.isEmpty() && STD_TYPES_MODULE_TYPES.contains(name);
    JsonTypePath path = new JsonTypePath(List.copyOf(pack), stdTypes ? STD_TYPES_MODULE : name, name, null);
    return new NamedType("TInst", new NamedType.PathWithParams(path, List.copyOf(parameters)));
  }

  private String identifier() {
    int start = index;
    while (index < text.length() && (Character.isLetterOrDigit(text.charAt(index)) || text.charAt(index) == '_')) {
      index++;
    }
    return text.substring(start, index);
  }

  private boolean consume(char c) {
    if (index < text.length() && text.charAt(index) == c) {
      index++;
      return true;
    }
    return false;
  }

  private char peek() {
    return index < text.length() ? text.charAt(index) : '\0';
  }

  private void skipWhitespace() {
    while (index < text.length() && Character.isWhitespace(text.charAt(index))) {
      index++;
    }
  }
}
