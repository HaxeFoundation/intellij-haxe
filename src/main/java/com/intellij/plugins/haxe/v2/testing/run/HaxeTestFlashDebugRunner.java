package com.intellij.plugins.haxe.v2.testing.run;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.configurations.RunProfile;
import com.intellij.execution.configurations.RunProfileState;
import com.intellij.execution.configurations.RunnerSettings;
import com.intellij.execution.executors.DefaultDebugExecutor;
import com.intellij.execution.process.NopProcessHandler;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.execution.runners.GenericProgramRunner;
import com.intellij.execution.testframework.sm.SMTestRunnerConnectionUtil;
import com.intellij.execution.testframework.sm.runner.SMTRunnerConsoleProperties;
import com.intellij.execution.testframework.ui.BaseTestsOutputConsoleView;
import com.intellij.execution.ui.RunContentDescriptor;
import com.intellij.openapi.application.ReadAction;
import com.intellij.plugins.haxe.util.HaxeReadActions;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.plugins.haxe.HaxeBundle;
import com.intellij.plugins.haxe.config.HaxeTarget;
import com.intellij.plugins.haxe.runner.debugger.flash.HaxeFlashDebuggingUtil;
import com.intellij.plugins.haxe.runner.debugger.flash.FlexPluginGate;
import com.intellij.plugins.haxe.v2.testing.run.HaxeTestLaunchPlanner.Plan;
import java.util.List;
import org.jetbrains.annotations.NotNull;

/**
 * Debug executor for flash-family unit-test configurations — registered ahead
 * of {@link HaxeTestDebugRunner} and claiming only the FLASH launch target,
 * which fdb hosts instead of a DAP adapter: fdb waits, adl launches the
 * {@code -debug} tests swf (default mode, no {@code -nodebug}), and the swf
 * dials fdb as it starts. In that mode the app's traces — the TeamCity test
 * protocol included — arrive on fdb's console instead of process stdout, so
 * the session console is an SM test console fed from those relayed lines
 * (see {@code HaxeFlashDebuggingUtil.getAirTestDescriptor}).
 *
 * The flex-touching code stays behind {@link FlexPluginGate}; keep it that
 * way, or a flex-less IDE throws NoClassDefFoundError instead of the readable
 * message.
 */
public class HaxeTestFlashDebugRunner extends GenericProgramRunner<RunnerSettings> {

  public static final String RUNNER_ID = "HaxeTestFlashDebugRunner";

  @NotNull
  @Override
  public String getRunnerId() {
    return RUNNER_ID;
  }

  @Override
  public boolean canRun(@NotNull String executorId, @NotNull RunProfile profile) {
    return DefaultDebugExecutor.EXECUTOR_ID.equals(executorId)
           && profile instanceof HaxeTestRunConfiguration configuration
           && launchesFlash(configuration);
  }

  private static boolean launchesFlash(@NotNull HaxeTestRunConfiguration configuration) {
    String buildFilePath = configuration.getBuildFilePath();
    if (StringUtil.isEmptyOrSpaces(buildFilePath)) return false;
    // runner selection happens on the EDT and on background threads alike
    HaxeTarget target = HaxeReadActions.compute(() -> HaxeTestLaunchPlanner.launchTarget(configuration.getProject(), buildFilePath));
    return target == HaxeTarget.FLASH;
  }

  @Override
  protected RunContentDescriptor doExecute(@NotNull RunProfileState state, @NotNull ExecutionEnvironment environment)
    throws ExecutionException {
    HaxeTestRunConfiguration configuration = (HaxeTestRunConfiguration)environment.getRunProfile();
    String flexSdkName = FlexPluginGate.requireFlexSdkName(configuration.getProject());
    Module module = HaxeTestRunConfigurations.buildFileModule(configuration);
    if (module == null) {
      throw new ExecutionException(HaxeBundle.message("haxe.test.debug.no.module"));
    }

    Plan plan = ReadAction.computeBlocking(() -> HaxeTestLaunchPlanner.planForDebug(configuration));
    GeneralCommandLine adlCommandLine = new GeneralCommandLine(plan.command())
      .withWorkDirectory(plan.workDirectory());
    List<String> sourceDirectories = HaxeTestRunConfigurations.sourceDirectories(configuration);

    // the test console: an SM view attached to a synthetic handler the debug
    // session feeds fdb's relayed trace lines into (fdb owns the real process)
    NopProcessHandler testOutputSink = new NopProcessHandler();
    SMTRunnerConsoleProperties properties = configuration.createTestConsoleProperties(environment.getExecutor());
    BaseTestsOutputConsoleView testConsole = SMTestRunnerConnectionUtil.createAndAttachConsole(
      HaxeTestRunConfiguration.TEST_FRAMEWORK_NAME, testOutputSink, properties);
    testOutputSink.startNotify();

    return HaxeFlashDebuggingUtil.getAirTestDescriptor(
      module, environment, flexSdkName, adlCommandLine, sourceDirectories, testConsole, testOutputSink);
  }
}
