/*
 * Copyright 2000-2013 JetBrains s.r.o.
 * Copyright 2014-2023 AS3Boyan
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

import com.intellij.execution.ExecutionException;
import com.intellij.execution.Executor;
import com.intellij.execution.configuration.EmptyRunProfileState;
import com.intellij.execution.configurations.ModuleBasedConfiguration;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.execution.configurations.RunProfileState;
import com.intellij.execution.configurations.RuntimeConfigurationException;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.execution.runners.RunConfigurationWithSuppressedDefaultRunAction;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.InvalidDataException;
import com.intellij.openapi.util.WriteExternalException;
import com.intellij.plugins.haxe.HaxeBundle;
import com.intellij.plugins.haxe.ide.module.HaxeModuleSettings;
import com.intellij.plugins.haxe.runner.ui.HaxeRunConfigurationEditorForm;
import com.intellij.util.xmlb.XmlSerializer;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Collection;

public class HaxeApplicationConfiguration extends HaxeApplicationConfigurationBase
  implements RunConfigurationWithSuppressedDefaultRunAction {
  private boolean customFileToLaunch = false;
  private String customFileToLaunchPath = "";
  private String customExecutablePath = "";
  private boolean customExecutable = false;
  private int customDebugPort = 6972;
  private boolean customRemoteDebugging = false;

  public HaxeApplicationConfiguration(String name, Project project, HaxeRunConfigurationType configurationType) {
    super(name, new HaxeApplicationModuleBasedConfiguration(project), configurationType.getConfigurationFactories()[0]);
  }

  @Override
  public Collection<Module> getValidModules() {
    Module[] modules = ModuleManager.getInstance(getProject()).getModules();
    return Arrays.asList(modules);
  }

  @Override
  protected ModuleBasedConfiguration createInstance() {
    return new HaxeApplicationConfiguration(getName(), getProject(), HaxeRunConfigurationType.getInstance());
  }

  public SettingsEditor<? extends RunConfiguration> getConfigurationEditor() {
    return new HaxeRunConfigurationEditorForm(getProject());
  }

  public RunProfileState getState(@NotNull Executor executor, @NotNull ExecutionEnvironment env) throws ExecutionException {
    return EmptyRunProfileState.INSTANCE;
  }

  @Override
  public void checkConfiguration() throws RuntimeConfigurationException {
    super.checkConfiguration();
    final HaxeApplicationModuleBasedConfiguration configurationModule = getConfigurationModule();
    final Module module = configurationModule.getModule();
    if (module == null) {
      throw new RuntimeConfigurationException(HaxeBundle.message("haxe.run.no.module", getName()));
    }
    final HaxeModuleSettings settings = HaxeModuleSettings.getInstance(module);
    if (settings.isUseHxmlToBuild() && !settings.getCompilationTarget().isNoOutput() && !customFileToLaunch) {
      throw new RuntimeConfigurationException(HaxeBundle.message("haxe.run.select.custom.file"));
    }
    if (settings.isUseNmmlToBuild() && customFileToLaunch) {
      throw new RuntimeConfigurationException(HaxeBundle.message("haxe.run.do.not.select.custom.file"));
    }
    if (settings.isUseNmmlToBuild() && customExecutable) {
      throw new RuntimeConfigurationException(HaxeBundle.message("haxe.run.do.not.select.custom.executable"));
    }
  }

  public boolean isCustomFileToLaunch() {
    return customFileToLaunch;
  }

  public void setCustomFileToLaunch(boolean customFileToLaunch) {
    this.customFileToLaunch = customFileToLaunch;
  }

  public boolean isCustomExecutable() {
    return customExecutable;
  }

  public void setCustomExecutable(boolean customExecutable) {
    this.customExecutable = customExecutable;
  }

  public String getCustomFileToLaunchPath() {
    return customFileToLaunchPath;
  }

  public void setCustomFileToLaunchPath(String customFileToLaunchPath) {
    this.customFileToLaunchPath = customFileToLaunchPath;
  }

  public String getCustomExecutablePath() {
    return customExecutablePath;
  }

  public void setCustomExecutablePath(String customExecutablePath) {
    this.customExecutablePath = customExecutablePath;
  }

  public int getCustomDebugPort() {
      return customDebugPort;
  }

  public void setCustomDebugPort(int customDebugPort) {
      this.customDebugPort = customDebugPort;
  }

  public boolean isCustomRemoteDebugging() {
    return customRemoteDebugging;
  }

  public void setCustomRemoteDebugging(boolean customRemoteDebugging) {
    this.customRemoteDebugging = customRemoteDebugging;
  }

  public void writeExternal(final Element element) throws WriteExternalException {
    super.writeExternal(element);
    writeModule(element);
    XmlSerializer.serializeInto(this, element);
  }

  public void readExternal(final Element element) throws InvalidDataException {
    super.readExternal(element);
    readModule(element);
    XmlSerializer.deserializeInto(this, element);
  }
}
