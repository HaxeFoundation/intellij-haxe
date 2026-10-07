package com.intellij.plugins.haxe.v2.buildtools;

import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.openapi.util.SystemInfo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.FieldSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.params.provider.Arguments.arguments;

@DisplayName("Build tools: lime projects")
public class LimeProjectsTest {

  private static final String NEKO_LAUNCHER_SUFFIX = SystemInfo.isWindows ? ".exe" : "";
  private static final String HOST_PLATFORM = LimeProjects.hostPlatformTarget();
  private static final String HOST_CPP_OUTPUT = switch (HOST_PLATFORM) {
    case "windows" -> "bin/windows/bin/MyApplication.exe";
    case "mac" -> "bin/macos/bin/MyApplication.app/Contents/MacOS/MyApplication";
    default -> "bin/linux/bin/MyApplication";
  };

  @Test
  @DisplayName("app file is the declared one")
  public void testAppFileIsTheDeclaredOne() {
    assertEquals("Shapes", LimeProjects.appFile("<project><app main=\"Main\" file=\"Shapes\"/></project>"));
  }

  @Test
  @DisplayName("app file defaults to limes when the project xml declares none")
  public void testAppFileDefaultsToLimesWhenTheProjectXmlDeclaresNone() {
    assertEquals("MyApplication", LimeProjects.appFile("<project><app main=\"Tests\"/></project>"));
  }

  @Test
  @DisplayName("display command line points lime at an export root no build uses")
  public void testDisplayCommandLinePointsLimeAtAnExportRootNoBuildUses() {
    GeneralCommandLine commandLine = LimeProjects.displayCommandLine("haxelib", "openfl", "/work", "project.xml", "html5");

    String appPathArgument = "--app-path=" + LimeProjects.displayAppPath();
    assertEquals(List.of("run", "openfl", "display", "project.xml", "html5", appPathArgument),
                 commandLine.getParametersList().getList());
  }

  /** (target flag, app file as declared, expected output relative to the project file). */
  static final List<Arguments> OUTPUTS = List.of(
    arguments("neko", "", "bin/neko/bin/MyApplication" + NEKO_LAUNCHER_SUFFIX),
    arguments("neko", "Shapes", "bin/neko/bin/Shapes" + NEKO_LAUNCHER_SUFFIX),
    arguments("windows", "", "bin/windows/bin/MyApplication.exe"),
    arguments("linux", "", "bin/linux/bin/MyApplication"),
    // lime exports the mac build into "macos" as a .app bundle; the executable inside it launches
    arguments("mac", "Shapes", "bin/macos/bin/Shapes.app/Contents/MacOS/Shapes"),
    // "cpp" is the tool's alias for the host desktop platform
    arguments("cpp", "", HOST_CPP_OUTPUT),
    arguments("html5", "", "bin/html5/bin/MyApplication.js"),
    arguments("flash", "", "bin/flash/bin/MyApplication.swf"),
    arguments("air", "Shapes", "bin/air/bin/Shapes.swf"),
    arguments("hl", "Shapes", "bin/hl/obj/ApplicationMain.hl"),
    arguments("android", "", null));

  @ParameterizedTest(name = "{0} / {1}")
  @FieldSource("OUTPUTS")
  public void testRelativeTargetOutput(String targetFlag, String appFile, String expected) {
    assertEquals(expected, LimeProjects.relativeTargetOutput(targetFlag, "bin", appFile, null));
  }

  /** (target flag, configured output directory, expected output relative to the project file). */
  static final List<Arguments> CONFIGURED_OUTPUT_DIRECTORIES = List.of(
    // the air descriptor and swf follow the configured directory; nothing is hardcoded to "air"
    arguments("air", "airdist", "Export/airdist/bin/Game.swf"),
    arguments("flash", "swf", "Export/swf/bin/Game.swf"),
    arguments("hl", "hashlink", "Export/hashlink/obj/ApplicationMain.hl"),
    arguments("mac", "osx", "Export/osx/bin/Game.app/Contents/MacOS/Game"),
    // an empty value means lime's default directory
    arguments("flash", "", "Export/flash/bin/Game.swf"));

  @ParameterizedTest(name = "{0} / {1}")
  @FieldSource("CONFIGURED_OUTPUT_DIRECTORIES")
  public void testRelativeTargetOutputHonoursTheConfiguredOutputDirectory(String targetFlag,
                                                                         String outputDirectory,
                                                                         String expected) {
    assertEquals(expected, LimeProjects.relativeTargetOutput(targetFlag, "Export", "Game", outputDirectory));
  }

  /** (target flag, the config key lime's platform reads the export directory from). */
  static final List<Arguments> OUTPUT_DIRECTORY_KEYS = List.of(
    arguments("air", "air.output-directory"),
    arguments("html5", "html5.output-directory"),
    arguments("mac", "mac.output-directory"),
    // host-built pseudo targets read the host platform's key
    arguments("hl", HOST_PLATFORM + ".output-directory"),
    arguments("neko", HOST_PLATFORM + ".output-directory"),
    arguments("cpp", HOST_PLATFORM + ".output-directory"));

  @ParameterizedTest(name = "{0}")
  @FieldSource("OUTPUT_DIRECTORY_KEYS")
  public void testOutputDirectoryConfigKey(String targetFlag, String expected) {
    assertEquals(expected, LimeProjects.outputDirectoryConfigKey(targetFlag));
  }
}
