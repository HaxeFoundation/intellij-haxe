package com.intellij.plugins.haxe.runner.debugger.dap.transport;

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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("DAP protocol: framing")
public class DapFramingTest {
  private static final String CRLF = new String(new char[]{13, 10});

  @Test
  @DisplayName("encode produces header and body")
  public void encodeProducesHeaderAndBody() {
    byte[] frame = DapFraming.encode("{}");
    byte[] expected = ("Content-Length: 2" + CRLF + CRLF + "{}").getBytes(StandardCharsets.UTF_8);
    assertArrayEquals(expected, frame);
  }

  @Test
  @DisplayName("round trip ascii")
  public void roundTripAscii() throws IOException {
    String payload = "{\"seq\":1,\"type\":\"request\",\"command\":\"initialize\"}";
    InputStream in = new ByteArrayInputStream(DapFraming.encode(payload));
    assertEquals(payload, DapFraming.readPayload(in));
  }

  @Test
  @DisplayName("content length counts utf 8 bytes")
  public void contentLengthCountsUtf8Bytes() throws IOException {
    String payload = "{\"name\":\"æøå\"}";
    byte[] frame = DapFraming.encode(payload);
    int expectedByteLength = payload.getBytes(StandardCharsets.UTF_8).length;
    String frameText = new String(frame, StandardCharsets.UTF_8);
    assertEquals(0, frameText.indexOf("Content-Length: " + expectedByteLength));
    assertEquals(payload, DapFraming.readPayload(new ByteArrayInputStream(frame)));
  }

  @Test
  @DisplayName("multiple frames in one stream")
  public void multipleFramesInOneStream() throws IOException {
    ByteArrayOutputStream stream = new ByteArrayOutputStream();
    stream.writeBytes(DapFraming.encode("{\"seq\":1}"));
    stream.writeBytes(DapFraming.encode("{\"seq\":2}"));

    InputStream in = new ByteArrayInputStream(stream.toByteArray());
    assertEquals("{\"seq\":1}", DapFraming.readPayload(in));
    assertEquals("{\"seq\":2}", DapFraming.readPayload(in));
  }

  @Test
  @DisplayName("clean end of stream returns null")
  public void cleanEndOfStreamReturnsNull() throws IOException {
    assertNull(DapFraming.readPayload(new ByteArrayInputStream(new byte[0])));
  }

  @Test
  @DisplayName("end of stream inside body throws")
  public void endOfStreamInsideBodyThrows() throws IOException {
    byte[] frame = DapFraming.encode("{\"seq\":1}");
    byte[] truncated = new byte[frame.length - 4];
    System.arraycopy(frame, 0, truncated, 0, truncated.length);
    assertThrows(EOFException.class, () -> DapFraming.readPayload(new ByteArrayInputStream(truncated)));
  }

  @Test
  @DisplayName("parse content length variants")
  public void parseContentLengthVariants() {
    assertEquals(42, DapFraming.parseContentLength("Content-Length: 42"));
    assertEquals(7, DapFraming.parseContentLength("content-length:7"));
    assertEquals(5, DapFraming.parseContentLength("Other: x" + CRLF + "Content-Length: 5"));
    assertEquals(-1, DapFraming.parseContentLength("Other: x"));
    assertEquals(-1, DapFraming.parseContentLength("Content-Length: nope"));
  }
}
