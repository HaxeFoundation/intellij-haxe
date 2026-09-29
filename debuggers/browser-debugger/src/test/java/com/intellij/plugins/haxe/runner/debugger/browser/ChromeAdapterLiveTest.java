package com.intellij.plugins.haxe.runner.debugger.browser;

import static com.intellij.plugins.haxe.runner.debugger.browser.LiveProbeUtil.awaitStartDebugging;
import static com.intellij.plugins.haxe.runner.debugger.browser.LiveProbeUtil.baseLaunchConfig;
import static com.intellij.plugins.haxe.runner.debugger.browser.LiveProbeUtil.chromiumExe;
import static com.intellij.plugins.haxe.runner.debugger.browser.LiveProbeUtil.dapServerJs;
import static com.intellij.plugins.haxe.runner.debugger.browser.LiveProbeUtil.haxeOnPath;
import static com.intellij.plugins.haxe.runner.debugger.browser.LiveProbeUtil.initializeRequest;
import static com.intellij.plugins.haxe.runner.debugger.browser.LiveProbeUtil.nodeExe;
import static org.junit.jupiter.api.Assertions.*;

import com.intellij.plugins.haxe.runner.debugger.dap.client.DapClient;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.Event;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.events.InitializedEvent;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.requests.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

/// Runs a utest tests build in a Chrome-family browser through the js-debug adapter
/// and checks that the session connects and captures the test output (no
/// breakpoints). Needs a Chrome-family browser, node, haxe and utest.
@DisplayName("Browser debugger: test capture (live)")
public class ChromeAdapterLiveTest {
  private static final long TIMEOUT = 15_000;

  private static final String PROBE_CASE_HX_SOURCE = """
    class ProbeCase extends utest.Test {
    	function testPasses() {
    		utest.Assert.isTrue(true);
    	}
    }
    """;
  private static final String PROBE_MAIN_HX_SOURCE = """
    class ProbeMain {
    	static function main() {
    		utest.UTest.run([new ProbeCase()]);
    	}
    }
    """;

  private Process adapter;
  private int adapterPort;
  private DapClient parent;

  private static Path utestReporterRoot() {
    return reporterRoot("utestLiveReporter");
  }

  private static Path sharedReporterRoot() {
    return reporterRoot("sharedLiveReporter");
  }

  private static Path reporterRoot(String directoryName) {
    return Path.of("../../src/main/resources/testing/" + directoryName).toAbsolutePath().normalize();
  }

  @BeforeEach
  public void spawnAdapter() throws IOException {
    Assumptions.assumeTrue(Files.isRegularFile(nodeExe()), "portable node not provisioned - skipping");
    Assumptions.assumeTrue(Files.isRegularFile(dapServerJs()), "js-debug adapter not provisioned - skipping");
    Assumptions.assumeTrue(chromiumExe() != null, "no chromium-family browser found - skipping");
    Assumptions.assumeTrue(Files.isDirectory(utestReporterRoot()), "reporter sources not found - skipping");

    adapterPort = LiveProbeUtil.freePort();
    List<String> command = List.of(nodeExe().toString(), dapServerJs().toString(), String.valueOf(adapterPort), "127.0.0.1");
    adapter = LiveProbeUtil.spawnAdapterServer(command, dapServerJs().getParent(), "Debug server listening", "adapter");

    parent = LiveProbeUtil.connectWithRetry(adapterPort, (int)TIMEOUT);
  }

  @AfterEach
  public void tearDown() throws Exception {
    if (parent != null) {
      try {
        parent.close();
      } catch (IOException ignored) {
      }
    }
    if (adapter != null) {
      LiveProbeUtil.killTree(adapter);
    }
  }

  @Test
  @Timeout(60)
  @DisplayName("utest reporter streams the protocol and the completion sentinel through the page console")
  public void utestReporterStreamsTheProtocolAndTheCompletionSentinelThroughThePageConsole() throws Exception {
    Assumptions.assumeTrue(haxeOnPath(), "haxe not on PATH - skipping");

    Path fixture = Files.createTempDirectory("haxe-browser-tests");
    Files.writeString(fixture.resolve("ProbeCase.hx"), PROBE_CASE_HX_SOURCE);
    Files.writeString(fixture.resolve("ProbeMain.hx"), PROBE_MAIN_HX_SOURCE);
    Files.writeString(fixture.resolve("index.html"), LiveProbeUtil.INDEX_HTML);

    compileWithReporter(fixture);

    String captured = driveAndCapture(fixture);
    assertTrue(captured.contains("##teamcity[testStarted"), "the reporter's service messages must reach the console capture: " + captured);
    assertTrue(captured.contains("##intellij-haxe[testRunFinished exit='0']"), "the completion sentinel must arrive after the tree: " + captured);
  }

  /// The planner's utest reporting set: teamcity defines + the reporter classpath + the runner patch.
  private static void compileWithReporter(Path fixture) throws Exception {
    List<String> reporterArgs = List.of(
      "-lib", "utest",
      "-cp", utestReporterRoot().toString(),
      "-cp", sharedReporterRoot().toString(),
      "-D", "teamcity", "-D", "teamcity_suite_name=Probe",
      "--macro", "intellij_utest.Macro.init()");
    LiveProbeUtil.compileHaxeJs(fixture, "ProbeMain", "app.js", 60, reporterArgs);
  }

  /// Parent handshake, child session, configurationDone, then output capture until the sentinel (or timeout).
  private String driveAndCapture(Path fixture) throws Exception {
    try (ContentHttpServer content = new ContentHttpServer(fixture)) {
      assertTrue(parent.sendRequest(initializeRequest("chrome"), TIMEOUT).isSuccess(), "parent initialize");

      Map<String, Object> launchConfig = baseLaunchConfig(content.getBaseUrl(), fixture);
      parent.sendRequestNoWait(ConfiguredLaunchRequest.of(launchConfig));

      StartDebuggingRequest startDebugging = awaitStartDebugging(parent, 20_000, TIMEOUT);
      assertNotNull(startDebugging, "no startDebugging reverse request");

      try (DapClient child = LiveProbeUtil.connectWithRetry(adapterPort, (int)TIMEOUT)) {
        assertTrue(child.sendRequest(initializeRequest("chrome"), TIMEOUT).isSuccess(), "child initialize");
        child.sendRequestNoWait(ConfiguredLaunchRequest.of(startDebugging.getArguments().getConfiguration()));

        StringBuilder captured = new StringBuilder();
        long deadline = System.currentTimeMillis() + 60_000;
        boolean configured = false;
        while (System.currentTimeMillis() < deadline) {
          Event event = child.pollEvent(100);
          if (event instanceof InitializedEvent && !configured) {
            configured = true;
            child.sendRequest(new ConfigurationDoneRequest(), TIMEOUT);
          }
          String outputText = LiveProbeUtil.outputTextOf(event);
          if (outputText != null) {
            captured.append(outputText);
            if (captured.toString().contains("##intellij-haxe[testRunFinished")) {
              break;
            }
          }
        }
        return captured.toString();
      }
    }
  }
}
