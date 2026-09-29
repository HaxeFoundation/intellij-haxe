package com.intellij.plugins.haxe.v2.testing.run;

import com.intellij.execution.BeforeRunTask;
import com.intellij.execution.DefaultExecutionResult;
import com.intellij.execution.ExecutionException;
import com.intellij.execution.ExecutionResult;
import com.intellij.execution.Executor;
import com.intellij.execution.configurations.CommandLineState;
import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.configurations.LocatableConfigurationBase;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.execution.configurations.RunProfileState;
import com.intellij.execution.configurations.RuntimeConfigurationError;
import com.intellij.execution.configurations.RuntimeConfigurationException;
import com.intellij.execution.process.KillableColoredProcessHandler;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.process.ProcessTerminatedListener;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.execution.runners.ProgramRunner;
import com.intellij.execution.testframework.sm.SMTestRunnerConnectionUtil;
import com.intellij.execution.testframework.sm.runner.SMRunnerConsolePropertiesProvider;
import com.intellij.execution.testframework.sm.runner.SMTRunnerConsoleProperties;
import com.intellij.execution.testframework.ui.BaseTestsOutputConsoleView;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.JDOMExternalizerUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.plugins.haxe.v2.testing.HaxeTestFrameworks;
import com.intellij.plugins.haxe.HaxeBundle;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.plugins.haxe.v2.buildsystem.HaxeBuildFileScanner;
import com.intellij.plugins.haxe.v2.buildsystem.HaxeBuildFileType;
import com.intellij.plugins.haxe.v2.buildtools.*;
import com.intellij.plugins.haxe.runner.debugger.browser.HaxeBrowserTestSupport;
import com.intellij.plugins.haxe.util.HaxeReadActions;
import com.intellij.plugins.haxe.v2.runconfig.HaxeActionBeforeRunTaskProvider;
import com.intellij.plugins.haxe.v2.testing.HaxeTestFramework;
import com.intellij.util.PathUtil;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;

/**
 * Runs a container's tests build file as unit tests. Reference-style like
 * {@code HaxeActionRunConfiguration}: only the tests build file path, an
 * optional filter pattern and the selected suites/test are stored - the
 * launch re-resolves everything at run time, so target and SDK changes always
 * apply. The SM test console attaches to the RUN process; for artifact
 * targets the compile happens in the attached before-run step (kept in sync
 * by {@link #syncCompileStep()}), while interp builds compile-and-run as the
 * single test process.
 */
