package com.intellij.plugins.haxe.v2.testing.run;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.module.Module;
import com.intellij.plugins.haxe.HaxeBundle;
import com.intellij.plugins.haxe.HaxeDebuggerBundle;
import com.intellij.plugins.haxe.config.HaxeTarget;
import com.intellij.plugins.haxe.runner.debugger.dap.ide.DapBackend;
import com.intellij.plugins.haxe.runner.debugger.dap.ide.DapDebugRunnerBase;
import com.intellij.plugins.haxe.runner.debugger.hashlink.HashLinkBackend;
import com.intellij.plugins.haxe.runner.debugger.hashlink.HashLinkDebugRunner;
import com.intellij.plugins.haxe.runner.debugger.hashlink.HlExecutableResolver;
import com.intellij.plugins.haxe.runner.debugger.browser.BrowserDebugBackend;
import com.intellij.plugins.haxe.runner.debugger.browser.HaxeBrowserTestSupport;
import com.intellij.plugins.haxe.runner.debugger.browser.NodeTestDebugBackend;
import com.intellij.plugins.haxe.runner.debugger.hxcpp.intellij.HxcppIntellijBackend;
import com.intellij.plugins.haxe.runner.debugger.interp.InterpDapBackend;
import com.intellij.plugins.haxe.v2.testing.run.HaxeTestLaunchPlanner.Plan;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import org.jetbrains.annotations.NotNull;

/**
 * Debug executor for Haxe unit-test configurations, dispatching on the launch
 * plan's target. An HL artifact debugs exactly like a HashLink session
 * ({@code hl --debug <port> --debug-wait}, bundled adapter attached by pid); a
 * single-stage interp build debugs through the eval lane (the compile IS the
 * debuggee: haxe spawned with {@code -D eval-debugger} pointing at the
 * in-process adapter, which holds execution until breakpoints are installed);
 * a desktop C++ binary debugs through its embedded intellij-hxcpp-debug-server
 * (compiled in by the before-run step's debug additions, connecting out via
 * the HXCPP_DEBUG_HOST/PORT env vars); a js artifact runs under
 * {@code node --inspect-brk} with the pinned vscode-js-debug adapter attached
 * to the inspector port. In every lane {@code DapDebugProcess} makes the
 * session console an SM test console (see {@code DapTestConsoles}) — the
 * TeamCity messages on the debuggee's stdout drive the test tree while
 * breakpoints work. For artifact targets, the before-run compile step attached
 * by {@code HaxeTestRunConfiguration.syncCompileStep()} builds with the
 * framework defines (plus the debug additions) before the session starts.
 * Flash tests debug through {@link HaxeTestFlashDebugRunner} (fdb, not DAP),
 * registered ahead of this runner and claiming the flash-family configurations.
 */
public class HaxeTestDebugRunner extends DapDebugRunnerBase<HaxeTestRunConfiguration, DapBackend> {
  public static final String RUNNER_ID = "HaxeTestDebugRunner";

  /** Generous: the eval VM connects during compiler startup, typically instantly. */
  private static final long VM_CONNECT_TIMEOUT_MILLIS = 30_000;

  /** Generous: the hxcpp debuggee's embedded server connects during process startup, typically instantly. */
  private static final int DEBUGGEE_CONNECT_TIMEOUT_MILLIS = 30_000;

  @NotNull
  @Override
  public String getRunnerId() {
    return RUNNER_ID;
  }

  @Override
  protected Class<HaxeTestRunConfiguration> configurationClass() {
    return HaxeTestRunConfiguration.class;
  }

  // one launch's plan, computed in validate and reused by the later hooks of
  // the same doExecute sequence - planning parses build files under a read
  // action, too costly to pay three times per launch. Weak keys: a failed
  // launch never reaches the last hook, so entries must not pin their
  // configuration.
  private final Map<HaxeTestRunConfiguration, Plan> plannedLaunches =
    Collections.synchronizedMap(new WeakHashMap<>());

  @Override
  protected void validate(HaxeTestRunConfiguration configuration) throws ExecutionException {
    Plan plan = debuggablePlan(configuration);
    plannedLaunches.put(configuration, plan);
    boolean needsSdkRuntime = plan.target() == HaxeTarget.HL && HaxeTestLaunchPlanner.packagedHlBoot(plan) == null;
    if (needsSdkRuntime) {
      resolveHlExecutable(configuration);
    }
  }

