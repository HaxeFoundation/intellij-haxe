package com.intellij.plugins.haxe.v2.display;

import com.intellij.plugins.haxe.v2.display.HaxeCompilerProblemMarks.Update;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.FieldSource;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.params.provider.Arguments.arguments;

@DisplayName("Compiler services: problem marks")
public class HaxeCompilerProblemMarksTest {
  private static final String MAIN = "/project/src/Main.hx";
  private static final String REACHED = "/project/src/Reached.hx";
  private static final String UNREACHED = "/project/src/Unreached.hx";

  private final HaxeCompilerProblemMarks marks = new HaxeCompilerProblemMarks();

  /** (scenario, first update, second update, files the second reports, files it clears). */
  static final List<Arguments> UPDATES = List.of(
    arguments("opening a broken file keeps its mark although the sweep skips it",
              new Apply(MAIN, false, Set.of(REACHED)), new Apply(REACHED, true, Set.of(MAIN)),
              Set.of(REACHED, MAIN), Set.of()),
    arguments("a sweep that does not reach a file leaves its own pass mark",
              new Apply(UNREACHED, true, Set.of()), new Apply(MAIN, true, Set.of(REACHED)),
              Set.of(UNREACHED, MAIN, REACHED), Set.of()),
    arguments("only a file's own clean pass clears its own pass mark",
              new Apply(UNREACHED, true, Set.of()), new Apply(UNREACHED, false, Set.of()),
              Set.of(), Set.of(UNREACHED)),
    arguments("a sweep clears the sweep marks it no longer lists",
              new Apply(MAIN, false, Set.of(REACHED)), new Apply(MAIN, false, Set.of()),
              Set.of(), Set.of(REACHED)),
    arguments("a failed sweep leaves the sweep marks as they are",
              new Apply(MAIN, false, Set.of(REACHED)), new Apply(MAIN, true, null),
              Set.of(MAIN, REACHED), Set.of()));

  @ParameterizedTest(name = "{0}")
  @FieldSource("UPDATES")
  @DisplayName("the second update reports and clears by both mark kinds")
  public void testTheSecondUpdateReportsAndClearsByBothMarkKinds(String scenario, Apply first, Apply second,
                                                                  Set<String> report, Set<String> clear) {
    apply(first);

    Update update = apply(second);

    assertEquals(report, update.report());
    assertEquals(clear, update.clear());
  }

  private Update apply(Apply call) {
    return marks.apply(call.editedPath(), call.editedBroken(), call.sweepBroken());
  }

  /** One update: the edited file, whether it is broken, and the sweep's broken files (null when the sweep failed). */
  record Apply(String editedPath, boolean editedBroken, @Nullable Set<String> sweepBroken) {}
}
