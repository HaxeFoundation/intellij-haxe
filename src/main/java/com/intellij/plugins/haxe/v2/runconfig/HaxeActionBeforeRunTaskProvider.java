package com.intellij.plugins.haxe.v2.runconfig;

import com.intellij.build.BuildDescriptor;
import com.intellij.build.BuildViewManager;
import com.intellij.build.DefaultBuildDescriptor;
import com.intellij.build.process.BuildProcessHandler;
import com.intellij.build.progress.BuildProgress;
import com.intellij.build.progress.BuildProgressDescriptor;
import com.intellij.execution.BeforeRunTask;
import com.intellij.execution.BeforeRunTaskProvider;
import com.intellij.execution.ExecutionException;
import com.intellij.execution.Executor;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.execution.executors.DefaultDebugExecutor;
import com.intellij.execution.process.KillableColoredProcessHandler;
import com.intellij.execution.process.ProcessEvent;
import com.intellij.execution.process.ProcessListener;
import com.intellij.execution.process.ProcessOutputType;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.notification.NotificationAction;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.plugins.haxe.HaxeBundle;
import com.intellij.plugins.haxe.HaxeDebuggerBundle;
import com.intellij.plugins.haxe.config.HaxeTarget;
import com.intellij.plugins.haxe.profiler.HaxeProfilerExecutorSupport;
import com.intellij.plugins.haxe.runner.debugger.browser.BrowserRunConfiguration;
import com.intellij.plugins.haxe.runner.debugger.flash.AirRunConfiguration;
import com.intellij.plugins.haxe.runner.debugger.flash.FlashRunConfiguration;
import com.intellij.plugins.haxe.runner.debugger.hashlink.HashLinkRunConfiguration;
import com.intellij.plugins.haxe.runner.debugger.hxcpp.intellij.HxcppIntellijRunConfiguration;
import com.intellij.plugins.haxe.runner.debugger.hxcpp.legacy.LegacyHxcppRunConfiguration;
import com.intellij.plugins.haxe.runner.debugger.hxcpp.vshaxe.HxcppVshaxeRunConfiguration;
import com.intellij.plugins.haxe.v2.buildsystem.*;
import com.intellij.plugins.haxe.v2.buildtools.*;
import com.intellij.plugins.haxe.v2.buildtools.HaxeDebugAdditions.Debugger;
import com.intellij.plugins.haxe.v2.buildtools.libraries.HaxelibInstaller;
import com.intellij.plugins.haxe.v2.buildtools.libraries.HaxelibMutations;
import com.intellij.plugins.haxe.v2.testing.run.HaxeTestRunConfiguration;
import com.intellij.plugins.haxe.util.HaxeReadActions;
import com.intellij.openapi.util.UserDataHolder;
import com.intellij.util.PathUtil;
import com.intellij.util.xmlb.XmlSerializerUtil;
import icons.HaxeIcons;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.concurrency.Promise;
import org.jetbrains.concurrency.Promises;

