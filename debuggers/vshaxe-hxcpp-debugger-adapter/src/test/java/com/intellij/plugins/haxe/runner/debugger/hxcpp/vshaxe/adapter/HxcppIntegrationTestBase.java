package com.intellij.plugins.haxe.runner.debugger.hxcpp.vshaxe.adapter;

import com.intellij.plugins.haxe.runner.debugger.dap.DapPaths;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

import com.intellij.plugins.haxe.runner.debugger.dap.client.DapClient;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.Event;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.Request;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.Response;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.Source;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.SourceBreakpoint;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.StackFrame;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.events.*;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.requests.*;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.responses.*;
import com.intellij.plugins.haxe.runner.debugger.dap.transport.DapConnection;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.After;

/**
 * Scaffolding for integration tests against a real debuggee fixture:
 * binds the adapter's listener, wires the DAP loopback pair exactly as the
 * plugin does, spawns the fixture exe, and drains its output for the whole
 * test (an undrained pipe blocks the debuggee once the OS buffer fills).
 * Tests skip when the fixture exe is missing (no haxe/hxcpp toolchain).
 */
abstract class HxcppIntegrationTestBase {
  protected static final long TIMEOUT = 15_000;

  protected HxcppDebugAdapter adapter;
  protected DapClient dapClient;
  protected Process debuggee;
  protected Path fixtureSource;

  private ServerSocket dapListener;
  private final StringBuilder debuggeeOutput = new StringBuilder();

  /**
   * Starts a session around the fixture named by the system property
   * (e.g. {@code hxcpp.fixture.spin.exe}); {@code sourceFile} is the fixture's
   * source under test-fixtures/src for breakpoint-marker lookup.
   */
  protected void launchFixture(String exeProperty, String sourceFile) throws IOException {
    String exeValue = System.getProperty(exeProperty);
    assumeTrue("fixture exe not built (" + exeProperty + "); haxe/hxcpp toolchain missing?",
               exeValue != null && Files.isRegularFile(Path.of(exeValue)));
    Path exe = Path.of(exeValue);
    fixtureSource = Path.of(System.getProperty("hxcpp.fixture.src.dir"), sourceFile);
    int port = Integer.getInteger("hxcpp.fixture.port", 6973);

    // listener must exist before the debuggee starts, or its connect fails
    adapter = new HxcppDebugAdapter("127.0.0.1", port, TIMEOUT);
    dapListener = new ServerSocket(0, 1, InetAddress.getLoopbackAddress());
    Socket clientSide = new Socket("127.0.0.1", dapListener.getLocalPort());
    adapter.start(new DapConnection(dapListener.accept()));
    dapClient = new DapClient(new DapConnection(clientSide));

    debuggee = new ProcessBuilder(exe.toString())
      .directory(exe.getParent().toFile())
      .redirectErrorStream(true)
      .start();
    Thread gobbler = new Thread(() -> {
      try (BufferedReader reader = new BufferedReader(new InputStreamReader(debuggee.getInputStream()))) {
        String line;
        while ((line = reader.readLine()) != null) {
          synchronized (debuggeeOutput) {
            debuggeeOutput.append(line).append('\n');
          }
        }
      } catch (IOException ignored) {
        // process ended
      }
    }, "debuggee-output-gobbler");
    gobbler.setDaemon(true);
    gobbler.start();
  }

  @After
  public void tearDownSession() throws IOException {
    if (debuggee != null && debuggee.isAlive()) {
      debuggee.destroyForcibly();
    }
    if (dapClient != null) {
      dapClient.close();
    }
    if (adapter != null) {
      adapter.close();
    }
    if (dapListener != null) {
      dapListener.close();
    }
  }

  /** initialize (+ initialized event) and launch; the debuggee is then held before main. */
  protected void initializeAndLaunch() throws Exception {
    assertTrue(dapClient.sendRequest(new InitializeRequest(), TIMEOUT).isSuccess());
    awaitEvent(InitializedEvent.class);
    assertTrue("launch failed - did the debuggee connect?",
               dapClient.sendRequest(new LaunchRequest(), TIMEOUT).isSuccess());
  }

  protected Event awaitEvent(Class<? extends Event> type) throws InterruptedException {
    long deadline = System.currentTimeMillis() + TIMEOUT;
    while (System.currentTimeMillis() < deadline) {
      Event event = dapClient.pollEvent(250);
      if (event != null && type.isInstance(event)) {
        return event;
      }
    }
    throw new AssertionError("No " + type.getSimpleName() + " within " + TIMEOUT + " ms; debuggee output so far:\n"
                             + output());
  }

