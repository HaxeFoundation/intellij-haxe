package com.intellij.plugins.haxe.runner.debugger.dap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.intellij.plugins.haxe.runner.debugger.dap.protocol.BreakpointEvent;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.ErrorResponse;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.Event;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.ExitedEvent;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.InitializeRequest;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.InitializeRequestArguments;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.InitializeResponse;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.InitializedEvent;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.LaunchRequest;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.LaunchRequestArguments;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.NextArguments;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.NextRequest;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.NextResponse;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.OutputEvent;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.ProtocolMessage;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.Request;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.SetBreakpointsResponse;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.StackTraceResponse;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.StepInResponse;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.StepOutResponse;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.StoppedEvent;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.TerminatedEvent;
import java.util.List;
import org.junit.Test;

public class DapJsonTest {

  @Test
  public void encodeOmitsNullFields() {
    InitializeRequest request = new InitializeRequest();
    request.setSeq(1);

    String json = DapJson.encode(request);
    assertFalse("null arguments must be omitted", json.contains("arguments"));
    assertTrue(json.contains("\"command\":\"initialize\""));
    assertTrue(json.contains("\"type\":\"request\""));
  }

  @Test
  public void encodeIncludesAssignedArguments() {
    InitializeRequest request = new InitializeRequest();
    request.setSeq(1);
    InitializeRequestArguments arguments = new InitializeRequestArguments();
    arguments.setAdapterID("intellij-haxe");
    request.setArguments(arguments);

    String json = DapJson.encode(request);
    assertTrue(json.contains("\"adapterID\":\"intellij-haxe\""));
    assertFalse("unset optional argument fields must be omitted", json.contains("clientID"));
  }

  @Test
  public void decodeDiscriminatesInitializeResponse() {
    String json = "{\"seq\":1,\"type\":\"response\",\"request_seq\":1,\"success\":true,\"command\":\"initialize\","
                  + "\"body\":{\"supportsConfigurationDoneRequest\":true}}";
    ProtocolMessage message = DapJson.decode(json);
    assertTrue(message instanceof InitializeResponse);
    InitializeResponse response = (InitializeResponse)message;
    assertEquals(1, response.getRequest_seq());
    assertTrue(response.isSuccess());
    assertEquals(Boolean.TRUE, response.getBody().getSupportsConfigurationDoneRequest());
  }

  @Test
  public void decodeDiscriminatesSetBreakpointsResponse() {
    String json = "{\"seq\":2,\"type\":\"response\",\"request_seq\":2,\"success\":true,\"command\":\"setBreakpoints\","
                  + "\"body\":{\"breakpoints\":[{\"id\":1,\"verified\":true,\"line\":10}]}}";
    ProtocolMessage message = DapJson.decode(json);
    assertTrue(message instanceof SetBreakpointsResponse);
    SetBreakpointsResponse response = (SetBreakpointsResponse)message;
    assertEquals(1, response.getBody().getBreakpoints().size());
    assertTrue(response.getBody().getBreakpoints().get(0).isVerified());
    assertEquals(Integer.valueOf(10), response.getBody().getBreakpoints().get(0).getLine());
  }

  @Test
  public void failedResponseDecodesAsErrorResponseRegardlessOfCommand() {
    String json = "{\"seq\":3,\"type\":\"response\",\"request_seq\":3,\"success\":false,\"command\":\"fooBar\","
                  + "\"message\":\"Unrecognized command: fooBar\",\"body\":{\"error\":{\"id\":1000,\"format\":\"Unrecognized command: fooBar\"}}}";
    ProtocolMessage message = DapJson.decode(json);
    assertTrue(message instanceof ErrorResponse);
    ErrorResponse response = (ErrorResponse)message;
    assertFalse(response.isSuccess());
    assertEquals(1000, response.getBody().getError().getId());
  }

  @Test
  public void decodeDiscriminatesInitializedEvent() {
    ProtocolMessage message = DapJson.decode("{\"seq\":2,\"type\":\"event\",\"event\":\"initialized\"}");
    assertTrue(message instanceof InitializedEvent);
  }

  @Test
  public void unknownEventDecodesAsGenericEvent() {
    ProtocolMessage message = DapJson.decode("{\"seq\":9,\"type\":\"event\",\"event\":\"custom\"}");
    assertTrue(message instanceof Event);
    assertEquals("custom", ((Event)message).getEvent());
  }

  @Test
  public void unknownFieldsAreIgnored() {
    ProtocolMessage message = DapJson.decode("{\"seq\":1,\"type\":\"request\",\"command\":\"x\",\"futureField\":123}");
    assertTrue(message instanceof Request);
  }

  @Test(expected = IllegalArgumentException.class)
  public void unknownMessageTypeIsRejected() {
    DapJson.decode("{\"seq\":1,\"type\":\"telegram\"}");
  }

