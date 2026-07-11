package com.intellij.plugins.haxe.runner.debugger.dap.integration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.intellij.plugins.haxe.runner.debugger.dap.client.DapClient;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.Breakpoint;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.ConfigurationDoneRequest;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.ContinueArguments;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.ContinueRequest;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.DisconnectRequest;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.Event;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.ExitedEvent;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.InitializeRequest;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.InitializeRequestArguments;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.LaunchRequest;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.LaunchRequestArguments;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.OutputEvent;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.Response;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.SetBreakpointsArguments;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.SetBreakpointsRequest;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.Source;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.SourceBreakpoint;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.StackTraceArguments;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.StackTraceRequest;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.StackTraceResponse;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.StoppedEvent;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.TerminatedEvent;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.ThreadsRequest;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import org.junit.After;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;

/**
 * Drives the adapter through a real HashLink debug session: launch, breakpoints,
 * stop, stack trace, continue, output and exit. Skipped when the adapter or a
 * HashLink executable is unavailable (see {@link HlExecutableResolver}).
 *
 * The breakpoint line constants mirror test-fixtures/src/Main.hx.
 */
public class DebugLifecycleIntegrationTest {
  private static final String LISTENING_PREFIX = "DAP-ADAPTER-LISTENING:";
  private static final long TIMEOUT = 8_000;
  private static final int FIXTURE_LOOP_LINE = 18;

  private Process adapterProcess;
  private DapClient client;
  private Path fixtureHl;
  private String fixtureSrc;
  private String hlExecutable;

  @Before
  public void startAdapter() throws IOException {
    String adapterProperty = System.getProperty("dap.adapter.hl", "");
    Assume.assumeTrue("adapter bytecode not built - skipping",
                      !adapterProperty.isEmpty() && Files.isRegularFile(Path.of(adapterProperty)));

    String fixtureProperty = System.getProperty("dap.fixture.hl", "");
    Assume.assumeTrue("debuggee fixture not built - skipping",
                      !fixtureProperty.isEmpty() && Files.isRegularFile(Path.of(fixtureProperty)));
    fixtureHl = Path.of(fixtureProperty);
    fixtureSrc = System.getProperty("dap.fixture.src", "");

    Optional<Path> hl = HlExecutableResolver.resolve();
    Assume.assumeTrue("HashLink executable not found - skipping", hl.isPresent());
    hlExecutable = hl.get().toString();

    adapterProcess = new ProcessBuilder(hlExecutable, adapterProperty, "--port", "0")
      .redirectErrorStream(true)
      .start();
    int port = awaitListeningPort();
    client = DapClient.connect("127.0.0.1", port, (int)TIMEOUT);
  }

  @After
  public void stopAdapter() throws Exception {
    if (client != null) {
      try {
        client.close();
      } catch (IOException ignored) {
      }
    }
    if (adapterProcess != null) {
      drainAdapterOutput();
      if (!adapterProcess.waitFor(3, TimeUnit.SECONDS)) {
        adapterProcess.destroyForcibly();
        adapterProcess.waitFor(5, TimeUnit.SECONDS);
      }
    }
  }

  // Surface anything the adapter printed after the port line (nothing reads that
  // pipe during the test, so a crash trace would otherwise be invisible).
  private void drainAdapterOutput() {
    try {
      var in = adapterProcess.getInputStream();
      int available = in.available();
      if (available > 0) {
        byte[] pending = in.readNBytes(available);
        System.out.println("[adapter output] " + new String(pending, StandardCharsets.UTF_8));
      }
    } catch (IOException ignored) {
    }
  }

  @Test
  public void fullBreakpointLifecycle() throws Exception {
    initialize();
    Response launch = launch(fixtureHl.toString());
    assertTrue("launch succeeds: " + launch.getMessage(), launch.isSuccess());

    Response setBreakpoints = setBreakpoints(fixtureSrc, FIXTURE_LOOP_LINE);
    assertTrue("setBreakpoints succeeds", setBreakpoints.isSuccess());
    SetBreakpointsResponseAssert.assertVerified(setBreakpoints);

    Response configurationDone = request(new ConfigurationDoneRequest());
    assertTrue("configurationDone succeeds", configurationDone.isSuccess());

    // The loop body runs three times, so we expect three breakpoint stops.
    List<String> output = new ArrayList<>();
    int stops = 0;
    boolean exited = false;
    boolean terminated = false;
    Integer exitCode = null;

    while (!terminated) {
      Event event = client.pollEvent(TIMEOUT);
      assertNotNull("expected a debug event", event);
      if (event instanceof StoppedEvent stopped) {
        stops++;
        assertEquals("breakpoint", stopped.getBody().getReason());
        int threadId = stopped.getBody().getThreadId();

        if (stops == 1) {
          // verify threads + stack trace on the first stop
          Response threads = request(new ThreadsRequest());
          assertTrue(threads.isSuccess());

          StackTraceRequest stackTraceRequest = new StackTraceRequest();
          StackTraceArguments stackArgs = new StackTraceArguments();
          stackArgs.setThreadId(threadId);
          stackTraceRequest.setArguments(stackArgs);
          Response stackTrace = request(stackTraceRequest);
          assertTrue("stackTrace succeeds", stackTrace.isSuccess());
          StackTraceResponse frames = (StackTraceResponse)stackTrace;
          assertTrue("has at least one frame", frames.getBody().getStackFrames().size() >= 1);
          assertEquals("top frame at breakpoint line", FIXTURE_LOOP_LINE,
                       frames.getBody().getStackFrames().get(0).getLine());
          String topPath = frames.getBody().getStackFrames().get(0).getSource().getPath().replace('\\', '/');
          assertTrue("top frame in Main.hx (" + topPath + ")", topPath.endsWith("Main.hx"));
        }

        // resume
        ContinueRequest continueRequest = new ContinueRequest();
        ContinueArguments continueArgs = new ContinueArguments();
        continueArgs.setThreadId(threadId);
        continueRequest.setArguments(continueArgs);
        Response resumed = request(continueRequest);
        assertTrue("continue succeeds", resumed.isSuccess());
      }
      else if (event instanceof OutputEvent out) {
        output.add(out.getBody().getOutput());
      }
      else if (event instanceof ExitedEvent exit) {
        exited = true;
        exitCode = exit.getBody().getExitCode();
      }
      else if (event instanceof TerminatedEvent) {
        terminated = true;
      }
    }

    assertEquals("breakpoint hit once per loop iteration", 3, stops);
    assertTrue("exited event received", exited);
    assertEquals("clean exit", Integer.valueOf(0), exitCode);
    String allOutput = String.join("", output);
    assertTrue("fixture start printed (" + allOutput + ")", allOutput.contains("fixture-start"));
    assertTrue("fixture total printed", allOutput.contains("fixture-total:3"));

    Response disconnect = request(new DisconnectRequest());
    assertTrue("disconnect succeeds", disconnect.isSuccess());
  }

