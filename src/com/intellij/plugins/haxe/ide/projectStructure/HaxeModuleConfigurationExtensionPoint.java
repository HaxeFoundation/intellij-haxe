package com.intellij.plugins.haxe.ide.projectStructure;

import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.options.UnnamedConfigurable;
import com.intellij.plugins.haxe.ide.module.HaxeModuleSettings;
import org.jetbrains.annotations.NotNull;

/**
 * @author: Fedor.Korotkov
 */
public interface HaxeModuleConfigurationExtensionPoint {
  ExtensionPointName<HaxeModuleConfigurationExtensionPoint> EP_NAME =
    ExtensionPointName.create("com.intellij.plugins.haxe.module.config");

  UnnamedConfigurable createConfigurable(@NotNull HaxeModuleSettings settings);
}
