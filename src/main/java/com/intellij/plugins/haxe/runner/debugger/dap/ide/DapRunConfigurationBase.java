package com.intellij.plugins.haxe.runner.debugger.dap.ide;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.execution.configurations.ModuleBasedConfiguration;
import com.intellij.execution.configurations.RunConfigurationModule;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.plugins.haxe.HaxeBundle;
import com.intellij.plugins.haxe.v2.runconfig.HaxeLaunchScopes;
import com.intellij.psi.search.GlobalSearchScope;
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

  /** Console links resolve inside the build step's classpaths before the project at large. */
  @Override
  public GlobalSearchScope getSearchScope() {
    return HaxeLaunchScopes.forConfiguration(this);
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

  /**
   * The given path: absolute as it is, relative against {@link #baseDirectory()};
   * null when unparseable.
   */
  // TODO: HashLinkRunConfigurations.resolveAgainstModule applies the same rule; fold it in once fix/hl-override is merged
  protected @Nullable Path resolveAgainstModule(String value) {
    try {
      Path path = Path.of(value);
      if (path.isAbsolute()) {
        return path;
      }
      Path base = baseDirectory();
      return base != null ? base.resolve(path) : path;
    } catch (InvalidPathException e) {
      return null;
    }
  }

  /**
   * The folder relative paths count from: the selected module's, so a module
   * works as a project of its own; the project folder while no module is
   * selected; null for a project without one.
   */
  protected @Nullable Path baseDirectory() {
    Module module = getConfigurationModule().getModule();
    VirtualFile moduleDir = module != null ? ProjectUtil.guessModuleDir(module) : null;
    if (moduleDir != null) {
      return Path.of(moduleDir.getPath());
    }
    String basePath = getProject().getBasePath();
    return basePath != null ? Path.of(basePath) : null;
  }
}
