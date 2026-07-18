package com.intellij.plugins.haxe;

import com.intellij.AbstractBundle;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.PropertyKey;

import java.lang.ref.Reference;
import java.lang.ref.SoftReference;
import java.util.ResourceBundle;

/**
 * Messages related to running the Haxe compiler.
 */
public class HaxeCompilerBundle {
  private static Reference<ResourceBundle> ourBundle;

  @NonNls
  public static final String BUNDLE = "messages.HaxeCompilerBundle";

  public static String message(@PropertyKey(resourceBundle = BUNDLE) String key, Object... params) {
    return AbstractBundle.message(getBundle(), key, params);
  }

  private static ResourceBundle getBundle() {
    ResourceBundle bundle = null;

    if (ourBundle != null) bundle = ourBundle.get();

    if (bundle == null) {
      bundle = ResourceBundle.getBundle(BUNDLE);
      ourBundle = new SoftReference<>(bundle);
    }
    return bundle;
  }
}
