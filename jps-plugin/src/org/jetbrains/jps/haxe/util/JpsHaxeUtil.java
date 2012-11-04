package org.jetbrains.jps.haxe.util;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jps.haxe.model.module.JpsHaxeModuleSettings;
import org.jetbrains.jps.model.JpsElement;
import org.jetbrains.jps.model.module.JpsModule;

/**
 * @author: Fedor.Korotkov
 */
public class JpsHaxeUtil {
  @Nullable
  public static JpsHaxeModuleSettings getModuleSettings(@NotNull JpsModule module) {
    final JpsElement result = module.getProperties();
    return result instanceof JpsHaxeModuleSettings ? (JpsHaxeModuleSettings)result : null;
  }
}
