package com.intellij.plugins.haxe.v2.testing.run;

import com.intellij.execution.BeforeRunTask;
import com.intellij.execution.Executor;
import com.intellij.execution.RunManager;
import com.intellij.execution.RunnerAndConfigurationSettings;
import com.intellij.execution.executors.DefaultDebugExecutor;
import com.intellij.execution.executors.DefaultRunExecutor;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.plugins.haxe.v2.runconfig.HaxeConfigurationLaunches;
import com.intellij.plugins.haxe.v2.buildtools.HaxeBuildClasspaths;
import com.intellij.util.concurrency.AppExecutorUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * The single dispatch every unit-test entry point (tool window tree, container
 * context actions, gutter markers) converges on: find or create
 * the tests build file's run configuration, update its filter, and launch it
 * through the run-configuration machinery so it lands in the dropdown and can
 * be rerun from the main UI.
 */
public final class HaxeTestRunConfigurations {

  private HaxeTestRunConfigurations() {
  }

  @NotNull
  private static RunnerAndConfigurationSettings findOrCreate(@NotNull Project project,
                                                             @NotNull String buildFilePath,
                                                             @Nullable String testClass,
                                                             @Nullable String testMethod) {
    RunManager runManager = RunManager.getInstance(project);
    String wantedClass = StringUtil.notNullize(testClass);
    String wantedMethod = StringUtil.notNullize(testMethod);
    RunnerAndConfigurationSettings settings = runManager.getAllSettings().stream()
      .filter(candidate -> runsSelection(candidate, buildFilePath, wantedClass, wantedMethod))
      .findFirst()
      .orElse(null);

    boolean created = settings == null;
    if (created) {
      HaxeTestConfigurationFactory factory = HaxeTestRunConfigurationType.getInstance().getFactory();
      settings = runManager.createConfiguration("haxe-tests", factory);
    }
    HaxeTestRunConfiguration configuration = (HaxeTestRunConfiguration)settings.getConfiguration();
    configuration.setBuildFilePath(buildFilePath);
    // the filter pattern is deliberately untouched: the configuration's
    // editor owns it, and a launch must not erase what the user typed there
    configuration.setSingleRun(testClass, testMethod);
    if (created) {
      settings.setName(StringUtil.notNullize(configuration.suggestedName(), settings.getName()));
    }
    configuration.syncCompileStep();
    if (created) {
      runManager.addConfiguration(settings);
    }
    return settings;
  }

  private static boolean runsSelection(@NotNull RunnerAndConfigurationSettings candidate,
                                       @NotNull String buildFilePath,
                                       @NotNull String testClass,
                                       @NotNull String testMethod) {
    return candidate.getConfiguration() instanceof HaxeTestRunConfiguration configuration
           && configuration.getBuildFilePath().equals(buildFilePath)
           && configuration.getTestClass().equals(testClass)
           && configuration.getTestMethod().equals(testMethod);
  }

  /** Runs the build file's tests under the Run executor, selecting the configuration in the dropdown. */
  public static void run(@NotNull Project project, @NotNull String buildFilePath) {
    launch(project, buildFilePath, null, null, DefaultRunExecutor.getRunExecutorInstance());
  }

  /** Runs the build file's tests under the Debug executor - only meaningful when {@link #isDebugSupported}. */
  public static void debug(@NotNull Project project, @NotNull String buildFilePath) {
    launch(project, buildFilePath, null, null, DefaultDebugExecutor.getDebugExecutorInstance());
  }

  /** Runs one suite class (or one of its tests, when {@code testMethod} is set) from a gutter marker. */
  public static void runSingle(@NotNull Project project,
                               @NotNull String buildFilePath,
                               @NotNull String testClass,
                               @Nullable String testMethod) {
    launch(project, buildFilePath, testClass, testMethod, DefaultRunExecutor.getRunExecutorInstance());
  }

  /** Debugs one suite class (or one of its tests) from a gutter marker - only meaningful when {@link #isDebugSupported}. */
  public static void debugSingle(@NotNull Project project,
                                 @NotNull String buildFilePath,
                                 @NotNull String testClass,
                                 @Nullable String testMethod) {
    launch(project, buildFilePath, testClass, testMethod, DefaultDebugExecutor.getDebugExecutorInstance());
  }

