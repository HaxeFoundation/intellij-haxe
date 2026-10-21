package com.intellij.plugins.haxe.display.client;

import com.intellij.plugins.haxe.display.protocol.InitializeResult.SemVer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

@DisplayName("Display protocol: haxe version probe")
public class HaxeVersionProbeTest {

  @Test
  @DisplayName("reads the bare version a server answers")
  public void readsTheBareVersionAServerAnswers() {
    assertEquals(new SemVer(3, 4, 7, null, null), HaxeVersionProbe.parse("3.4.7\n"));
  }

  @Test
  @DisplayName("reads a pre release version")
  public void readsAPreReleaseVersion() {
    assertEquals(new SemVer(5, 0, 0, "preview.1", null), HaxeVersionProbe.parse("5.0.0-preview.1"));
  }

  @Test
  @DisplayName("an answer without a version names none")
  public void anAnswerWithoutAVersionNamesNone() {
    assertNull(HaxeVersionProbe.parse(""));
  }
}
