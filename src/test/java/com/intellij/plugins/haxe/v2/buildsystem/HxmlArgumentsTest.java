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
  @DisplayName("unreadable hxml reference is kept as-is")
  public void testUnreadableHxmlReferenceIsKeptAsIs() {
    List<String> expanded = HxmlArguments.expandReferences(List.of("missing.hxml", "-main", "Main"));
    assertTrue(expanded.contains("missing.hxml"), "expansion failure leaves the argument untouched");
  }
}