  private static void launch(@NotNull Project project,
                             @NotNull String buildFilePath,
                             @Nullable String testClass,
                             @Nullable String testMethod,
                             @NotNull Executor executor) {
    RunnerAndConfigurationSettings settings = findOrCreate(project, buildFilePath, testClass, testMethod);
    HaxeConfigurationLaunches.runSelected(project, settings, executor);
  }

  /** Whether the tests build's target has a debug lane (interp, HL, desktop C++ - see {@code HaxeTestDebugRunner}). */
  public static boolean isDebugSupported(@NotNull Project project, @NotNull String buildFilePath) {
    return HaxeTestLaunchPlanner.isDebuggableTarget(project, buildFilePath);
  }

  /** The configuration's tests build file's module, resolved inside a read action; null when unresolvable. */
  @Nullable
  static Module buildFileModule(@NotNull HaxeTestRunConfiguration configuration) {
    String buildFilePath = configuration.getBuildFilePath();
    if (StringUtil.isEmptyOrSpaces(buildFilePath)) return null;
    VirtualFile file = LocalFileSystem.getInstance().findFileByPath(buildFilePath);
    if (file == null) return null;
    return ReadAction.computeBlocking(() -> ModuleUtilCore.findModuleForFile(file, configuration.getProject()));
  }

  /** The tests build's classpath roots, scoping breakpoint binding and frame resolution to THIS build's files. */
  @NotNull
  static List<String> sourceDirectories(@NotNull HaxeTestRunConfiguration configuration) {
    String buildFilePath = configuration.getBuildFilePath();
    if (StringUtil.isEmptyOrSpaces(buildFilePath)) return List.of();
    return ReadAction.computeBlocking(
      () -> HaxeBuildClasspaths.sourceDirectories(configuration.getProject(), buildFilePath));
  }

  /**
   * Recomputes every test configuration's before-run compile step. Artifact
   * targets persist their compile arguments in that step, so a settings change
   * feeding into them (the live-reporter injection) must resync explicitly -
   * otherwise it only applies after the configuration is next edited.
   * The computation parses build files (PSI/VFS), so it runs in the
   * background; only the configuration mutation lands on the EDT.
   */
  public static void resyncCompileSteps(@NotNull Project project) {
    ReadAction.nonBlocking(() -> compileStepUpdates(project))
      .expireWith(project)
      .finishOnUiThread(ModalityState.defaultModalityState(), applications -> applications.forEach(Runnable::run))
      .submit(AppExecutorUtil.getAppExecutorService());
  }

  /** One deferred setBeforeRunTasks per test configuration, computed under the caller's read action. */
  @NotNull
  private static List<Runnable> compileStepUpdates(@NotNull Project project) {
    List<Runnable> applications = new ArrayList<>();
    for (RunnerAndConfigurationSettings settings : RunManager.getInstance(project).getAllSettings()) {
      if (settings.getConfiguration() instanceof HaxeTestRunConfiguration configuration) {
        List<BeforeRunTask<?>> tasks = configuration.computedCompileStep();
        applications.add(() -> configuration.setBeforeRunTasks(tasks));
      }
    }
    return applications;
  }

  /**
   * Recomputes ONE configuration's compile step in the background — the
   * dialog's Apply must not parse build files on the EDT (the platform even
   * applies to validation snapshots while the user types). Nothing needs the
   * result synchronously: the dialog shows no step-derived field, and the
   * before-run task re-derives its arguments at launch.
   */
  public static void resyncCompileStepAsync(@NotNull HaxeTestRunConfiguration configuration) {
    Project project = configuration.getProject();
    ReadAction.nonBlocking(configuration::computedCompileStep)
      .expireWith(project)
      .finishOnUiThread(ModalityState.defaultModalityState(), configuration::setBeforeRunTasks)
      .submit(AppExecutorUtil.getAppExecutorService());
  }
}
