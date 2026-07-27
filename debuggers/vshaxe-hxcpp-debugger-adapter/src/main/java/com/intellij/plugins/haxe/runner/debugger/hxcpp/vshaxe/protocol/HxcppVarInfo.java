package com.intellij.plugins.haxe.runner.debugger.hxcpp.vshaxe.protocol;

/**
 * One variable of a {@code getVariables}/{@code evaluate}/{@code setVariable}
 * result (VarInfo in Protocol.hx). {@code variablesReference} > 0 marks an
 * expandable value; the server omits the field on some paths (observed on
 * setVariable results), so it is nullable — use {@link #reference()}.
 */
public record HxcppVarInfo(String name, String type, String value, Integer variablesReference,
                           Integer namedVariables, Integer indexedVariables) {
  /** The variablesReference with the omitted-field case mapped to 0 (a leaf). */
  public int reference() {
    return variablesReference == null ? 0 : variablesReference;
  }
}
