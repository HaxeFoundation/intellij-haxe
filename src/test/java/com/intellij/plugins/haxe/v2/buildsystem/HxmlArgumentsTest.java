package com.intellij.plugins.haxe.v2.buildsystem;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Build system: hxml arguments")
public class HxmlArgumentsTest {

  @Test
  @DisplayName("parses lines into flag argument pairs")
  public void testParsesLinesIntoFlagArgumentPairs() {
    List<String> args = HxmlArguments.parseLines(List.of(
      "# build config",
      "-cp src dir",
      "-main Main",
      "--interp",
      "build.hxml"));

    assertEquals(List.of("-cp", "src dir", "-main", "Main", "--interp", "build.hxml"), args);
  }

  @Test
  @DisplayName("hxml file references expand against the preceding cwd")
  public void testHxmlFileReferencesExpandAgainstThePrecedingCwd(@TempDir Path projectDir) throws IOException {
    Files.writeString(projectDir.resolve("build.hxml"), """
      -cp src
      -main Main
      # output
      -js build/out.js
      """);

    List<String> args = List.of("--cwd", projectDir.toString(), "build.hxml");
    List<String> expanded = HxmlArguments.expandReferences(args);

    assertFalse(expanded.contains("build.hxml"), "the reference is replaced by the file's flags");

    List<String> values = List.of("-cp", "src", "-main", "Main", "-js", "build/out.js");
    assertTrue(expanded.containsAll(values), "flags read out of the referenced file");

    assertEquals(0, expanded.indexOf("--cwd"), "the cwd pair stays in place");

  }

  @Test
  @DisplayName("nested hxml references expand too")
  public void testNestedHxmlReferencesExpandToo(@TempDir Path projectDir) throws IOException {
    Files.writeString(projectDir.resolve("build.hxml"), """
      common.hxml
      -main Main
      """);
    Files.writeString(projectDir.resolve("common.hxml"), """
      -cp src
      -D shared
      """);

    List<String> expanded = HxmlArguments.expandReferences(List.of("--cwd", projectDir.toString(), "build.hxml"));

    assertEquals(List.of("--cwd", projectDir.toString(), "-cp", "src", "-D", "shared", "-main", "Main"), expanded);
  }

  @Test
  @DisplayName("an include cycle expands each file once")
  public void testAnIncludeCycleExpandsEachFileOnce(@TempDir Path projectDir) throws IOException {
    Files.writeString(projectDir.resolve("a.hxml"), """
      -D a
      b.hxml
      """);
    Files.writeString(projectDir.resolve("b.hxml"), """
      -D b
      a.hxml
      """);

    List<String> expanded = HxmlArguments.expandReferences(List.of("--cwd", projectDir.toString(), "a.hxml"));

    assertEquals(List.of("--cwd", projectDir.toString(), "-D", "a", "-D", "b", "a.hxml"), expanded);
  }

  @Test
  @DisplayName("the work directory composes every cwd in order")
  public void testTheWorkDirectoryComposesEveryCwdInOrder() {
    Path root = Path.of("/work/project");

    Path composed = HxmlArguments.workDirectory(List.of("--cwd", root.toString(), "-cp", "src", "--cwd", "sub"));

    assertEquals(root.resolve("sub"), composed);
    assertEquals(null, HxmlArguments.workDirectory(List.of("-cp", "src")));
  }

  @Test
  @DisplayName("an unusable cwd leaves the work directory unchanged")
  public void testAnUnusableCwdLeavesTheWorkDirectoryUnchanged() {
    Path composed = HxmlArguments.workDirectory(List.of("--cwd", "build", "--cwd", "//server/share"));

    assertEquals(Path.of("build"), composed);
  }

  @Test
  @DisplayName("unreadable hxml reference is kept as-is")
  public void testUnreadableHxmlReferenceIsKeptAsIs() {
    List<String> expanded = HxmlArguments.expandReferences(List.of("missing.hxml", "-main", "Main"));
    assertTrue(expanded.contains("missing.hxml"), "expansion failure leaves the argument untouched");
  }
}
