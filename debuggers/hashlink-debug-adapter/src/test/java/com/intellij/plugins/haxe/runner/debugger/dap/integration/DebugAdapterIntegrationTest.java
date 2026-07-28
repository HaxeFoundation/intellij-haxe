package com.intellij.plugins.haxe.runner.debugger.dap.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.intellij.plugins.haxe.runner.debugger.dap.protocol.Breakpoint;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.requests.*;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.responses.*;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.Event;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.events.*;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.Request;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.Response;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.Source;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Protocol-level tests against the real adapter WITHOUT a debuggee: initialize
 * handshake, provisional breakpoints, error responses, sequence numbers, and
 * clean shutdown. One protocol behaviour per test.
 */
@DisplayName("HashLink debugger: debug adapter (integration)")
public class DebugAdapterIntegrationTest extends DapIntegrationTestBase {
  @Test
  @DisplayName("initialize replies with capabilities and initialized event")
  public void initializeRepliesWithCapabilitiesAndInitializedEvent() throws Exception {
    Response response = sendInitialize();

    assertTrue(response instanceof InitializeResponse);
    assertTrue(response.isSuccess());
    assertEquals("initialize", response.getCommand());
    assertEquals(Boolean.TRUE, ((InitializeResponse)response).getBody().getSupportsConfigurationDoneRequest());

    Event event = client.pollEvent(TIMEOUT);
    assertNotNull(event, "expected an event after the initialize response");
    assertTrue(event instanceof InitializedEvent);
  }

  @Test
  @DisplayName("set breakpoints before launch are provisional")
  public void setBreakpointsBeforeLaunchAreProvisional() throws Exception {
    // Without a launched program the adapter cannot resolve breakpoints yet, so
    // it answers provisionally (unverified); they are re-verified after launch.
    // The full verified path is covered by DebugLifecycleIntegrationTest.
    sendInitialize();

    SetBreakpointsRequest request = new SetBreakpointsRequest();
    SetBreakpointsArguments arguments = new SetBreakpointsArguments();
    Source source = new Source();
    source.setName("Main.hx");
    source.setPath("/project/src/Main.hx");
    arguments.setSource(source);
    arguments.setBreakpoints(List.of(sourceBreakpoint(10), sourceBreakpoint(20), sourceBreakpoint(30)));
    request.setArguments(arguments);

    Response response = request(request);

    assertTrue(response instanceof SetBreakpointsResponse);
    assertTrue(response.isSuccess());
    List<Breakpoint> breakpoints = ((SetBreakpointsResponse)response).getBody().getBreakpoints();
    assertEquals(3, breakpoints.size());
    assertEquals(Integer.valueOf(10), breakpoints.get(0).getLine());
    assertEquals(Integer.valueOf(30), breakpoints.get(2).getLine());
    for (Breakpoint breakpoint : breakpoints) {
      assertTrue(!breakpoint.isVerified(), "breakpoint should be unverified before launch");
    }
    assertNotEquals(breakpoints.get(0).getId(), breakpoints.get(1).getId());
  }

  @Test
  @DisplayName("configuration done is acknowledged")
  public void configurationDoneIsAcknowledged() throws Exception {
    sendInitialize();
    Response response = request(new ConfigurationDoneRequest());
    assertTrue(response.isSuccess());
    assertEquals("configurationDone", response.getCommand());
  }

  @Test
  @DisplayName("launch without program is rejected")
  public void launchWithoutProgramIsRejected() throws Exception {
    sendInitialize();
    // a launch with no 'program' argument must fail validation rather than hang
    Response response = request(new LaunchRequest());
    assertTrue(!response.isSuccess(), "launch without a program should fail");
  }

  @Test
  @DisplayName("threads returns stub main thread")
  public void threadsReturnsStubMainThread() throws Exception {
    sendInitialize();
    Response response = request(new ThreadsRequest());

    assertTrue(response instanceof ThreadsResponse);
    ThreadsResponse threadsResponse = (ThreadsResponse)response;
    assertEquals(1, threadsResponse.getBody().getThreads().size());
    assertEquals(1, threadsResponse.getBody().getThreads().get(0).getId());
    assertEquals("main", threadsResponse.getBody().getThreads().get(0).getName());
  }

  @Test
  @DisplayName("unknown command yields error response")
  public void unknownCommandYieldsErrorResponse() throws Exception {
    Request request = new Request();
    request.setCommand("fooBar");

    Response response = request(request);

    assertFalse(response.isSuccess());
    assertTrue(response instanceof ErrorResponse);
    assertEquals("fooBar", response.getCommand());
    assertTrue(response.getMessage().contains("fooBar"));
    assertEquals(1000, ((ErrorResponse)response).getBody().getError().getId());
  }

  @Test
  @DisplayName("responses echo request seq and adapter seq is monotonic")
  public void responsesEchoRequestSeqAndAdapterSeqIsMonotonic() throws Exception {
    Response first = sendInitialize();
    Response second = request(new ConfigurationDoneRequest());
    Response third = request(new ThreadsRequest());

    assertEquals(1, first.getRequest_seq());
    assertEquals(2, second.getRequest_seq());
    assertEquals(3, third.getRequest_seq());
    assertTrue(first.getSeq() < second.getSeq(), "adapter seq must increase");
    assertTrue(second.getSeq() < third.getSeq(), "adapter seq must increase");
  }

  @Test
  @DisplayName("disconnect is acknowledged and adapter exits cleanly")
  public void disconnectIsAcknowledgedAndAdapterExitsCleanly() throws Exception {
    sendInitialize();
    Response response = request(new DisconnectRequest());
    assertTrue(response.isSuccess());

    assertTrue(adapterProcess.waitFor(5, TimeUnit.SECONDS), "adapter should exit after disconnect");
    assertEquals(0, adapterProcess.exitValue());
  }

  @Override
  protected boolean needsFixture() {
    return false; // these tests never launch a program
  }

  // Raw initialize without draining the initialized event — the first test
  // asserts on that event itself. Other tests use it where the event is simply
  // skipped by later polls.
  private Response sendInitialize() throws Exception {
    InitializeRequest request = new InitializeRequest();
    InitializeRequestArguments arguments = new InitializeRequestArguments();
    arguments.setAdapterID("intellij-haxe-test");
    arguments.setClientID("junit");
    request.setArguments(arguments);
    return request(request);
  }
}
