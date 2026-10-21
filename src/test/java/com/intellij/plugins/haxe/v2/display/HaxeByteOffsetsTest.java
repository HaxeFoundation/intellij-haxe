package com.intellij.plugins.haxe.v2.display;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.FieldSource;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.params.provider.Arguments.arguments;

/** The compiler counts bytes of the file on disk; the editor counts UTF-16 units of LF-only text. */
@DisplayName("Compiler types: byte offsets")
public class HaxeByteOffsetsTest {

  /** (file text as on disk, byte offset, expected text offset). */
  static final List<Arguments> OFFSETS = List.of(
    arguments("var a", 4, 4),
    // the two-byte e-acute is one unit
    arguments("\u00e9=1", 2, 1),
    arguments("\u00e9=1", 3, 2),
    // the CR of a CRLF line end vanishes from the text
    arguments("a\r\nb", 1, 1),
    arguments("a\r\nb", 2, 1),
    arguments("a\r\nb", 3, 2),
    // a four-byte emoji is a surrogate pair
    arguments("\ud83d\ude00x", 4, 2),
    // past the end lands on the text's end
    arguments("ab", 9, 2));

  @ParameterizedTest(name = "{0} byte {1} -> text {2}")
  @FieldSource("OFFSETS")
  @DisplayName("maps bytes to text offsets")
  public void testMapsBytesToTextOffsets(String diskText, int byteOffset, int textOffset) {
    HaxeByteOffsets offsets = HaxeByteOffsets.of(diskText.getBytes(StandardCharsets.UTF_8));

    assertEquals(textOffset, offsets.textOffset(byteOffset));
  }
}
