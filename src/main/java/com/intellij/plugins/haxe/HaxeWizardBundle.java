package com.intellij.plugins.haxe;

import com.intellij.DynamicBundle;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.PropertyKey;

/** Messages for the New Project wizard surfaces (language entry + the Haxe Template generator). */
public final class HaxeWizardBundle {
  @NonNls
  private static final String BUNDLE = "messages.HaxeWizardBundle";
  private static final DynamicBundle INSTANCE = new DynamicBundle(HaxeWizardBundle.class, BUNDLE);

  private HaxeWizardBundle() {
  }

  public static String message(@PropertyKey(resourceBundle = BUNDLE) String key, Object... params) {
    return INSTANCE.getMessage(key, params);
  }
}
