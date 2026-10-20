package com.intellij.plugins.haxe.v2.display;

import com.intellij.plugins.haxe.config.HaxeTarget;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/** Haxe 4 writes {@code <dump-path>/<target>/}, Haxe 5 {@code <dump-path>/AfterDce/<target>/}; both add a {@code macro/} sibling. */
@DisplayName("Compiler services: generated dump service dirs")
public class HaxeGeneratedDumpServiceDirsTest {

  @TempDir
  Path dumpParent;

  @Test
  @DisplayName("haxe 4 lays the target out at the top")
  public void testHaxe4LaysTheTargetOutAtTheTop() throws IOException {
    Files.createDirectories(dumpParent.resolve("js"));
    Files.createDirectories(dumpParent.resolve("macro"));

    assertEquals(dumpParent.resolve("js"), HaxeGeneratedDumpService.targetDumpDir(dumpParent, HaxeTarget.JAVA_SCRIPT));
  }

  @Test
  @DisplayName("haxe 5 nests the target under a stage directory")
  public void testHaxe5NestsTheTargetUnderAStageDirectory() throws IOException {
    Files.createDirectories(dumpParent.resolve("AfterDce/eval"));
    Files.createDirectories(dumpParent.resolve("AfterDce/macro"));

    assertEquals(dumpParent.resolve("AfterDce/eval"), HaxeGeneratedDumpService.targetDumpDir(dumpParent, HaxeTarget.INTERP));
  }

  @Test
  @DisplayName("the macro sibling is never taken for the target")
  public void testTheMacroSiblingIsNeverTakenForTheTarget() throws IOException {
    Files.createDirectories(dumpParent.resolve("AfterDce/macro"));

    assertNull(HaxeGeneratedDumpService.targetDumpDir(dumpParent, HaxeTarget.FLASH));
  }
}