  @Test
  public void runsToCompletionWithoutBreakpoints() throws Exception {
    initialize();
    assertTrue(launch(fixtureHl.toString()).isSuccess());
    assertTrue(request(new ConfigurationDoneRequest()).isSuccess());

    boolean terminated = false;
    boolean sawOutput = false;
    while (!terminated) {
      Event event = client.pollEvent(TIMEOUT);
      assertNotNull("expected an event before termination", event);
      if (event instanceof OutputEvent out && out.getBody().getOutput().contains("fixture-start")) {
        sawOutput = true;
      }
      else if (event instanceof TerminatedEvent) {
        terminated = true;
      }
    }
    assertTrue("debuggee produced output", sawOutput);
  }

  @Test
  public void nonexistentProgramFailsButAdapterStillServesDisconnect() throws Exception {
    initialize();
    Response launch = launch("does-not-exist.hl");
    assertTrue("launch of a missing program fails", !launch.isSuccess());

    Response disconnect = request(new DisconnectRequest());
    assertTrue("adapter still answers disconnect after a failed launch", disconnect.isSuccess());
  }

  // --- helpers ---

  private void initialize() throws Exception {
    InitializeRequest request = new InitializeRequest();
    InitializeRequestArguments arguments = new InitializeRequestArguments();
    arguments.setAdapterID("intellij-haxe-test");
    request.setArguments(arguments);
    Response response = request(request);
    assertTrue("initialize succeeds", response.isSuccess());
    // drain the initialized event
    Event initialized = client.pollEvent(TIMEOUT);
    assertNotNull("initialized event", initialized);
  }

  private Response launch(String program) throws Exception {
    LaunchRequest request = new LaunchRequest();
    LaunchRequestArguments arguments = new LaunchRequestArguments();
    arguments.setProgram(program);
    arguments.setHlPath(hlExecutable);
    request.setArguments(arguments);
    return request(request);
  }

  private Response setBreakpoints(String sourcePath, int... lines) throws Exception {
    SetBreakpointsRequest request = new SetBreakpointsRequest();
    SetBreakpointsArguments arguments = new SetBreakpointsArguments();
    Source source = new Source();
    source.setPath(sourcePath);
    source.setName(Path.of(sourcePath).getFileName().toString());
    arguments.setSource(source);
    List<SourceBreakpoint> breakpoints = new ArrayList<>();
    for (int line : lines) {
      SourceBreakpoint breakpoint = new SourceBreakpoint();
      breakpoint.setLine(line);
      breakpoints.add(breakpoint);
    }
    arguments.setBreakpoints(breakpoints);
    request.setArguments(arguments);
    return request(request);
  }

  private Response request(com.intellij.plugins.haxe.runner.debugger.dap.protocol.Request request) throws Exception {
    return client.sendRequest(request, TIMEOUT);
  }

  private int awaitListeningPort() throws IOException {
    BufferedReader stdout = new BufferedReader(
      new InputStreamReader(adapterProcess.getInputStream(), StandardCharsets.UTF_8));
    String line;
    while ((line = stdout.readLine()) != null) {
      if (line.startsWith(LISTENING_PREFIX)) {
        return Integer.parseInt(line.substring(LISTENING_PREFIX.length()).trim());
      }
    }
    throw new IOException("Adapter exited before announcing its listening port");
  }

  private static final class SetBreakpointsResponseAssert {
    static void assertVerified(Response response) {
      var body = ((com.intellij.plugins.haxe.runner.debugger.dap.protocol.SetBreakpointsResponse)response).getBody();
      assertTrue("at least one breakpoint returned", body.getBreakpoints().size() >= 1);
      for (Breakpoint breakpoint : body.getBreakpoints()) {
        assertTrue("breakpoint verified", breakpoint.isVerified());
      }
    }
  }
}
