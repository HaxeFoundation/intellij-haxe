/*
 * Copyright 2019 Eric Bishton
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
import com.intellij.execution.configurations.ModuleBasedConfiguration;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;

/*
@replace.with.plugin.autogen.warning@
*/

/**
 * A wrapper around ModuleBasedConfiguration to deal with the change in
 * generic arguments between versions 18.2 and 18.3 of IDEA.  This file supports 18.3+.
 */
// package
abstract class HaxeApplicationConfigurationBase
  extends ModuleBasedConfiguration<HaxeApplicationModuleBasedConfiguration, Element> {
  public HaxeApplicationConfigurationBase(String name,
                                         @NotNull HaxeApplicationModuleBasedConfiguration configurationModule,
                                         @NotNull ConfigurationFactory factory) {
    super(name, configurationModule, factory);
  }
}

