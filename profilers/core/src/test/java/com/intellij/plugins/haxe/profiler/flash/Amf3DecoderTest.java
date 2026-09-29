package com.intellij.plugins.haxe.profiler.flash;

import com.intellij.plugins.haxe.profiler.flash.Amf3Decoder.Amf3Object;
import com.intellij.plugins.haxe.profiler.model.ProfilerFormatException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("Flash profiler: AMF3 decoder")
public class Amf3DecoderTest {

  @Test
  @DisplayName("integers cover all U29 widths and the negative range")
  public void testIntegersCoverAllU29WidthsAndTheNegativeRange() throws IOException {
    Amf3Decoder decoder = decoder(
      0x04, 0x05, // 5
      0x04, 0x82, 0x2C, // 300
      0x04, 0xFF, 0xFF, 0xFF, 0xFF); // 29-bit all ones = -1

    assertEquals(5L, decoder.readValue());
    assertEquals(300L, decoder.readValue());
    assertEquals(-1L, decoder.readValue());
  }

  @Test
  @DisplayName("strings enter the reference table and references resolve")
  public void testStringsEnterTheReferenceTableAndReferencesResolve() throws IOException {
    Amf3Decoder decoder = decoder(
      0x06, 0x07, 'a', 'b', 'c', // inline "abc", length 3<<1|1
      0x06, 0x00); // reference 0

    assertEquals("abc", decoder.readValue());
    assertEquals("abc", decoder.readValue());
  }

  @Test
  @DisplayName("typed objects reuse traits by reference")
  public void testTypedObjectsReuseTraitsByReference() throws IOException {
    Amf3Decoder decoder = decoder(
      // inline traits: class "T", one sealed member "m", value 1
      0x0A, 0x13, 0x03, 'T', 0x03, 'm', 0x04, 0x01,
      // second object referencing traits 0, value 2
      0x0A, 0x01, 0x04, 0x02);

    Amf3Object first = assertInstanceOf(Amf3Object.class, decoder.readValue());
    assertEquals("T", first.className());
    assertEquals(1L, first.member("m"));

    Amf3Object second = assertInstanceOf(Amf3Object.class, decoder.readValue());
    assertEquals("T", second.className());
    assertEquals(2L, second.member("m"));
  }

  @Test
  @DisplayName("doubles, double vectors, byte arrays and dictionaries decode")
  public void testDoublesDoubleVectorsByteArraysAndDictionariesDecode() throws IOException {
    Amf3Decoder decoder = decoder(
      0x05, 0x3F, 0xF8, 0, 0, 0, 0, 0, 0, // 1.5
      0x0F, 0x05, 0x00, // Vector.<Number>, 2 fixed entries
      0x3F, 0xF0, 0, 0, 0, 0, 0, 0, // 1.0
      0x40, 0x00, 0, 0, 0, 0, 0, 0, // 2.0
      0x0C, 0x05, 0xAA, 0xBB, // ByteArray of 2
      0x11, 0x03, 0x00, 0x06, 0x03, 'k', 0x03); // {"k": true}

    assertEquals(1.5, decoder.readValue());
    assertEquals(List.of(1.0, 2.0), decoder.readValue());
    assertArrayEquals(new byte[]{(byte)0xAA, (byte)0xBB}, (byte[])decoder.readValue());
    assertEquals(Map.of("k", Boolean.TRUE), decoder.readValue());
  }

  @Test
  @DisplayName("undefined stays distinct from null")
  public void testUndefinedStaysDistinctFromNull() throws IOException {
    Amf3Decoder decoder = decoder(0x00, 0x01);

    assertSame(Amf3Decoder.UNDEFINED, decoder.readValue());
    assertEquals(null, decoder.readValue());
  }

  @Test
  @DisplayName("an unknown marker raises a format error")
  public void testAnUnknownMarkerRaisesAFormatError() {
    assertThrows(ProfilerFormatException.class, () -> decoder(0x7F).readValue());
  }

  private static Amf3Decoder decoder(int... bytes) {
    byte[] raw = new byte[bytes.length];
    for (int i = 0; i < bytes.length; i++) {
      raw[i] = (byte)bytes[i];
    }
    return new Amf3Decoder(new ByteArrayInputStream(raw));
  }
}
