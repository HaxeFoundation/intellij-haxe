package com.intellij.plugins.haxe.runner.nodejs;

import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.executors.DefaultDebugExecutor;
import com.intellij.execution.executors.DefaultRunExecutor;
import com.intellij.plugins.haxe.HaxeLightFixtureTestCase;
import com.intellij.plugins.haxe.runner.HaxeRunConfigurationType;
import com.intellij.plugins.haxe.runner.nodejs.NodeDebugBackend;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Node.js debugger: debug runner")
public class NodeDebugRunnerTest extends HaxeLightFixtureTestCase {
  // any existing executable: the test inspects the command line, it never starts it
  private static final String NODE_STAND_IN = ProcessHandle.current().info().command().orElseThrow();

  @Override
  protected String getBasePath() {
    return "";
  }

  @Test
  @DisplayName("debug runner claims node configurations under debug only")
  public void testDebugRunnerClaimsNodeConfigurationsUnderDebugOnly() {
    NodeRunConfiguration configuration = newConfiguration();
    NodeDebugRunner runner = new NodeDebugRunner();

    assertTrue(runner.canRun(DefaultDebugExecutor.EXECUTOR_ID, configuration));
    assertFalse(runner.canRun(DefaultRunExecutor.EXECUTOR_ID, configuration));
  }

  @Test
  @DisplayName("debug command line holds the script at the inspector port")
  public void testDebugCommandLineHoldsTheScriptAtTheInspectorPort() throws Exception {
    NodeRunConfiguration configuration = newConfiguration();
    configuration.setModule(myFixture.getModule());
    configuration.setExecutablePath(NODE_STAND_IN);
    configuration.setProgramArguments("out/server.js --verbose");
    NodeDebugBackend backend = new NodeDebugBackend(Path.of(NODE_STAND_IN), 9229, null);

    GeneralCommandLine commandLine = new NodeDebugRunner().createCommandLine(configuration, backend);

    assertEquals(List.of("--inspect-brk=9229", "out/server.js", "--verbose"), commandLine.getParametersList().getList());
  }

  private NodeRunConfiguration newConfiguration() {
    HaxeRunConfigurationType type = HaxeRunConfigurationType.getInstance();
    return new NodeRunConfiguration("node", getProject(), new NodeConfigurationFactory(type));
  }
}
