package com.intellij.plugins.haxe.hashlink;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/** hxml/argument scanning for the HashLink-bytecode gate. */
public class HlBuildSnifferTest {
  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  @Test
  public void findsHlOutputInArguments() {
    HlBuildSniffer.HlBuild build = HlBuildSniffer.fromArguments("-main Main -hl out/game.hl -debug");
    assertTrue(build.hlBytecode());
    assertEquals("out/game.hl", build.output());
  }

  @Test
  public void doubleDashFormIsAccepted() {
    assertTrue(HlBuildSniffer.fromArguments("--hl out.hl").hlBytecode());
  }

  @Test
  public void hlCNativeOutputIsNotBytecode() {
    HlBuildSniffer.HlBuild build = HlBuildSniffer.fromArguments("-hl out/main.c");
    assertFalse("HL/C native output cannot be debugged as bytecode", build.hlBytecode());
    assertEquals("out/main.c", build.output());
  }

  @Test
  public void nonHlArgumentsYieldNone() {
    assertFalse(HlBuildSniffer.fromArguments("-main Main -js out.js").hlBytecode());
    assertFalse(HlBuildSniffer.fromArguments(null).hlBytecode());
  }

  @Test
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
  public void followsHxmlIncludes() throws IOException {
    write("common.hxml", "-cp src\n-hl bin/app.hl\n");
    Path main = write("build.hxml", "common.hxml\n-debug\n");
    assertTrue(HlBuildSniffer.fromHxml(main).hlBytecode());
  }

  @Test
  public void cacheRefreshesWhenTheFileChanges() throws IOException, InterruptedException {
    Path hxml = write("mutable.hxml", "-js out.js\n");
    assertFalse(HlBuildSniffer.fromHxml(hxml).hlBytecode());
    Thread.sleep(20); // ensure a different modification stamp
    Files.writeString(hxml, "-hl out.hl\n");
    Files.setLastModifiedTime(hxml, java.nio.file.attribute.FileTime.fromMillis(System.currentTimeMillis() + 1000));
    assertTrue("cache must notice the changed file", HlBuildSniffer.fromHxml(hxml).hlBytecode());
  }

  private Path write(String name, String content) throws IOException {
    Path file = temp.getRoot().toPath().resolve(name);
    Files.writeString(file, content);
    return file;
  }
}
