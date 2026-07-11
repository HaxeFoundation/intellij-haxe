package com.intellij.plugins.haxe.runner.debugger.dap.integration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.intellij.plugins.haxe.runner.debugger.dap.client.DapClient;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.ConfigurationDoneRequest;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.ContinueArguments;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.ContinueRequest;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.DisconnectRequest;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.Event;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.InitializeRequest;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.InitializeRequestArguments;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.LaunchRequest;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.LaunchRequestArguments;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.NextArguments;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.NextRequest;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.Request;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.Response;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.SetBreakpointsArguments;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.SetBreakpointsRequest;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.Source;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.SourceBreakpoint;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.StackTraceArguments;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.StackTraceRequest;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.StackTraceResponse;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.StepInArguments;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.StepInRequest;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.StepOutArguments;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.StepOutRequest;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.StoppedEvent;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import org.junit.After;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;

/**
 * Drives step over / into / out against a real HashLink debug session. Skipped
 * when the adapter or a HashLink executable is unavailable. Line constants mirror
 * test-fixtures/src/Main.hx.
 */
public class SteppingIntegrationTest {
  private static final String LISTENING_PREFIX = "DAP-ADAPTER-LISTENING:";
  private static final long TIMEOUT = 8_000;
  private static final int FIXTURE_LOOP_LINE = 18; // total = add(total, i)

  private Process adapterProcess;
  private DapClient client;
  private Path fixtureHl;
  private String fixtureSrc;
  private String hlExecutable;

  @Before
  public void startAdapter() throws IOException {
    String adapter = System.getProperty("dap.adapter.hl", "");
    Assume.assumeTrue("adapter bytecode not built - skipping",
                      !adapter.isEmpty() && Files.isRegularFile(Path.of(adapter)));
    String fixtureProperty = System.getProperty("dap.fixture.hl", "");
    Assume.assumeTrue("debuggee fixture not built - skipping",
                      !fixtureProperty.isEmpty() && Files.isRegularFile(Path.of(fixtureProperty)));
    fixtureHl = Path.of(fixtureProperty);
    fixtureSrc = System.getProperty("dap.fixture.src", "");
    Optional<Path> hl = HlExecutableResolver.resolve();
    Assume.assumeTrue("HashLink executable not found - skipping", hl.isPresent());
    hlExecutable = hl.get().toString();

    ProcessBuilder builder = new ProcessBuilder(hlExecutable, adapter, "--port", "0")
      .redirectErrorStream(true);
    builder.environment().put("DAP_ADAPTER_TRACE", "1");
    adapterProcess = builder.start();
    client = DapClient.connect("127.0.0.1", awaitListeningPort(), (int)TIMEOUT);
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
  public void stepInThenOutThenOver() throws Exception {
    // run to the first breakpoint on the loop body line
    initialize();
    assertTrue(launch().isSuccess());
    assertTrue(setBreakpoint(FIXTURE_LOOP_LINE).isSuccess());
    assertTrue(request(new ConfigurationDoneRequest()).isSuccess());

    StoppedEvent atLoop = awaitStopped();
    assertEquals("breakpoint", atLoop.getBody().getReason());
    int thread = atLoop.getBody().getThreadId();
    assertTrue("stopped in main", topFrameName(thread).endsWith("main"));

    // step INTO add()
    Response stepIn = request(stepInRequest(thread));
    assertTrue("stepIn accepted", stepIn.isSuccess());
    StoppedEvent inAdd = awaitStopped();
    assertEquals("step", inAdd.getBody().getReason());
    thread = inAdd.getBody().getThreadId();
    assertTrue("stepped into add (was " + topFrameName(thread) + ")",
               topFrameName(thread).endsWith("add"));

    // step OUT back into main
    assertTrue("stepOut accepted", request(stepOutRequest(thread)).isSuccess());
    StoppedEvent backInMain = awaitStopped();
    assertEquals("step", backInMain.getBody().getReason());
    thread = backInMain.getBody().getThreadId();
    assertTrue("stepped out to main (was " + topFrameName(thread) + ")",
               topFrameName(thread).endsWith("main"));

    // step OVER a line within main
    assertTrue("next accepted", request(nextRequest(thread)).isSuccess());
    StoppedEvent afterNext = awaitStopped();
    assertEquals("step", afterNext.getBody().getReason());
    assertTrue("still in main after step over",
               topFrameName(afterNext.getBody().getThreadId()).endsWith("main"));

    // let it finish
    request(continueRequest(afterNext.getBody().getThreadId()));
    request(new DisconnectRequest());
  }

  // --- helpers ---

  private StoppedEvent awaitStopped() throws Exception {
    while (true) {
      Event event = client.pollEvent(TIMEOUT);
      assertNotNull("expected a stopped event", event);
      if (event instanceof StoppedEvent stopped) {
        return stopped;
      }
      // ignore output/other events while waiting
    }
  }

  private String topFrameName(int threadId) throws Exception {
    StackTraceRequest request = new StackTraceRequest();
    StackTraceArguments args = new StackTraceArguments();
    args.setThreadId(threadId);
    request.setArguments(args);
    StackTraceResponse response = (StackTraceResponse)request(request);
    assertTrue("has frames", response.getBody().getStackFrames().size() >= 1);
    return response.getBody().getStackFrames().get(0).getName();
  }

  private void initialize() throws Exception {
    InitializeRequest request = new InitializeRequest();
    InitializeRequestArguments args = new InitializeRequestArguments();
    args.setAdapterID("intellij-haxe-test");
    request.setArguments(args);
    assertTrue(request(request).isSuccess());
    assertNotNull("initialized event", client.pollEvent(TIMEOUT));
  }

  private Response launch() throws Exception {
    LaunchRequest request = new LaunchRequest();
    LaunchRequestArguments args = new LaunchRequestArguments();
    args.setProgram(fixtureHl.toString());
    args.setHlPath(hlExecutable);
    request.setArguments(args);
    return request(request);
  }

  private Response setBreakpoint(int line) throws Exception {
    SetBreakpointsRequest request = new SetBreakpointsRequest();
    SetBreakpointsArguments args = new SetBreakpointsArguments();
    Source source = new Source();
    source.setPath(fixtureSrc);
    source.setName("Main.hx");
    args.setSource(source);
    SourceBreakpoint bp = new SourceBreakpoint();
    bp.setLine(line);
    args.setBreakpoints(List.of(bp));
    request.setArguments(args);
    return request(request);
  }

  private NextRequest nextRequest(int threadId) {
    NextRequest request = new NextRequest();
    NextArguments args = new NextArguments();
    args.setThreadId(threadId);
    request.setArguments(args);
    return request;
  }

  private StepInRequest stepInRequest(int threadId) {
    StepInRequest request = new StepInRequest();
    StepInArguments args = new StepInArguments();
    args.setThreadId(threadId);
    request.setArguments(args);
    return request;
  }

  private StepOutRequest stepOutRequest(int threadId) {
    StepOutRequest request = new StepOutRequest();
    StepOutArguments args = new StepOutArguments();
    args.setThreadId(threadId);
    request.setArguments(args);
    return request;
  }

  private ContinueRequest continueRequest(int threadId) {
    ContinueRequest request = new ContinueRequest();
    ContinueArguments args = new ContinueArguments();
    args.setThreadId(threadId);
    request.setArguments(args);
    return request;
  }

  private Response request(Request request) throws Exception {
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
}
