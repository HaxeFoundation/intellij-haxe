/*
 * Copyright 2000-2013 JetBrains s.r.o.
 * Copyright 2014-2014 AS3Boyan
 * Copyright 2014-2014 Elias Ku
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
import com.intellij.openapi.extensions.Extensions;
import com.intellij.openapi.project.Project;
import com.intellij.plugins.haxe.HaxeBundle;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

public class HaxeRunConfigurationType implements ConfigurationType {
  private final HaxeFactory configurationFactory;

  public HaxeRunConfigurationType() {
    configurationFactory = new HaxeFactory(this);
  }

  public static HaxeRunConfigurationType getInstance() {
    return ContainerUtil.findInstance(Extensions.getExtensions(CONFIGURATION_TYPE_EP), HaxeRunConfigurationType.class);
  }

  public String getDisplayName() {
    return HaxeBundle.message("runner.configuration.name");
  }

  public String getConfigurationTypeDescription() {
    return HaxeBundle.message("runner.configuration.name");
  }

  public Icon getIcon() {
    return icons.HaxeIcons.Haxe_16;
  }

  @NotNull
  public String getId() {
    return "HaxeApplicationRunConfiguration";
  }

  public ConfigurationFactory[] getConfigurationFactories() {
    return new ConfigurationFactory[]{configurationFactory};
  }

  public static class HaxeFactory extends ConfigurationFactory {

    public HaxeFactory(ConfigurationType type) {
      super(type);
    }

    public RunConfiguration createTemplateConfiguration(Project project) {
      final String name = HaxeBundle.message("runner.configuration.name");
      return new HaxeApplicationConfiguration(name, project, getInstance());
    }

    @NotNull
    @Override
    @NonNls
    public String getId() {
      // Must not come from a localized bundle.
      return "Haxe Application";
    }
  }
}
