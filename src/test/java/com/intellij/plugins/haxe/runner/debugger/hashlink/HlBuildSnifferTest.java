package com.intellij.plugins.haxe.runner.debugger.hashlink;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.FieldSource;

import static org.junit.jupiter.params.provider.Arguments.arguments;

/** hxml/argument scanning for the HashLink-bytecode gate. */
@DisplayName("Debugger: hl build sniffer")
public class HlBuildSnifferTest {
  @TempDir
  Path temp;

  /** (compiler arguments, expected bytecode gate, expected -hl output). */
  static final List<Arguments> ARGUMENT_SNIFFS = List.of(
    arguments("-main Main -hl out/game.hl -debug", true, "out/game.hl"),
    arguments("--hl out.hl", true, "out.hl"),
    // HL/C native output cannot be debugged as bytecode
    arguments("-hl out/main.c", false, "out/main.c"),
    arguments("-main Main -js out.js", false, null),
    arguments(null, false, null));

  @ParameterizedTest(name = "{0}")
  @FieldSource("ARGUMENT_SNIFFS")
  @DisplayName("sniffs hl output from arguments")
  public void sniffsHlOutputFromArguments(String compilerArguments, boolean hlBytecode, String output) {
    HlBuildSniffer.HlBuild build = HlBuildSniffer.fromArguments(compilerArguments);
    assertEquals(hlBytecode, build.hlBytecode(), "hl bytecode gate");
    assertEquals(output, build.output(), "sniffed -hl output");
  }

  @Test
  @DisplayName("reads hxml file with comments and per line args")
  public void readsHxmlFileWithCommentsAndPerLineArgs() throws IOException {
    Path hxml = write("build.hxml", """
      # build for hashlink
      -cp src
      -main Main
      -hl bin/game.hl
      -debug
      """);

    HlBuildSniffer.HlBuild build = HlBuildSniffer.fromHxml(hxml);
    assertTrue(build.hlBytecode());
    assertEquals("bin/game.hl", build.output());
  }

  @Test
  @DisplayName("follows hxml includes")
  public void followsHxmlIncludes() throws IOException {
    write("common.hxml", "-cp src\n-hl bin/app.hl\n");
    Path main = write("build.hxml", "common.hxml\n-debug\n");
    assertTrue(HlBuildSniffer.fromHxml(main).hlBytecode());
  }

  @Test
  @DisplayName("cache refreshes when the file changes")
  public void cacheRefreshesWhenTheFileChanges() throws IOException, InterruptedException {
    Path hxml = write("mutable.hxml", "-js out.js\n");
    assertFalse(HlBuildSniffer.fromHxml(hxml).hlBytecode());
    Thread.sleep(20); // ensure a different modification stamp
    Files.writeString(hxml, "-hl out.hl\n");
    Files.setLastModifiedTime(hxml, java.nio.file.attribute.FileTime.fromMillis(System.currentTimeMillis() + 1000));
    assertTrue(HlBuildSniffer.fromHxml(hxml).hlBytecode(), "cache must notice the changed file");
  }

  private Path write(String name, String content) throws IOException {
    Path file = temp.resolve(name);
    Files.writeString(file, content);
    return file;
  }
}
