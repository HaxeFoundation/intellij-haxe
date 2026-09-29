package com.intellij.plugins.haxe.v2.display;

import java.util.HashSet;
import java.util.Set;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Which files carry a compiler problem mark, decided from two kinds of
 * evidence that must not overrule each other. A file's OWN diagnostics say
 * whether it is broken, and only they can clear that verdict: a file marked
 * by its own pass keeps the mark through sweeps that do not reach it. The
 * whole-project sweep says which OTHER files are broken; a file missing from
 * a sweep is either clean or not reached by the build, and in both cases
 * loses its sweep mark. Paths are compared as given; the caller normalizes
 * them.
 */
final class HaxeCompilerProblemMarks {

  /**
   * The outcome of one update: {@code report} holds every file that is
   * marked afterwards (reporting a file that is already marked is harmless),
   * {@code clear} the files that lost their mark.
   */
  record Update(@NotNull Set<String> report, @NotNull Set<String> clear) {
  }

  private final Set<String> markedByOwnPass = new HashSet<>();
  private final Set<String> markedBySweep = new HashSet<>();

  /**
   * Applies one diagnostics pass: the edited file's own verdict, and the
   * broken files of the sweep when it answered (null leaves the sweep marks
   * as they are).
   */
  @NotNull
  synchronized Update apply(@NotNull String editedPath, boolean editedBroken, @Nullable Set<String> sweepBroken) {
    Set<String> before = marked();
    if (editedBroken) {
      markedByOwnPass.add(editedPath);
    } else {
      markedByOwnPass.remove(editedPath);
    }
    if (sweepBroken != null) {
      markedBySweep.clear();
      markedBySweep.addAll(sweepBroken);
      markedBySweep.remove(editedPath);
    }
    Set<String> after = marked();
    Set<String> clear = new HashSet<>(before);
    clear.removeAll(after);
    return new Update(after, clear);
  }

  @NotNull
  private Set<String> marked() {
    Set<String> all = new HashSet<>(markedByOwnPass);
    all.addAll(markedBySweep);
    return all;
  }
}
