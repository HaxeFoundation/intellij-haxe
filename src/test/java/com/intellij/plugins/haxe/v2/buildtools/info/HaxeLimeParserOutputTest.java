package com.intellij.plugins.haxe.v2.buildtools.info;

import com.intellij.openapi.util.SystemInfo;
import com.intellij.plugins.haxe.config.HaxeTarget;
import com.intellij.plugins.haxe.v2.buildsystem.HaxeBuildFileInfo;
import com.intellij.plugins.haxe.v2.buildsystem.HaxeBuildFileInfo.HaxeDefine;
import com.intellij.plugins.haxe.v2.buildsystem.HaxeBuildFileInfo.HaxeLibDependency;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Build tools: lime parser tool output")
public class HaxeLimeParserOutputTest {

  @Test
  @DisplayName("full output maps defines libraries sources and app")
  public void fullOutputMapsDefinesLibrariesSourcesAndApp() {
    String json = """
      {
        "defines": {"lime": "8.0.2", "flagOnly": ""},
        "haxelibs": [{"name": "lime", "version": "8.0.2"}, {"name": "local"}],
        "sources": ["src", "gen"],
        "app": {"path": "Export", "file": "Game"},
        "futureField": {"ignored": true}
      }""";

    HaxeBuildFileInfo info = HaxeLimeProjectInfoService.parseToolOutput(json, "windows");

    assertNotNull(info);
    assertEquals(HaxeTarget.CPP, info.target());
    assertEquals("Export/windows/bin/Game.exe", info.targetOutput());
    assertEquals(List.of(new HaxeDefine("lime", "8.0.2"), new HaxeDefine("flagOnly", null)), info.defines());
    assertEquals(List.of(new HaxeLibDependency("lime", "8.0.2"), new HaxeLibDependency("local", null)), info.libraries());
    assertEquals(List.of("src", "gen"), info.classpaths());
  }

  @Test
  @DisplayName("empty object yields defaults")
  public void emptyObjectYieldsDefaults() {
    HaxeBuildFileInfo info = HaxeLimeProjectInfoService.parseToolOutput("{}", "hl");

    assertNotNull(info);
    // lime's default export root is "bin" - "Export" is only a template convention
    assertEquals("bin/hl/obj/ApplicationMain.hl", info.targetOutput());
    assertTrue(info.defines().isEmpty());
    assertTrue(info.libraries().isEmpty());
    assertTrue(info.classpaths().isEmpty());
  }

  @Test
  @DisplayName("missing app path defaults to bin for the neko launcher")
  public void missingAppPathDefaultsToBinForTheNekoLauncher() {
    String json = """
      {"haxelibs": [], "sources": [], "app": {"file": "StarlingTests"}}
      """;
    HaxeBuildFileInfo info = HaxeLimeProjectInfoService.parseToolOutput(json, "neko");

    assertNotNull(info);
    assertEquals(HaxeTarget.NEKO, info.target());
    String expected = SystemInfo.isWindows ? "bin/neko/bin/StarlingTests.exe" : "bin/neko/bin/StarlingTests";
    assertEquals(expected, info.targetOutput());
  }

  @Test
  @DisplayName("malformed output yields null")
  public void malformedOutputYieldsNull() {
    assertNull(HaxeLimeProjectInfoService.parseToolOutput("not json at all", "hl"));
  }
}
