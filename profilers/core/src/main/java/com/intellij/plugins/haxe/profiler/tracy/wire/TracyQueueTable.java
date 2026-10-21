package com.intellij.plugins.haxe.profiler.tracy.wire;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.EnumMap;
import java.util.Map;

/**
 * One protocol version's queue-item numbering: wire ordinal to
 * {@link TracyQueueType}, and each item's fixed size on the wire INCLUDING
 * the ordinal byte. Loaded from {@code tracy/queue-v<N>.txt}, one
 * {@code Name;size} line per ordinal, generated mechanically from that
 * version's {@code TracyQueue.hpp} with the probe under
 * {@code profilers/core/tools/tracy-queue-table} (the enum order plus the
 * compiler's own {@code QueueDataSize[]}) - never typed by hand: a single
 * misnumbered item silently misparses everything after it.
 */
public final class TracyQueueTable {

  private final TracyQueueType[] byOrdinal;
  private final int[] sizeByOrdinal;
  private final Map<TracyQueueType, Integer> ordinalByType = new EnumMap<>(TracyQueueType.class);

  private TracyQueueTable(TracyQueueType[] byOrdinal, int[] sizeByOrdinal) {
    this.byOrdinal = byOrdinal;
    this.sizeByOrdinal = sizeByOrdinal;
    for (int ordinal = 0; ordinal < byOrdinal.length; ordinal++) {
      ordinalByType.put(byOrdinal[ordinal], ordinal);
    }
  }

  @NotNull
  static TracyQueueTable load(@NotNull TracyProtocolVersion version) {
    String resource = "/tracy/queue-v" + version.wire() + ".txt";
    try (InputStream in = TracyQueueTable.class.getResourceAsStream(resource)) {
      if (in == null) throw new IllegalStateException("missing tracy queue table " + resource);
      return parse(new String(in.readAllBytes(), StandardCharsets.UTF_8));
    }
    catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  @NotNull
  static TracyQueueTable parse(@NotNull String text) {
    // one item per line; the resource is written with LF endings
    String[] lines = text.strip().split("\n");
    TracyQueueType[] types = new TracyQueueType[lines.length];
    int[] sizes = new int[lines.length];
    for (int ordinal = 0; ordinal < lines.length; ordinal++) {
      // "Name;size", size being the item's wire size including the ordinal byte
      String[] parts = lines[ordinal].strip().split(";");
      types[ordinal] = TracyQueueType.valueOf(parts[0]);
      sizes[ordinal] = Integer.parseInt(parts[1]);
    }
    return new TracyQueueTable(types, sizes);
  }

  /** The item type a wire ordinal names, or null for one this version does not define. */
  @Nullable
  public TracyQueueType of(int wireOrdinal) {
    return wireOrdinal >= 0 && wireOrdinal < byOrdinal.length ? byOrdinal[wireOrdinal] : null;
  }

  /** The type's wire ordinal in this version, or -1 when the version does not define it. */
  public int ordinalOf(@NotNull TracyQueueType type) {
    return ordinalByType.getOrDefault(type, -1);
  }

  /** Whether this version has the item at all (older versions lack the ones added later). */
  public boolean defines(@NotNull TracyQueueType type) {
    return ordinalByType.containsKey(type);
  }

  /** The item's fixed size on the wire, including the ordinal byte; payload data (see {@link TracyQueueType#payload()}) follows it. */
  public int wireSize(@NotNull TracyQueueType type) {
    Integer ordinal = ordinalByType.get(type);
    if (ordinal == null) throw new IllegalArgumentException(type + " is not part of this protocol version");
    return sizeByOrdinal[ordinal];
  }

  /** The number of items this version defines: its enum's entry count. */
  public int size() {
    return byOrdinal.length;
  }
}
