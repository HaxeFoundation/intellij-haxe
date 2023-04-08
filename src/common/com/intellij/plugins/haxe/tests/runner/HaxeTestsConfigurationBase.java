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
package com.intellij.plugins.haxe.tests.runner;

import com.intellij.execution.configurations.ModuleBasedConfiguration;
import com.intellij.openapi.project.Project;
import com.intellij.plugins.haxe.runner.HaxeApplicationModuleBasedConfiguration;
import org.jdom.Element;

/**
 * Wraps ModuleBasedConfiguration, which changed class signatures between IDEA 2018.2 and 2018.3.
 * This file supports 2018.3+.
 */
abstract public class HaxeTestsConfigurationBase extends ModuleBasedConfiguration<HaxeApplicationModuleBasedConfiguration, Element>
{
  public HaxeTestsConfigurationBase(String name,
                                    Project configurationModule,
                                    HaxeTestsRunConfigurationType configurationType) {
    super(name, new HaxeApplicationModuleBasedConfiguration(configurationModule), configurationType.getConfigurationFactories()[0]);
  }
}
