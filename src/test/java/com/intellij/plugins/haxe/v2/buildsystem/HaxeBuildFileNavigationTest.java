package com.intellij.plugins.haxe.v2.buildsystem;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Build system: build file navigation")
public class HaxeBuildFileNavigationTest {

  @Test
  @DisplayName("hxml define offset points at the name after the flag")
  public void hxmlDefineOffsetPointsAtTheNameAfterTheFlag() {
    String content = "-cp src\n-D analyzer-optimize\n-main Main\n";
    int offset = HaxeBuildFileNavigation.findDefineOffset(content, HaxeBuildFileType.HXML, "analyzer-optimize");
    assertEquals(content.indexOf("analyzer-optimize"), offset);
  }

  @Test
  @DisplayName("xml define offset points at the name attribute value")
  public void xmlDefineOffsetPointsAtTheNameAttributeValue() {
    String content = "<project>\n  <haxedef name=\"no-traces\"/>\n</project>\n";
    int offset = HaxeBuildFileNavigation.findDefineOffset(content, HaxeBuildFileType.OPENFL, "no-traces");
    assertEquals(content.indexOf("no-traces"), offset);
  }

  @Test
  @DisplayName("unknown define falls back to file start")
  public void unknownDefineFallsBackToFileStart() {
    assertEquals(0, HaxeBuildFileNavigation.findDefineOffset("-cp src\n", HaxeBuildFileType.HXML, "missing"));
  }
}
