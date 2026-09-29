package com.intellij.plugins.haxe.v2.compiler.settings;

import com.intellij.plugins.haxe.HaxeBundle;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Where completion and resolve get their symbols: from the IDE's static
 * analysis alone, from static analysis enriched with compiler-known symbols
 * (macro-generated types and members the source never declares), or from the
 * compilation server alone. In the compiler-only mode every completion is a
 * {@code display/completion} request, and the IDE's contributors stay
 * silent. The mode switches the compiler-backed resolve and completion on or
 * off; diagnostics highlighting has its own toggle.
 */
public enum HaxeCompletionMode {
  IDE_ONLY("ide", "haxe.compiler.completion.mode.ide"),
  IDE_AND_COMPILER("ide+compiler", "haxe.compiler.completion.mode.ide.and.compiler"),
  COMPILER_ONLY("compiler", "haxe.compiler.completion.mode.compiler");

  private final String id;
  private final String presentableKey;

  HaxeCompletionMode(String id, String presentableKey) {
    this.id = id;
    this.presentableKey = presentableKey;
  }

  /** Stable identifier used for persistence. */
  @NotNull
  public String getId() {
    return id;
  }

  @NotNull
  public String getPresentableText() {
    return HaxeBundle.message(presentableKey);
  }

  public boolean usesCompiler() {
    return this != IDE_ONLY;
  }

  @NotNull
  public static HaxeCompletionMode fromId(@Nullable String id) {
    for (HaxeCompletionMode mode : values()) {
      if (mode.id.equals(id)) return mode;
    }
    return IDE_AND_COMPILER;
  }
}
