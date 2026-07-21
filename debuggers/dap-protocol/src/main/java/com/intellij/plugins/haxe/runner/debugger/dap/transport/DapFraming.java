package com.intellij.plugins.haxe.runner.debugger.dap.transport;

import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * Encodes/decodes the DAP wire framing: an ASCII header block
 * ("Content-Length: &lt;n&gt;") terminated by CRLF CRLF, followed by
 * &lt;n&gt; bytes of UTF-8 encoded JSON.
 *
 * CR and LF are written as byte values so the framing is identical on
 * every platform; Content-Length counts UTF-8 bytes, not characters.
 */
public final class DapFraming {
  static final int CR = 13;
  static final int LF = 10;

  private DapFraming() {
  }

  public static byte[] encode(String json) {
    byte[] body = json.getBytes(StandardCharsets.UTF_8);
    byte[] header = ("Content-Length: " + body.length).getBytes(StandardCharsets.US_ASCII);
    ByteArrayOutputStream frame = new ByteArrayOutputStream(header.length + 4 + body.length);
    frame.write(header, 0, header.length);
    frame.write(CR);
    frame.write(LF);
    frame.write(CR);
    frame.write(LF);
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
    String header = readHeaderBlock(in, first);
    int length = parseContentLength(header);
    if (length < 0) {
      throw new IOException("Missing or invalid Content-Length header: " + header);
    }
    byte[] body = in.readNBytes(length);
    if (body.length < length) {
      throw new EOFException("Stream ended inside DAP frame body (expected " + length + " bytes, got " + body.length + ")");
    }
    return new String(body, StandardCharsets.UTF_8);
  }

  /**
   * Extracts the Content-Length value from a header block.
   * Tolerates additional headers and is case-insensitive on the header name.
   * Returns -1 when no valid Content-Length header is present.
   */
  static int parseContentLength(String header) {
    for (String line : header.split(String.valueOf((char)LF))) {
      int separator = line.indexOf(':');
      if (separator <= 0) {
        continue;
      }
      if (!line.substring(0, separator).trim().equalsIgnoreCase("Content-Length")) {
        continue;
      }
      try {
        int length = Integer.parseInt(line.substring(separator + 1).trim());
        if (length >= 0) {
          return length;
        }
      } catch (NumberFormatException ignored) {
        // fall through to -1
      }
    }
    return -1;
  }

  /** Consumes bytes up to and including the CRLF CRLF terminator, returning the header text. */
  private static String readHeaderBlock(InputStream in, int firstByte) throws IOException {
    ByteArrayOutputStream buf = new ByteArrayOutputStream();
    int matched = 0;
    int b = firstByte;
    while (true) {
      int expected = (matched == 0 || matched == 2) ? CR : LF;
      if (b == expected) {
        matched++;
        if (matched == 4) {
          return buf.toString(StandardCharsets.US_ASCII);
        }
      } else {
        matched = (b == CR) ? 1 : 0;
      }
      buf.write(b);
      b = in.read();
      if (b < 0) {
        throw new EOFException("Stream ended inside DAP frame header");
      }
    }
  }
}
