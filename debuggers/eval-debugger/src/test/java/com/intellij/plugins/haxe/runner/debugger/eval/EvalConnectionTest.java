package com.intellij.plugins.haxe.runner.debugger.eval;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * Drives EvalConnection against an in-process FAKE VM on piped streams: the
 * fake reads 2-byte-LE-framed requests and answers with 4-byte-LE-framed
 * responses, mirroring the real asymmetric framing end to end.
 */
@DisplayName("Eval debugger: connection")
public class EvalConnectionTest {
  private static final ObjectMapper MAPPER = new ObjectMapper();

  private EvalConnection connection;
  private Thread fakeVm;

  @AfterEach
  public void tearDown() {
    if (connection != null) {
      connection.close();
    }
  }

  @Test
  @DisplayName("correlates responses by id and unwraps result")
  public void correlatesResponsesByIdAndUnwrapsResult() throws Exception {
    startFake(request -> {
      assertEquals("2.0", request.path("jsonrpc").asString(), "strict envelope");
      int id = request.path("id").asInt();
      return List.of("""
          {
            "jsonrpc": "2.0",
            "id": %d,
            "result": [
              { "id": 0, "name": "Thread 0" }
            ]
          }""".formatted(id));
    });

    connection.start();
    JsonNode result = connection.request("getThreads", null, 5000);
    assertEquals("Thread 0", result.get(0).path("name").asString());
  }

  @Test
  @DisplayName("error responses surface as protocol exceptions")
  public void errorResponsesSurfaceAsProtocolExceptions() throws Exception {
    startFake(request -> {
      int id = request.path("id").asInt();
      return List.of("""
          {
            "jsonrpc": "2.0",
            "id": %d,
            "error": { "code": -32601, "message": "Method not found" }
          }""".formatted(id));
    });

    connection.start();
    EvalProtocolException error = assertThrows(EvalProtocolException.class,
                                               () -> connection.request("bogus", null, 5000));
    assertEquals(-32601, error.getCode());
    assertTrue(error.getMessage().contains("Method not found"));
  }

  @Test
  @DisplayName("id less messages route to the event listener")
  public void idLessMessagesRouteToTheEventListener() throws Exception {
    startFake(request -> {
      // reply to the request, then push an unrelated notification
      int id = request.path("id").asInt();
      return List.of(
          """
          {
            "jsonrpc": "2.0",
            "id": %d,
            "result": null
          }""".formatted(id),
          """
          {
            "jsonrpc": "2.0",
            "method": "breakpointStop",
            "params": { "threadId": 0 }
          }""");
    });

    BlockingQueue<String> events = new LinkedBlockingQueue<>();
    connection.setEventListener((method, params) -> events.add(method + ":" + params.path("threadId").asInt(-1)));

    connection.start();
    connection.request("continue", null, 5000);
    assertEquals("breakpointStop:0", events.poll(5, TimeUnit.SECONDS));
  }

  @Test
  @DisplayName("transport death fails pending requests")
  public void transportDeathFailsPendingRequests() throws Exception {
    startFake(request -> null); // never answers
    connection.start();

    Thread killer = new Thread(() -> {
      try {
        Thread.sleep(300);
      } catch (InterruptedException ignored) {
      }
      connection.close();
    });
    killer.setDaemon(true);
    killer.start();

    assertThrows(IOException.class, () -> connection.request("getThreads", null, 10_000));
  }

  /**
   * Starts a fake VM whose reply is computed from each decoded request: the
   * function returns the JSON messages to send back (each framed on its own),
   * or null to stay silent.
   */
  private void startFake(Function<JsonNode, List<String>> replyFor) throws IOException {
    PipedInputStream vmSees = new PipedInputStream(1 << 16);
    PipedOutputStream toVm = new PipedOutputStream(vmSees);
    PipedInputStream weSee = new PipedInputStream(1 << 16);
    PipedOutputStream fromVm = new PipedOutputStream(weSee);

    fakeVm = new Thread(() -> {
      try {
        while (true) {
          int low = vmSees.read();
          if (low < 0) {
            return;
          }
          int high = vmSees.read();
          byte[] body = vmSees.readNBytes(low | (high << 8));
          JsonNode request = MAPPER.readTree(new String(body, StandardCharsets.UTF_8));
          List<String> reply = replyFor.apply(request);
          if (reply != null) {
            for (String message : reply) {
              byte[] replyBody = message.getBytes(StandardCharsets.UTF_8);
              fromVm.write(new byte[]{(byte)replyBody.length, (byte)(replyBody.length >>> 8),
                                      (byte)(replyBody.length >>> 16), (byte)(replyBody.length >>> 24)});
              fromVm.write(replyBody);
            }
            fromVm.flush();
          }
        }
      } catch (IOException ignored) {
        // pipe closed: test over
      }
    }, "fake-eval-vm");
    fakeVm.setDaemon(true);
    fakeVm.start();

    connection = new EvalConnection(weSee, toVm);
  }
}
