package com.intellij.plugins.haxe.runner;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.Executor;
import com.intellij.execution.configurations.ModuleBasedConfiguration;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.execution.configurations.RunProfileState;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.execution.runners.RunConfigurationWithSuppressedDefaultRunAction;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.InvalidDataException;
import com.intellij.openapi.util.WriteExternalException;
import com.intellij.plugins.haxe.runner.ui.HaxeRunConfigurationEditorForm;
import com.intellij.util.xmlb.XmlSerializer;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Collection;

public class HaxeApplicationConfiguration extends ModuleBasedConfiguration<HaxeApplicationModuleBasedConfiguration>
  implements RunConfigurationWithSuppressedDefaultRunAction {
  private boolean customFileToLaunch = false;
  private String customFileToLaunchPath = "";

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
    return HaxeRunner.EMPTY_RUN_STATE;
  }

  public boolean isCustomFileToLaunch() {
    return customFileToLaunch;
  }

  public void setCustomFileToLaunch(boolean customFileToLaunch) {
    this.customFileToLaunch = customFileToLaunch;
  }

  public String getCustomFileToLaunchPath() {
    return customFileToLaunchPath;
  }

  public void setCustomFileToLaunchPath(String customFileToLaunchPath) {
    this.customFileToLaunchPath = customFileToLaunchPath;
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