public class HaxeTestRunConfiguration extends LocatableConfigurationBase<RunProfileState>
  implements SMRunnerConsolePropertiesProvider {

  /** SM framework id: keys the console's splitter/settings storage and debug diagnostics; not user-visible. */
  static final String TEST_FRAMEWORK_NAME = "HaxeUnitTests";

  private static final String BUILD_FILE = "buildFile";
  private static final String FILTER_PATTERN = "filterPattern";
  private static final String TEST_CLASS = "testClass";
  private static final String TEST_METHOD = "testMethod";
  // a haxe qualified name never holds a comma
  private static final String SUITE_SEPARATOR = ",";

  private String buildFilePath = "";
  private String filterPattern = "";
  private String testClass = "";
  private String testMethod = "";

  public HaxeTestRunConfiguration(@NotNull Project project, @NotNull ConfigurationFactory factory, @Nullable String name) {
    super(project, factory, name);
  }

  public String getBuildFilePath() {
    return buildFilePath;
  }

  public void setBuildFilePath(@Nullable String path) {
    buildFilePath = StringUtil.notNullize(path);
  }

  public String getFilterPattern() {
    return filterPattern;
  }

  public void setFilterPattern(@Nullable String pattern) {
    filterPattern = StringUtil.notNullize(pattern);
  }

  /** Narrows the run to one suite class or one of its tests (a gutter marker or a context-menu run). */
  public void setSingleRun(@Nullable String testClassName, @Nullable String testMethodName) {
    testClass = StringUtil.notNullize(testClassName);
    testMethod = StringUtil.notNullize(testMethodName);
  }

  /** Narrows the run to several suite classes (a file, directory or multi-selection run). */
  public void setSingleRun(@NotNull List<String> testClassNames) {
    setSingleRun(String.join(SUITE_SEPARATOR, testClassNames), null);
  }

  /** The suite classes, joined by {@link #SUITE_SEPARATOR} - one for a gutter run, several for a directory run. */
  public String getTestClass() {
    return testClass;
  }

  @NotNull
  public List<String> getTestClasses() {
    return testClass.isEmpty() ? List.of() : List.of(testClass.split(SUITE_SEPARATOR));
  }

  public String getTestMethod() {
    return testMethod;
  }

  /** Whether this configuration runs selected suites/tests instead of the whole tests build. */
  public boolean hasSingleRun() {
    return !testClass.isEmpty();
  }

  /**
   * Whether the before-run compile is the template compile of
   * {@link HaxeTestSingleRuns} (hxml single runs) rather than the build's
   * own action: a lime-family single run compiles through the tool with the
   * generated main overriding the app's, so it stays an action compile whose
   * extra arguments carry the override.
   */
  public boolean compilesThroughTemplate() {
    if (!hasSingleRun()) return false;
    VirtualFile file = LocalFileSystem.getInstance().findFileByPath(buildFilePath);
    if (file == null || !file.isValid()) return false;
    // the before-run task calls this on a pooled thread
    HaxeBuildFileType type = ReadAction.nonBlocking(() -> HaxeBuildFileScanner.detectType(getProject(), file))
      .executeSynchronously();
    return !LimeProjects.isLimeFamily(type);
  }

  /** The selection, or null for a whole-build run. */
  @Nullable
  HaxeTestSingleRuns.SingleRun singleRun() {
    if (testClass.isEmpty()) return null;
    return new HaxeTestSingleRuns.SingleRun(getTestClasses(), StringUtil.nullize(testMethod));
  }

  /**
   * The template compile the before-run step performs for an hxml single run
   * (see {@link #compilesThroughTemplate}) — the whole command, not extra
   * arguments: the template main replaces the build's own, so the normal
   * action-plus-file resolution cannot be reused. Null when this is a
   * whole-build configuration or the compile cannot be resolved.
   */
  @Nullable
  public HaxeCompileCommands.Resolved resolveSingleRunCompile() {
    HaxeTestSingleRuns.SingleRun run = singleRun();
    if (run == null) return null;
    VirtualFile file = LocalFileSystem.getInstance().findFileByPath(buildFilePath);
    if (file == null || !file.isValid()) return null;
    // the before-run task calls this on a pooled thread
    return ReadAction.nonBlocking(() -> {
        HaxeTestFramework framework = HaxeTestFrameworks.forBuildFile(getProject(), buildFilePath);
        return HaxeTestLaunchPlanner.singleRunCompile(getProject(), file, framework, run);
      })
      .executeSynchronously();
  }

  /**
   * Aligns the compile step with the current tests file and filter: artifact
   * targets get a before-run compile of the tests build file with the framework's
   * reporting/filter defines appended; interp builds (compile IS the run) get
   * none. Call after mutating the build file or filter.
   */
  public void syncCompileStep() {
    setBeforeRunTasks(computedCompileStep());
  }

  /** The before-run tasks {@link #syncCompileStep} would set, computed without mutating (parses build files). */
  @NotNull
  List<BeforeRunTask<?>> computedCompileStep() {
    boolean singleStage = HaxeReadActions.compute(
      () -> HaxeTestLaunchPlanner.isSingleStage(getProject(), buildFilePath));
    if (singleStage) {
      return List.of();
    }
    HaxeActionBeforeRunTaskProvider.Task compileTask = new HaxeActionBeforeRunTaskProvider.Task();
    compileTask.setBuildFilePath(buildFilePath);
    compileTask.setActionName(HaxeReadActions.compute(this::buildActionName));
    // the stored arguments are a snapshot the launch recomputes; a run that cannot compute them fails at launch
    compileTask.setExtraArguments(StringUtil.notNullize(currentCompileArguments()));
    // a multi-section hxml compiles only its selected --next section, so the
    // reporting arguments reach that section instead of the chain's last one
    compileTask.setSectionScoped(true);
    return List.of(compileTask);
  }

  /**
   * The compile arguments for the CURRENT framework/reporter wiring and
   * selection (a lime-family single run's main override included). The
   * before-run task recomputes these at launch — its stored snapshot goes
   * stale whenever the wiring evolves (a plugin update changing the injection
   * would otherwise keep compiling with the old arguments forever). Null
   * when a lime single run's main could not be generated.
   */
  @Nullable
  public String currentCompileArguments() {
    return HaxeReadActions.compute(
      () -> HaxeTestCompileArguments.compileArguments(getProject(), buildFilePath, filterPattern, singleRun()));
  }

  @NotNull
  private String buildActionName() {
    VirtualFile file = LocalFileSystem.getInstance().findFileByPath(buildFilePath);
    HaxeBuildFileType type = file == null ? null : HaxeBuildFileScanner.detectType(getProject(), file);
    return HaxeBuildSystem.of(Objects.requireNonNullElse(type, HaxeBuildFileType.HXML)).defaultBuildActionName();
  }

  @Override
  public @NotNull SettingsEditor<? extends RunConfiguration> getConfigurationEditor() {
    return new HaxeTestRunConfigurationEditor(getProject());
  }

  @Override
  public void checkConfiguration() throws RuntimeConfigurationException {
    // checked from the editor (EDT) and before launches (background threads)
    String problem = HaxeReadActions.compute(this::planProblem);
    if (problem != null) {
      throw new RuntimeConfigurationError(problem);
    }
  }

  /** Why no launch can be planned, or null when it can. Call in a read action. */
  @Nullable
  private String planProblem() {
    try {
      HaxeTestLaunchPlanner.planFor(this);
      return null;
    }
    catch (ExecutionException e) {
      return e.getMessage();
    }
  }

  @Override
  public @Nullable String suggestedName() {
    if (StringUtil.isEmptyOrSpaces(buildFilePath)) return null;
    List<String> suites = getTestClasses();
    if (suites.size() > 1) {
      return HaxeBundle.message("haxe.test.config.suggested.suites.name", suites.size(), PathUtil.getFileName(buildFilePath));
    }
    if (hasSingleRun()) {
      String shortClass = StringUtil.getShortName(testClass);
      String test = testMethod.isEmpty() ? shortClass : shortClass + "." + testMethod;
      return HaxeBundle.message("haxe.test.config.suggested.single.name", test, PathUtil.getFileName(buildFilePath));
    }
    return HaxeBundle.message("haxe.test.config.suggested.name", PathUtil.getFileName(buildFilePath));
  }

  @Override
  public @NotNull SMTRunnerConsoleProperties createTestConsoleProperties(@NotNull Executor executor) {
    return new HaxeTestConsoleProperties(this, executor);
  }

  @Override
  public @Nullable RunProfileState getState(@NotNull Executor executor, @NotNull ExecutionEnvironment environment) {
    return new TestCommandLineState(environment);
  }

  @Override
  public void readExternal(@NotNull Element element) {
    super.readExternal(element);
    buildFilePath = StringUtil.notNullize(JDOMExternalizerUtil.readField(element, BUILD_FILE));
    filterPattern = StringUtil.notNullize(JDOMExternalizerUtil.readField(element, FILTER_PATTERN));
    testClass = StringUtil.notNullize(JDOMExternalizerUtil.readField(element, TEST_CLASS));
    testMethod = StringUtil.notNullize(JDOMExternalizerUtil.readField(element, TEST_METHOD));
  }

  @Override
  public void writeExternal(@NotNull Element element) {
    super.writeExternal(element);
    JDOMExternalizerUtil.writeField(element, BUILD_FILE, buildFilePath);
    JDOMExternalizerUtil.writeField(element, FILTER_PATTERN, filterPattern);
    JDOMExternalizerUtil.writeField(element, TEST_CLASS, testClass);
    JDOMExternalizerUtil.writeField(element, TEST_METHOD, testMethod);
  }

  private final class TestCommandLineState extends CommandLineState {

    private TestCommandLineState(@NotNull ExecutionEnvironment environment) {
      super(environment);
    }

    @Override
    public @NotNull ExecutionResult execute(@NotNull Executor executor, @NotNull ProgramRunner<?> runner)
      throws ExecutionException {
      ProcessHandler processHandler = startProcess();
      SMTRunnerConsoleProperties properties = createTestConsoleProperties(executor);
      BaseTestsOutputConsoleView console =
        SMTestRunnerConnectionUtil.createAndAttachConsole(TEST_FRAMEWORK_NAME, processHandler, properties);
      return new DefaultExecutionResult(console, processHandler, createActions(console, processHandler));
    }

    @Override
    protected @NotNull ProcessHandler startProcess() throws ExecutionException {
      HaxeUnsavedDocuments.saveAll();
      HaxeTestLaunchPlanner.Plan plan = ReadAction.computeBlocking(
        () -> HaxeTestLaunchPlanner.planFor(HaxeTestRunConfiguration.this));
      if (plan.hint() != null) {
        HaxeCommandNotifications.notify(getProject(), plan.hint(), NotificationType.INFORMATION);
      }
      if (plan.browserHosted()) {
        // no debuggee process: the tests run in a served page whose console
        // the host replays; the run ends on the reporter's completion sentinel
        return HaxeBrowserTestSupport.createRunHost(getProject(), HaxeTestLaunchPlanner.browserWebRoot(plan));
      }
      GeneralCommandLine commandLine = new GeneralCommandLine(plan.command())
        .withWorkDirectory(plan.workDirectory());
      KillableColoredProcessHandler processHandler = new KillableColoredProcessHandler(commandLine);
      ProcessTerminatedListener.attach(processHandler);
      return processHandler;
    }
  }
}
