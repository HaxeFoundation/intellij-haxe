package com.intellij.plugins.haxe.profiler.tracy.connect;

import com.intellij.plugins.haxe.profiler.model.ProfilerFormatException;
import com.intellij.plugins.haxe.profiler.tracy.wire.TracyProtocolVersion;
import org.jetbrains.annotations.NotNull;

/**
 * The client answered the handshake with {@code HandshakeProtocolMismatch}:
 * it speaks a different version than the one offered. The client closes its
 * socket and keeps listening, so the receiver may reconnect and offer another.
 */
public final class TracyProtocolMismatchException extends ProfilerFormatException {

  private final TracyProtocolVersion offered;

  public TracyProtocolMismatchException(@NotNull TracyProtocolVersion offered) {
    super("tracy client refused protocol " + offered.wire());
    this.offered = offered;
  }

  @NotNull
  public TracyProtocolVersion offered() {
    return offered;
  }
}
