package com.intellij.plugins.haxe.v2.buildsystem;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.FieldSource;

import java.util.List;

import static com.intellij.plugins.haxe.v2.buildsystem.HaxeBuildFileType.HXML;
import static com.intellij.plugins.haxe.v2.buildsystem.HaxeBuildFileType.LIME;
import static com.intellij.plugins.haxe.v2.buildsystem.HaxeBuildFileType.OPENFL;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.params.provider.Arguments.arguments;

@DisplayName("Build system: build file navigation")
public class HaxeBuildFileNavigationTest {

  private static final String CARET = "<caret>";

  /**
   * (content with {@code <caret>} at the expected offset, file type, define name). The name occurs
   * before its declaration in most rows, where the bare-name fallback would land on the wrong spot.
   */
  static final List<Arguments> DEFINE_OFFSETS = List.of(
    // hxml define offset points at the name after the flag
    arguments("-cp analyzer-optimize/src\n-D <caret>analyzer-optimize\n-main Main\n", HXML, "analyzer-optimize"),
    arguments("-cp analyzer-optimize/src\n--define <caret>analyzer-optimize\n", HXML, "analyzer-optimize"),
    // compiler spelling finds the dashed declaration: the define context
    // carries the compiler's key (my_flag) for a file writing -D my-flag
    arguments("-cp analyzer-optimize/src\n-D <caret>analyzer-optimize\n", HXML, "analyzer_optimize"),
    // xml define offset points at the name attribute value
    arguments("<!-- no-traces keeps the build quiet -->\n<haxedef name=\"<caret>no-traces\"/>\n", OPENFL, "no-traces"),
    arguments("<!-- no-traces -->\n<haxedef name='<caret>no-traces'/>\n", LIME, "no-traces"),
    // attribute spacing the patterns do not know: the bare name still lands on it
    arguments("<haxedef name = \"<caret>no-traces\"/>\n", LIME, "no-traces"),
    // unknown define falls back to file start
    arguments("<caret>-cp src\n", HXML, "missing"));

  @ParameterizedTest(name = "[{index}] {1} {2}")
  @FieldSource("DEFINE_OFFSETS")
  @DisplayName("define offset points at the declaration")
  public void defineOffsetPointsAtTheDeclaration(String markedContent, HaxeBuildFileType type, String defineName) {
    String content = markedContent.replace(CARET, "");

    int offset = HaxeBuildFileNavigation.findDefineOffset(content, type, defineName);

    assertEquals(markedContent.indexOf(CARET), offset);
  }
}