  @Test
  public void launchRequestEncodesTypedArguments() {
    LaunchRequest request = new LaunchRequest();
    request.setSeq(2);
    LaunchRequestArguments arguments = new LaunchRequestArguments();
    arguments.setProgram("/project/out/app.hl");
    arguments.setArgs(List.of("--flag"));
    request.setArguments(arguments);

    String json = DapJson.encode(request);
    assertTrue(json.contains("\"program\":\"/project/out/app.hl\""));
    assertTrue(json.contains("\"args\":[\"--flag\"]"));
    assertFalse("unset cwd must be omitted", json.contains("cwd"));
  }

  @Test
  public void nextRequestEncodesThreadId() {
    NextRequest request = new NextRequest();
    request.setSeq(4);
    NextArguments arguments = new NextArguments();
    arguments.setThreadId(7);
    request.setArguments(arguments);

    String json = DapJson.encode(request);
    assertTrue(json.contains("\"command\":\"next\""));
    assertTrue(json.contains("\"threadId\":7"));
    assertFalse("unset granularity omitted", json.contains("granularity"));
  }

  @Test
  public void decodeDiscriminatesStepResponses() {
    ProtocolMessage next = DapJson.decode(
      "{\"seq\":1,\"type\":\"response\",\"request_seq\":1,\"success\":true,\"command\":\"next\"}");
    assertTrue(next instanceof NextResponse);

    ProtocolMessage stepIn = DapJson.decode(
      "{\"seq\":2,\"type\":\"response\",\"request_seq\":2,\"success\":true,\"command\":\"stepIn\"}");
    assertTrue(stepIn instanceof StepInResponse);

    ProtocolMessage stepOut = DapJson.decode(
      "{\"seq\":3,\"type\":\"response\",\"request_seq\":3,\"success\":true,\"command\":\"stepOut\"}");
    assertTrue(stepOut instanceof StepOutResponse);
  }

  @Test
  public void decodeDiscriminatesStackTraceResponse() {
    String json = "{\"seq\":9,\"type\":\"response\",\"request_seq\":5,\"success\":true,\"command\":\"stackTrace\","
                  + "\"body\":{\"stackFrames\":[{\"id\":0,\"name\":\"Main.main\",\"line\":12,\"column\":1,"
                  + "\"source\":{\"name\":\"Main.hx\",\"path\":\"/p/Main.hx\"}}],\"totalFrames\":1}}";
    ProtocolMessage message = DapJson.decode(json);
    assertTrue(message instanceof StackTraceResponse);
    StackTraceResponse response = (StackTraceResponse)message;
    assertEquals(1, response.getBody().getStackFrames().size());
    assertEquals("Main.main", response.getBody().getStackFrames().get(0).getName());
    assertEquals(12, response.getBody().getStackFrames().get(0).getLine());
  }

  @Test
  public void decodeDiscriminatesStoppedEvent() {
    String json = "{\"seq\":11,\"type\":\"event\",\"event\":\"stopped\","
                  + "\"body\":{\"reason\":\"breakpoint\",\"threadId\":42,\"allThreadsStopped\":true,\"hitBreakpointIds\":[1]}}";
    ProtocolMessage message = DapJson.decode(json);
    assertTrue(message instanceof StoppedEvent);
    StoppedEvent event = (StoppedEvent)message;
    assertEquals("breakpoint", event.getBody().getReason());
    assertEquals(Integer.valueOf(42), event.getBody().getThreadId());
    assertEquals(List.of(1), event.getBody().getHitBreakpointIds());
  }

  @Test
  public void decodeDiscriminatesLifecycleEvents() {
    assertTrue(DapJson.decode("{\"seq\":1,\"type\":\"event\",\"event\":\"terminated\"}") instanceof TerminatedEvent);

    ProtocolMessage exited = DapJson.decode("{\"seq\":2,\"type\":\"event\",\"event\":\"exited\",\"body\":{\"exitCode\":3}}");
    assertTrue(exited instanceof ExitedEvent);
    assertEquals(3, ((ExitedEvent)exited).getBody().getExitCode());

    ProtocolMessage output = DapJson.decode(
      "{\"seq\":3,\"type\":\"event\",\"event\":\"output\",\"body\":{\"category\":\"stdout\",\"output\":\"hi\"}}");
    assertTrue(output instanceof OutputEvent);
    assertEquals("hi", ((OutputEvent)output).getBody().getOutput());

    ProtocolMessage breakpoint = DapJson.decode(
      "{\"seq\":4,\"type\":\"event\",\"event\":\"breakpoint\","
      + "\"body\":{\"reason\":\"changed\",\"breakpoint\":{\"id\":7,\"verified\":true,\"line\":10}}}");
    assertTrue(breakpoint instanceof BreakpointEvent);
    assertTrue(((BreakpointEvent)breakpoint).getBody().getBreakpoint().isVerified());
  }
}
