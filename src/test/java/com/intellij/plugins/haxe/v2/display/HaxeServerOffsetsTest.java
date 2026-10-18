package com.intellij.plugins.haxe.v2.display;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.FieldSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.params.provider.Arguments.arguments;

/** The editor counts one character per line break; a CRLF file on disk has two, and the server reads the disk unless the text is sent. */
@DisplayName("Compiler types: server offsets")
public class HaxeServerOffsetsTest {

  /** (line separator detected on the file, contents sent with the request, whether offsets shift). */
  static final List<Arguments> SHIFTS = List.of(
    arguments("\r\n", null, true),
    // the request carries the editor's text, which is what the server then reads
    arguments("\r\n", "class A {}", false),
    arguments("\n", null, false),
    // nothing detected: a file without a line break
    arguments(null, null, false));

  @ParameterizedTest(name = "separator {0}, contents {1} -> {2}")
  @FieldSource("SHIFTS")
  @DisplayName("shifts only for CRLF files read from disk")
  public void testShiftsOnlyForCrlfFilesReadFromDisk(String separator, String contents, boolean shifts) {
    assertEquals(shifts, HaxeServerOffsets.shiftsByLine(separator, contents));
  }

  /** (editor offset, its 0-based line, whether offsets shift, server offset). */
  static final List<Arguments> OFFSETS = List.of(
    arguments(0, 0, true, 0),
    // one CR per line break before the offset: a name on line 242 of a 5.7 KB file
    arguments(4562, 241, true, 4803),
    arguments(4562, 241, false, 4562));

  @ParameterizedTest(name = "offset {0} on line {1}, shifting {2} -> {3}")
  @FieldSource("OFFSETS")
  @DisplayName("adds one per preceding line")
  public void testAddsOnePerPrecedingLine(int offset, int line, boolean shifts, int serverOffset) {
    assertEquals(serverOffset, HaxeServerOffsets.serverOffset(offset, line, shifts));
  }
}
