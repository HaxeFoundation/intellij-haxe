package com.intellij.plugins.haxe.runner.debugger.dap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.intellij.plugins.haxe.runner.debugger.dap.protocol.ErrorResponse;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.Event;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.InitializeRequest;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.InitializeRequestArguments;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.InitializeResponse;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.InitializedEvent;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.ProtocolMessage;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.Request;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.SetBreakpointsResponse;
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
}
