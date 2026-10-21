package com.intellij.plugins.haxe.runner.debugger.dap.transport;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.ToIntFunction;

/**
 * An external DAP adapter spawned as a process: the process, the TCP port it
 * listens on, and the stdout reader used during startup. Keep reading from
 * THIS reader ({@link #drainOutput}): it may hold buffered bytes beyond the
 * announcement, and an adapter whose stdout nobody reads blocks on a full
 * pipe.
 */
public record AdapterProcess(Process process, int port, BufferedReader stdout) {

  /** How long an adapter may take to announce its port before the launch fails. */
  public static final long ANNOUNCE_TIMEOUT_MILLIS = 15_000;
  /** How long a teardown waits for a killed adapter to be gone. */
  public static final long KILL_WAIT_SECONDS = 2;
  private static final long POLL_INTERVAL_MILLIS = 20;
  private static final String OUTPUT_PREFIX = "[adapter] ";

  /**
   * Starts the command with stderr merged into stdout and waits for the line
   * {@code isAnnouncement} accepts; {@code portOf} reads the port from that
   * line and may throw for a malformed one. The process is destroyed when the
   * launch fails.
   */
  public static AdapterProcess launch(ProcessBuilder command, Predicate<String> isAnnouncement, ToIntFunction<String> portOf)
    throws IOException {
    Process process = command.redirectErrorStream(true).start();
    BufferedReader stdout = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8));
    try {
      String announcement = awaitAnnouncement(process, stdout, isAnnouncement);
      return new AdapterProcess(process, parsePort(announcement, portOf), stdout);
    } catch (IOException e) {
      process.destroyForcibly();
      throw e;
    }
  }

  private static int parsePort(String announcement, ToIntFunction<String> portOf) throws IOException {
    try {
      return portOf.applyAsInt(announcement);
    } catch (RuntimeException e) {
      throw new IOException("Malformed adapter port announcement: " + announcement, e);
    }
  }

  // Polls stdout for the announcement line with a deadline (a blocking
  // readLine could hang the launch on a wedged adapter forever). Everything
  // read before the announcement is kept for the failure message.
  private static String awaitAnnouncement(Process process, BufferedReader stdout, Predicate<String> isAnnouncement)
    throws IOException {
    long deadline = System.currentTimeMillis() + ANNOUNCE_TIMEOUT_MILLIS;
    StringBuilder seen = new StringBuilder();
    StringBuilder line = new StringBuilder();
    while (System.currentTimeMillis() < deadline) {
      while (stdout.ready()) {
        int c = stdout.read();
        if (c < 0) {
          break;
        }
        if (c == '\n') {
          String text = line.toString().trim();
          line.setLength(0);
          if (isAnnouncement.test(text)) {
            return text;
          }
          seen.append(text).append('\n');
        } else if (c != '\r') {
          line.append((char)c);
        }
      }
      if (!process.isAlive() && !stdout.ready()) {
        throw new IOException("The debug adapter exited (code " + process.exitValue()
                              + ") before announcing its port." + outputSuffix(seen));
      }
      try {
        Thread.sleep(POLL_INTERVAL_MILLIS);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        throw new IOException("Interrupted while starting the debug adapter", e);
      }
    }
    throw new IOException("The debug adapter did not announce its port within "
                          + (ANNOUNCE_TIMEOUT_MILLIS / 1000) + "s." + outputSuffix(seen));
  }

  private static String outputSuffix(StringBuilder seen) {
    return seen.isEmpty() ? "" : " Adapter output:\n" + seen;
  }

  /** Relays the adapter's remaining output to {@code sink} line by line on a daemon thread, each line prefixed {@code [adapter] }. */
  public static void drainOutput(BufferedReader stdout, Consumer<String> sink) {
    Thread gobbler = new Thread(() -> relay(stdout, sink), "Debug adapter output");
    gobbler.setDaemon(true);
    gobbler.start();
  }

  private static void relay(BufferedReader stdout, Consumer<String> sink) {
    try (BufferedReader reader = stdout) {
      String line;
      while ((line = reader.readLine()) != null) {
        sink.accept(OUTPUT_PREFIX + line);
      }
    } catch (IOException ignored) {
      // adapter ended
    }
  }
}
