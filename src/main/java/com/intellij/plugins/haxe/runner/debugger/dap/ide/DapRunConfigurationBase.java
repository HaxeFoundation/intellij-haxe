package com.intellij.plugins.haxe.runner.debugger.dap.ide;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.execution.configurations.ModuleBasedConfiguration;
import com.intellij.execution.configurations.RunConfigurationModule;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.plugins.haxe.HaxeBundle;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;
import org.jdom.Element;
import org.jetbrains.annotations.Nullable;

/**
 * Base for every DAP-debugger run configuration: the module plumbing (any
 * module is valid — it is only used for source lookup — and a fresh
 * configuration defaults to the first one) plus the small shared helpers.
 * Subclasses add their own fields, editor, validation and state.
 */
public abstract class DapRunConfigurationBase extends ModuleBasedConfiguration<RunConfigurationModule, Element> {

  protected DapRunConfigurationBase(String name, Project project, ConfigurationFactory factory) {
    super(name, new RunConfigurationModule(project), factory);
  }

  @Override
  public Collection<Module> getValidModules() {
    return Arrays.asList(ModuleManager.getInstance(getProject()).getModules());
  }

  @Override
  public void onNewConfigurationCreated() {
    super.onNewConfigurationCreated();
    if (getConfigurationModule().getModule() == null) {
      Module[] modules = ModuleManager.getInstance(getProject()).getModules();
      if (modules.length > 0) {
        setModule(modules[0]);
      }
    }
  }

  /** The configured module, or a clear failure when none is set. */
  public Module requireModule() throws ExecutionException {
    Module module = getConfigurationModule().getModule();
    if (module == null) {
      throw new ExecutionException(HaxeBundle.message("no.module.for.run.configuration", getName()));
    }
    return module;
  }

  protected static String orEmpty(@Nullable String value) {
    return value == null ? "" : value;
  }

  /** The given path, resolved against the project base dir when relative; null when unparseable. */
  protected @Nullable Path resolveAgainstProject(String value) {
    try {
      Path path = Path.of(value);
      if (path.isAbsolute()) {
        return path;
      }

      String basePath = getProject().getBasePath();
      return basePath != null ? Path.of(basePath).resolve(path) : path;

    } catch (InvalidPathException e) {
      return null;
    }
  }
}
