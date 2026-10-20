package com.intellij.plugins.haxe.v2.buildtools;

import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.plugins.haxe.HaxeCodeInsightFixtureTestCase;
import com.intellij.plugins.haxe.util.HaxeSdkUtilBase;
import com.intellij.plugins.haxe.v2.buildsystem.HaxeBuildFile;
import com.intellij.plugins.haxe.v2.buildsystem.HaxeBuildFileInfo.HaxeDefine;
import com.intellij.plugins.haxe.v2.buildsystem.HaxeBuildFileType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.FieldSource;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.params.provider.Arguments.arguments;

@DisplayName("Build tools: hxcpp executables")
public class HxcppExecutablesTest extends HaxeCodeInsightFixtureTestCase {

  private static final String CPP_HXML_SOURCE = """
    -cp src
    --main app.Main
    -cpp bin/cpp
    """;

  @Override
  protected String getBasePath() {
    return "";
  }

  /** (defines of the build, whether hxcpp links an executable this machine runs). */
  static final List<Arguments> LINK_KINDS = List.of(
    arguments(List.of(), true),
    arguments(List.of(define("static_link")), false),
    // exe_link overrides a static link
    arguments(List.of(define("static_link"), define("exe_link")), true),
    // a shared-library define overrides exe_link
    arguments(List.of(define("exe_link"), define("dll_link")), false),
    arguments(List.of(define("dll_import")), false),
    // a cross toolchain builds for another platform, whatever it links
    arguments(List.of(define("android"), define("exe_link")), false),
    arguments(List.of(define("iphoneos")), false),
    arguments(List.of(define("emscripten")), false),
    arguments(List.of(define("mingw")), true));

  @ParameterizedTest(name = "{0}")
  @FieldSource("LINK_KINDS")
  @DisplayName("builds host executable follows the link and platform defines")
  public void testBuildsHostExecutableFollowsTheLinkAndPlatformDefines(List<HaxeDefine> defines, boolean hostExecutable) {
    assertEquals(hostExecutable, HxcppExecutables.buildsHostExecutable(defines));
  }

  /** (main class, defines, expected file name before the platform extension). */
  static final List<Arguments> FILE_NAMES = List.of(
    arguments("app.Main", List.of(), "Main"),
    arguments("app.Main", List.of(define("debug")), "Main-debug"),
    arguments("app.Main", List.of(define("debug"), define("DEBUGSUFFIX", "_d")), "Main_d"),
    arguments("app.Main", List.of(define("HAXE_OUTPUT_PART", "Game"), define("debug")), "Game-debug"),
    // HAXE_OUTPUT_FILE is the whole name: no debug suffix
    arguments("app.Main", List.of(define("HAXE_OUTPUT_FILE", "Game"), define("debug")), "Game"),
    arguments(null, List.of(), "output"));

  @ParameterizedTest(name = "{0} {1}")
  @FieldSource("FILE_NAMES")
  @DisplayName("file name follows hxcpp's naming rule")
  public void testFileNameFollowsHxcppsNamingRule(String mainClass, List<HaxeDefine> defines, String expectedName) {
    assertEquals(HaxeSdkUtilBase.getExecutableName(expectedName), HxcppExecutables.fileName(mainClass, defines));
  }

  @Test
  @DisplayName("command line defines read the debug flag and both define spellings")
  public void testCommandLineDefinesReadTheDebugFlagAndBothDefineSpellings() {
    List<String> command = List.of("haxe", "cpp.hxml", "-debug", "-D", "HAXE_OUTPUT_FILE=Game", "--define", "fast", "-lib", "x");

    assertEquals(List.of(define("debug"), define("HAXE_OUTPUT_FILE", "Game"), define("fast")),
                 HxcppExecutables.commandLineDefines(command));
  }

  @Test
  @DisplayName("executable lands in the cpp folder and follows the launch's debug flag")
  public void testExecutableLandsInTheCppFolderAndFollowsTheLaunchsDebugFlag() {
    HaxeBuildFile buildFile = hxml("native/cpp.hxml", CPP_HXML_SOURCE);
    Path cppFolder = HaxeBuildWorkDirectories.resolve(getProject(), buildFile.file(), "bin/cpp");

    Path release = HxcppExecutables.executable(getProject(), buildFile, List.of());
    Path debug = HxcppExecutables.executable(getProject(), buildFile, List.of("haxe", "cpp.hxml", "-debug"));

    assertEquals(cppFolder.resolve(HaxeSdkUtilBase.getExecutableName("Main")), release);
    assertEquals(cppFolder.resolve(HaxeSdkUtilBase.getExecutableName("Main-debug")), debug);
  }

  @Test
  @DisplayName("a library or another target has no executable")
  public void testALibraryOrAnotherTargetHasNoExecutable() {
    HaxeBuildFile library = hxml("lib/static.hxml", CPP_HXML_SOURCE + "-D static_link\n");
    HaxeBuildFile web = hxml("web/js.hxml", "--main Main\n-js out/main.js\n");

    assertNull(HxcppExecutables.executable(getProject(), library, List.of()));
    assertNull(HxcppExecutables.executable(getProject(), web, List.of()));
  }

  private HaxeBuildFile hxml(String path, String source) {
    VirtualFile file = myFixture.addFileToProject(path, source).getVirtualFile();
    return new HaxeBuildFile(file, HaxeBuildFileType.HXML);
  }

  private static HaxeDefine define(String name) {
    return new HaxeDefine(name, null);
  }

  private static HaxeDefine define(String name, String value) {
    return new HaxeDefine(name, value);
  }
}
