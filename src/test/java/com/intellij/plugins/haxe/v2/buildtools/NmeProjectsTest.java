package com.intellij.plugins.haxe.v2.buildtools;

import com.intellij.openapi.util.SystemInfo;
import com.intellij.plugins.haxe.config.HaxeTarget;
import com.intellij.plugins.haxe.config.NMETarget;
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
  private static final String HOST_NEKO_DIRECTORY = SystemInfo.isWindows ? "windows-neko" : SystemInfo.isMac ? "mac64-neko" : "linux64-neko";

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

  /** (nme target flag, the project nmml, the generated source folder under the project dir; null for an unmapped platform). */
  static final List<Arguments> GENERATED_SOURCE_FOLDERS = List.of(
    arguments("windows", "<project><app path=\"Export\"/></project>", "Export/windows/haxe"),
    // the app path defaults to lime's; the 64-bit desktop builds carry the suffix
    arguments("mac", "<project/>", "bin/mac64/haxe"),
    arguments("cppia", "<project/>", "bin/cppia/haxe"),
    arguments("neko", "<project/>", "bin/" + HOST_NEKO_DIRECTORY + "/haxe"),
    arguments("winrt", "<project/>", null));

  @ParameterizedTest(name = "{0}")
  @FieldSource("GENERATED_SOURCE_FOLDERS")
  @DisplayName("generated source directory follows the platform directory")
  public void testGeneratedSourceDirectoryFollowsThePlatformDirectory(String targetFlag, String projectNmml, String folder) {
    assertEquals(folder, NmeProjects.generatedSourceDirectory(projectNmml, targetFlag));
  }

  /** Every target the settings offer compiles through the haxe target its enum names; cppia and the unlisted jsprime are also checked by name. */
  @Test
  @DisplayName("every listed nme target compiles through its haxe target")
  public void testEveryListedNmeTargetCompilesThroughItsHaxeTarget() {
    for (NMETarget target : NMETarget.values()) {
      assertEquals(target.getOutputTarget(), NmeProjects.targetFor(target.getTargetFlag()), target.getTargetFlag());
    }
    assertEquals(HaxeTarget.CPPIA, NmeProjects.targetFor("cppia"));
    assertEquals(HaxeTarget.JAVA_SCRIPT, NmeProjects.targetFor("jsprime"));
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
