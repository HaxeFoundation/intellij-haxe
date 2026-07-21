package com.intellij.plugins.haxe.runner.debugger.hxcpp.vshaxe.adapter;

import com.intellij.plugins.haxe.runner.debugger.hxcpp.vshaxe.jsonrpc.JsonRpcFraming;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Function;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

/**
 * Plays the debuggee's hxcpp.debug.jsonrpc.Server in adapter tests: connects
 * to the adapter's listener like the real Server does, answers scripted
 * methods, records every request for assertions, and can push notifications.
 *
 * Faithful to Server.hx in one important way: a response is the request
 * object echoed back with result/error filled in, so it still carries
 * method and params.
 */
class FakeHxcppServer implements Closeable {
  private static final ObjectMapper MAPPER = JsonMapper.builder().build();

  private final Socket socket;
  private final InputStream in;
  private final OutputStream out;
  private final Map<String, Function<JsonNode, String>> handlers = new ConcurrentHashMap<>();
  private final List<JsonNode> requests = new CopyOnWriteArrayList<>();
  private volatile boolean closed = false;

  FakeHxcppServer(int adapterPort) throws IOException {
    socket = new Socket("127.0.0.1", adapterPort);
    in = socket.getInputStream();
    out = socket.getOutputStream();
    // Void-result methods every session uses; tests override what they assert on
    handle("continue", params -> "null");
    handle("next", params -> "null");
    handle("stepIn", params -> "null");
    handle("stepOut", params -> "null");
    handle("pause", params -> "null");
    handle("setExceptionOptions", params -> "null");
    handle("switchFrame", params -> "null");
    Thread reader = new Thread(this::serveLoop, "fake-hxcpp-server");
    reader.setDaemon(true);
    reader.start();
  }

  /** Scripts {@code method}: the function gets the params tree, returns result JSON. */
  final void handle(String method, Function<JsonNode, String> handler) {
    handlers.put(method, handler);
  }

  /** Every request seen so far, oldest first ({id, method, params} trees). */
  List<JsonNode> requests() {
    return requests;
  }

  /** The recorded requests for one method. */
  List<JsonNode> requests(String method) {
    return requests.stream().filter(r -> method.equals(r.path("method").asString())).toList();
  }

  /** Pushes a notification (a message without id) to the adapter. */
  void notify(String method, String paramsJson) throws IOException {
    sendRaw("{\"method\":\"" + method + "\",\"params\":" + paramsJson + "}");
  }

  private void serveLoop() {
    try {
      while (true) {
        String payload = JsonRpcFraming.readPayload(in);
        if (payload == null) {
          return;
        }
        JsonNode request = MAPPER.readTree(payload);
        requests.add(request);
        String method = request.path("method").asString();
        Function<JsonNode, String> handler = handlers.get(method);
        String envelope = "\"id\":" + request.path("id").asInt()
                          + ",\"method\":\"" + method + "\",\"params\":" + request.path("params").toString();
        if (handler == null) {
          sendRaw("{" + envelope + ",\"error\":{\"code\":422,\"message\":\"unscripted method " + method + "\"}}");
          continue;
        }
        String response;
        try {
          response = "{" + envelope + ",\"result\":" + handler.apply(request.path("params")) + "}";
        } catch (RuntimeException e) {
          response = "{" + envelope + ",\"error\":{\"code\":500,\"message\":\"" + e.getMessage() + "\"}}";
        }
        sendRaw(response);
      }
    } catch (IOException e) {
      if (!closed) {
        System.err.println("FakeHxcppServer died: " + e);
      }
    }
  }

  private synchronized void sendRaw(String json) throws IOException {
    out.write(JsonRpcFraming.encode(json));
    out.flush();
  }

  @Override
  public void close() throws IOException {
    closed = true;
    socket.close();
  }
}
