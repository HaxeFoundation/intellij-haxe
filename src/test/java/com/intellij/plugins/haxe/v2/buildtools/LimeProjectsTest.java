package com.intellij.plugins.haxe.v2.buildtools;

import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.plugins.haxe.config.HaxeTarget;
import com.intellij.plugins.haxe.config.LimeTarget;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.FieldSource;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.params.provider.Arguments.arguments;

@DisplayName("Build tools: lime projects")
public class LimeProjectsTest {

  private static final String HOST_LAUNCHER_SUFFIX = SystemInfo.isWindows ? ".exe" : "";
  private static final String HOST_PLATFORM = LimeProjects.hostPlatformTarget();
  private static final String HOST_CPP_OUTPUT = switch (HOST_PLATFORM) {
    case "windows" -> "bin/windows/bin/MyApplication.exe";
    case "mac" -> "bin/macos/bin/MyApplication.app/Contents/MacOS/MyApplication";
    default -> "bin/linux/bin/MyApplication";
  };
  private static final String HOST_TARGET_DIRECTORY = HOST_PLATFORM.equals("mac") ? "macos" : HOST_PLATFORM;

  /** Every target the settings offer compiles through the haxe target its enum names; Electron, a JavaScript target, is also checked by name. */
  @Test
  @DisplayName("every listed lime target compiles through its haxe target")
  public void testEveryListedLimeTargetCompilesThroughItsHaxeTarget() {
    for (LimeTarget target : LimeTarget.values()) {
      assertEquals(target.getOutputTarget(), LimeProjects.targetFor(target.getTargetFlag()), target.getTargetFlag());
    }
    assertEquals(HaxeTarget.JAVA_SCRIPT, LimeProjects.targetFor("electron"));
  }

  /** (evaluated output under the project dir, target flag, launcher under the project dir); the evaluation honours an output-directory config, the launcher follows lime's bin layout. */
  static final List<Arguments> EVALUATED_LAUNCHERS = List.of(
    arguments("dist/bin/App.exe", "windows", "dist/bin/App.exe"),
    arguments("bin/linux/bin/App", "linux", "bin/linux/bin/App"),
    // an HL evaluation names the bytecode inside obj; the renamed runtime sits in bin beside it
    arguments("dist/obj/ApplicationMain.hl", "hl", "dist/bin/App" + HOST_LAUNCHER_SUFFIX),
    // an evaluation naming the obj directory itself
    arguments("bin/windows/obj", "windows", "bin/windows/bin/App.exe"),
    arguments("bin/macos/obj", "mac", "bin/macos/bin/App.app/Contents/MacOS/App"),
    arguments("bin/html5/bin/App.js", "html5", null));

  @ParameterizedTest(name = "{0} for {1}")
  @FieldSource("EVALUATED_LAUNCHERS")
  public void testPackagedBinaryBehindAnEvaluatedOutput(String evaluatedOutput, String targetFlag, String launcher) {
    Path projectDir = Path.of("/work/game");
    Path expected = launcher == null ? null : projectDir.resolve(launcher);

    assertEquals(expected, LimeProjects.packagedBinary(projectDir.resolve(evaluatedOutput), targetFlag, "App"));
  }

  /** (target flag, the project xml, the generated source folders under the project dir). */
  static final List<Arguments> GENERATED_SOURCE_FOLDERS = List.of(
    arguments("html5", "<project><app path=\"Export\"/></project>", List.of("Export/html5/haxe", "Export/html5/haxe/_generated")),
    arguments("hl", "<project><app path=\"Export\"/></project>", List.of("Export/hl/haxe", "Export/hl/haxe/_generated")),
    // the app path defaults to lime's; the mac target directory is macos
    arguments("mac", "<project/>", List.of("bin/macos/haxe", "bin/macos/haxe/_generated")),
    // cpp builds through the host desktop platform
    arguments("cpp", "<project/>", List.of("bin/" + HOST_TARGET_DIRECTORY + "/haxe", "bin/" + HOST_TARGET_DIRECTORY + "/haxe/_generated")));

  @ParameterizedTest(name = "{0}")
  @FieldSource("GENERATED_SOURCE_FOLDERS")
  @DisplayName("generated source directories follow the export layout")
  public void testGeneratedSourceDirectoriesFollowTheExportLayout(String targetFlag, String projectXml, List<String> folders) {
    Path projectDir = Path.of("/work/game");
    List<Path> expected = folders.stream().map(projectDir::resolve).toList();

    assertEquals(expected, LimeProjects.generatedSourceDirectories(projectDir, projectXml, targetFlag));
  }

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
    arguments("neko", "", "bin/neko/bin/MyApplication" + HOST_LAUNCHER_SUFFIX),
    arguments("neko", "Shapes", "bin/neko/bin/Shapes" + HOST_LAUNCHER_SUFFIX),
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
