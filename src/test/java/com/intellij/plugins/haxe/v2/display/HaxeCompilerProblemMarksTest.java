package com.intellij.plugins.haxe.v2.display;

import com.intellij.plugins.haxe.v2.display.HaxeCompilerProblemMarks.Update;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("Compiler services: problem marks")
public class HaxeCompilerProblemMarksTest {
  private static final String MAIN = "/project/src/Main.hx";
  private static final String REACHED = "/project/src/Reached.hx";
  private static final String UNREACHED = "/project/src/Unreached.hx";

  private final HaxeCompilerProblemMarks marks = new HaxeCompilerProblemMarks();

  @Test
  @DisplayName("opening a broken file keeps its mark although the sweep skips it")
  public void testOpeningABrokenFileKeepsItsMarkAlthoughTheSweepSkipsIt() {
    marks.apply(MAIN, false, Set.of(REACHED));

    Update opened = marks.apply(REACHED, true, Set.of(MAIN));

    assertEquals(Set.of(REACHED, MAIN), opened.report());
    assertEquals(Set.of(), opened.clear());
  }

  @Test
  @DisplayName("a sweep that does not reach a file leaves its own pass mark")
  public void testASweepThatDoesNotReachAFileLeavesItsOwnPassMark() {
    marks.apply(UNREACHED, true, Set.of());

    Update sweep = marks.apply(MAIN, true, Set.of(REACHED));

    assertEquals(Set.of(UNREACHED, MAIN, REACHED), sweep.report());
    assertEquals(Set.of(), sweep.clear());
  }

  @Test
  @DisplayName("only a file's own clean pass clears its own pass mark")
  public void testOnlyAFilesOwnCleanPassClearsItsOwnPassMark() {
    marks.apply(UNREACHED, true, Set.of());

    Update fixed = marks.apply(UNREACHED, false, Set.of());

    assertEquals(Set.of(), fixed.report());
    assertEquals(Set.of(UNREACHED), fixed.clear());
  }

  @Test
  @DisplayName("a sweep clears the sweep marks it no longer lists")
  public void testASweepClearsTheSweepMarksItNoLongerLists() {
    marks.apply(MAIN, false, Set.of(REACHED));

    Update repaired = marks.apply(MAIN, false, Set.of());

    assertEquals(Set.of(), repaired.report());
    assertEquals(Set.of(REACHED), repaired.clear());
  }

  @Test
  @DisplayName("a failed sweep leaves the sweep marks as they are")
  public void testAFailedSweepLeavesTheSweepMarksAsTheyAre() {
    marks.apply(MAIN, false, Set.of(REACHED));

    Update noSweep = marks.apply(MAIN, true, null);

    assertEquals(Set.of(MAIN, REACHED), noSweep.report());
    assertEquals(Set.of(), noSweep.clear());
  }
}
