package com.intellij.plugins.haxe.runner.debugger.eval;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.intellij.plugins.haxe.runner.debugger.dap.client.DapClient;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.Event;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.Request;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.Response;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.Source;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.StackFrame;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.events.*;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.requests.*;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.responses.*;
import com.intellij.plugins.haxe.runner.debugger.dap.transport.DapConnection;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.After;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;

/**
 * Drives an UNCAUGHT exception through the real adapter, doing exactly what
 * the IDE's reportStopped does on a stop (request threads THEN stackTrace),
 * to catch a stall on the exception path. If the adapter deadlocked, the
 * bounded requests here would time out and FAIL rather than hang forever.
 */
public class EvalExceptionLiveTest {
  private static final int THROW_LINE = 9;
  private static final long TIMEOUT = 15_000;

  private EvalDebugAdapter adapter;
  private DapClient dapClient;
  private ServerSocket dapListener;
  private Process haxe;

  private static boolean haxeOnPath() {
    try {
      Process probe = new ProcessBuilder("haxe", "--version").redirectErrorStream(true).start();
      return probe.waitFor(10, TimeUnit.SECONDS) && probe.exitValue() == 0;
    } catch (Exception e) {
      return false;
    }
  }

  private static Path fixtureDir() {
    String fromGradle = System.getProperty("eval.fixture.src.dir");
    return fromGradle != null ? Path.of(fromGradle) : Path.of("test-fixtures").toAbsolutePath();
  }

  @Before
  public void wire() throws IOException {
    Assume.assumeTrue("haxe not on PATH - skipping", haxeOnPath());
    Path fixtures = fixtureDir();
    Assume.assumeTrue("throw fixture missing - skipping", Files.isRegularFile(fixtures.resolve("EvalThrow.hx")));

    adapter = new EvalDebugAdapter(TIMEOUT);
    haxe = new ProcessBuilder("haxe", "-cp", fixtures.toString(), "-main", "EvalThrow",
                              "-D", "eval-debugger=127.0.0.1:" + adapter.getVmPort(), "--interp")
      .redirectErrorStream(true).start();
    dapListener = new ServerSocket(0, 1, InetAddress.getLoopbackAddress());
    Socket clientSide = new Socket(InetAddress.getLoopbackAddress(), dapListener.getLocalPort());
    adapter.start(new DapConnection(dapListener.accept()));
    dapClient = new DapClient(new DapConnection(clientSide));
  }

  @After
  public void tearDown() throws Exception {
    if (dapClient != null) {
      try {
        dapClient.close();
      } catch (IOException ignored) {
      }
    }
    if (adapter != null) {
      adapter.close();
    }
    if (haxe != null && !haxe.waitFor(3, TimeUnit.SECONDS)) {
      haxe.descendants().forEach(ProcessHandle::destroyForcibly);
      haxe.destroyForcibly();
      haxe.waitFor(5, TimeUnit.SECONDS);
    }
    if (dapListener != null) {
      dapListener.close();
    }
  }

  private Response request(Request request) throws Exception {
    return dapClient.sendRequest(request, TIMEOUT);
  }

  private <T extends Event> T awaitEvent(Class<T> type) throws Exception {
    long deadline = System.currentTimeMillis() + TIMEOUT;
    while (System.currentTimeMillis() < deadline) {
      Event event = dapClient.pollEvent(250);
      if (type.isInstance(event)) {
        return type.cast(event);
      }
    }
    throw new AssertionError("no " + type.getSimpleName() + " within " + TIMEOUT + "ms");
  }

  @Test(timeout = 60_000)
  public void uncaughtExceptionStopsInspectsAndTerminatesWithoutStalling() throws Exception {
    InitializeRequest initialize = new InitializeRequest();
    initialize.setArguments(new InitializeRequestArguments());
    assertTrue("initialize", request(initialize).isSuccess());
    dapClient.pollEvent(TIMEOUT);
    assertTrue("launch", request(new LaunchRequest()).isSuccess());

    // the IDE sends exception filters (our backend reports it can't honor
    // them, but the request must still not wedge the session)
    SetExceptionBreakpointsRequest exceptions = new SetExceptionBreakpointsRequest();
    SetExceptionBreakpointsArguments exArgs = new SetExceptionBreakpointsArguments();
    exArgs.setFilters(List.of("uncaught"));
    exceptions.setArguments(exArgs);
    assertTrue("setExceptionBreakpoints", request(exceptions).isSuccess());

    assertTrue("configurationDone", request(new ConfigurationDoneRequest()).isSuccess());

    StoppedEvent stopped = awaitEvent(StoppedEvent.class);
    assertEquals("stopped for an exception", "exception", stopped.getBody().getReason());
    assertTrue("carries the thrown text", stopped.getBody().getDescription() != null
                                          && stopped.getBody().getDescription().contains("uncaught-boom"));
    int threadId = stopped.getBody().getThreadId();

    // EXACTLY the IDE's reportStopped sequence — this is where a stall shows
    assertTrue("threads answered at the exception stop", request(new ThreadsRequest()).isSuccess());
    StackTraceRequest stackTrace = new StackTraceRequest();
    StackTraceArguments stArgs = new StackTraceArguments();
    stArgs.setThreadId(threadId);
    stackTrace.setArguments(stArgs);
    StackTraceResponse stResponse = (StackTraceResponse)request(stackTrace);
    assertTrue("stackTrace answered at the exception stop", stResponse.isSuccess());
    List<StackFrame> frames = stResponse.getBody().getStackFrames();
    assertFalse("frames at the exception", frames.isEmpty());
    assertEquals("top frame is the throwing line", THROW_LINE, frames.get(0).getLine());

    // resume: the program runs off the uncaught exception and the session ends
    ContinueRequest resume = new ContinueRequest();
    ContinueArguments cArgs = new ContinueArguments();
    cArgs.setThreadId(threadId);
    resume.setArguments(cArgs);
    assertTrue("continue past the exception", request(resume).isSuccess());
    awaitEvent(TerminatedEvent.class);
    assertTrue("haxe exited", haxe.waitFor(TIMEOUT, TimeUnit.MILLISECONDS));
  }
}
