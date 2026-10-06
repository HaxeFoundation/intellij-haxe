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
    arguments("html5", "", "bin/html5/bin/MyApplication.js"),
    arguments("flash", "", "bin/flash/bin/MyApplication.swf"),
    arguments("hl", "Shapes", "bin/hl/obj/ApplicationMain.hl"));

  @ParameterizedTest(name = "{0} / {1}")
  @FieldSource("OUTPUTS")
  public void testRelativeTargetOutput(String targetFlag, String appFile, String expected) {
    assertEquals(expected, LimeProjects.relativeTargetOutput(targetFlag, "bin", appFile));
  }
}
