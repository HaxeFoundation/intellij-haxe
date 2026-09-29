package com.intellij.plugins.haxe.display.transport;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/// Sends one request to a `haxe --wait <port>` server. It writes every
/// argument followed by `\n`, ends the request with a single `\0` byte, and
/// reads the answer until the server closes the connection.
///
/// Every request uses its own socket and closes it at once; never pool
/// connections. The server handles one connection at a time, and an idle
/// open connection stalls every other client, builds included, until a
/// server-side read timeout.
public final class HaxeDisplayTransport {

  private static final int CONNECT_TIMEOUT_MS = 3_000;

  private HaxeDisplayTransport() {
  }

  public static DisplayResponse request(String host, int port, List<String> args, int readTimeoutMs)
    throws DisplayRequestException {
    byte[] raw = exchange(host, port, encode(args), readTimeoutMs);
    return classify(raw);
  }

  private static byte[] encode(List<String> args) {
    StringBuilder message = new StringBuilder();
    for (String arg : args) {
      message.append(arg).append('\n');
    }
    message.append('\0');
    return message.toString().getBytes(StandardCharsets.UTF_8);
  }

  private static byte[] exchange(String host, int port, byte[] message, int readTimeoutMs)
    throws DisplayRequestException {
    try (Socket socket = new Socket()) {
      socket.connect(new InetSocketAddress(host, port), CONNECT_TIMEOUT_MS);
      socket.setSoTimeout(readTimeoutMs);
      OutputStream out = socket.getOutputStream();
      out.write(message);
      out.flush();
      return readAll(socket.getInputStream());
    } catch (IOException e) {
      throw new DisplayRequestException("Haxe server request to " + host + ":" + port + " failed: " + e.getMessage(), e);
    }
  }

  private static byte[] readAll(InputStream in) throws IOException {
    ByteArrayOutputStream buffer = new ByteArrayOutputStream();
    byte[] chunk = new byte[65536];
    int read;
    while ((read = in.read(chunk)) >= 0) {
      buffer.write(chunk, 0, read);
    }
    return buffer.toByteArray();
  }

  /**
   * Splits the response into lines and sorts each by its first byte. 0x01
   * starts a log line, and a newline inside a log message arrives as another
   * 0x01 byte. 0x02 is the fatal-error marker. Any other line is payload.
   */
  static DisplayResponse classify(byte[] raw) {
    String text = new String(raw, StandardCharsets.UTF_8);
    List<String> logs = new ArrayList<>();
    StringBuilder payload = new StringBuilder();
    boolean hasError = false;
    for (String line : text.split("\n", -1)) {
      if (line.isEmpty()) continue;
      if (line.charAt(0) == 0x01) {
        logs.add(line.substring(1).replace('\u0001', '\n'));
      } else if (line.charAt(0) == 0x02) {
        hasError = true;
      } else {
        if (payload.length() > 0) payload.append('\n');
        payload.append(line);
      }
    }
    return new DisplayResponse(payload.toString(), List.copyOf(logs), hasError);
  }
}
