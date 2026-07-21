package com.intellij.plugins.haxe.runner.debugger.hxcpp.vshaxe.jsonrpc;

import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * Encodes/decodes the hxcpp-debug-server wire framing: a 4-byte
 * little-endian body length followed by that many bytes of UTF-8 JSON
 * (see Connection.hx in vshaxe/hxcpp-debugger). The length counts UTF-8
 * bytes, not characters.
 */
public final class JsonRpcFraming {
  /**
   * Refuse frames larger than this: the peer speaks JSON in the tens of
   * kilobytes, so a huge length is a corrupt or misaligned prefix and
   * honouring it would buffer garbage gigabytes.
   */
  static final int MAX_FRAME_BYTES = 64 * 1024 * 1024;

  private JsonRpcFraming() {
  }

  /** Wraps a JSON payload in a length-prefixed frame. */
  public static byte[] encode(String json) {
    byte[] body = json.getBytes(StandardCharsets.UTF_8);
    ByteArrayOutputStream frame = new ByteArrayOutputStream(4 + body.length);
    frame.write(body.length & 0xFF);
    frame.write((body.length >>> 8) & 0xFF);
    frame.write((body.length >>> 16) & 0xFF);
    frame.write((body.length >>> 24) & 0xFF);
    frame.write(body, 0, body.length);
    return frame.toByteArray();
  }

  /**
   * Blocks until one full frame is available and returns its JSON payload.
   * Returns null when the stream ends cleanly before a new frame starts;
   * throws EOFException when the stream ends mid-frame.
   */
  public static String readPayload(InputStream in) throws IOException {
    int first = in.read();
    if (first < 0) {
      return null;
    }
    byte[] rest = in.readNBytes(3);
    if (rest.length < 3) {
      throw new EOFException("Stream ended inside jsonrpc length prefix");
    }
    int length = first
                 | (rest[0] & 0xFF) << 8
                 | (rest[1] & 0xFF) << 16
                 | (rest[2] & 0xFF) << 24;
    if (length < 0 || length > MAX_FRAME_BYTES) {
      throw new IOException("Implausible jsonrpc frame length " + (length & 0xFFFFFFFFL)
                            + " - stream is corrupt or misaligned");
    }
    byte[] body = in.readNBytes(length);
    if (body.length < length) {
      throw new EOFException("Stream ended inside jsonrpc frame body (expected " + length
                             + " bytes, got " + body.length + ")");
    }
    return new String(body, StandardCharsets.UTF_8);
  }
}
