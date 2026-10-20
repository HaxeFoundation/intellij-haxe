package com.intellij.plugins.haxe.profiler.bridge;

import org.jetbrains.annotations.NotNull;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/** Where the IDE-side capture receivers persist their session files. */
public final class HaxeCaptureFiles {

  private static final DateTimeFormatter CAPTURE_STAMP = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

  private HaxeCaptureFiles() {
  }

  /**
   * A per-capture home for the session: a {@code captures/<timestamp>}
   * directory beside the base, with the file KEEPING ITS NAME. The profiler
   * tool window keys open sessions by their file, so a rerun writing the
   * same path would front the previous run's stale tab instead of opening
   * the new capture; a unique directory per capture makes every run its own
   * session while the stable name keeps pinned tabs and same-named files
   * from other projects working.
   */
  @NotNull
  public static Path perCaptureSessionPath(@NotNull Path base) {
    Path captures = base.resolveSibling("captures");
    String stamp = LocalDateTime.now().format(CAPTURE_STAMP);
    Path directory = captures.resolve(stamp);
    for (int suffix = 2; Files.exists(directory); suffix++) {
      directory = captures.resolve(stamp + "-" + suffix);
    }
    return directory.resolve(base.getFileName());
  }
}
