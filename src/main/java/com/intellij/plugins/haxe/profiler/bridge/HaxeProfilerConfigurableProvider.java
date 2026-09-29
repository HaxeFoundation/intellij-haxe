package com.intellij.plugins.haxe.profiler.bridge;

import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurableProvider;
import com.intellij.plugins.haxe.HaxeProfilerBundle;
import com.intellij.profiler.configurations.ui.EditProfilerConfigurationsComponent;
import org.jetbrains.annotations.Nullable;

/**
 * The "Haxe Profiler" page under Settings | Build, Execution, Deployment |
 * Profiler: the platform's stock configurations editor filtered to the Haxe
 * language group. Every language ships its own page this way (the "Java
 * Profiler" page filters to the JVM types' group and never lists other
 * languages' types). Each Haxe configuration renders its own form.
 */
public class HaxeProfilerConfigurableProvider extends ConfigurableProvider {

  @Override
  public @Nullable Configurable createConfigurable() {
    // the first argument must equal the Haxe configuration types' getLanguageSettingsGroup
    String languageGroup = HaxeProfilerBundle.message("haxe.profiler.settings.group");
    return new EditProfilerConfigurationsComponent(languageGroup, "procedures.profiler");
  }
}
