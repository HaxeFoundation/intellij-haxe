package com.intellij.plugins.haxe.v2.buildtools;

import com.intellij.plugins.haxe.HaxeCodeInsightFixtureTestCase;
import com.intellij.plugins.haxe.v2.buildtools.settings.HaxeBuildToolSettings;
import com.intellij.util.execution.ParametersListUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("Build tools: custom command variables")
public class HaxeCustomCommandsTest extends HaxeCodeInsightFixtureTestCase {

  @Override
  protected String getBasePath() {
    return "/testing/runner/";
  }

  @Test
  @DisplayName("commands without the server port variable pass through unchanged")
  public void testCommandsWithoutTheServerPortVariablePassThroughUnchanged() {
    List<String> command = List.of("haxe", "build.hxml");

    assertEquals(command, HaxeCustomCommands.expandServerPort(getProject(), "module", command));
  }

  @Test
  @DisplayName("server port variable stays literal while the server feature is off")
  public void testServerPortVariableStaysLiteralWhileTheServerFeatureIsOff() {
    HaxeBuildToolSettings.getInstance(getProject()).setCompilationServerEnabled(false);

    // the unapplied variable must stay visible instead of silently vanishing
    List<String> command = List.of("mytool", "--connect", "${serverPort}");
    assertEquals(command, HaxeCustomCommands.expandServerPort(getProject(), "module", command));
  }

  @Test
  @DisplayName("root variables expand to the given directories")
  public void testRootVariablesExpandToTheGivenDirectories() {
    String expanded = HaxeCustomCommands.expandRoots("${moduleRoot}/bin and ${projectRoot}/out", "/work/mod", "/work");

    assertEquals("/work/mod/bin and /work/out", expanded);
  }

  @Test
  @DisplayName("unresolvable roots keep the literal visible")
  public void testUnresolvableRootsKeepTheLiteralVisible() {
    String expanded = HaxeCustomCommands.expandRoots("${moduleRoot} ${projectRoot}", null, "/work");

    assertEquals("${moduleRoot} /work", expanded);
  }

  @Test
  @DisplayName("tool variables expand to the sdk executables and back")
  public void testToolVariablesExpandToTheSdkExecutablesAndBack() {
    String haxe = HaxeToolPathResolver.resolveHaxeExecutable(getProject(), null);
    String haxelib = HaxeToolPathResolver.resolveHaxelibExecutable(getProject(), null);

    String expanded = HaxeCustomCommands.expandTools(getProject(), null, "${haxe} build.hxml && ${haxelib} run lime");

    assertEquals(HaxeCustomCommands.quoted(haxe) + " build.hxml && " + HaxeCustomCommands.quoted(haxelib) + " run lime", expanded);
    assertEquals("${haxe}", HaxeCustomCommands.toolVariableFor(getProject(), null, haxe));
    assertEquals("${haxelib}", HaxeCustomCommands.toolVariableFor(getProject(), null, haxelib));
    assertEquals("/opt/other/haxe", HaxeCustomCommands.toolVariableFor(getProject(), null, "/opt/other/haxe"));
  }

  @Test
  @DisplayName("an expanded path with whitespace stays one argument")
  public void testAnExpandedPathWithWhitespaceStaysOneArgument() {
    String command = HaxeCustomCommands.quoted("C:/Program Files/Haxe/haxe.exe") + " build.hxml";

    assertEquals(List.of("C:/Program Files/Haxe/haxe.exe", "build.hxml"), ParametersListUtil.parse(command));
  }

  @Test
  @DisplayName("a copied command names the selected target as the target variable")
  public void testACopiedCommandNamesTheSelectedTargetAsTheTargetVariable() {
    List<String> generated = List.of("haxelib", "run", "lime", "build", "project.xml", "windows", "-64");

    assertEquals(List.of("haxelib", "run", "lime", "build", "project.xml", "${target}", "-64"),
                 HaxeCustomCommands.withTargetVariable(generated, "windows"));
    assertEquals(generated, HaxeCustomCommands.withTargetVariable(generated, null), "hxml has no selectable target");
  }

  @Test
  @DisplayName("a copied command keeps its tool variable as typed")
  public void testACopiedCommandKeepsItsToolVariableAsTyped() {
    assertEquals("${haxe} build.hxml", ParametersListUtil.join(List.of("${haxe}", "build.hxml")));
  }

  @Test
  @DisplayName("blank work directory falls back to the row kind's default")
  public void testBlankWorkDirectoryFallsBackToTheRowKindsDefault() {
    assertEquals("/fallback", HaxeCustomCommands.resolveWorkDirectory("  ", "/fallback", "/work/mod", "/work"));
  }

  @Test
  @DisplayName("work directory expands root variables")
  public void testWorkDirectoryExpandsRootVariables() {
    assertEquals("/work/sub", HaxeCustomCommands.resolveWorkDirectory("${projectRoot}/sub", "/fallback", "/work/mod", "/work"));
  }
}
