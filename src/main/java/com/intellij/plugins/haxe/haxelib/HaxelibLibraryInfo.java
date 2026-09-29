package com.intellij.plugins.haxe.haxelib;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * One library's server-side metadata, parsed from {@code haxelib info}
 * output. The output starts with arbitrary preamble noise (an update-available
 * banner, blank lines) before the {@code Name:} field — parsing keys on the
 * field labels, never on line positions.
 */
public record HaxelibLibraryInfo(@NotNull String name,
                                 @NotNull String description,
                                 @NotNull String website,
                                 @NotNull String license,
                                 @NotNull String owner,
                                 @NotNull String latestVersion,
                                 @NotNull List<Release> releases) {

  /** One release line: {@code 2013-12-31 22:17:57 0.9.2 : Restore Emscripten support}. */
  public record Release(@NotNull String version, @NotNull String date, @NotNull String note) {
  }

  // a release line: date, version, then ':' and the release note
  static final Pattern RELEASE_LINE =
    Pattern.compile("(?<date>\\d{4}-\\d{2}-\\d{2}\\s\\d{2}:\\d{2}:\\d{2})\\s(?<version>.*?)\\s:\\s?(?<note>.*)");

  /** Parses one {@code haxelib info} invocation's output; null when no {@code Name:} field is present (unknown library, network failure). */
  @Nullable
  public static HaxelibLibraryInfo parse(@NotNull List<String> infoOutput) {
    String name = null;
    String description = "";
    String website = "";
    String license = "";
    String owner = "";
    String latestVersion = "";
    List<Release> releases = new ArrayList<>();

    for (String line : infoOutput) {
      String trimmed = line.trim();
      Matcher release = RELEASE_LINE.matcher(trimmed);
      if (release.matches()) {
        releases.add(new Release(release.group("version").trim(), release.group("date"), release.group("note")));
      }
      else if (trimmed.startsWith("Name:")) {
        name = valueOf(trimmed);
      }
      else if (trimmed.startsWith("Desc:")) {
        description = valueOf(trimmed);
      }
      else if (trimmed.startsWith("Website:")) {
        website = valueOf(trimmed);
      }
      else if (trimmed.startsWith("License:")) {
        license = valueOf(trimmed);
      }
      else if (trimmed.startsWith("Owner:")) {
        owner = valueOf(trimmed);
      }
      else if (trimmed.startsWith("Version:")) {
        latestVersion = valueOf(trimmed);
      }
    }
    if (name == null) return null;
    return new HaxelibLibraryInfo(name, description, website, license, owner, latestVersion, List.copyOf(releases));
  }

  @NotNull
  private static String valueOf(@NotNull String fieldLine) {
    return fieldLine.substring(fieldLine.indexOf(':') + 1).trim();
  }
}
