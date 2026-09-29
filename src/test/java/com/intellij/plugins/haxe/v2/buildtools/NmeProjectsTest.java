package com.intellij.plugins.haxe.v2.buildtools;

import com.intellij.openapi.util.SystemInfo;
import com.intellij.plugins.haxe.config.HaxeTarget;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.FieldSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.params.provider.Arguments.arguments;

@DisplayName("Build tools: NME projects")
public class NmeProjectsTest {

  /** (nme target flag, app name, output root, haxe target, the packaged artifact nme lays out). */
  static final List<Arguments> PACKAGED_ARTIFACTS = List.of(
    arguments("windows", "MyGame", "bin", HaxeTarget.CPP, "bin/windows/MyGame/MyGame.exe"),
    // the nmml app path overrides the default output root
    arguments("windows", "NyanCat", "Export", HaxeTarget.CPP, "Export/windows/NyanCat/NyanCat.exe"),
    arguments("mac", "MyGame", "bin", HaxeTarget.CPP, "bin/mac64/MyGame.app/Contents/MacOS/MyGame"),
    arguments("flash", "MyGame", "bin", HaxeTarget.FLASH, "bin/flash/MyGame/MyGame.swf"));

  @ParameterizedTest(name = "{0} {1}/{2}")
  @FieldSource("PACKAGED_ARTIFACTS")
  @DisplayName("target flags map to their packaged artifacts")
  public void testTargetFlagsMapToTheirPackagedArtifacts(String flag, String app, String root,
                                                         HaxeTarget target, String output) {
    NmeProjects.TargetArtifact artifact = NmeProjects.targetArtifact(flag, app, root);
    assertEquals(target, artifact.target());
    assertEquals(output, artifact.relativeOutput());
  }

  @Test
  @DisplayName("cpp target maps to the host desktop build")
  public void testCppTargetMapsToTheHostDesktopBuild() {
    NmeProjects.TargetArtifact artifact = NmeProjects.targetArtifact("cpp", "MyGame", "bin");
    assertEquals(HaxeTarget.CPP, artifact.target());
    NmeProjects.TargetArtifact host = hostDesktopArtifact();
    assertEquals(host.relativeOutput(), artifact.relativeOutput());
  }

  @Test
  @DisplayName("neko packages a host suffixed launcher")
  public void testNekoPackagesAHostSuffixedLauncher() {
    NmeProjects.TargetArtifact artifact = NmeProjects.targetArtifact("neko", "MyGame", "bin");
    assertNotNull(artifact);
    assertEquals(HaxeTarget.NEKO, artifact.target());
    // bin/<host>-neko/<app>/<app>[.exe] - nme names the folder after the BUILD host
    boolean hostSuffixedLauncher =
      artifact.relativeOutput().matches("bin/(windows|mac64|linux64)-neko/MyGame/MyGame(\\.exe)?");
    assertTrue(hostSuffixedLauncher, artifact.relativeOutput());
  }

  @Test
  @DisplayName("target flags without a launchable artifact report none")
  public void testTargetFlagsWithoutALaunchableArtifactReportNone() {
    assertNull(NmeProjects.targetArtifact("html5", "MyGame", "bin"));
    assertNull(NmeProjects.targetArtifact("android", "MyGame", "bin"));
    assertNull(NmeProjects.targetArtifact("ps4", "MyGame", "bin"));
  }

  /// What "cpp" should resolve to on the machine running the test.
  private static NmeProjects.TargetArtifact hostDesktopArtifact() {
    if (SystemInfo.isWindows) return NmeProjects.targetArtifact("windows", "MyGame", "bin");
    if (SystemInfo.isMac) return NmeProjects.targetArtifact("mac", "MyGame", "bin");
    return NmeProjects.targetArtifact("linux", "MyGame", "bin");
  }
}
