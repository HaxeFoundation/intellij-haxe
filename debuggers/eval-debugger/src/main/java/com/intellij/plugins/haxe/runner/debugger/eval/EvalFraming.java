package com.intellij.plugins.haxe.runner.debugger.eval;

import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * Encodes/decodes the eval-debugger wire framing, which is ASYMMETRIC
 * (verified against haxe 4.3.7 and the compiler's src/core/socket.ml):
 * requests TO the VM carry a 2-byte little-endian length prefix
 * ({@code Socket.read_string} reads a ui16), while responses/events FROM
 * the VM carry a 4-byte little-endian prefix ({@code send_string} writes
 * an i32). Both directions are UTF-8 JSON-RPC 2.0 payloads; the length
 * counts UTF-8 bytes, not characters.
 */
public final class EvalFraming {
  /**
   * The request prefix is a ui16, so a request payload can never exceed
   * 64KB - 1. Our requests are small (paths, expressions); anything larger
   * indicates a caller bug, not a legitimate message.
   */
  static final int MAX_REQUEST_BYTES = 0xFFFF;

  /**
   * Refuse inbound frames larger than this: the VM speaks JSON in the tens
   * of kilobytes, so a huge length is a corrupt or misaligned prefix and
   * honouring it would buffer garbage gigabytes.
   */
  static final int MAX_RESPONSE_BYTES = 64 * 1024 * 1024;

  private EvalFraming() {
  }

  /** Wraps a request payload in the 2-byte-LE frame the VM reads. */
  public static byte[] encodeRequest(String json) {
    byte[] body = json.getBytes(StandardCharsets.UTF_8);
    if (body.length > MAX_REQUEST_BYTES) {
      throw new IllegalArgumentException("Eval request exceeds the ui16 frame limit ("
                                         + body.length + " bytes): " + json.substring(0, 80) + "...");
    }
    ByteArrayOutputStream frame = new ByteArrayOutputStream(2 + body.length);
    frame.write(body.length & 0xFF);
    frame.write((body.length >>> 8) & 0xFF);
    frame.write(body, 0, body.length);
    return frame.toByteArray();
  }

  /**
   * Blocks until one full VM frame (4-byte-LE prefixed) is available and
   * returns its JSON payload. Returns null when the stream ends cleanly
   * before a new frame starts; throws EOFException when it ends mid-frame.
   */
  public static String readResponse(InputStream in) throws IOException {
    int first = in.read();
    if (first < 0) {
      return null;
    }
    byte[] rest = in.readNBytes(3);
    if (rest.length < 3) {
      throw new EOFException("Stream ended inside eval frame length prefix");
    }
    int length = first
                 | (rest[0] & 0xFF) << 8
                 | (rest[1] & 0xFF) << 16
                 | (rest[2] & 0xFF) << 24;
    if (length < 0 || length > MAX_RESPONSE_BYTES) {
      throw new IOException("Implausible eval frame length " + (length & 0xFFFFFFFFL)
                            + " - stream is corrupt or misaligned");
    }
    byte[] body = in.readNBytes(length);
    if (body.length < length) {
      throw new EOFException("Stream ended inside eval frame body (expected " + length
                             + " bytes, got " + body.length + ")");
    }
    return new String(body, StandardCharsets.UTF_8);
  }
}
