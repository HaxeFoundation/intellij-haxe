package com.intellij.plugins.haxe;

import com.intellij.AbstractBundle;
import com.intellij.DynamicBundle;
import java.lang.ref.Reference;
import java.lang.ref.SoftReference;
import java.util.ResourceBundle;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.PropertyKey;

/**
 * Messages of the DAP-based debuggers and their run configurations
 * (HashLink, HXCPP vshaxe/IntelliJ, Haxe interpreter) plus the debugger
 * settings page. The LEGACY Flash/hxcpp debugger's messages stay in
 * {@link HaxeBundle}.
 */
public class HaxeDebuggerBundle extends DynamicBundle {
  private static Reference<ResourceBundle> ourBundle;

  @NonNls
  private static final String BUNDLE = "messages.HaxeDebuggerBundle";

  public HaxeDebuggerBundle() {
    super(BUNDLE);
  }

  public static String message(@PropertyKey(resourceBundle = BUNDLE) String key, Object... params) {
    return AbstractBundle.message(getBundle(), key, params);
  }

  private static ResourceBundle getBundle() {
    ResourceBundle bundle = null;
    if (ourBundle != null) {
      bundle = ourBundle.get();
    }
    if (bundle == null) {
      bundle = ResourceBundle.getBundle(BUNDLE);
      ourBundle = new SoftReference<>(bundle);
    }
    return bundle;
  }
}
