package com.intellij.plugins.haxe.profiler.tracy.wire;

import com.intellij.plugins.haxe.profiler.model.ProfilerFormatException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("tracy receiver: lz4 stream")
public class TracyLz4StreamTest {

  @Test
  @DisplayName("carries the dictionary window across frames")
  public void testCarriesTheDictionaryWindowAcrossFrames() throws IOException {
    // frame 2 opens with a match reaching 16 bytes back - into frame 1's output
    byte[] literals = "0123456789abcdef".getBytes(StandardCharsets.US_ASCII);
    byte[] block1 = new byte[2 + literals.length];
    block1[0] = (byte)0xF0;
    block1[1] = 0x01;
    System.arraycopy(literals, 0, block1, 2, literals.length);
    byte[] block2 = {0x04, 0x10, 0x00, 0x50, 'A', 'B', 'C', 'D', 'E'};

    TracyLz4Stream stream = new TracyLz4Stream(new ByteArrayInputStream(framed(block1, block2)));
    String out = new String(stream.readAllBytes(), StandardCharsets.US_ASCII);

    assertEquals("0123456789abcdef01234567ABCDE", out);
  }

  @Test
  @DisplayName("a truncated frame body fails as truncated")
  public void testATruncatedFrameBodyFailsAsTruncated() {
    byte[] framed = framed(new byte[]{0x50, 'A', 'B', 'C', 'D', 'E'});
    byte[] cut = new byte[framed.length - 3];
    System.arraycopy(framed, 0, cut, 0, cut.length);

    TracyLz4Stream stream = new TracyLz4Stream(new ByteArrayInputStream(cut));

    ProfilerFormatException failure = assertThrows(ProfilerFormatException.class, stream::readAllBytes);
    assertTrue(failure.getMessage().contains("truncated"), failure.getMessage());
  }

  /** Wraps each block as one tracy frame: u32 LE compressed size + block. */
  private static byte[] framed(byte[]... blocks) {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    for (byte[] block : blocks) {
      for (int i = 0; i < 4; i++) out.write(block.length >> (8 * i) & 0xFF);
      out.writeBytes(block);
    }
    return out.toByteArray();
  }
}
