package com.intellij.plugins.haxe.v2.buildtools.info;

import com.intellij.plugins.haxe.config.HaxeTarget;
import com.intellij.plugins.haxe.v2.buildsystem.HaxeBuildFileInfo;
import com.intellij.plugins.haxe.v2.buildsystem.HaxeBuildFileInfo.HaxeDefine;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.FieldSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.params.provider.Arguments.arguments;

@DisplayName("Build tools: lime display output")
public class HaxeLimeDisplayOutputTest {

  static final String PROJECT_XML_SOURCE = """
    <project>
      <app main="Main" path="Export" file="Game"/>
    </project>
    """;

  /** The hxml {@code lime display} prints: the compile output flag plus the flattened dependencies. */
  static final String ANDROID_DISPLAY_OUTPUT = """
    -main ApplicationMain
    -cp src
    -cp /haxelib/lime/8,0,2
    -D lime
    -D android
    -cpp Export/android/obj
    """;

  /** (target flag, the tool's output flag line, project xml, expected target, expected output). */
  static final List<Arguments> TARGET_OUTPUTS = List.of(
    // mobile builds compile to C++ but package no host-launchable output
    arguments("android", "-cpp Export/android/obj", PROJECT_XML_SOURCE, HaxeTarget.CPP, null),
    arguments("ios", "-cpp Export/ios/obj", PROJECT_XML_SOURCE, HaxeTarget.CPP, null),
    // the hxml names the compile directory; the launchable binary sits in lime's bin
    arguments("windows", "-cpp Export/windows/obj", PROJECT_XML_SOURCE, HaxeTarget.CPP, "Export/windows/bin/Game.exe"),
    arguments("html5", "-js Export/html5/bin/Game.js", PROJECT_XML_SOURCE, HaxeTarget.JAVA_SCRIPT, "Export/html5/bin/Game.js"),
    arguments("hl", "-hl bin/hl/obj/ApplicationMain.hl", "<project/>", HaxeTarget.HL, "bin/hl/obj/ApplicationMain.hl"),
    // an unreadable project file leaves lime's defaults
    arguments("html5", "-js bin/html5/bin/MyApplication.js", "", HaxeTarget.JAVA_SCRIPT, "bin/html5/bin/MyApplication.js"));

  @ParameterizedTest(name = "{0}")
  @FieldSource("TARGET_OUTPUTS")
  public void testTargetAndOutputFollowLimesExportLayout(String targetFlag,
                                                          String outputFlagLine,
                                                          String projectXml,
                                                          HaxeTarget target,
                                                          String targetOutput) {
    HaxeBuildFileInfo info = HaxeLimeProjectInfoService.parseDisplayOutput(outputFlagLine + "\n", targetFlag, projectXml);

    assertEquals(target, info.target());
    assertEquals(targetOutput, info.targetOutput());
  }

  @Test
  @DisplayName("defines and classpaths come from the display output")
  public void testDefinesAndClasspathsComeFromTheDisplayOutput() {
    HaxeBuildFileInfo info = HaxeLimeProjectInfoService.parseDisplayOutput(ANDROID_DISPLAY_OUTPUT, "android", PROJECT_XML_SOURCE);

    assertEquals(List.of(new HaxeDefine("lime", null), new HaxeDefine("android", null)), info.defines());
    assertEquals(List.of("src", "/haxelib/lime/8,0,2"), info.classpaths());
    assertTrue(info.libraries().isEmpty(), "display output flattens haxelibs into classpaths");
  }
}
