package com.intellij.plugins.haxe.runner.debugger.eval;

import java.io.IOException;

/**
 * The VM ended the connection while a request was in flight. Distinct from
 * other transport failures because for RESUME-shaped requests it is a
 * legitimate outcome, not an error: the VM acks continue/step from a helper
 * thread while the resumed program runs on, and when the program finishes
 * the process can exit before that ack is flushed — the resume undeniably
 * HAPPENED, its acknowledgement just lost the race against process exit.
 */
public final class EvalConnectionClosedException extends IOException {
  public EvalConnectionClosedException(String message) {
    super(message);
  }
}
