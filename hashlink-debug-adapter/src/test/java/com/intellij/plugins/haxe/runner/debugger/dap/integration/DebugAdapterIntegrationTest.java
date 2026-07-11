package com.intellij.plugins.haxe.runner.debugger.dap.integration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.intellij.plugins.haxe.runner.debugger.dap.protocol.Breakpoint;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.requests.ConfigurationDoneRequest;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.requests.DisconnectRequest;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.responses.ErrorResponse;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.Event;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.requests.InitializeRequest;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.requests.InitializeRequestArguments;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.responses.InitializeResponse;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.events.InitializedEvent;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.requests.LaunchRequest;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.Request;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.Response;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.requests.SetBreakpointsArguments;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.requests.SetBreakpointsRequest;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.responses.SetBreakpointsResponse;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.Source;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.requests.ThreadsRequest;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.responses.ThreadsResponse;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.Test;

/**
 * Protocol-level tests against the real adapter WITHOUT a debuggee: initialize
 * handshake, provisional breakpoints, error responses, sequence numbers, and
 * clean shutdown. One protocol behaviour per test.
 */
public class DebugAdapterIntegrationTest extends DapIntegrationTestBase {

  @Override
  protected boolean needsFixture() {
    return false; // these tests never launch a program
  }

  @Test
  public void initializeRepliesWithCapabilitiesAndInitializedEvent() throws Exception {
    Response response = sendInitialize();

    assertTrue(response instanceof InitializeResponse);
    assertTrue(response.isSuccess());
    assertEquals("initialize", response.getCommand());
    assertEquals(Boolean.TRUE, ((InitializeResponse)response).getBody().getSupportsConfigurationDoneRequest());

    Event event = client.pollEvent(TIMEOUT);
    assertNotNull("expected an event after the initialize response", event);
    assertTrue(event instanceof InitializedEvent);
  }

  @Test
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
      assertTrue("breakpoint should be unverified before launch", !breakpoint.isVerified());
    }
    assertNotEquals(breakpoints.get(0).getId(), breakpoints.get(1).getId());
  }

  @Test
  public void configurationDoneIsAcknowledged() throws Exception {
    sendInitialize();
    Response response = request(new ConfigurationDoneRequest());
    assertTrue(response.isSuccess());
    assertEquals("configurationDone", response.getCommand());
  }

  @Test
  public void launchWithoutProgramIsRejected() throws Exception {
    sendInitialize();
    // a launch with no 'program' argument must fail validation rather than hang
    Response response = request(new LaunchRequest());
    assertTrue("launch without a program should fail", !response.isSuccess());
  }

  @Test
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
  public void responsesEchoRequestSeqAndAdapterSeqIsMonotonic() throws Exception {
    Response first = sendInitialize();
    Response second = request(new ConfigurationDoneRequest());
    Response third = request(new ThreadsRequest());

    assertEquals(1, first.getRequest_seq());
    assertEquals(2, second.getRequest_seq());
    assertEquals(3, third.getRequest_seq());
    assertTrue("adapter seq must increase", first.getSeq() < second.getSeq());
    assertTrue("adapter seq must increase", second.getSeq() < third.getSeq());
  }

  @Test
  public void disconnectIsAcknowledgedAndAdapterExitsCleanly() throws Exception {
    sendInitialize();
    Response response = request(new DisconnectRequest());
    assertTrue(response.isSuccess());

    assertTrue("adapter should exit after disconnect", adapterProcess.waitFor(5, TimeUnit.SECONDS));
    assertEquals(0, adapterProcess.exitValue());
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
