package com.intellij.plugins.haxe.display.protocol;

import org.jetbrains.annotations.Nullable;

/**
 * One item of a {@code display/completion} result, reduced to what a lookup
 * element needs.
 * <ul>
 *   <li>{@code kind} is the compiler's item kind: {@code Local},
 *   {@code ClassField}, {@code EnumField}, {@code EnumAbstractField},
 *   {@code Type}, {@code Package}, {@code Module}, {@code Literal},
 *   {@code Metadata}, {@code Keyword}, {@code TypeParameter} or
 *   {@code Define}.</li>
 *   <li>{@code name} is the text to insert.</li>
 *   <li>{@code detail} is a type's qualified path or a package's path.</li>
 *   <li>{@code moduleTypeKind} is a type's declaration kind, such as {@code class}.</li>
 *   <li>{@code type} is the item's type when the compiler knows one.</li>
 *   <li>{@code doc} is the doc comment of a field, type, metadata or define.</li>
 *   <li>{@code index} is the number a {@code display/completionItem/resolve}
 *   request names the item by.</li>
 *   <li>{@code importStatus} is a type's import status at the position; null
 *   for the other kinds.</li>
 * </ul>
 */
public record CompletionItem(String kind, String name, @Nullable String detail, @Nullable String moduleTypeKind,
                             @Nullable JsonTypeRef type, @Nullable String doc, int index, @Nullable ImportStatus importStatus) {

  /** Whether the item is a keyword or a literal such as {@code null} or {@code true}. */
  public boolean isKeywordOrLiteral() {
    return "Keyword".equals(kind) || "Literal".equals(kind);
  }

  public boolean isType() {
    return "Type".equals(kind);
  }

  /** Whether the item is a class field or an enum abstract value. */
  public boolean isField() {
    return "ClassField".equals(kind) || "EnumAbstractField".equals(kind);
  }

  public boolean isEnumField() {
    return "EnumField".equals(kind);
  }

  /** Whether the item is a local variable or a type parameter. */
  public boolean isLocalOrTypeParameter() {
    return "Local".equals(kind) || "TypeParameter".equals(kind);
  }

  public boolean isPackageOrModule() {
    return "Package".equals(kind) || "Module".equals(kind);
  }

  /**
   * The item's full dot path, which completion matches a dotted prefix such
   * as {@code go.math.Ma} against: a type's qualified path, a package's path,
   * a module's package plus its name. Null for the other kinds.
   */
  @Nullable
  public String dotPath() {
    return switch (kind) {
      case "Type", "Package" -> detail;
      case "Module" -> detail == null || detail.isEmpty() ? name : detail + "." + name;
      default -> null;
    };
  }
}
