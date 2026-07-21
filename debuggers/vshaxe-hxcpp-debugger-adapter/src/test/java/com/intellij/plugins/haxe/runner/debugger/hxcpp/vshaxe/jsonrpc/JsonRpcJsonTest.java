package com.intellij.plugins.haxe.runner.debugger.hxcpp.vshaxe.jsonrpc;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Map;
import org.junit.Test;

public class JsonRpcJsonTest {

  @Test
  public void encodeRequestCarriesIdMethodParams() {
    String json = JsonRpcJson.encode(new JsonRpcRequest(7, "continue", Map.of("threadId", 0)));
    assertEquals("{\"id\":7,\"method\":\"continue\",\"params\":{\"threadId\":0}}", json);
  }

  @Test
  public void encodeNullParamsAsEmptyObject() {
    String json = JsonRpcJson.encode(new JsonRpcRequest(1, "pause", null));
    assertEquals("{\"id\":1,\"method\":\"pause\",\"params\":{}}", json);
  }

  @Test
  public void decodeSuccessResponse() {
    JsonRpcServerMessage message = JsonRpcJson.decode("{\"id\":3,\"result\":[{\"id\":0,\"name\":\"main\"}]}");
    assertTrue(message instanceof JsonRpcResponse);
    JsonRpcResponse response = (JsonRpcResponse)message;
    assertEquals(3, response.id());
    assertFalse(response.isError());
    assertEquals("main", response.result().get(0).path("name").asString());
  }

  @Test
  public void decodeErrorResponse() {
    JsonRpcServerMessage message = JsonRpcJson.decode(
      "{\"id\":4,\"error\":{\"code\":422,\"message\":\"wrong request\"}}");
    JsonRpcResponse response = (JsonRpcResponse)message;
    assertTrue(response.isError());
    assertEquals(422, response.error().code());
    assertEquals("wrong request", response.error().message());
  }

  @Test
  public void decodeVoidResultResponse() {
    JsonRpcResponse response = (JsonRpcResponse)JsonRpcJson.decode("{\"id\":5}");
    assertFalse(response.isError());
    assertNull(response.result());
  }

  @Test
  public void decodeNotification() {
    JsonRpcServerMessage message = JsonRpcJson.decode(
      "{\"method\":\"breakpointStop\",\"params\":{\"threadId\":2}}");
    assertTrue(message instanceof JsonRpcNotification);
    JsonRpcNotification notification = (JsonRpcNotification)message;
    assertEquals("breakpointStop", notification.method());
    assertEquals(2, notification.params().path("threadId").asInt());
  }

  @Test
  public void decodeResponseEchoingRequestFields() {
    // Server.hx answers by sending the request object back with result/error
    // filled in, so a response also carries method and params — id decides.
    JsonRpcServerMessage message = JsonRpcJson.decode(
      "{\"id\":9,\"method\":\"continue\",\"params\":{\"threadId\":0},\"result\":null}");
    assertTrue(message instanceof JsonRpcResponse);
    JsonRpcResponse response = (JsonRpcResponse)message;
    assertEquals(9, response.id());
    assertFalse(response.isError());
  }

  @Test(expected = IllegalArgumentException.class)
  public void decodeRejectsNeitherIdNorMethod() {
    JsonRpcJson.decode("{\"something\":\"else\"}");
  }
}