import javax.swing.*;
import java.io.OutputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Before-launch step that runs a Haxe action (a build file's compile/build command).
 * Under the Debug executor the target's debug additions (e.g. {@code -debug}) for
 * the launched configuration's debugger are appended automatically, so one visible
 * configuration compiles a normal build on Run and a debuggable build on Debug.
 */
public final class HaxeActionBeforeRunTaskProvider extends BeforeRunTaskProvider<HaxeActionBeforeRunTaskProvider.Task> {

  public static final Key<Task> ID = Key.create("HaxeActionBeforeRun");
  /** The compile command the step last ran for a configuration: {@code ${executable}} reads the launch's debug flags and defines from it. */
  private static final Key<List<String>> LAST_COMPILE = Key.create("haxe.build.step.last.compile");

  public static final class Task extends BeforeRunTask<Task> implements PersistentStateComponent<Task.State> {

    /** Serialized as {@code <option name="field" value="..."/>} children: the field names are the stored keys. */
    public static final class State {
      public String buildFile = "";
      public String action = HxmlProjects.BUILD_ACTION;
      public String arguments = "";
      // opt-out for projects whose build files already carry the debug flags/lib
      public boolean injectDebugArguments = true;
      // test compiles set this: a multi-section hxml compiles only its selected
      // --next section, so the extra arguments reach that section (they would
      // otherwise land in the LAST one - haxe's trailing-argument rule)
      public boolean sectionScoped;
    }

    private State state = new State();

    public Task() {
      super(ID);
    }

    @Override
    public @NotNull State getState() {
      return state;
    }

    @Override
    public void loadState(@NotNull State state) {
      this.state = state;
    }

    /** The copy gets its own state - the platform edits cloned tasks in the configuration dialog. */
    @Override
    public Task clone() {
      Task copy = (Task)super.clone();
      copy.state = XmlSerializerUtil.createCopy(state);
      return copy;
    }

    public String getBuildFilePath() {
      return StringUtil.notNullize(state.buildFile);
    }

    public String getActionName() {
      return StringUtil.notNullize(state.action, HxmlProjects.BUILD_ACTION);
    }

    public String getExtraArguments() {
      return StringUtil.notNullize(state.arguments);
    }

    public boolean isInjectDebugArguments() {
      return state.injectDebugArguments;
    }

    public boolean isSectionScoped() {
      return state.sectionScoped;
    }

    public void setBuildFilePath(@Nullable String path) {
      state.buildFile = StringUtil.notNullize(path);
    }

    public void setActionName(@Nullable String name) {
      state.action = StringUtil.notNullize(name);
    }

    public void setExtraArguments(@Nullable String arguments) {
      state.arguments = StringUtil.notNullize(arguments);
    }

    public void setInjectDebugArguments(boolean inject) {
      state.injectDebugArguments = inject;
    }

    public void setSectionScoped(boolean scoped) {
      state.sectionScoped = scoped;
    }
  }

  @Override
  public Key<Task> getId() {
    return ID;
  }

  @Override
  public String getName() {
    return HaxeDebuggerBundle.message("haxe.before.run.name");
  }

  @Override
  public Icon getIcon() {
    return HaxeIcons.HAXE_LOGO;
  }

  @Override
  public String getDescription(Task task) {
    if (task.getBuildFilePath().isEmpty()) {
      return HaxeDebuggerBundle.message("haxe.before.run.name");
    }
    return HaxeDebuggerBundle.message("haxe.before.run.description",
                                      task.getActionName(), PathUtil.getFileName(task.getBuildFilePath()));
  }

  @Override
  public boolean isConfigurable() {
    return true;
  }

  @Override
  public @Nullable Task createTask(@NotNull RunConfiguration configuration) {
    return new Task();
  }

  @Override
  public @NotNull Promise<Boolean> configureTask(@NotNull DataContext context,
                                                 @NotNull RunConfiguration configuration,
                                                 @NotNull Task task) {
    Debugger debugger = debuggerOf(configuration);
    // a test configuration's step is derived from its tests file and filter: the dialog shows it read-only, an edit would be resynced away
    boolean derived = configuration instanceof HaxeTestRunConfiguration;
    HaxeActionBeforeRunDialog dialog = new HaxeActionBeforeRunDialog(configuration.getProject(), task, debugger, derived);
    return Promises.resolvedPromise(dialog.showAndGet() && !derived);
  }

  @Override
  public boolean executeTask(@NotNull DataContext context,
                             @NotNull RunConfiguration configuration,
                             @NotNull ExecutionEnvironment environment,
                             @NotNull Task task) {
    Project project = configuration.getProject();
    // the platform does not block run configurations in an untrusted project;
    // the action, test and interp configurations ask in getState, and this
    // covers a Haxe step on any other configuration - no dialog off the EDT
    if (!HaxeProjectTrust.isTrusted(project)) {
      String message = HaxeDebuggerBundle.message("haxe.before.run.untrusted");
      HaxeCommandNotifications.notify(project, getName(), message, NotificationType.ERROR);
      return false;
    }

    HaxeUnsavedDocuments.saveAll();
    // test compiles derive their arguments at LAUNCH - the task's stored
    // snapshot goes stale when the framework/reporter wiring evolves. An
    // hxml single-run (gutter) compile arrives fully formed: its generated
    // main replaces the build's own, so the action-plus-file resolution and
    // the section scoping must not touch it.
    boolean templateCompile = configuration instanceof HaxeTestRunConfiguration testConfiguration
                              && testConfiguration.compilesThroughTemplate();
    if (!templateCompile && !runBeforeActions(project, task)) return false;
    HaxeCompileCommands.Resolved resolved = resolveCompile(project, configuration, task, templateCompile);
    if (resolved == null) return false;
    // a chain-only action did its work in the actions it runs first
    if (resolved.chainOnly()) return true;

    List<String> command = compileCommand(project, configuration, environment.getExecutor(), task, templateCompile, resolved);
    if (command == null) return false;
    if (configuration instanceof UserDataHolder holder) {
      holder.putUserData(LAST_COMPILE, command);
    }

    String title = HaxeDebuggerBundle.message("haxe.before.run.build.title",
                                              task.getActionName(), PathUtil.getFileName(task.getBuildFilePath()));
    String workDirectory = StringUtil.notNullize(resolved.workDirectory(), StringUtil.notNullize(project.getBasePath()));
    return runInBuildView(project, resolved.containerId(), command, workDirectory, title);
  }

  /** Runs the actions the task's action runs before itself, each in the Build tool window; false at the first failure or an unusable chain (notified). */
  private static boolean runBeforeActions(@NotNull Project project, @NotNull Task task) {
    String buildFilePath = task.getBuildFilePath();
    String actionName = task.getActionName();
    HaxeCompileCommands.RunBefore runBefore =
      ReadAction.nonBlocking(() -> HaxeCompileCommands.resolveRunBefore(project, buildFilePath, actionName)).executeSynchronously();
    if (runBefore.problem() != null) {
      notifyFailure(project, runBefore.problem());
      return false;
    }
    String fileName = PathUtil.getFileName(buildFilePath);
    for (HaxeCompileCommands.NamedAction action : runBefore.actions()) {
      HaxeCompileCommands.Resolved resolved = action.resolved();
      List<String> command =
        HaxeCompileCommands.connectIfEnabled(project, resolved.containerId(), resolved.connectEligible(), resolved.command());
      String workDirectory = StringUtil.notNullize(resolved.workDirectory(), StringUtil.notNullize(project.getBasePath()));
      String title = HaxeDebuggerBundle.message("haxe.before.run.build.title", action.name(), fileName);
      if (!runInBuildView(project, resolved.containerId(), command, workDirectory, title)) return false;
    }
    return true;
  }

  /** The compile to run, or null when it does not resolve (the user was notified). */
  @Nullable
  private static HaxeCompileCommands.Resolved resolveCompile(@NotNull Project project,
                                                            @NotNull RunConfiguration configuration,
                                                            @NotNull Task task,
                                                            boolean templateCompile) {
    HaxeCompileCommands.Resolved resolved;
    if (templateCompile) {
      resolved = ((HaxeTestRunConfiguration)configuration).resolveSingleRunCompile();
    }
    else {
      String extraArguments = configuration instanceof HaxeTestRunConfiguration testConfiguration
                              ? testConfiguration.currentCompileArguments()
                              : task.getExtraArguments();
      if (extraArguments == null) {
        String buildFileName = PathUtil.getFileName(task.getBuildFilePath());
        notifyFailure(project, HaxeBundle.message("haxe.test.single.unresolvable", buildFileName));
        return null;
      }
      String buildFilePath = task.getBuildFilePath();
      String actionName = task.getActionName();
      resolved = ReadAction.nonBlocking(() -> HaxeCompileCommands.resolveAction(project, buildFilePath, actionName, extraArguments))
        .executeSynchronously();
    }
    if (resolved == null) {
      notifyFailure(project, HaxeDebuggerBundle.message("haxe.before.run.unresolvable", task.getBuildFilePath()));
    }
    return resolved;
  }

  /**
   * The resolved command plus the launch's debug and profiling additions,
   * connected to the compilation server when enabled; null when the debug
   * build lacks its server lib (the user was notified).
   */
  @Nullable
  private static List<String> compileCommand(@NotNull Project project,
                                             @NotNull RunConfiguration configuration,
                                             @NotNull Executor executor,
                                             @NotNull Task task,
                                             boolean templateCompile,
                                             @NotNull HaxeCompileCommands.Resolved resolved) {
    List<String> command = new ArrayList<>(baseCommand(project, task, templateCompile, resolved));
    boolean debug = DefaultDebugExecutor.EXECUTOR_ID.equals(executor.getId());
    if (debug && task.isInjectDebugArguments()) {
      Debugger debugger = debuggerOf(configuration);
      // a single-run compile is a DIRECT haxe compile whatever the build
      // system, so its additions use the haxe spelling - the tool spellings
      // (lime's --haxelib=) are unknown options to haxe itself
      List<String> additions = templateCompile
                               ? singleRunDebugAdditions(project, task.getBuildFilePath(), debugger)
                               : debugAdditions(project, task.getBuildFilePath(), debugger);
      if (additions != null) {
        if (!ensureHxcppDebugServerInstalled(project, additions, debugger)) return null;
        command.addAll(additions);
      }
    }
    if (!templateCompile) {
      List<String> profilingAdditions = profilingAdditions(project, configuration, executor, task);
      if (profilingAdditions != null) {
        command.addAll(profilingAdditions);
      }
    }
    return HaxeCompileCommands.connectIfEnabled(project, resolved.containerId(), resolved.connectEligible(), command);
  }

  /**
   * Runs the compile with its output streamed into the Build tool window
   * (activated on start) - it runs before the launch, and without visible
   * output a native build's minutes of compilation look like a hang.
   */
  private static boolean runInBuildView(@NotNull Project project,
                                        @NotNull String containerId,
                                        @NotNull List<String> command,
                                        @NotNull String workDirectory,
                                        @NotNull String title) {
    KillableColoredProcessHandler handler;
    try {
      GeneralCommandLine commandLine = HaxeToolCommandLines.interactive(project, containerId, command, workDirectory);
      handler = new KillableColoredProcessHandler(commandLine);
    }
    catch (ExecutionException e) {
      notifyFailure(project, StringUtil.notNullize(e.getMessage()));
      return false;
    }

    BuildProgress<BuildProgressDescriptor> progress = BuildViewManager.createBuildProgress(project);
    // the root progress takes its id FROM the descriptor - progress.getId()
    // asserts before start(), so the build id must be our own object
    DefaultBuildDescriptor buildDescriptor =
      new DefaultBuildDescriptor(new Object(), title, workDirectory, System.currentTimeMillis());
    buildDescriptor.setActivateToolWindowWhenAdded(true);
    // surfacing the compile as the build's process handler enables the Build
    // view's Stop action - the escape hatch when a compile hangs (e.g. on a
    // wedged compilation server connection), hence the hard kill below
    buildDescriptor.withProcessHandler(new CompileProcessHandler(handler, title), null);
    progress.start(descriptorFor(title, buildDescriptor));

    // the process handler announces the command line as its first output -
    // printing it manually doubles the line
    handler.addProcessListener(new ProcessListener() {
      @Override
      public void onTextAvailable(@NotNull ProcessEvent event, @NotNull Key outputType) {
        ProcessOutputType type = outputType instanceof ProcessOutputType outputKind ? outputKind : ProcessOutputType.STDOUT;
        progress.output(event.getText(), type);
      }
    });
    handler.startNotify();
    // a cancelled wait (the launch's progress cancelled) must not leave the compiler running
    if (!handler.waitFor()) {
      handler.killProcess();
    }
    int exitCode = Objects.requireNonNullElse(handler.getExitCode(), -1);
    if (exitCode != 0) {
      String failure = HaxeDebuggerBundle.message("haxe.before.run.failed", String.valueOf(exitCode));
      progress.fail(System.currentTimeMillis(), failure);
      return false;
    }
    progress.finish();
    return true;
  }

  /** Adapts the compiler process to the Build view: its Stop action kills the underlying process outright. */
  private static final class CompileProcessHandler extends BuildProcessHandler {
    private final KillableColoredProcessHandler delegate;
    private final String executionName;

    CompileProcessHandler(@NotNull KillableColoredProcessHandler delegate, @NotNull String executionName) {
      this.delegate = delegate;
      this.executionName = executionName;
      delegate.addProcessListener(new ProcessListener() {
        @Override
        public void processTerminated(@NotNull ProcessEvent event) {
          notifyProcessTerminated(event.getExitCode());
        }
      });
    }

    @Override
    public String getExecutionName() {
      return executionName;
    }

    @Override
    protected void destroyProcessImpl() {
      delegate.killProcess();
    }

    @Override
    protected void detachProcessImpl() {
      delegate.detachProcess();
      notifyProcessDetached();
    }

    @Override
    public boolean detachIsDefault() {
      return false;
    }

    /** Console input reaches the compiler's stdin, so prompts can be answered. */
    @Override
    public @Nullable OutputStream getProcessInput() {
      return delegate.getProcessInput();
    }
  }

  @NotNull
  private static BuildProgressDescriptor descriptorFor(@NotNull String title,
                                                       @NotNull DefaultBuildDescriptor buildDescriptor) {
    return new BuildProgressDescriptor() {
      @Override
      public @NotNull String getTitle() {
        return title;
      }

      @Override
      public @NotNull BuildDescriptor getBuildDescriptor() {
        return buildDescriptor;
      }
    };
  }

  /** The compile command; a section-scoped suite run narrows it to the selected section, template compiles keep it whole. */
  @NotNull
  private static List<String> baseCommand(@NotNull Project project,
                                          @NotNull Task task,
                                          boolean templateCompile,
                                          @NotNull HaxeCompileCommands.Resolved resolved) {
    if (templateCompile || !task.isSectionScoped()) return resolved.command();
    return ReadAction.nonBlocking(() -> sectionScopedCommand(project, task.getBuildFilePath(), resolved.command()))
      .executeSynchronously();
  }

  /** The resolved command scoped to the hxml's selected {@code --next} section; unchanged for non-hxml files. */
  @NotNull
  private static List<String> sectionScopedCommand(@NotNull Project project,
                                                   @NotNull String buildFilePath,
                                                   @NotNull List<String> command) {
    HaxeBuildFile buildFile = HaxeBuildFileScanner.findBuildFile(project, buildFilePath);
    if (buildFile == null || buildFile.type() != HaxeBuildFileType.HXML) return command;
    return HxmlProjects.scopeToSelectedSection(project, buildFile.file(), command);
  }

  /**
   * The build's classpath roots from the configuration's Haxe build step, or
   * empty without one — debug runners scope breakpoint binding and frame
   * resolution with them (the configuration itself only knows the artifact).
   */
  @NotNull
  public static List<String> buildStepSourceDirectories(@NotNull RunConfiguration configuration) {
    String buildFilePath = buildStepFilePath(configuration);
    if (buildFilePath == null) return List.of();
    return HaxeReadActions.compute(
      () -> HaxeBuildClasspaths.sourceDirectories(configuration.getProject(), buildFilePath));
  }

  /** The build file of the configuration's Haxe build step, or null without one. */
  @Nullable
  public static String buildStepFilePath(@NotNull RunConfiguration configuration) {
    return configuration.getBeforeRunTasks().stream()
      .filter(Task.class::isInstance)
      .map(task -> ((Task)task).getBuildFilePath())
      .filter(path -> !path.isBlank())
      .findFirst()
      .orElse(null);
  }

  /**
   * The executable the configuration's Haxe build step writes for the current
   * launch, which {@code ${executable}} stands for: an hxml C++ build's
   * {@code -cpp} folder and hxcpp's name for the result, the debug flags and
   * defines of the step's last compile counted in. Null without a build step,
   * or for a build that links no executable this machine runs.
   */
  @Nullable
  public static Path builtExecutable(@NotNull RunConfiguration configuration) {
    String path = buildStepFilePath(configuration);
    if (path == null) return null;
    Project project = configuration.getProject();
    HaxeBuildFile buildFile = HaxeReadActions.compute(() -> HaxeBuildFileScanner.findBuildFile(project, path));
    if (buildFile == null) return null;
    List<String> lastCompile = configuration instanceof UserDataHolder holder ? holder.getUserData(LAST_COMPILE) : null;
    return HxcppExecutables.executable(project, buildFile, lastCompile != null ? lastCompile : List.of());
  }

  /**
   * The compile additions a PROFILING launch injects — the hxcpp telemetry
   * entry's defines plus its start/stop bootstrap, the tracy entry's plain
   * defines, the flash entry's telemetry opt-in, or the browser entry's
   * source-map emission — in the build system's tool spelling; null on every
   * non-profiling launch.
   */
  @Nullable
  private static List<String> profilingAdditions(@NotNull Project project,
                                                 @NotNull RunConfiguration configuration,
                                                 @NotNull Executor executor,
                                                 @NotNull Task task) {
    HaxeBuildFile buildFile = HaxeBuildFileScanner.findBuildFile(project, task.getBuildFilePath());
    if (buildFile == null) return null;
    List<String> plain = plainProfilingAdditions(configuration, executor);
    return plain == null ? null : HaxeBuildSystem.of(buildFile.type()).forwardedArguments(plain);
  }

  /** The profiler's additions in plain haxe spelling; null when the executor is no profiler of the configuration's lane. */
  @Nullable
  private static List<String> plainProfilingAdditions(@NotNull RunConfiguration configuration, @NotNull Executor executor) {
    if (configuration instanceof HxcppIntellijRunConfiguration hxcpp) {
      Path dumpPath = hxcpp.expectedDumpPath();
      if (dumpPath == null) return null;
      List<String> telemetry = HaxeProfilerExecutorSupport.hxcppProfilingAdditions(executor, dumpPath);
      return telemetry != null ? telemetry : HaxeProfilerExecutorSupport.hxcppTracyAdditions(executor);
    }
    if (configuration instanceof HashLinkRunConfiguration) return HaxeProfilerExecutorSupport.hlProfilingAdditions(executor);
    if (configuration instanceof AirRunConfiguration) return HaxeProfilerExecutorSupport.flashProfilingAdditions(executor);
    if (configuration instanceof BrowserRunConfiguration) return HaxeProfilerExecutorSupport.jsProfilingAdditions(executor);
    return null;
  }

  /**
   * The debugger the configuration attaches on Debug - it picks the hxcpp
   * server lib and the Flash debugger tag of the debug build. Everything
   * without a dedicated debugger (HashLink, browser, AIR, HXCPP (IntelliJ),
   * tests) takes the default additions.
   */
  @NotNull
  static Debugger debuggerOf(@NotNull RunConfiguration configuration) {
    return switch (configuration) {
      case HxcppVshaxeRunConfiguration _ -> Debugger.HXCPP_VSHAXE;
      case LegacyHxcppRunConfiguration _ -> Debugger.HXCPP_LEGACY;
      case FlashRunConfiguration _ -> Debugger.FLASH_PLAYER;
      default -> Debugger.DEFAULT;
    };
  }

  /** The build system's debug compile additions for the file's current selection and the debugger, or null when it has none. */
  @Nullable
  static List<String> debugAdditions(@NotNull Project project, @NotNull String buildFilePath, @NotNull Debugger debugger) {
    HaxeBuildFile buildFile = HaxeBuildFileScanner.findBuildFile(project, buildFilePath);
    if (buildFile == null) return null;
    return HaxeReadActions.compute(
      () -> HaxeBuildSystem.of(buildFile.type()).debugCompileAdditions(project, buildFile, debugger));
  }

  /** Debug additions for a single-run compile: always the plain haxe spelling for the selected target (see the call site). */
  @Nullable
  private static List<String> singleRunDebugAdditions(@NotNull Project project,
                                                      @NotNull String buildFilePath,
                                                      @NotNull Debugger debugger) {
    HaxeBuildFile buildFile = HaxeBuildFileScanner.findBuildFile(project, buildFilePath);
    if (buildFile == null) return null;
    return ReadAction.nonBlocking(() -> {
        HaxeTarget target = HaxeBuildSystem.of(buildFile.type()).launchTarget(project, buildFile);
        return target != null ? HaxeDebugAdditions.forTarget(target, debugger) : null;
      })
      .executeSynchronously();
  }

  /**
   * A debug compile pulling in the debugger's hxcpp server haxelib dies with
   * a raw haxelib error in the build console when the lib is absent. Checked
   * up front instead, turning the failure into a notification whose Install
   * action fetches the lib (published on lib.haxe.org). True when the
   * additions need no server lib or it is already present.
   */
  private static boolean ensureHxcppDebugServerInstalled(@NotNull Project project,
                                                         @NotNull List<String> additions,
                                                         @NotNull Debugger debugger) {
    String serverLib = debugger.hxcppServerLib();
    if (serverLib == null) return true;
    // covers every spelling: haxe's "-lib X", lime's "--haxelib=X", nme's "--library X"
    boolean needsServerLib = additions.stream().anyMatch(addition -> addition.contains(serverLib));
    if (!needsServerLib) return true;
    if (HaxelibInstaller.isInstalled(project, serverLib)) return true;

    String title = HaxeDebuggerBundle.message("haxe.before.run.name");
    String message = HaxeDebuggerBundle.message("haxe.before.run.debug.server.missing", serverLib);
    AnAction installAction = installDebugServerAction(project, serverLib);
    HaxeCommandNotifications.notify(project, title, message, NotificationType.ERROR, installAction);
    return false;
  }

  @NotNull
  private static AnAction installDebugServerAction(@NotNull Project project, @NotNull String serverLib) {
    String text = HaxeDebuggerBundle.message("haxe.before.run.debug.server.install");
    return NotificationAction.createSimpleExpiring(text, () -> installDebugServerInBackground(project, serverLib));
  }

  private static void installDebugServerInBackground(@NotNull Project project, @NotNull String serverLib) {
    String progressTitle = HaxeDebuggerBundle.message("haxe.before.run.debug.server.installing", serverLib);
    String failureTitle = HaxeDebuggerBundle.message("haxe.before.run.debug.server.install.failed", serverLib);
    HaxelibMutations.Mutation install = indicator -> HaxelibInstaller.install(project, serverLib, null, null);
    HaxelibMutations.queue(project, progressTitle, failureTitle, install, () -> notifyDebugServerInstalled(project, serverLib));
  }

  private static void notifyDebugServerInstalled(@NotNull Project project, @NotNull String serverLib) {
    String message = HaxeDebuggerBundle.message("haxe.before.run.debug.server.installed", serverLib);
    HaxeCommandNotifications.notify(project, message, NotificationType.INFORMATION);
  }

  private static void notifyFailure(@NotNull Project project, @NotNull String message) {
    String title = HaxeDebuggerBundle.message("haxe.before.run.name");
    HaxeCommandNotifications.notify(project, title, message, NotificationType.ERROR);
  }
}
