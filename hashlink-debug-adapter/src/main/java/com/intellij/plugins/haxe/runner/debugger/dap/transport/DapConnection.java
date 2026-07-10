package com.intellij.plugins.haxe.runner.debugger.dap.transport;

import com.intellij.plugins.haxe.runner.debugger.dap.DapJson;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.ProtocolMessage;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;

/**
 * A DAP session over one TCP socket: sends and receives whole
 * {@link ProtocolMessage}s. Does not assign seq numbers or match
 * responses to requests — that is {@code DapClient}'s job.
 */
public class DapConnection implements Closeable {
  private final Socket socket;
  private final InputStream in;
  private final OutputStream out;

  public static DapConnection connect(String host, int port, int connectTimeoutMillis) throws IOException {
    Socket socket = new Socket();
    socket.connect(new InetSocketAddress(host, port), connectTimeoutMillis);
    return new DapConnection(socket);
  }

  public DapConnection(Socket socket) throws IOException {
    this.socket = socket;
    this.in = new BufferedInputStream(socket.getInputStream());
    this.out = new BufferedOutputStream(socket.getOutputStream());
  }

  /** Writes one message as a DAP frame. Thread-safe. */
  public void send(ProtocolMessage message) throws IOException {
    byte[] frame = DapFraming.encode(DapJson.encode(message));
    synchronized (out) {
      out.write(frame);
      out.flush();
    }
  }

  /**
   * Blocks until one message is available and returns it decoded.
   * Returns null when the peer closed the connection cleanly.
   */
  public ProtocolMessage receive() throws IOException {
    String payload = DapFraming.readPayload(in);
    return payload == null ? null : DapJson.decode(payload);
  }

  @Override
  public void close() throws IOException {
    socket.close();
  }
}
