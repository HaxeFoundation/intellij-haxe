package com.intellij.plugins.haxe.v2.display;

import com.intellij.plugins.haxe.config.HaxeTarget;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.FieldSource;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.params.provider.Arguments.arguments;

@DisplayName("Compiler services: generated dump service args")
public class HaxeGeneratedDumpServiceArgsTest {

  private static final Path DUMP_ROOT = Path.of("dump-root");

  @Test
  @DisplayName("redirects the output and appends the dump defines")
  public void testRedirectsTheOutputAndAppendsTheDumpDefines() {
    HaxeGeneratedDumpService.DumpBuild build = HaxeGeneratedDumpService.dumpArgs(List.of("-main", "Main", "-js", "bin/out.js"), DUMP_ROOT);
    List<String> args = build.args();

    assertFalse(args.contains("bin/out.js"), "the user's output path must never be written by a dump build");
    int jsFlag = args.indexOf("-js");
    assertTrue(args.get(jsFlag + 1).startsWith(DUMP_ROOT.toString()), "output redirected into the dump root");
    assertTrue(args.contains("dump=pretty"), "dump mode requested");
    assertTrue(args.contains("no-compilation"), "native compilation suppressed");
  }

  @Test
  @DisplayName("directory targets redirect to a directory path")
  public void testDirectoryTargetsRedirectToADirectoryPath() {
    List<String> args = HaxeGeneratedDumpService.dumpArgs(List.of("-main", "Main", "-cpp", "out/cpp"), DUMP_ROOT).args();

    int cppFlag = args.indexOf("-cpp");
    String redirected = args.get(cppFlag + 1);
    assertTrue(redirected.startsWith(DUMP_ROOT.toString()), "output redirected into the dump root");
    assertFalse(redirected.endsWith(".exe"), "a directory target gets a directory, not a file name");
  }

  @Test
  @DisplayName("post-build cmd steps are dropped")
  public void testPostBuildCmdStepsAreDropped() {
    List<String> args = HaxeGeneratedDumpService.dumpArgs(
      List.of("-main", "Main", "-js", "out.js", "-cmd", "echo done"), DUMP_ROOT).args();

    assertFalse(args.contains("-cmd"), "a dump build must run no post-build commands");
    assertFalse(args.contains("echo done"), "the command argument goes with its flag");
  }

  @Test
  @DisplayName("no-output is stripped so the generation pass runs")
  public void testNoOutputIsStrippedSoTheGenerationPassRuns() {
    List<String> args = HaxeGeneratedDumpService.dumpArgs(
      List.of("-main", "Main", "-js", "_", "--no-output"), DUMP_ROOT).args();

    assertFalse(args.contains("--no-output"), "lime display hxml disables generation; dumps need it");
  }

  @Test
  @DisplayName("an interp build keeps eval and lists its main class instead of running it")
  public void testAnInterpBuildKeepsEvalAndListsItsMainClassInsteadOfRunningIt() {
    HaxeGeneratedDumpService.DumpBuild build = HaxeGeneratedDumpService.dumpArgs(List.of("-m", "Main", "--dce", "full", "--interp"), DUMP_ROOT);

    assertNotNull(build);
    assertEquals(HaxeTarget.INTERP, build.target());
    assertTrue(build.args().contains("--interp"), "the user's target and its defines stay");
    assertFalse(build.args().contains("-m"), "an entry point would run main right after dumping");
    int mainClass = build.args().indexOf("Main");
    assertEquals(List.of("Main", "--macro", "keep('Main')"), build.args().subList(mainClass, mainClass + 3));
  }

  @Test
  @DisplayName("a generating target keeps its entry point")
  public void testAGeneratingTargetKeepsItsEntryPoint() {
    List<String> args = HaxeGeneratedDumpService.dumpArgs(List.of("-main", "pack.Main", "-js", "out.js"), DUMP_ROOT).args();

    int mainFlag = args.indexOf("-main");
    assertEquals("pack.Main", args.get(mainFlag + 1));
    assertFalse(args.contains("--macro"));
  }

  /** (compiler args, why no dump build can come from them). */
  static final List<Arguments> REFUSED_SHAPES = List.of(
    // run shapes are refused
    arguments(List.of("-x", "Main"), "-x executes the program"),
    arguments(List.of("--run", "Main"), "--run executes the program"),
    // target-less arg sets are refused
    arguments(List.of("-main", "Main", "--no-output"), "no generation pass means no dumps"));

  @ParameterizedTest(name = "{1}")
  @FieldSource("REFUSED_SHAPES")
  @DisplayName("shapes without a generation pass are refused")
  public void testShapesWithoutAGenerationPassAreRefused(List<String> args, String reason) {
    assertNull(HaxeGeneratedDumpService.dumpArgs(args, DUMP_ROOT), reason);
  }
}
