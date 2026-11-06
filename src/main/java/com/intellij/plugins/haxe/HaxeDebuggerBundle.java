package com.intellij.plugins.haxe;

import com.intellij.DynamicBundle;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.PropertyKey;

/**
 * Messages of the DAP-based debuggers and their run configurations
 * (HashLink, HXCPP vshaxe/IntelliJ, Haxe interpreter) plus the debugger
 * settings page. The LEGACY Flash/hxcpp debugger's messages stay in
 * {@link HaxeBundle}.
 */
public final class HaxeDebuggerBundle {
  @NonNls
  private static final String BUNDLE = "messages.HaxeDebuggerBundle";
  private static final DynamicBundle INSTANCE = new DynamicBundle(HaxeDebuggerBundle.class, BUNDLE);

  private HaxeDebuggerBundle() {
  }

  public static String message(@PropertyKey(resourceBundle = BUNDLE) String key, Object... params) {
    return INSTANCE.getMessage(key, params);
  }
}
