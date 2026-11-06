package com.intellij.plugins.haxe;

import com.intellij.DynamicBundle;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.PropertyKey;

/** Messages of the profiler integration: snapshot parsers, capture UI and the IU bridge. */
public final class HaxeProfilerBundle {
  @NonNls
  private static final String BUNDLE = "messages.HaxeProfilerBundle";
  private static final DynamicBundle INSTANCE = new DynamicBundle(HaxeProfilerBundle.class, BUNDLE);

  private HaxeProfilerBundle() {
  }

  public static String message(@PropertyKey(resourceBundle = BUNDLE) String key, Object... params) {
    return INSTANCE.getMessage(key, params);
  }
}