  @Override
  protected DapBackend createBackend(HaxeTestRunConfiguration configuration) throws ExecutionException {
    Plan plan = plannedLaunch(configuration);
    try {
      return switch (plan.target()) {
        case HL -> hashLinkBackend(configuration, plan);
        case CPP -> hxcppBackend(configuration);
        case JAVA_SCRIPT -> jsBackend(configuration, plan);
        default -> new InterpDapBackend(VM_CONNECT_TIMEOUT_MILLIS);
      };
    }
    catch (IOException e) {
      throw new ExecutionException(HaxeDebuggerBundle.message("interp.runner.listener.failed", e.getMessage()));
    }
  }

  @Override
  protected GeneralCommandLine createCommandLine(HaxeTestRunConfiguration configuration, DapBackend backend)
    throws ExecutionException {
    Plan plan = plannedLaunch(configuration);
    return switch (backend) {
      // the adapter launches the browser itself; nothing is spawned here
      case BrowserDebugBackend ignored -> null;
      case HashLinkBackend hashLink -> hashLinkCommandLine(configuration, plan, hashLink);
      case HxcppIntellijBackend hxcpp -> hxcppCommandLine(plan, hxcpp);
      case NodeTestDebugBackend node -> nodeCommandLine(plan, node);
      case InterpDapBackend interp -> interpCommandLine(plan, interp);
      default -> throw new IllegalStateException("no command line for backend " + backend.getClass().getName());
    };
  }

  @NotNull
  private static DapBackend hashLinkBackend(HaxeTestRunConfiguration configuration, @NotNull Plan plan)
    throws ExecutionException, IOException {
    List<String> sourceDirectories = HaxeTestRunConfigurations.sourceDirectories(configuration);
    return new HashLinkBackend(hlRuntime(configuration, plan), hlProgram(plan),
                               HashLinkDebugRunner.findFreePort(), sourceDirectories);
  }

  @NotNull
  private static DapBackend hxcppBackend(HaxeTestRunConfiguration configuration) throws IOException {
    List<String> sourceDirectories = HaxeTestRunConfigurations.sourceDirectories(configuration);
    return new HxcppIntellijBackend(DEBUGGEE_CONNECT_TIMEOUT_MILLIS, sourceDirectories);
  }

  /** Browser-hosted html5 packages debug through the shared browser lane; node-hosted artifacts attach at --inspect-brk. */
  @NotNull
  private static DapBackend jsBackend(HaxeTestRunConfiguration configuration, @NotNull Plan plan)
    throws ExecutionException, IOException {
    if (plan.browserHosted()) {
      // TODO: browser-hosted test output - the browser adapters route program output through the
      //  CDP connection, so the SM console sees the TeamCity stream only once debugger output
      //  events are replayed into the process handler
      return HaxeBrowserTestSupport.createBackend(configuration.getProject(), HaxeTestLaunchPlanner.browserWebRoot(plan));
    }
    return new NodeTestDebugBackend(Path.of(plan.command().getFirst()), HashLinkDebugRunner.findFreePort(), plan.workDirectory());
  }

  @NotNull
  private static GeneralCommandLine hashLinkCommandLine(HaxeTestRunConfiguration configuration,
                                                        @NotNull Plan plan,
                                                        @NotNull HashLinkBackend backend) throws ExecutionException {
    return new GeneralCommandLine()
      .withExePath(hlRuntime(configuration, plan).toString())
      .withParameters("--debug", Integer.toString(backend.getDebugPort()), "--debug-wait", hlProgram(plan).toString())
      .withWorkDirectory(plan.workDirectory());
  }

  /** The binary's embedded debug server (compiled in by the debug additions) connects out to the backend's listener during startup. */
  @NotNull
  private static GeneralCommandLine hxcppCommandLine(@NotNull Plan plan, @NotNull HxcppIntellijBackend backend) {
    return new GeneralCommandLine(plan.command())
      .withWorkDirectory(plan.workDirectory())
      .withEnvironment(HxcppIntellijBackend.ENV_DEBUG_HOST, backend.getHost())
      .withEnvironment(HxcppIntellijBackend.ENV_DEBUG_PORT, Integer.toString(backend.getPort()));
  }

