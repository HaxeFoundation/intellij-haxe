package com.intellij.plugins.haxe.runner.debugger.hxcpp.vshaxe.jsonrpc;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("HXCPP debugger (vshaxe): json rpc framing")
public class JsonRpcFramingTest {

  @Test
  @DisplayName("encode prefixes little endian length")
  public void encodePrefixesLittleEndianLength() {
    byte[] frame = JsonRpcFraming.encode("{}");
    assertArrayEquals(new byte[]{2, 0, 0, 0, '{', '}'}, frame);
  }

  @Test
  @DisplayName("encode length bytes are little endian across byte boundaries")
  public void encodeLengthBytesAreLittleEndianAcrossByteBoundaries() {
    // 300 = 0x012C — proves the low byte comes first
    byte[] frame = JsonRpcFraming.encode("x".repeat(300));
    assertEquals(0x2C, frame[0] & 0xFF);
    assertEquals(0x01, frame[1] & 0xFF);
    assertEquals(0, frame[2]);
    assertEquals(0, frame[3]);
    assertEquals(4 + 300, frame.length);
  }

  @Test
  @DisplayName("round trip ascii")
  public void roundTripAscii() throws IOException {
    String payload = "{\"id\":1,\"method\":\"threads\",\"params\":{}}";
    InputStream in = new ByteArrayInputStream(JsonRpcFraming.encode(payload));
    assertEquals(payload, JsonRpcFraming.readPayload(in));
  }

  @Test
  @DisplayName("length counts utf 8 bytes")
  public void lengthCountsUtf8Bytes() throws IOException {
    String payload = "{\"name\":\"æøå\"}";
    byte[] frame = JsonRpcFraming.encode(payload);
    int expectedByteLength = payload.getBytes(StandardCharsets.UTF_8).length;
    assertEquals(expectedByteLength, frame[0] & 0xFF);
    assertEquals(payload, JsonRpcFraming.readPayload(new ByteArrayInputStream(frame)));
  }

  @Test
  @DisplayName("multiple frames in one stream")
  public void multipleFramesInOneStream() throws IOException {
    ByteArrayOutputStream stream = new ByteArrayOutputStream();
    stream.writeBytes(JsonRpcFraming.encode("{\"id\":1}"));
    stream.writeBytes(JsonRpcFraming.encode("{\"id\":2}"));

    InputStream in = new ByteArrayInputStream(stream.toByteArray());
    assertEquals("{\"id\":1}", JsonRpcFraming.readPayload(in));
    assertEquals("{\"id\":2}", JsonRpcFraming.readPayload(in));
  }

  @Test
  @DisplayName("clean end of stream returns null")
  public void cleanEndOfStreamReturnsNull() throws IOException {
    assertNull(JsonRpcFraming.readPayload(new ByteArrayInputStream(new byte[0])));
  }

  @Test
  @DisplayName("end of stream inside length prefix throws")
  public void endOfStreamInsideLengthPrefixThrows() {
    assertThrows(EOFException.class, () -> JsonRpcFraming.readPayload(new ByteArrayInputStream(new byte[]{5, 0})));
  }

  @Test
  @DisplayName("end of stream inside body throws")
  public void endOfStreamInsideBodyThrows() {
    byte[] frame = JsonRpcFraming.encode("{\"id\":1}");
    byte[] truncated = Arrays.copyOf(frame, frame.length - 3);
    assertThrows(EOFException.class, () -> JsonRpcFraming.readPayload(new ByteArrayInputStream(truncated)));
  }

  @Test
  @DisplayName("implausible length is rejected instead of buffered")
  public void implausibleLengthIsRejectedInsteadOfBuffered() {
    // 0xFFFFFFFF as unsigned little-endian — a misaligned/corrupt prefix
    byte[] corrupt = {(byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, '{', '}'};
    assertThrows(IOException.class, () -> JsonRpcFraming.readPayload(new ByteArrayInputStream(corrupt)));
  }

  // The POSITIVE over-cap branch (a negative length is a different check):
  // this binary prefix always "parses", so the plausibility cap is the only
  // corruption detection the framing has — unlike the DAP framing, whose
  // ASCII header self-detects garbage by failing integer parsing.
  @Test
  @DisplayName("a positive length above the cap is rejected")
  public void aPositiveLengthAboveTheCapIsRejected() {
    // MAX_FRAME_BYTES + 1 = 0x04000001, little-endian
    byte[] oversized = {0x01, 0x00, 0x00, 0x04, '{', '}'};
    assertThrows(IOException.class, () -> JsonRpcFraming.readPayload(new ByteArrayInputStream(oversized)));
  }

  @Test
  @DisplayName("a length at the cap is accepted")
  public void aLengthAtTheCapIsAccepted() throws IOException {
    // exactly MAX_FRAME_BYTES must NOT be rejected (boundary); the body is
    // absent, so the read must fail as a mid-frame EOF, not as implausible
    byte[] atCap = {0x00, 0x00, 0x00, 0x04};
    try {
      JsonRpcFraming.readPayload(new ByteArrayInputStream(atCap));
      throw new AssertionError("expected EOFException");
    } catch (EOFException expected) {
      // the length passed the plausibility check and the body read began
    }
  }
}
