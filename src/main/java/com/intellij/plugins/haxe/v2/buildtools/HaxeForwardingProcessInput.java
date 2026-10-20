package com.intellij.plugins.haxe.v2.buildtools;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.OutputStream;
import java.util.function.Supplier;

/**
 * The stdin of a chain of processes: every write goes to the stream the
 * supplier returns at that moment, so a typed answer reaches the running step.
 * A console takes a handler's input stream ONCE, when it attaches and before
 * any step has started; a per-step stream would be null then and the console
 * would never write again.
 */
public final class HaxeForwardingProcessInput extends OutputStream {

  private final Supplier<@Nullable OutputStream> running;

  public HaxeForwardingProcessInput(@NotNull Supplier<@Nullable OutputStream> running) {
    this.running = running;
  }

  @Override
  public void write(int b) throws IOException {
    OutputStream input = running.get();
    if (input != null) input.write(b);
  }

  @Override
  public void write(byte @NotNull [] bytes, int offset, int length) throws IOException {
    OutputStream input = running.get();
    if (input != null) input.write(bytes, offset, length);
  }

  @Override
  public void flush() throws IOException {
    OutputStream input = running.get();
    if (input != null) input.flush();
  }
}
