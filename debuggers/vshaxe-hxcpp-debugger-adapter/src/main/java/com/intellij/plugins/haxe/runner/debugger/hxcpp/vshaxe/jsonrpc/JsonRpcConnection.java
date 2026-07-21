package com.intellij.plugins.haxe.runner.debugger.hxcpp.vshaxe.jsonrpc;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;

/**
 * A jsonrpc session over one TCP socket: sends whole {@link JsonRpcRequest}s
 * and receives whole {@link JsonRpcServerMessage}s. Does not assign ids or
 * match responses to requests — that is {@link JsonRpcClient}'s job.
 *
 * In the normal launch flow the debuggee connects to us, so the socket
 * usually comes from a ServerSocket accept rather than {@link #connect}.
 */
public class JsonRpcConnection implements Closeable {
  private final Socket socket;
  private final InputStream in;
  private final OutputStream out;

  public static JsonRpcConnection connect(String host, int port, int connectTimeoutMillis) throws IOException {
    Socket socket = new Socket();
    socket.connect(new InetSocketAddress(host, port), connectTimeoutMillis);
    return new JsonRpcConnection(socket);
  }

  public JsonRpcConnection(Socket socket) throws IOException {
    this.socket = socket;
    this.in = new BufferedInputStream(socket.getInputStream());
    this.out = new BufferedOutputStream(socket.getOutputStream());
  }

  /** Writes one request as a length-prefixed frame. Thread-safe. */
  public void send(JsonRpcRequest request) throws IOException {
    byte[] frame = JsonRpcFraming.encode(JsonRpcJson.encode(request));
    synchronized (out) {
      out.write(frame);
      out.flush();
    }
  }

  /**
   * Blocks until one message is available and returns it decoded.
   * Returns null when the peer closed the connection cleanly.
   */
  public JsonRpcServerMessage receive() throws IOException {
    String payload = JsonRpcFraming.readPayload(in);
    return payload == null ? null : JsonRpcJson.decode(payload);
  }

  @Override
  public void close() throws IOException {
    socket.close();
  }
}