  protected String output() {
    synchronized (debuggeeOutput) {
      return debuggeeOutput.toString();
    }
  }

  /** A stop with its (already fetched) stack; frames are never empty. */
  protected record Stop(StoppedEvent event, List<StackFrame> frames) {
    int threadId() {
      return event.getBody().getThreadId();
    }

    StackFrame top() {
      return frames.get(0);
    }
  }

  /**
   * Awaits a stop whose TOP frame is at {@code line} of the fixture source.
   * Stops elsewhere — observed on slow CI runners as the debuggee's startup
   * hold surfacing late, with a stack of only debugger-internal frames — are
   * released with a continue and waiting resumes, so a test never mistakes
   * such a stray stop for its breakpoint hit.
   */
  protected Stop awaitStopAtLine(int line) throws Exception {
    StringBuilder seen = new StringBuilder();
    for (int attempt = 0; attempt < 5; attempt++) {
      StoppedEvent stopped = (StoppedEvent)awaitEvent(StoppedEvent.class);
      List<StackFrame> frames = stackTrace(stopped.getBody().getThreadId()).getBody().getStackFrames();
      if (!frames.isEmpty() && frames.get(0).getLine() == line) {
        return new Stop(stopped, frames);
      }
      seen.append("\n  reason=").append(stopped.getBody().getReason())
        .append(" threadId=").append(stopped.getBody().getThreadId())
        .append(" top=").append(frames.isEmpty() ? "<no frames>"
                                                 : frames.get(0).getName() + ":" + frames.get(0).getLine());
      sendContinue(stopped.getBody().getThreadId());
    }
    throw new AssertionError("No stop at line " + line + "; stray stops seen:" + seen
                             + "\ndebuggee output so far:\n" + output());
  }

  protected StackTraceResponse stackTrace(int threadId) throws Exception {
    StackTraceArguments arguments = new StackTraceArguments();
    arguments.setThreadId(threadId);
    StackTraceRequest request = new StackTraceRequest();
    request.setArguments(arguments);
    return require(request);
  }

  protected void sendContinue(int threadId) throws Exception {
    ContinueArguments arguments = new ContinueArguments();
    arguments.setThreadId(threadId);
    ContinueRequest request = new ContinueRequest();
    request.setArguments(arguments);
    require(request);
  }

  /** Sends the request and fails with the server's error message rather than a cast error. */
  @SuppressWarnings("unchecked")
  protected <T extends Response> T require(Request request) throws Exception {
    Response response = dapClient.sendRequest(request, TIMEOUT);
    assertTrue("'" + request.getCommand() + "' failed: " + response.getMessage(), response.isSuccess());
    return (T)response;
  }

  /** The 1-based line of a "// bp:<marker>" comment in the fixture source. */
  protected int lineOfMarker(String marker) throws IOException {
    List<String> lines = Files.readAllLines(fixtureSource);
    for (int i = 0; i < lines.size(); i++) {
      if (lines.get(i).contains("// bp:" + marker)) {
        return i + 1;
      }
    }
    throw new AssertionError("No '// bp:" + marker + "' marker in " + fixtureSource);
  }

  protected SetBreakpointsResponse setBreakpoints(SourceBreakpoint... breakpoints) throws Exception {
    Source source = new Source();
    // deliberately IDE-shaped (forward slashes, as VirtualFile.getPath()
    // reports on Windows): the adapter must convert before the server's
    // exact-string path matching
    source.setPath(DapPaths.toForwardSlashes(fixtureSource.toString()));
    SetBreakpointsArguments arguments = new SetBreakpointsArguments();
    arguments.setSource(source);
    arguments.setBreakpoints(List.of(breakpoints));
    SetBreakpointsRequest request = new SetBreakpointsRequest();
    request.setArguments(arguments);
    SetBreakpointsResponse response = require(request);
    assertNotNull(response.getBody());
    return response;
  }

  protected SetBreakpointsResponse setBreakpointLines(int... lines) throws Exception {
    SourceBreakpoint[] breakpoints = new SourceBreakpoint[lines.length];
    for (int i = 0; i < lines.length; i++) {
      breakpoints[i] = new SourceBreakpoint();
      breakpoints[i].setLine(lines[i]);
    }
    return setBreakpoints(breakpoints);
  }
}
