package com.intellij.plugins.haxe.profiler.tracy.connect;

import com.intellij.plugins.haxe.profiler.model.ProfilerFormatException;
import com.intellij.plugins.haxe.profiler.tracy.wire.TracyProtocolVersion;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/** The client refused every version the strategy offered: it speaks one the receiver does not know. */
public final class TracyProtocolUnsupportedException extends ProfilerFormatException {

  private final List<TracyProtocolVersion> refused;

  public TracyProtocolUnsupportedException(@NotNull List<TracyProtocolVersion> refused) {
    super("tracy client refused every offered protocol version: " + refused);
    this.refused = List.copyOf(refused);
  }

  @NotNull
  public List<TracyProtocolVersion> refused() {
    return refused;
  }
}
