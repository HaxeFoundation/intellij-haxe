package com.intellij.plugins.haxe.display.protocol;

/**
 * One entry of the {@code display/metadata} registry: a metadata built into
 * the compiler, or one a library registered with
 * {@code Compiler.registerCustomMetadata}. {@code name} carries the leading
 * colon ({@code :bind}). {@code internal} marks metadata the compiler uses
 * internally, which user code should not use.
 */
public record MetadataEntry(String name, String doc, boolean internal) {

  /** The name without its leading colon, as user code and the PSI spell it. */
  public String bareName() {
    return name.startsWith(":") ? name.substring(1) : name;
  }
}
