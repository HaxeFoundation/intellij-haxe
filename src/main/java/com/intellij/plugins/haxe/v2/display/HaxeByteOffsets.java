package com.intellij.plugins.haxe.v2.display;

import org.jetbrains.annotations.NotNull;

/**
 * Maps the compiler's byte offsets into a file to offsets in the editor's
 * text: a UTF-8 sequence counts as the UTF-16 units it decodes to, and a CR
 * before an LF counts nothing, as the document holds a single LF there.
 */
final class HaxeByteOffsets {

  private final int[] textOffsetByByte;

  private HaxeByteOffsets(int[] textOffsetByByte) {
    this.textOffsetByByte = textOffsetByByte;
  }

  @NotNull
  static HaxeByteOffsets of(byte @NotNull [] bytes) {
    int[] table = new int[bytes.length + 1];
    int textOffset = 0;
    int at = 0;
    while (at < bytes.length) {
      int length = Math.min(sequenceLength(bytes[at]), bytes.length - at);
      for (int i = at; i < at + length; i++) {
        table[i] = textOffset;
      }
      textOffset += unitsOf(bytes, at, length);
      at += length;
    }
    table[bytes.length] = textOffset;
    return new HaxeByteOffsets(table);
  }

  /** The text offset of a byte offset; one beyond the file maps to the text's end. */
  int textOffset(int byteOffset) {
    if (byteOffset < 0) return 0;
    return textOffsetByByte[Math.min(byteOffset, textOffsetByByte.length - 1)];
  }

  // the byte count of the UTF-8 sequence a lead byte starts; a stray continuation byte counts as one
  private static int sequenceLength(byte lead) {
    int value = lead & 0xFF;
    if (value < 0x80) return 1;
    if (value >= 0xF0) return 4;
    if (value >= 0xE0) return 3;
    if (value >= 0xC0) return 2;
    return 1;
  }

  // the UTF-16 units a sequence becomes: two for a supplementary code point, none for a CR that an LF follows
  private static int unitsOf(byte[] bytes, int at, int length) {
    if (length == 4) return 2;
    boolean crBeforeLf = bytes[at] == '\r' && at + 1 < bytes.length && bytes[at + 1] == '\n';
    return crBeforeLf ? 0 : 1;
  }
}
