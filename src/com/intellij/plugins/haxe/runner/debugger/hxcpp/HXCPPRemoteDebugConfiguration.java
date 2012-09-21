package com.intellij.plugins.haxe.runner.debugger.hxcpp;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.Executor;
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
import com.intellij.plugins.haxe.runner.HaxeApplicationModuleBasedConfiguration;
import com.intellij.plugins.haxe.runner.HaxeRunner;
import com.intellij.plugins.haxe.runner.debugger.hxcpp.ui.HXCPPRunConfigurationEditorForm;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Collection;

/**
 * @author: Fedor.Korotkov
 */
public class HXCPPRemoteDebugConfiguration extends ModuleBasedConfiguration<HaxeApplicationModuleBasedConfiguration>
  implements RunConfigurationWithSuppressedDefaultRunAction {

  public HXCPPRemoteDebugConfiguration(String name, Project project, HXCPPRemoteRunConfigurationType configurationType) {
    super(name, new HaxeApplicationModuleBasedConfiguration(project), configurationType.getConfigurationFactories()[0]);
  }

  @Override
  public Collection<Module> getValidModules() {
    Module[] modules = ModuleManager.getInstance(getProject()).getModules();
    return Arrays.asList(modules);
  }

  @Override
  protected ModuleBasedConfiguration createInstance() {
    return new HXCPPRemoteDebugConfiguration(getName(), getProject(), HXCPPRemoteRunConfigurationType.getInstance());
  }

  public SettingsEditor<? extends RunConfiguration> getConfigurationEditor() {
    return new HXCPPRunConfigurationEditorForm(getProject());
  }

  public RunProfileState getState(@NotNull Executor executor, @NotNull ExecutionEnvironment env) throws ExecutionException {
    return HaxeRunner.EMPTY_RUN_STATE;
  }

  @Override
  public void checkConfiguration() throws RuntimeConfigurationException {
    super.checkConfiguration();
    final HaxeApplicationModuleBasedConfiguration configurationModule = getConfigurationModule();
    final Module module = configurationModule.getModule();
    if (module == null) {
      throw new RuntimeConfigurationException(HaxeBundle.message("haxe.run.no.module", getName()));
    }
  }

  public void writeExternal(final Element element) throws WriteExternalException {
    super.writeExternal(element);
    writeModule(element);
  }

  public void readExternal(final Element element) throws InvalidDataException {
    super.readExternal(element);
    readModule(element);
  }
}
