package com.intellij.plugins.haxe.profiler.bridge.flash;

import com.intellij.openapi.diagnostic.Logger;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * Points the flash runtime's telemetry at the session's receiver.
 * {@code ~/.telemetry.cfg} is user-GLOBAL — every flash/AIR runtime reads
 * it at start (Adobe Scout uses the same file) — so the session writes its
 * own config and puts the user's back when the run ends. A config left by
 * a crashed IDE is recognized by shape and replaced, never backed up, so a
 * stale one cannot leak into the backup chain.
 */
public final class FlashTelemetryConfig {

  private static final Logger LOG = Logger.getInstance(FlashTelemetryConfig.class);
  private static final String BACKUP_SUFFIX = ".ijhaxe-backup";

  private final Path config;
  private final Path backup;
  private final String written;

  public FlashTelemetryConfig(int port) {
    Path home = Path.of(System.getProperty("user.home"));
    config = home.resolve(".telemetry.cfg");
    backup = home.resolve(".telemetry.cfg" + BACKUP_SUFFIX);
    // SamplerEnabled turns on the stack ticks (debugger runtime only),
    // CPUCapture the .player.cpu readings, DisplayObjectCapture the
    // per-object render regions
    written = """
      TelemetryAddress=127.0.0.1:%d
      SamplerEnabled=true
      CPUCapture=true
      DisplayObjectCapture=true
      """.formatted(port);
  }

  /** Installs the session config, preserving a user's own file as the backup. */
  public synchronized void install() throws IOException {
    if (Files.exists(backup)) {
      // a crashed session never restored - put the user's file back first
      Files.move(backup, config, StandardCopyOption.REPLACE_EXISTING);
    }
    if (Files.exists(config) && !isOurs(config)) {
      Files.move(config, backup, StandardCopyOption.REPLACE_EXISTING);
    }
    Files.writeString(config, written);
  }

  /** Puts the user's config back (or removes ours); safe to call more than once. */
  public synchronized void restore() {
    try {
      if (Files.exists(backup)) {
        Files.move(backup, config, StandardCopyOption.REPLACE_EXISTING);
      }
      else if (Files.exists(config) && isOurs(config)) {
        Files.delete(config);
      }
    }
    catch (IOException e) {
      LOG.warn("could not restore " + config, e);
    }
  }

  /** A session config written by this class (any port): overwritten or deleted, never backed up. */
  private static boolean isOurs(@NotNull Path file) throws IOException {
    String content = Files.readString(file);
    // the loopback telemetry address followed by the fixed key set above
    return content.matches("(?s)TelemetryAddress=127\\.0\\.0\\.1:\\d+\\s+SamplerEnabled=true\\s+CPUCapture=true\\s+DisplayObjectCapture=true\\s*");
  }
}
