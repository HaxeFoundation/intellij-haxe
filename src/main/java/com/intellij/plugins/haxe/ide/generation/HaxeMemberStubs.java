package com.intellij.plugins.haxe.ide.generation;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The body convention for generated member stubs, shared by Implement
 * Methods and the compiler's missing-fields quick fix: a method that returns
 * a value throws until implemented, so the stub compiles as written; a Void
 * method, or one without a declared return type, gets an empty body.
 */
public final class HaxeMemberStubs {

  private static final String NOT_IMPLEMENTED_STATEMENT = "throw new haxe.exceptions.NotImplementedException();";
  private static final String EMPTY_BODY = "{\n}";
  private static final String THROWING_BODY = "{\n" + NOT_IMPLEMENTED_STATEMENT + "\n}";

  private HaxeMemberStubs() {
  }

  /** The stub body: one throwing {@code NotImplementedException} when the method must return a value, else an empty one. */
  @NotNull
  public static String body(boolean returnsValue) {
    return returnsValue ? THROWING_BODY : EMPTY_BODY;
  }

  @NotNull
  public static String bodyFor(@Nullable String returnTypeText) {
    return body(returnsValue(returnTypeText));
  }

  /** Whether a method with this declared return type must return something; {@code Void} and no declared type do not. */
  public static boolean returnsValue(@Nullable String returnTypeText) {
    return returnTypeText != null && !returnTypeText.equals("Void");
  }
}
