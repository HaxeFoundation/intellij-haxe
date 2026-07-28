package com.intellij.plugins.haxe.runner.debugger.eval;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.ByteArrayInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Pins the ASYMMETRIC wire framing: requests carry a 2-byte LE prefix (the
 * VM's Socket.read_string reads a ui16), responses a 4-byte LE prefix (its
 * send_string writes an i32). Getting either side wrong yields the VM's
 * -32700 "Invalid token" — which is exactly how the asymmetry was found.
 */
@DisplayName("Eval debugger: framing")
public class EvalFramingTest {

  @Test
  @DisplayName("request frame is two byte little endian prefixed")
  public void requestFrameIsTwoByteLittleEndianPrefixed() {
    byte[] frame = EvalFraming.encodeRequest("{\"a\":1}");
    assertEquals(2 + 7, frame.length, "prefix + body");
    assertEquals(7, frame[0], "low length byte");
    assertEquals(0, frame[1], "high length byte");
    assertArrayEquals("{\"a\":1}".getBytes(StandardCharsets.UTF_8), Arrays.copyOfRange(frame, 2, frame.length), "body is the UTF-8 JSON");
  }

  @Test
  @DisplayName("request length counts utf 8 bytes not characters")
  public void requestLengthCountsUtf8BytesNotCharacters() {
    String twoByteChar = "\"é\""; // é is 2 UTF-8 bytes
    byte[] frame = EvalFraming.encodeRequest(twoByteChar);
    assertEquals(4, frame[0], "é counted as 2 bytes");
  }

  @Test
  @DisplayName("oversized request is rejected not truncated")
  public void oversizedRequestIsRejectedNotTruncated() {
    String big = "x".repeat(EvalFraming.MAX_REQUEST_BYTES + 1);
    assertThrows(IllegalArgumentException.class, () -> EvalFraming.encodeRequest(big));
  }

  @Test
  @DisplayName("response frame is four byte little endian prefixed")
  public void responseFrameIsFourByteLittleEndianPrefixed() throws Exception {
    byte[] body = "{\"jsonrpc\":\"2.0\"}".getBytes(StandardCharsets.UTF_8);
    byte[] frame = new byte[4 + body.length];
    frame[0] = (byte)body.length;
    System.arraycopy(body, 0, frame, 4, body.length);
    assertEquals("{\"jsonrpc\":\"2.0\"}",
                 EvalFraming.readResponse(new ByteArrayInputStream(frame)));
  }

  @Test
  @DisplayName("clean end of stream yields null")
  public void cleanEndOfStreamYieldsNull() throws Exception {
    assertNull(EvalFraming.readResponse(new ByteArrayInputStream(new byte[0])));
  }

  @Test
  @DisplayName("truncated prefix and body throw eof")
  public void truncatedPrefixAndBodyThrowEof() {
    assertThrows(EOFException.class,
                 () -> EvalFraming.readResponse(new ByteArrayInputStream(new byte[]{5, 0})));
    assertThrows(EOFException.class,
                 () -> EvalFraming.readResponse(new ByteArrayInputStream(new byte[]{5, 0, 0, 0, 'a'})));
  }

  @Test
  @DisplayName("implausible response length is refused")
  public void implausibleResponseLengthIsRefused() {
    byte[] corrupt = {(byte)0xFF, (byte)0xFF, (byte)0xFF, 0x7F};
    assertThrows(IOException.class, () -> EvalFraming.readResponse(new ByteArrayInputStream(corrupt)), "2GB frame refused");
  }
}
