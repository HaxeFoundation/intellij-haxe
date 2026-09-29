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
import com.intellij.plugins.haxe.HaxeBundle;
import com.intellij.plugins.haxe.runner.debugger.flash.AirConfigurationFactory;
import com.intellij.plugins.haxe.runner.debugger.flash.FlashConfigurationFactory;
import com.intellij.plugins.haxe.runner.debugger.hashlink.HashLinkConfigurationFactory;
import com.intellij.plugins.haxe.runner.debugger.hxcpp.legacy.LegacyHxcppConfigurationFactory;
import com.intellij.plugins.haxe.runner.debugger.hxcpp.vshaxe.HxcppVshaxeConfigurationFactory;
import com.intellij.plugins.haxe.runner.debugger.hxcpp.intellij.HxcppIntellijConfigurationFactory;
import com.intellij.plugins.haxe.runner.debugger.interp.InterpConfigurationFactory;
import com.intellij.plugins.haxe.runner.neko.NekoConfigurationFactory;
import com.intellij.plugins.haxe.runner.debugger.browser.BrowserConfigurationFactory;
import com.intellij.plugins.haxe.v2.runconfig.HaxeActionConfigurationFactory;
import com.intellij.util.containers.ContainerUtil;
import icons.HaxeIcons;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

/**
 * The "Haxe" run/debug configuration GROUP: one configuration type whose
 * factories are the individual runner flavours, so the Edit Configurations
 * dialog shows them nested under a single Haxe node:
 * <ul>
 *   <li>HXCPP Application (legacy) — the old hxcpp.DebugSocket debugger</li>
 *   <li>Haxe Action — one action of a build file</li>
 *   <li>HashLink Application</li>
 *   <li>HXCPP Application (IntelliJ)</li>
 *   <li>HXCPP Application (vshaxe)</li>
 *   <li>Interp (eval) Application</li>
 *   <li>Browser Application</li>
 *   <li>Haxe Flash Application and AIR Application</li>
 *   <li>Neko Application</li>
 * </ul>
 *
 * Compatibility: the type keeps the historical id
 * {@code HaxeApplicationRunConfiguration} and the legacy factory keeps its
 * historical id {@code Haxe Application}; saved configurations resolve by
 * that id. A configuration saved without a {@code factoryName} attribute
 * (pre-group plugin versions) resolves to the FIRST factory in the array,
 * so the legacy factory must stay first; the rest of the order is only the
 * display order of the Add New Configuration list.
 */
public class HaxeRunConfigurationType implements ConfigurationType {
  private final ConfigurationFactory[] factories;

  public HaxeRunConfigurationType() {
    factories = new ConfigurationFactory[]{
      new LegacyHxcppConfigurationFactory(this),
      new HaxeActionConfigurationFactory(this),
      new HashLinkConfigurationFactory(this),
      new HxcppIntellijConfigurationFactory(this),
      new HxcppVshaxeConfigurationFactory(this),
      new InterpConfigurationFactory(this),
      new BrowserConfigurationFactory(this),
      new FlashConfigurationFactory(this),
      new AirConfigurationFactory(this),
      new NekoConfigurationFactory(this),
    };
  }

  /** The factory of the given flavour — for the tool window paths that create configurations. */
  public <T extends ConfigurationFactory> T getFactory(Class<T> factoryClass) {
    return ContainerUtil.findInstance(factories, factoryClass);
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
    return HaxeIcons.HAXE_LOGO;
  }

  @NotNull
  public String getId() {
    return "HaxeApplicationRunConfiguration";
  }

  public ConfigurationFactory[] getConfigurationFactories() {
    return factories;
  }
}
