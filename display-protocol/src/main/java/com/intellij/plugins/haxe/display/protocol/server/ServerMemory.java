package com.intellij.plugins.haxe.display.protocol.server;

import java.util.List;

/**
 * The {@code server/memory} answer: the server's total cache size and each
 * compilation context's share of it, all in bytes.
 */
public record ServerMemory(long totalCacheBytes, List<ContextSize> contexts) {

  /** The cache memory of one context. The context matches a {@code server/contexts} entry by signature. */
  public record ContextSize(HaxeServerContext context, long bytes) {
  }
}