  /** The plan's [node, artifact] with the inspector hold inserted; the adapter attaches to the port and releases the hold once configured. */
  @NotNull
  private static GeneralCommandLine nodeCommandLine(@NotNull Plan plan, @NotNull NodeTestDebugBackend backend) {
    return new GeneralCommandLine()
      .withExePath(plan.command().getFirst())
      .withParameters("--inspect-brk=" + backend.getInspectorPort(), plan.command().get(1))
      .withWorkDirectory(plan.workDirectory());
  }

  /** The plan's compile command (framework defines included) IS the debuggee - the eval VM inside it connects out to the adapter's port. */
  @NotNull
  private static GeneralCommandLine interpCommandLine(@NotNull Plan plan, @NotNull InterpDapBackend backend) {
    List<String> command = new ArrayList<>(plan.command());
    command.add("-D");
    command.add("eval-debugger=127.0.0.1:" + backend.getVmPort());
    return new GeneralCommandLine(command).withWorkDirectory(plan.workDirectory());
  }

  /** This launch's plan from validate's run; recomputed only if the entry was collected in between. */
  @NotNull
  private Plan plannedLaunch(HaxeTestRunConfiguration configuration) throws ExecutionException {
    Plan plan = plannedLaunches.get(configuration);
    return plan != null ? plan : debuggablePlan(configuration);
  }

  /**
   * The launch plan, required to be a debuggable shape: an HL artifact or lime
   * HL package, a desktop C++ binary, a node-hosted js artifact, or a
   * single-stage interp build.
   */
  @NotNull
  private static Plan debuggablePlan(HaxeTestRunConfiguration configuration) throws ExecutionException {
    Plan plan = ReadAction.computeBlocking(() -> HaxeTestLaunchPlanner.planForDebug(configuration));
    // every js tests plan that materializes is node-hosted ([node, artifact]);
    // browser-hosted html5 builds are refused at PLAN time with their own message
    boolean debuggable = plan.singleStage()
      || plan.target() == HaxeTarget.CPP
      || plan.target() == HaxeTarget.JAVA_SCRIPT
      || HaxeTestLaunchPlanner.hlArtifact(plan) != null
      || HaxeTestLaunchPlanner.packagedHlBoot(plan) != null;
    if (!debuggable) {
      throw new ExecutionException(HaxeBundle.message("haxe.test.debug.unsupported.target"));
    }
    return plan;
  }

  /**
   * The runtime that executes the HL debuggee AND the bundled adapter: the
   * SDK-resolved hl for a plain artifact; for a lime package the bundled
   * runtime beside the bytecode - it carries the app's .hdll libraries, which
   * the SDK's plain hl lacks.
   */
  @NotNull
  private static Path hlRuntime(HaxeTestRunConfiguration configuration, @NotNull Plan plan) throws ExecutionException {
    return HaxeTestLaunchPlanner.packagedHlBoot(plan) != null
           ? Path.of(plan.command().getFirst())
           : resolveHlExecutable(configuration);
  }

  /** The HL bytecode to debug: the plain artifact, or the lime package's hlboot.dat. */
  @NotNull
  private static Path hlProgram(@NotNull Plan plan) throws ExecutionException {
    Path artifact = HaxeTestLaunchPlanner.hlArtifact(plan);
    if (artifact != null) return artifact;
    Path boot = HaxeTestLaunchPlanner.packagedHlBoot(plan);
    if (boot != null) return boot;
    throw new ExecutionException(HaxeBundle.message("haxe.test.debug.unsupported.target"));
  }

  /** Same precedence as the plain run's launch command: the SDK-configured HashLink, then env, then PATH. */
  @NotNull
  private static Path resolveHlExecutable(HaxeTestRunConfiguration configuration) throws ExecutionException {
    Module module = HaxeTestRunConfigurations.buildFileModule(configuration);
    return HlExecutableResolver.resolve(module)
      .orElseThrow(() -> new ExecutionException(HaxeBundle.message("haxe.test.debug.no.hl")));
  }
}
