/*
 * Copyright 2000-2013 JetBrains s.r.o.
 * Copyright 2014-2015 AS3Boyan
 * Copyright 2014-2015 Elias Ku
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

import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.execution.configurations.ConfigurationType;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.openapi.project.Project;
import com.intellij.plugins.haxe.HaxeBundle;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

public class HaxeTestsRunConfigurationType implements ConfigurationType {

  private final HaxeTestsFactory configurationFactory;

  public HaxeTestsRunConfigurationType() {
    configurationFactory = new HaxeTestsFactory(this);
  }

  @Override
  public String getDisplayName() {
    return HaxeBundle.message("haxe.tests.runner.configuration.name");
  }

  @Override
  public String getConfigurationTypeDescription() {
    return HaxeBundle.message("haxe.tests.runner.configuration.name");
  }

  @Override
  public Icon getIcon() {
    return icons.HaxeIcons.HAXE_LOGO;
  }

  @NotNull
  @Override
  public String getId() {
    return "HaxeTestsRunConfiguration";
  }

  @Override
  public ConfigurationFactory[] getConfigurationFactories() {
    return new ConfigurationFactory[]{configurationFactory};
  }

  public static class HaxeTestsFactory extends ConfigurationFactory {

    private HaxeTestsRunConfigurationType configurationType;

    public HaxeTestsFactory(ConfigurationType type) {
      super(type);
      configurationType = (HaxeTestsRunConfigurationType)type;
    }

    public RunConfiguration createTemplateConfiguration(Project project) {
      final String name = HaxeBundle.message("runner.configuration.name");
      return new HaxeTestsConfiguration(name, project, configurationType);
    }
  }
}
