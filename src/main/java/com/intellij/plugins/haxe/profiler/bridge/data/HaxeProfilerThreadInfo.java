package com.intellij.plugins.haxe.profiler.bridge.data;

import com.intellij.profiler.model.ThreadInfo;
import org.jetbrains.annotations.NotNull;

/** A captured thread as the IU profiler views list it. */
record HaxeProfilerThreadInfo(@NotNull String name, @NotNull String nativeId) implements ThreadInfo {

  @Override
  public @NotNull String getName() {
    return name;
  }

  @Override
  public @NotNull String getNativeId() {
    return nativeId;
  }
}
