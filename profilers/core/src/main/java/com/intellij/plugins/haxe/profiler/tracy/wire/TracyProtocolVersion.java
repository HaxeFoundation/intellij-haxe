package com.intellij.plugins.haxe.profiler.tracy.wire;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * The Tracy wire protocol versions the receiver speaks. Tracy locks its
 * protocol per release: the client refuses any server announcing a
 * different number, and each bump renumbers the queue items (and may
 * change item layouts), so the receiver must know the client's exact
 * version before reading a byte. Each version selects the
 * {@link TracyWireFormat} that decodes its bytes; the version itself is
 * only the handshake number and the probe order. hxcpp bundles one client
 * per checkout: 0.11 speaks 69, 0.12 speaks 74, 0.13 speaks 76.
 */
public enum TracyProtocolVersion {
  V69(69, "0.11"),
  V74(74, "0.12"),
  V76(76, "0.13"),
  V82(82, "0.14");

  /**
   * The order the receiver tries versions in when the client's is unknown:
   * what current hxcpp ships first, then the previous one, then the newest
   * Tracy, then the oldest client still in the wild.
   */
  public static final List<TracyProtocolVersion> PROBE_ORDER = List.of(V76, V74, V82, V69);

  private final int wire;
  private final String tracyRelease;
  private volatile TracyWireFormat format;

  TracyProtocolVersion(int wire, String tracyRelease) {
    this.wire = wire;
    this.tracyRelease = tracyRelease;
  }

  /** The number sent in the handshake and compared by the client. */
  public int wire() {
    return wire;
  }

  /** The Tracy release line speaking this version, for messages ("0.13"). */
  @NotNull
  public String tracyRelease() {
    return tracyRelease;
  }

  /** How this version's bytes decode; loads the item table on first use. */
  @NotNull
  public TracyWireFormat format() {
    TracyWireFormat loaded = format;
    if (loaded == null) {
      loaded = createFormat();
      format = loaded;
    }
    return loaded;
  }

  /** This version's wire ordinals and item sizes. */
  @NotNull
  public TracyQueueTable table() {
    return format().table();
  }

  private TracyWireFormat createFormat() {
    TracyQueueTable table = TracyQueueTable.load(this);
    return switch (this) {
      case V69, V74 -> new TracyClassicWireFormat(table, true);
      case V76 -> new TracyClassicWireFormat(table, false);
      case V82 -> new TracyPackedWireFormat(table);
    };
  }

  /** The version speaking the wire number, or null for one the receiver does not know. */
  @Nullable
  public static TracyProtocolVersion of(int wire) {
    for (TracyProtocolVersion version : values()) {
      if (version.wire == wire) return version;
    }
    return null;
  }

  @Override
  public String toString() {
    return wire + " (Tracy " + tracyRelease + ")";
  }
}
