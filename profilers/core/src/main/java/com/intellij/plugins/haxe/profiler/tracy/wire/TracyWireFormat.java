package com.intellij.plugins.haxe.profiler.tracy.wire;

import org.jetbrains.annotations.NotNull;

import java.io.DataInputStream;
import java.io.IOException;

/**
 * What a protocol version defines about the BYTES: its item table, its
 * welcome layout, and the few primitive reads whose encoding changed
 * between versions. The reader owns every meaning (zone stacks, memory
 * pools, timelines) and asks the format only for these; a new Tracy
 * release is a new table plus, when its encoding moved, a new format.
 * Item names are the canonical {@link TracyQueueType} vocabulary; a format
 * is only ever asked about items its own table defines.
 */
public interface TracyWireFormat {

  /** This version's wire ordinals and item sizes. */
  @NotNull
  TracyQueueTable table();

  /** The packed welcome message's size on the wire. */
  int welcomeSize();

  /**
   * The welcome message in this version's layout; {@code version} is
   * recorded in the result because one format serves several versions.
   */
  @NotNull
  TracyWelcome parseWelcome(byte @NotNull [] welcome, @NotNull TracyProtocolVersion version);

  /**
   * The thread-stream delta of a zone begin or zone end item, in ticks:
   * the item's 64-bit form or, where the format has them, its packed
   * 32/16-bit forms.
   */
  long zoneDelta(@NotNull TracyQueueType type, @NotNull DataInputStream in) throws IOException;

  /**
   * Consumes a sampled-callstack item's fields and returns its ctx-stream
   * delta in ticks; the thread id is consumed with it (its position moved
   * between versions).
   */
  long callstackSampleDelta(@NotNull TracyQueueType type, @NotNull DataInputStream in) throws IOException;

  /** The length prefix of the item's inline payload (0 for none), decoded per this version's rules. */
  int payloadLength(@NotNull TracyQueueType type, @NotNull DataInputStream in) throws IOException;

  /**
   * Consumes what follows a message item's time and colour, and says
   * whether the message is the program's own (a format with a metadata
   * byte also carries the tracy client's status lines on the same item).
   */
  boolean messageFromProgram(@NotNull DataInputStream in) throws IOException;
}
