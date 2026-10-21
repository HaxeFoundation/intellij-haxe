package com.intellij.plugins.haxe.v2.buildtools;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * "The most specific classpath owns the file", the rule the resolve contexts
 * and the test contexts share: a build owns the paths under its source
 * directories, and among several owners the one with the longest containing
 * directory is the closest. Which builds are candidates and how ties break
 * stay with each consumer.
 */
public final class HaxeBuildOwnership {

  private HaxeBuildOwnership() {
  }

  /** Length of the longest directory containing the path ({@code pathAsPrefix} ends in a slash), or -1 when none does. */
  public static int specificity(@NotNull List<String> sourceDirectories, @NotNull String pathAsPrefix) {
    int longest = -1;
    for (String directory : sourceDirectories) {
      if (pathAsPrefix.startsWith(directory + "/")) longest = Math.max(longest, directory.length());
    }
    return longest;
  }

  /** The candidates whose directories contain the path, tied for the longest one, in candidate order; empty when none contains it. */
  @NotNull
  public static <T> List<T> closest(@NotNull String path,
                                    @NotNull List<T> candidates,
                                    @NotNull Function<T, List<String>> directoriesOf) {
    String pathAsPrefix = path + "/";
    int best = -1;
    List<T> closest = new ArrayList<>();
    for (T candidate : candidates) {
      int specificity = specificity(directoriesOf.apply(candidate), pathAsPrefix);
      if (specificity < 0 || specificity < best) continue;
      if (specificity > best) {
        best = specificity;
        closest.clear();
      }
      closest.add(candidate);
    }
    return closest;
  }
}
