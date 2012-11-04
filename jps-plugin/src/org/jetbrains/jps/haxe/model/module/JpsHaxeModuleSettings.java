package org.jetbrains.jps.haxe.model.module;

import com.intellij.plugins.haxe.module.HaxeModuleSettingsBase;
import org.jetbrains.jps.model.JpsElement;

/**
 * @author: Fedor.Korotkov
 */
public interface JpsHaxeModuleSettings extends HaxeModuleSettingsBase, JpsElement {
  HaxeModuleSettingsBase getProperties();
}
