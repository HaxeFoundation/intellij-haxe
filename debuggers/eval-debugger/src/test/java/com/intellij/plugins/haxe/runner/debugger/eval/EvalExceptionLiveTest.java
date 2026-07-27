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
public class EvalExceptionLiveTest extends EvalLiveTestBase {
  @Override
  protected String fixtureMain() {
    return "EvalThrow";
  }

  private static final int THROW_LINE = 9;


  @Test(timeout = 60_000)
  public void uncaughtExceptionStopsInspectsAndTerminatesWithoutStalling() throws Exception {
    InitializeRequest initialize = new InitializeRequest();
    initialize.setArguments(new InitializeRequestArguments());
    assertTrue("initialize", request(initialize).isSuccess());
    dapClient.pollEvent(TIMEOUT);
    launch();

    // the IDE sends exception filters (the backend reports it can't honor
    // them, but the request must still not wedge the session)
    SetExceptionBreakpointsRequest exceptions = exceptionBreakpointsRequest(List.of("uncaught"));
    assertTrue("setExceptionBreakpoints", request(exceptions).isSuccess());

    configurationDone();

    StoppedEvent stopped = awaitEvent(StoppedEvent.class);
    assertEquals("stopped for an exception", "exception", stopped.getBody().getReason());
    String description = stopped.getBody().getDescription();
    assertTrue("carries the thrown text", description != null && description.contains("uncaught-boom"));
    int threadId = stopped.getBody().getThreadId();

    // EXACTLY the IDE's reportStopped sequence — this is where a stall shows
    assertTrue("threads answered at the exception stop", request(new ThreadsRequest()).isSuccess());
    StackTraceRequest stackTrace = stackTraceRequest(threadId);
    StackTraceResponse stResponse = (StackTraceResponse)request(stackTrace);
    assertTrue("stackTrace answered at the exception stop", stResponse.isSuccess());
    List<StackFrame> frames = stResponse.getBody().getStackFrames();
    assertFalse("frames at the exception", frames.isEmpty());
    assertEquals("top frame is the throwing line", THROW_LINE, frames.get(0).getLine());

    // resume: the program runs off the uncaught exception and the session ends
    ContinueRequest resume = continueRequest(threadId);
    assertTrue("continue past the exception", request(resume).isSuccess());
    awaitTerminated();
    assertTrue("haxe exited", haxe.waitFor(TIMEOUT, TimeUnit.MILLISECONDS));
  }

  @Override
  protected String fixtureMain() {
    return "EvalThrow";
  }
}
