package com.intellij.plugins.haxe.runner.debugger.dap;

import org.jetbrains.annotations.Nullable;

/**
 * Builds the access-path EXPRESSION for a variable-tree node
 * ("this.someObject.someArray[0].myVar"), used to pre-fill the Evaluate
 * Expression dialog from a selected node.
 *
 * A node only gets a path when the WHOLE chain is a valid field/index access:
 * every segment must be a Haxe identifier (a field) or an array index. Adapters
 * name children in two conventions — hxcpp renders array elements as "[0]",
 * HashLink as bare "0" — and both are accepted. Anything else (a map keyed by
 * a string/object, the "…" truncation marker, a synthetic "captured" slot)
 * makes the path inexpressible, so its whole subtree returns null and the
 * dialog simply opens empty rather than pre-filling something that won't parse.
 */
public final class EvaluationPath {
  private EvaluationPath() {
  }

  /** The path of a frame-root local: its name if that is a usable identifier, else null. */
  public static @Nullable String root(@Nullable String name) {
    return isIdentifier(name) ? name : null;
  }

  /**
   * The path of a child given its parent's path. Null when the parent has no
   * path (an inexpressible ancestor) or the child itself is neither a field nor
   * an index.
   */
  public static @Nullable String child(@Nullable String parentPath, @Nullable String name) {
    if (parentPath == null) {
      return null; // parent isn't expressible, so nothing under it is
    }
    String index = asIndex(name);
    if (index != null) {
      return parentPath + "[" + index + "]";
    }
    if (isIdentifier(name)) {
      return parentPath + "." + name;
    }
    return null;
  }

  // A pure integer index in either convention: "0" (HashLink) or "[0]" (hxcpp).
  private static @Nullable String asIndex(@Nullable String name) {
    if (name == null || name.isEmpty()) {
      return null;
    }
    String inner = isBracketed(name) ? name.substring(1, name.length() - 1) : name;
    if (inner.isEmpty()) {
      return null;
    }
    for (int i = 0; i < inner.length(); i++) {
      if (!Character.isDigit(inner.charAt(i))) {
        return null;
      }
    }
    return inner;
  }

  // True when `name` is wrapped in brackets — hxcpp's "[0]" array-element name.
  private static boolean isBracketed(String name) {
    return name.length() >= 2 && name.charAt(0) == '[' && name.charAt(name.length() - 1) == ']';
  }

  /** Whether the name is a plain Haxe identifier (usable as a field-access segment). */
  public static boolean isIdentifier(@Nullable String name) {
    if (name == null || name.isEmpty()) {
      return false;
    }
    char first = name.charAt(0);
    if (!Character.isLetter(first) && first != '_') {
      return false;
    }
    for (int i = 1; i < name.length(); i++) {
      char c = name.charAt(i);
      if (!Character.isLetterOrDigit(c) && c != '_') {
        return false;
      }
    }
    return true;
  }
}
