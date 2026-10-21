package com.intellij.plugins.haxe.profiler.tracy.connect;

import com.intellij.plugins.haxe.profiler.tracy.wire.TracyProtocolVersion;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Which protocol version to offer the client, and in what order. A PINNED
 * version (the user's setting) is offered alone; otherwise the version the
 * broadcast announced goes first - it may arrive while the connect is still
 * retrying, so it is consulted per attempt - followed by
 * {@link TracyProtocolVersion#PROBE_ORDER}. The client re-listens after
 * every refusal, so each offer is one connect plus one handshake.
 */
public final class TracyVersionStrategy {

  private final @Nullable TracyProtocolVersion pinned;
  private final Supplier<@Nullable TracyProtocolVersion> announced;

  private TracyVersionStrategy(@Nullable TracyProtocolVersion pinned,
                               @NotNull Supplier<@Nullable TracyProtocolVersion> announced) {
    this.pinned = pinned;
    this.announced = announced;
  }

  /** Only this version is offered; a refusal ends the capture. */
  @NotNull
  public static TracyVersionStrategy pinned(@NotNull TracyProtocolVersion version) {
    return new TracyVersionStrategy(version, () -> null);
  }

  /** The probe ladder, with whatever {@code announced} reports (the broadcast) tried first. */
  @NotNull
  public static TracyVersionStrategy detect(@NotNull Supplier<@Nullable TracyProtocolVersion> announced) {
    return new TracyVersionStrategy(null, announced);
  }

  /** The next version to offer given the ones already refused; null once every candidate was tried. */
  @Nullable
  public TracyProtocolVersion next(@NotNull List<TracyProtocolVersion> refused) {
    for (TracyProtocolVersion candidate : candidates()) {
      if (!refused.contains(candidate)) return candidate;
    }
    return null;
  }

  @NotNull
  List<TracyProtocolVersion> candidates() {
    if (pinned != null) return List.of(pinned);
    List<TracyProtocolVersion> candidates = new ArrayList<>();
    TracyProtocolVersion heard = announced.get();
    if (heard != null) candidates.add(heard);
    for (TracyProtocolVersion version : TracyProtocolVersion.PROBE_ORDER) {
      if (version != heard) candidates.add(version);
    }
    return candidates;
  }
}
