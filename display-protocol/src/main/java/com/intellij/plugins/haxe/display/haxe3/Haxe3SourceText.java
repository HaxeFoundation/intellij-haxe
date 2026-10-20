package com.intellij.plugins.haxe.display.haxe3;

import com.intellij.plugins.haxe.display.protocol.Position;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * The text a haxe 3 server reads for one file. The client API counts
 * characters, while haxe 3 counts UTF-8 bytes: in a request's offset and in
 * the columns of its results. A line ends at its LF; a CR before the LF
 * belongs to the line, as it does in the file on disk.
 */
final class Haxe3SourceText {

  private final String text;
  /** The character offset each line starts at. */
  private final int[] lineStarts;

  private Haxe3SourceText(String text) {
    this.text = text;
    this.lineStarts = lineStartsOf(text);
  }

  static Haxe3SourceText of(String text) {
    return new Haxe3SourceText(text);
  }

  static Haxe3SourceText read(Path file) throws IOException {
    return new Haxe3SourceText(Files.readString(file));
  }

  String text() {
    return text;
  }

  /** The UTF-8 byte offset of a character offset; an offset past the end counts as the end. */
  int byteOffset(int charOffset) {
    int clamped = Math.clamp(charOffset, 0, text.length());
    return text.substring(0, clamped).getBytes(StandardCharsets.UTF_8).length;
  }

  /** The position of a byte column on a 0-based line; a line past the end keeps the column as it is. */
  Position position(int line, int byteColumn) {
    if (line < 0 || line >= lineStarts.length) return new Position(line, byteColumn);
    byte[] lineBytes = lineText(line).getBytes(StandardCharsets.UTF_8);
    int bytes = Math.clamp(byteColumn, 0, lineBytes.length);
    return new Position(line, new String(lineBytes, 0, bytes, StandardCharsets.UTF_8).length());
  }

  /** The position after the last character of a 0-based line, its line break not counted. */
  Position lineEnd(int line) {
    if (line < 0 || line >= lineStarts.length) return new Position(line, 0);
    String lineText = lineText(line);
    int length = lineText.endsWith("\r") ? lineText.length() - 1 : lineText.length();
    return new Position(line, length);
  }

  /** A line's text with a trailing CR, without its LF. */
  private String lineText(int line) {
    int start = lineStarts[line];
    int lf = text.indexOf('\n', start);
    return text.substring(start, lf < 0 ? text.length() : lf);
  }

  private static int[] lineStartsOf(String text) {
    List<Integer> starts = new ArrayList<>(List.of(0));
    for (int i = text.indexOf('\n'); i >= 0; i = text.indexOf('\n', i + 1)) {
      starts.add(i + 1);
    }
    return starts.stream()
      .mapToInt(Integer::intValue)
      .toArray();
  }
}
