package com.intellij.plugins.haxe.matrix;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/** Console + progress-file logging (append; a tailing reader is harmless). */
final class Log {
  private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm:ss");
  private final Path file;

  Log(Path file) throws IOException {
    this.file = file;
    Files.createDirectories(file.getParent());
  }

  // synchronized: in parallel-lane mode three threads log concurrently and
  // lines must not interleave mid-write. The lane tag comes from the thread
  // name ("<lane>-lane", set by MatrixMain) so every message a lane thread
  // logs - including live test-progress lines - says which lane it is.
  synchronized void line(String message) {
    String thread = Thread.currentThread().getName();
    String tag = thread.endsWith("-lane")
      ? thread.substring(0, thread.length() - "-lane".length()) + " | " : "";
    String stamped = "[" + LocalTime.now().format(TIME) + "] " + tag + message;
    System.out.println(stamped);
    try {
      Files.writeString(file, stamped + System.lineSeparator(),
                        StandardOpenOption.CREATE, StandardOpenOption.APPEND);
    } catch (IOException ignored) {
      // the console line already happened; a locked progress file is not fatal
    }
  }
}
