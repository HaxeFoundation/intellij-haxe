package com.intellij.plugins.haxe;

import com.intellij.DynamicBundle;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.PropertyKey;

/** Code style and formatting settings UI: tab titles, option labels, hxformat.json integration. */
public final class HaxeCodeStyleBundle {
  @NonNls
  private static final String BUNDLE = "messages.HaxeCodeStyleBundle";
  private static final DynamicBundle INSTANCE = new DynamicBundle(HaxeCodeStyleBundle.class, BUNDLE);

  private HaxeCodeStyleBundle() {
  }

  public static String message(@PropertyKey(resourceBundle = BUNDLE) String key, Object... params) {
    return INSTANCE.getMessage(key, params);
  }
}
