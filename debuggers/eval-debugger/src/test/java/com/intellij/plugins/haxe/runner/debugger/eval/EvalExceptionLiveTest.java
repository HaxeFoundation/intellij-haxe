package com.intellij.plugins.haxe.runner.debugger.eval;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.intellij.plugins.haxe.runner.debugger.dap.protocol.StackFrame;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.events.*;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.requests.*;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.responses.*;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

/**
 * Drives an UNCAUGHT exception through the real adapter, doing exactly what
 * the IDE's reportStopped does on a stop (request threads THEN stackTrace),
 * to catch a stall on the exception path. If the adapter deadlocked, the
 * bounded requests here would time out and FAIL rather than hang forever.
 */
@DisplayName("Eval debugger: exception (live)")
public class EvalExceptionLiveTest extends EvalLiveTestBase {
  private static final int THROW_LINE = 9;

  @Timeout(60)
  @Test
  @DisplayName("uncaught exception stops inspects and terminates without stalling")
  public void uncaughtExceptionStopsInspectsAndTerminatesWithoutStalling() throws Exception {
    InitializeRequest initialize = new InitializeRequest();
    initialize.setArguments(new InitializeRequestArguments());
    assertTrue(request(initialize).isSuccess(), "initialize");
    dapClient.pollEvent(TIMEOUT);
    launch();

    // the IDE sends exception filters (the backend reports it can't honor
    // them, but the request must still not wedge the session)
    SetExceptionBreakpointsRequest exceptions = exceptionBreakpointsRequest(List.of("uncaught"));
    assertTrue(request(exceptions).isSuccess(), "setExceptionBreakpoints");

    configurationDone();

    StoppedEvent stopped = awaitEvent(StoppedEvent.class);
    assertEquals("exception", stopped.getBody().getReason(), "stopped for an exception");
    String description = stopped.getBody().getDescription();
    assertTrue(description != null && description.contains("uncaught-boom"), "carries the thrown text");
    int threadId = stopped.getBody().getThreadId();

    // EXACTLY the IDE's reportStopped sequence — this is where a stall shows
    assertTrue(request(new ThreadsRequest()).isSuccess(), "threads answered at the exception stop");
    StackTraceRequest stackTrace = stackTraceRequest(threadId);
    StackTraceResponse stResponse = (StackTraceResponse)request(stackTrace);
    assertTrue(stResponse.isSuccess(), "stackTrace answered at the exception stop");
    List<StackFrame> frames = stResponse.getBody().getStackFrames();
    assertFalse(frames.isEmpty(), "frames at the exception");
    assertEquals(THROW_LINE, frames.get(0).getLine(), "top frame is the throwing line");

    // resume: the program runs off the uncaught exception and the session ends
    ContinueRequest resume = continueRequest(threadId);
    assertTrue(request(resume).isSuccess(), "continue past the exception");
    awaitTerminated();
    assertTrue(haxe.waitFor(TIMEOUT, TimeUnit.MILLISECONDS), "haxe exited");
  }

  @Override
  protected String fixtureMain() {
    return "EvalThrow";
  }
}
