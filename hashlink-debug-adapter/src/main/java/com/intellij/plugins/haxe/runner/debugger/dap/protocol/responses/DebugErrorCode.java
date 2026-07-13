package com.intellij.plugins.haxe.runner.debugger.dap.protocol.responses;

/**
 * Machine-readable error identifiers carried in {@link ErrorMessage#getId()}.
 *
 * <p>Mirrors the adapter's Haxe {@code debug.DebugErrorCode} — the numeric values
 * are the wire contract and MUST stay in sync between the two. The human-readable
 * {@code format} text may change freely; clients branch on the code, not the text.
 * DAP leaves the id space to the adapter, so these numbers are ours to define.
 */
public enum DebugErrorCode {
  /** No specific machine meaning — a plain rejection. */
  GENERIC(1001),

  /**
   * A name in an evaluate expression could not be resolved against the frame
   * (not a local, {@code this} field, or class the module knows). {@code
   * variables.get("name")} holds the offending identifier, so a client can
   * resolve it with its own source knowledge (imports) and re-issue a qualified
   * expression.
   */
  UNRESOLVED_NAME(2001);

  private final int id;

  DebugErrorCode(int id) {
    this.id = id;
  }

  /** The wire value ({@code Message.id}). */
  public int id() {
    return id;
  }

  /** True when {@code error} carries this code. */
  public boolean matches(ErrorMessage error) {
    return error != null && error.getId() == id;
  }
}
