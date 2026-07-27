package com.intellij.plugins.haxe.runner.debugger.eval;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
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
   * 64KB - 1. Requests here are small (paths, expressions); anything larger
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
    // the size check above keeps the narrowing to the ui16 prefix lossless
    return ByteBuffer.allocate(Short.BYTES + body.length)
      .order(ByteOrder.LITTLE_ENDIAN)
      .putShort((short)body.length)
      .put(body)
      .array();
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
    // the first byte is already consumed (it distinguishes a clean end of
    // stream from a truncated prefix), so the rest is read in behind it
    byte[] prefix = new byte[Integer.BYTES];
    prefix[0] = (byte)first;
    if (in.readNBytes(prefix, 1, prefix.length - 1) < prefix.length - 1) {
      throw new EOFException("Stream ended inside eval frame length prefix");
    }
    int length = ByteBuffer.wrap(prefix)
      .order(ByteOrder.LITTLE_ENDIAN)
      .getInt();
    // a prefix with the top bit set decodes to a negative int
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
