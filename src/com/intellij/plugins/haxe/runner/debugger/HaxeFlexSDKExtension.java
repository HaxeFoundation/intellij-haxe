package com.intellij.plugins.haxe.runner.debugger;

import com.intellij.openapi.options.UnnamedConfigurable;
import com.intellij.plugins.haxe.ide.module.HaxeModuleSettings;
import com.intellij.plugins.haxe.ide.projectStructure.HaxeModuleConfigurationExtensionPoint;
import org.jetbrains.annotations.NotNull;

/**
 * @author: Fedor.Korotkov
 */
public class HaxeFlexSDKExtension implements HaxeModuleConfigurationExtensionPoint {
  @Override
  public UnnamedConfigurable createConfigurable(@NotNull HaxeModuleSettings settings) {
    return new HaxeFlexSDKConfigurable(settings);
  }
}
