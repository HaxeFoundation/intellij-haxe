package com.intellij.plugins.haxe.display.protocol;

/** Wire values of std {@code haxe.display.FindReferencesKind}. */
public enum FindReferencesKind {
  /** Only direct references; ignores parent/overriding methods. */
  DIRECT("direct"),
  /** References to the base field and all overriding fields in the inheritance chain. */
  WITH_BASE_AND_DESCENDANTS("withBaseAndDescendants"),
  /** References to the field itself and its overriding fields, not the base. */
  WITH_DESCENDANTS("withDescendants");

  private final String wireValue;

  FindReferencesKind(String wireValue) {
    this.wireValue = wireValue;
  }

  public String wireValue() {
    return wireValue;
  }
}
