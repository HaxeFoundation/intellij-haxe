package com.intellij.plugins.haxe.runner.debugger.hxcpp.vshaxe.jsonrpc;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Drives {@link JsonRpcClient} against a scripted fake server on a loopback
 * socket: the test thread plays the server through {@link #serverIn}/{@link #serverOut}.
 */
public class JsonRpcClientTest {
  private static final long TIMEOUT = 5_000;

  private ServerSocket listener;
  private JsonRpcClient client;
  private Socket serverSide;
  private InputStream serverIn;
  private OutputStream serverOut;

  @Before
  public void setUp() throws IOException {
    listener = new ServerSocket(0);
    Socket clientSocket = new Socket("127.0.0.1", listener.getLocalPort());
    serverSide = listener.accept();
    serverIn = serverSide.getInputStream();
    serverOut = serverSide.getOutputStream();
    client = new JsonRpcClient(new JsonRpcConnection(clientSocket));
  }

  @After
  public void tearDown() throws IOException {
    client.close();
    serverSide.close();
    listener.close();
  }

  private void serverSend(String json) throws IOException {
    serverOut.write(JsonRpcFraming.encode(json));
    serverOut.flush();
  }

  private JsonNode serverReceive() throws IOException {
    String payload = JsonRpcFraming.readPayload(serverIn);
    assertNotNull("server side saw EOF", payload);
    return JsonMapper.builder().build().readTree(payload);
  }

  @Test
  public void requestGetsItsResponse() throws Exception {
    CountDownLatch requestSeen = new CountDownLatch(1);
    Thread server = new Thread(() -> {
      try {
        JsonNode request = serverReceive();
        assertEquals("threads", request.path("method").asString());
        requestSeen.countDown();
        serverSend("{\"id\":" + request.path("id").asInt() + ",\"result\":[{\"id\":0,\"name\":\"main\"}]}");
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    });
    server.start();

    JsonNode result = client.call("threads", null, TIMEOUT);
    assertTrue(requestSeen.await(1, TimeUnit.SECONDS));
    assertEquals("main", result.get(0).path("name").asString());
    server.join(TIMEOUT);
  }

  @Test
  public void responsesAreMatchedByIdNotArrivalOrder() throws Exception {
    // two concurrent requests answered in reverse order must land with their owners
    Thread server = new Thread(() -> {
      try {
        JsonNode first = serverReceive();
        JsonNode second = serverReceive();
        serverSend("{\"id\":" + second.path("id").asInt() + ",\"result\":\"for-" + second.path("method").asString() + "\"}");
        serverSend("{\"id\":" + first.path("id").asInt() + ",\"result\":\"for-" + first.path("method").asString() + "\"}");
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    });
    server.start();

    String[] results = new String[2];
    Thread alpha = new Thread(() -> results[0] = callQuietly("alpha"));
    alpha.start();
    // the fake server reads frames sequentially, so make the send order deterministic
    Thread.sleep(200);
    Thread beta = new Thread(() -> results[1] = callQuietly("beta"));
    beta.start();

    alpha.join(TIMEOUT);
    beta.join(TIMEOUT);
    server.join(TIMEOUT);
    assertEquals("for-alpha", results[0]);
    assertEquals("for-beta", results[1]);
  }

  private String callQuietly(String method) {
    try {
      return client.call(method, null, TIMEOUT).asString();
    } catch (IOException | InterruptedException e) {
      throw new RuntimeException(e);
    }
  }

  @Test
  public void notificationsAreQueuedNotDropped() throws Exception {
    serverSend("{\"method\":\"threadStart\",\"params\":{\"threadId\":1}}");
    serverSend("{\"method\":\"breakpointStop\",\"params\":{\"threadId\":1}}");

    JsonRpcNotification first = client.pollNotification(TIMEOUT);
    JsonRpcNotification second = client.pollNotification(TIMEOUT);
    assertNotNull(first);
    assertNotNull(second);
    assertEquals("threadStart", first.method());
    assertEquals("breakpointStop", second.method());
    assertNull(client.pollNotification(50));
  }

  @Test
  public void notificationArrivingBeforeResponseDoesNotStealIt() throws Exception {
    Thread server = new Thread(() -> {
      try {
        JsonNode request = serverReceive();
        serverSend("{\"method\":\"pauseStop\",\"params\":{\"threadId\":0}}");
        serverSend("{\"id\":" + request.path("id").asInt() + ",\"result\":null}");
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    });
    server.start();

    JsonRpcResponse response = client.sendRequest("pause", null, TIMEOUT);
    assertEquals(false, response.isError());
    JsonRpcNotification notification = client.pollNotification(TIMEOUT);
    assertNotNull(notification);
    assertEquals("pauseStop", notification.method());
    server.join(TIMEOUT);
  }

  @Test
  public void errorResponseSurfacesAsException() throws Exception {
    Thread server = new Thread(() -> {
      try {
        JsonNode request = serverReceive();
        serverSend("{\"id\":" + request.path("id").asInt()
                   + ",\"error\":{\"code\":422,\"message\":\"wrong request\"}}");
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    });
    server.start();

    try {
      client.call("bogus", null, TIMEOUT);
      fail("expected JsonRpcErrorException");
    } catch (JsonRpcErrorException e) {
      assertEquals("bogus", e.getMethod());
      assertEquals(422, e.getError().code());
    }
    server.join(TIMEOUT);
  }

  @Test
  public void missingResponseTimesOutWithMethodInMessage() throws Exception {
    try {
      client.sendRequest("threads", null, 100);
      fail("expected timeout IOException");
    } catch (IOException e) {
      assertTrue(e.getMessage(), e.getMessage().contains("threads"));
    }
    // drain the unanswered request so teardown doesn't race the reader
    JsonNode unanswered = serverReceive();
    assertEquals("threads", unanswered.path("method").asString());
  }

  @Test
  public void inFlightRequestFailsFastWhenTheConnectionDies() throws Exception {
    // the timeout is deliberately huge: the failure must come from the
    // connection death, not from waiting out the timer
    CompletableFuture<Exception> failure = new CompletableFuture<>();
    Thread caller = new Thread(() -> {
      try {
        client.sendRequest("threads", null, 60_000);
        failure.complete(null);
      } catch (Exception e) {
        failure.complete(e);
      }
    });
    caller.start();
    serverReceive(); // the request is on the wire; the caller is parked
    serverSide.close(); // the debuggee dies mid-request

    Exception e = failure.get(2, TimeUnit.SECONDS); // must beat the 60s timer by far
    assertNotNull("the in-flight request must fail, not report success", e);
    assertTrue("an honest message, not a timeout: " + e.getMessage(),
               e.getMessage().contains("connection"));
    caller.join(TIMEOUT);
  }

  @Test
  public void requestAfterConnectionDeathFailsImmediately() throws Exception {
    serverSide.close();
    long deadline = System.currentTimeMillis() + TIMEOUT;
    while (!client.isConnectionFinished() && System.currentTimeMillis() < deadline) {
      Thread.sleep(10);
    }
    assertTrue("reader noticed the death", client.isConnectionFinished());

    long start = System.currentTimeMillis();
    try {
      client.sendRequest("threads", null, 60_000);
      fail("expected IOException");
    } catch (IOException e) {
      assertTrue(e.getMessage(), e.getMessage().contains("connection"));
    }
    assertTrue("failed by the flag, not the timer", System.currentTimeMillis() - start < 5_000);
  }

  @Test
  public void utf8SurvivesTheWire() throws Exception {
    Thread server = new Thread(() -> {
      try {
        JsonNode request = serverReceive();
        assertEquals("æøå 🐛", request.path("params").path("expr").asString());
        serverSend("{\"id\":" + request.path("id").asInt() + ",\"result\":\"rød grød\"}");
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    });
    server.start();

    JsonNode result = client.call("evaluate",
                                  Map.of("expr", "æøå 🐛", "frameId", 0), TIMEOUT);
    assertEquals("rød grød", new String(result.asString().getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8));
    server.join(TIMEOUT);
  }
}
