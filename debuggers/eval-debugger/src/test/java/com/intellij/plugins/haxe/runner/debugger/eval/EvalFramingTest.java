package com.intellij.plugins.haxe.runner.debugger.eval;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;

import java.io.ByteArrayInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import org.junit.Test;

/**
 * Pins the ASYMMETRIC wire framing: requests carry a 2-byte LE prefix (the
 * VM's Socket.read_string reads a ui16), responses a 4-byte LE prefix (its
 * send_string writes an i32). Getting either side wrong yields the VM's
 * -32700 "Invalid token" — which is exactly how the asymmetry was found.
 */
public class EvalFramingTest {

  @Test
  public void requestFrameIsTwoByteLittleEndianPrefixed() {
    byte[] frame = EvalFraming.encodeRequest("{\"a\":1}");
    assertEquals("prefix + body", 2 + 7, frame.length);
    assertEquals("low length byte", 7, frame[0]);
    assertEquals("high length byte", 0, frame[1]);
    assertArrayEquals("body is the UTF-8 JSON",
                      "{\"a\":1}".getBytes(StandardCharsets.UTF_8),
                      Arrays.copyOfRange(frame, 2, frame.length));
  }

  @Test
  public void requestLengthCountsUtf8BytesNotCharacters() {
    String twoByteChar = "\"é\""; // é is 2 UTF-8 bytes
    byte[] frame = EvalFraming.encodeRequest(twoByteChar);
    assertEquals("é counted as 2 bytes", 4, frame[0]);
  }

  @Test
  public void oversizedRequestIsRejectedNotTruncated() {
    String big = "x".repeat(EvalFraming.MAX_REQUEST_BYTES + 1);
    assertThrows(IllegalArgumentException.class, () -> EvalFraming.encodeRequest(big));
  }

  @Test
  public void responseFrameIsFourByteLittleEndianPrefixed() throws Exception {
    byte[] body = "{\"jsonrpc\":\"2.0\"}".getBytes(StandardCharsets.UTF_8);
    byte[] frame = new byte[4 + body.length];
    frame[0] = (byte)body.length;
    System.arraycopy(body, 0, frame, 4, body.length);
    assertEquals("{\"jsonrpc\":\"2.0\"}",
                 EvalFraming.readResponse(new ByteArrayInputStream(frame)));
  }

  @Test
  public void cleanEndOfStreamYieldsNull() throws Exception {
    assertNull(EvalFraming.readResponse(new ByteArrayInputStream(new byte[0])));
  }

  @Test
  public void truncatedPrefixAndBodyThrowEof() {
    assertThrows(EOFException.class,
                 () -> EvalFraming.readResponse(new ByteArrayInputStream(new byte[]{5, 0})));
    assertThrows(EOFException.class,
                 () -> EvalFraming.readResponse(new ByteArrayInputStream(new byte[]{5, 0, 0, 0, 'a'})));
  }

  @Test
  public void implausibleResponseLengthIsRefused() {
    byte[] corrupt = {(byte)0xFF, (byte)0xFF, (byte)0xFF, 0x7F};
    assertThrows("2GB frame refused", IOException.class,
                 () -> EvalFraming.readResponse(new ByteArrayInputStream(corrupt)));
  }
}
