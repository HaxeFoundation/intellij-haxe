/*
 * Copyright 2000-2013 JetBrains s.r.o.
 * Copyright 2014-2014 AS3Boyan
 * Copyright 2014-2014 Elias Ku
 * Copyright 2020 Eric Bishton
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.intellij.plugins.haxe.runner;

import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.execution.configurations.ConfigurationType;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.openapi.project.Project;
import com.intellij.plugins.haxe.HaxeBundle;
import com.intellij.plugins.haxe.runner.debugger.hashlink.HashLinkConfigurationFactory;
import com.intellij.plugins.haxe.runner.debugger.hxcpp.vshaxe.HxcppVshaxeConfigurationFactory;
import com.intellij.plugins.haxe.runner.debugger.hxcpp.intellij.HxcppIntellijConfigurationFactory;
import com.intellij.plugins.haxe.runner.debugger.interp.InterpConfigurationFactory;
import com.intellij.plugins.haxe.runner.debugger.browser.BrowserConfigurationFactory;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

/**
 * The "Haxe" run/debug configuration GROUP: one configuration type whose
 * factories are the individual runner flavours, so the Edit Configurations
 * dialog shows them nested under a single Haxe node:
 * <ul>
 *   <li>Haxe Application (legacy) — the original runner (also carries
 *       Flash/Flex debugging)</li>
 *   <li>HashLink Application</li>
 *   <li>HXCPP Application (vshaxe)</li>
 *   <li>HXCPP Application (IntelliJ)</li>
 * </ul>
 *
 * Compatibility: the type keeps the historical id
 * {@code HaxeApplicationRunConfiguration} and the legacy factory keeps its
 * historical id {@code Haxe Application} AND stays FIRST in the factory
 * array, so run configurations saved by earlier plugin versions still load.
 */
public class HaxeRunConfigurationType implements ConfigurationType {
  private final ConfigurationFactory[] factories;
  private final HaxeFactory legacyFactory;

  public HaxeRunConfigurationType() {
    legacyFactory = new HaxeFactory(this);
    factories = new ConfigurationFactory[]{
      legacyFactory, // first: pre-group configurations saved without a factory name resolve to it
      new HashLinkConfigurationFactory(this),
      new HxcppVshaxeConfigurationFactory(this),
      new HxcppIntellijConfigurationFactory(this),
      new InterpConfigurationFactory(this),
      new BrowserConfigurationFactory(this),
    };
  }

  public static HaxeRunConfigurationType getInstance() {
    return ContainerUtil.findInstance(CONFIGURATION_TYPE_EP.getExtensionList(), HaxeRunConfigurationType.class);
  }

  public String getDisplayName() {
    return HaxeBundle.message("haxe.runner.group.name");
  }

  public String getConfigurationTypeDescription() {
    return HaxeBundle.message("haxe.runner.group.description");
  }

  public Icon getIcon() {
    return icons.HaxeIcons.HAXE_LOGO;
  }

  @NotNull
  public String getId() {
    return "HaxeApplicationRunConfiguration";
  }

  public ConfigurationFactory[] getConfigurationFactories() {
    return factories;
  }

  public static class HaxeFactory extends ConfigurationFactory {

    public HaxeFactory(ConfigurationType type) {
      super(type);
    }

    @Override
    @NotNull
    public String getName() {
      return HaxeBundle.message("runner.configuration.name.legacy");
    }

    public RunConfiguration createTemplateConfiguration(Project project) {
      final String name = HaxeBundle.message("runner.configuration.name");
      return new HaxeApplicationConfiguration(name, project, getInstance());
    }

    // @Override - not in 2016
    @NotNull
    @NonNls
    public String getId() {
      // Must not come from a localized bundle - and must stay the historical
      // value so previously saved configurations keep resolving.
      return "Haxe Application";
    }
  }
}
