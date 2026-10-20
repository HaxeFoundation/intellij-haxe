package com.intellij.plugins.haxe.v2.runconfig;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.Executor;
import com.intellij.execution.configurations.CommandLineState;
import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.configurations.LocatableConfigurationBase;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.execution.configurations.RunProfileState;
import com.intellij.execution.configurations.RuntimeConfigurationError;
import com.intellij.execution.configurations.RuntimeConfigurationException;
import com.intellij.execution.configurations.SearchScopeProvidingRunProfile;
import com.intellij.execution.process.KillableColoredProcessHandler;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.process.ProcessTerminatedListener;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.JDOMExternalizerUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.plugins.haxe.HaxeBundle;
import com.intellij.plugins.haxe.util.HaxeReadActions;
import com.intellij.plugins.haxe.v2.buildtools.*;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.util.PathUtil;
import com.intellij.util.io.BaseOutputReader;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.ArrayList;

/**
 * Runs one action of a build file (compile / test / build / a custom action).
 * Stores a REFERENCE (build file path + action name), never a resolved command:
 * the command line re-resolves at launch, so target selection, environment SDK
 * and compilation server settings always apply as currently configured.
 */
public class HaxeActionRunConfiguration extends LocatableConfigurationBase<RunProfileState>
  implements SearchScopeProvidingRunProfile {

  private static final String BUILD_FILE = "buildFile";
  private static final String ACTION_NAME = "actionName";
  private static final String EXTRA_ARGUMENTS = "extraArguments";

  private String buildFilePath = "";
  private String actionName = "";
  private String extraArguments = "";

  public HaxeActionRunConfiguration(@NotNull Project project, @NotNull ConfigurationFactory factory, @Nullable String name) {
    super(project, factory, name);
  }

  public String getBuildFilePath() {
    return buildFilePath;
  }

  public void setBuildFilePath(@Nullable String path) {
    buildFilePath = StringUtil.notNullize(path);
  }

  public String getActionName() {
    return actionName;
  }

  public void setActionName(@Nullable String name) {
    actionName = StringUtil.notNullize(name);
  }

  public String getExtraArguments() {
    return extraArguments;
  }

  public void setExtraArguments(@Nullable String arguments) {
    extraArguments = StringUtil.notNullize(arguments);
  }

  @Override
  public @NotNull SettingsEditor<? extends RunConfiguration> getConfigurationEditor() {
    return new HaxeActionRunConfigurationEditor(getProject());
  }

  /** Console links resolve inside the action's build (its work directory and classpaths) before the project at large. */
  @Override
  public @NotNull GlobalSearchScope getSearchScope() {
    if (StringUtil.isEmptyOrSpaces(buildFilePath)) return GlobalSearchScope.allScope(getProject());
    return HaxeLaunchScopes.forBuildFile(getProject(), buildFilePath);
  }

  @Override
  public void checkConfiguration() throws RuntimeConfigurationException {
    if (StringUtil.isEmptyOrSpaces(buildFilePath)) {
      throw new RuntimeConfigurationError(HaxeBundle.message("haxe.action.config.no.build.file"));
    }
    if (StringUtil.isEmptyOrSpaces(actionName)) {
      throw new RuntimeConfigurationError(HaxeBundle.message("haxe.action.config.no.action"));
    }
    if (resolve() == null) {
      throw new RuntimeConfigurationError(unresolvableMessage());
    }
    String chainProblem = resolveRunBefore().problem();
    if (chainProblem != null) {
      throw new RuntimeConfigurationError(chainProblem);
    }
  }

  @Override
  public @Nullable String suggestedName() {
    if (StringUtil.isEmptyOrSpaces(buildFilePath)) return null;
    return PathUtil.getFileName(buildFilePath) + " " + actionName;
  }

  @Override
  public @Nullable RunProfileState getState(@NotNull Executor executor, @NotNull ExecutionEnvironment environment)
    throws ExecutionException {
    HaxeProjectTrust.requireTrustForRun(getProject());
    return new CommandLineState(environment) {
      @Override
      protected @NotNull ProcessHandler startProcess() throws ExecutionException {
        HaxeUnsavedDocuments.saveAll();
        HaxeCompileCommands.Resolved resolved = resolve();
        if (resolved == null) {
          throw new ExecutionException(unresolvableMessage());
        }
        HaxeCompileCommands.RunBefore runBefore = resolveRunBefore();
        if (runBefore.problem() != null) {
          throw new ExecutionException(runBefore.problem());
        }
        ProcessHandler processHandler = processHandler(runBefore.actions(), resolved);
        ProcessTerminatedListener.attach(processHandler);
        return processHandler;
      }
    };
  }

  /**
   * A lone command runs as itself. Otherwise the actions running first, then
   * this one unless it is chain-only, each announced in the console; the first
   * failure ends the chain.
   */
  @NotNull
  private ProcessHandler processHandler(@NotNull List<HaxeCompileCommands.NamedAction> before,
                                        @NotNull HaxeCompileCommands.Resolved resolved) throws ExecutionException {
    if (before.isEmpty() && resolved.chainOnly()) {
      throw new ExecutionException(HaxeBundle.message("haxe.action.config.nothing.to.run", actionName));
    }
    if (before.isEmpty()) return processHandlerFor(resolved);
    List<HaxeChainedProcessHandler.Step> steps = new ArrayList<>();
    for (HaxeCompileCommands.NamedAction action : before) {
      steps.add(step(action.name(), action.resolved()));
    }
    if (!resolved.chainOnly()) steps.add(step(actionName, resolved));
    return new HaxeChainedProcessHandler(steps);
  }

  @NotNull
  private HaxeChainedProcessHandler.Step step(@NotNull String name, @NotNull HaxeCompileCommands.Resolved resolved) {
    String header = HaxeBundle.message("haxe.action.chain.step", name, resolved.presentable());
    return new HaxeChainedProcessHandler.Step(header, () -> processHandlerFor(resolved));
  }

  @NotNull
  private ProcessHandler processHandlerFor(@NotNull HaxeCompileCommands.Resolved resolved) throws ExecutionException {
    List<String> command =
      HaxeCompileCommands.connectIfEnabled(getProject(), resolved.containerId(), resolved.connectEligible(), resolved.command());
    GeneralCommandLine commandLine =
      HaxeToolCommandLines.interactive(getProject(), resolved.containerId(), command, resolved.workDirectory());
    return new KillableColoredProcessHandler(commandLine) {
      // a run action wraps the app - long-running, sparse output; the default reader
      // busy-polls it (a pty needs its own blocking reader)
      @Override
      protected @NotNull BaseOutputReader.Options readerOptions() {
        return hasPty() ? super.readerOptions() : BaseOutputReader.Options.forMostlySilentProcess();
      }
    };
  }

  /** The referenced action's command as currently configured, or null when it no longer resolves. */
  @Nullable
  private HaxeCompileCommands.Resolved resolve() {
    return HaxeReadActions.compute(() -> HaxeCompileCommands.resolveAction(getProject(), buildFilePath, actionName, extraArguments));
  }

  @NotNull
  private HaxeCompileCommands.RunBefore resolveRunBefore() {
    return HaxeReadActions.compute(() -> HaxeCompileCommands.resolveRunBefore(getProject(), buildFilePath, actionName));
  }

  @NotNull
  private String unresolvableMessage() {
    return HaxeBundle.message("haxe.action.config.unresolvable", actionName, PathUtil.getFileName(buildFilePath));
  }

  @Override
  public void readExternal(@NotNull Element element) {
    super.readExternal(element);
    buildFilePath = StringUtil.notNullize(JDOMExternalizerUtil.readField(element, BUILD_FILE));
    actionName = StringUtil.notNullize(JDOMExternalizerUtil.readField(element, ACTION_NAME));
    extraArguments = StringUtil.notNullize(JDOMExternalizerUtil.readField(element, EXTRA_ARGUMENTS));
  }

  @Override
  public void writeExternal(@NotNull Element element) {
    super.writeExternal(element);
    JDOMExternalizerUtil.writeField(element, BUILD_FILE, buildFilePath);
    JDOMExternalizerUtil.writeField(element, ACTION_NAME, actionName);
    JDOMExternalizerUtil.writeField(element, EXTRA_ARGUMENTS, extraArguments);
  }
}
