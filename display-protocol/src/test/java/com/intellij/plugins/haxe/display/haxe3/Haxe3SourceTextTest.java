package com.intellij.plugins.haxe.display.haxe3;

import com.intellij.plugins.haxe.display.protocol.Position;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("Display protocol: haxe3 source text")
public class Haxe3SourceTextTest {

  @Test
  @DisplayName("byte offset counts utf8 bytes")
  public void byteOffsetCountsUtf8Bytes() {
    Haxe3SourceText text = Haxe3SourceText.of("aé€b");

    assertEquals(0, text.byteOffset(0));
    assertEquals(3, text.byteOffset(2));
    assertEquals(6, text.byteOffset(3));
    assertEquals(7, text.byteOffset(99), "an offset past the end counts as the end");
  }

  @Test
  @DisplayName("byte offset counts the cr of a crlf line break")
  public void byteOffsetCountsTheCrOfACrlfLineBreak() {
    Haxe3SourceText text = Haxe3SourceText.of("a\r\nb");

    assertEquals(3, text.byteOffset(3));
  }

  @Test
  @DisplayName("position turns a byte column into characters on its line")
  public void positionTurnsAByteColumnIntoCharactersOnItsLine() {
    Haxe3SourceText text = Haxe3SourceText.of("first\nété x\n");

    assertEquals(new Position(1, 4), text.position(1, 6));
    assertEquals(new Position(1, 5), text.position(1, 99), "a column past the line counts as its end");
    assertEquals(new Position(7, 3), text.position(7, 3), "a line past the end keeps its column");
  }
}
